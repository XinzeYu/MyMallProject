package com.yxz.mymall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yxz.common.utils.R;
import com.yxz.mymall.seckill.feign.CouponFeignService;
import com.yxz.mymall.seckill.feign.ProductFeignService;
import com.yxz.mymall.seckill.service.SeckillService;
import com.yxz.mymall.seckill.to.SeckillSkuRedisTo;
import com.yxz.mymall.seckill.vo.SeckillSessionsWithSkus;
import com.yxz.mymall.seckill.vo.SeckillSkuVo;
import com.yxz.mymall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SECKILL_CHARE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";    //+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //扫描最近三天参与秒杀的活动
        R lates3DaySession = couponFeignService.getLates3DaySession();
        if (lates3DaySession.getCode() == 0) {
            //上架商品
            List<SeckillSessionsWithSkus> sessionData = lates3DaySession.getData("data", new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //缓存到Redis
            //1、缓存活动信息
            saveSessionInfos(sessionData);

            //2、缓存活动的关联商品信息
            saveSessionSkuInfo(sessionData);
        }
    }

    private void saveSessionSkuInfo(List<SeckillSessionsWithSkus> sessionData) {
        sessionData.stream().forEach(session -> {
            //准备hash操作，绑定hash
            BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
            session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                //生成随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                String redisKey = seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId().toString();
                if (!operations.hasKey(redisKey)) {

                    //缓存我们商品信息
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    Long skuId = seckillSkuVo.getSkuId();
                    //1、先查询sku的基本信息，调用远程服务
                    R info = productFeignService.getSkuInfo(skuId);
                    if (info.getCode() == 0) {
                        SkuInfoVo skuInfo = info.getData("skuInfo",new TypeReference<SkuInfoVo>(){});
                        redisTo.setSkuInfo(skuInfo);
                    }

                    //2、sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo,redisTo);

                    //3、设置当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    //4、设置商品的随机码，开始的一刻随机码才暴漏（防止恶意攻击）
                    redisTo.setRandomCode(token);

                    //序列化json格式存入Redis中
                    String seckillValue = JSON.toJSONString(redisTo);
                    operations.put(seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId().toString(),seckillValue);

                    //如果当前这个场次的商品库存信息已经上架就不需要上架
                    //5、使用库存作为分布式Redisson信号量------->（限流）
                    // 使用库存作为分布式信号量
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    // 商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }
            });
        });
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessionData) {
        sessionData.stream().forEach(session -> {

            //获取当前活动的开始和结束时间的时间戳
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();

            //存入到Redis中的key
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;

            //判断Redis中是否有该信息，如果没有才进行添加
            Boolean hasKey = redisTemplate.hasKey(key);
            //缓存活动信息
            if (!hasKey) {
                //获取到活动中所有商品的skuId
                List<String> skuIds = session.getRelationSkus().stream()
                        .map(item-> item.getPromotionSessionId() + "-" + item.getSkuId().toString()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, skuIds);
            }
        });
    }

    
    
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1、确定当前属于哪个秒杀场次
        long currentTime = new Date().getTime();

        //从Redis中查询到所有key以seckill:sessions开头的所有数据
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        for (String key : keys) {
            //seckill:sessions:1594396764000_1594453242000
            String replace = key.replace(SESSION_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            //获取存入Redis商品的开始时间
            long startTime = Long.parseLong(s[0]);
            //获取存入Redis商品的结束时间
            long endTime = Long.parseLong(s[1]);

            //判断是否是当前秒杀场次
            if (currentTime >= startTime && currentTime <= endTime) {
                //2、获取这个秒杀场次需要的所有商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hasOps = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
                assert range != null;
                List<String> listValue = hasOps.multiGet(range);
                if (listValue != null && listValue.size() >= 0) {

                    List<SeckillSkuRedisTo> collect = listValue.stream().map(item -> {
                        String items = (String) item;
                        SeckillSkuRedisTo redisTo = JSON.parseObject(items, SeckillSkuRedisTo.class);
                        // redisTo.setRandomCode(null);当前秒杀开始需要随机码
                        return redisTo;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }
        return null;
    }

    /**
     *
     * 存在某些问题，如果有多条匹配上的数据，如何保证获取的To时间段是有效的
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //找到所有需要秒杀的商品的key信息---seckill:skus
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);

        //拿到所有的key
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            //4-45 正则表达式进行匹配
            String reg = "\\d-" + skuId;
            for (String key : keys) {
                //如果匹配上了
                if (Pattern.matches(reg,key)) {
                    //从Redis中取出数据来
                    String redisValue = hashOps.get(key);
                    //进行序列化
                    SeckillSkuRedisTo redisTo = JSON.parseObject(redisValue, SeckillSkuRedisTo.class);
                    Long currentTime = System.currentTimeMillis();
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    //如果当前时间大于等于秒杀活动开始时间并且要小于活动结束时间
                    if (currentTime >= startTime && currentTime <= endTime) {
                        return redisTo;
                    }
                    //秒杀未开始，随机码不赋值
                    redisTo.setRandomCode(null);
                    return redisTo;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) throws InterruptedException {
        return null;
    }
}
