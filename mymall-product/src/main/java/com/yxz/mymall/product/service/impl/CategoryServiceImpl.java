package com.yxz.mymall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yxz.mymall.product.service.CategoryBrandRelationService;
import com.yxz.mymall.product.vo.Catelog2Vo;
import org.apache.ibatis.ognl.CollectionElementsAccessor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxz.common.utils.PageUtils;
import com.yxz.common.utils.Query;

import com.yxz.mymall.product.dao.CategoryDao;
import com.yxz.mymall.product.entity.CategoryEntity;
import com.yxz.mymall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }


    @Override
    public List<CategoryEntity> listWithTree() {
        //查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //组装成父子的树形结构
        //首先找到所有的一级分类，其parentid为0
        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu)->{
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted((menu1, menu2)->{
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());



        return level1Menus;
    }

    /**
     * 递归查找当前菜单的所有子菜单
     *
     * @param root
     * @param entities
     * @return
     */
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> entities) {

        List<CategoryEntity> children = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //递归遍历子菜单
            categoryEntity.setChildren(getChildren(categoryEntity, entities));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {

        baseMapper.deleteBatchIds(asList);

    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();
        findParentPath(catelogId, path);
        Collections.reverse(path);
        return (Long[]) path.toArray(new Long[path.size()]);
    }

    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        //级联更新所有的数据
        this.updateById(category);
        if(!StringUtils.isEmpty(category.getName())) {
            //同步更新关联表数据
            categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

            //TODO 更新其他关联表
        }
    }

    /*

        自定义：
        1） 指定使用的key：key属性指定
        2） 指定缓存数据的存活时间:配置文件中指定
        3） 将数据保存为json格式
     */
    @Cacheable(value = {"category"}, key = "#root.method.name")  //代表当前结果需要缓存
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> entityList = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return entityList;
    }

    //TODO 产生堆外内存溢出

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {

        /**
         * 1 空结果缓存，解决缓存穿透问题
         * 2 设置过期时间，随机值，解决缓存雪崩问题
         * 3 加锁，解决缓存击穿问题
         */

        //缓存中的对象时JSON字符串
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            //缓存中没有，查数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();

            return catalogJsonFromDb;
        }

        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>(){});
        return result;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        //1. 占分布式锁，锁的粒度选择，具体缓存的是某个数据，11号商品：product-11-lock
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();
        System.out.println("获取分布式锁成功。。");

        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }
        return dataFromDb;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {

        //1. 占分布式锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功。。");
            //加锁成功，执行业务
            //设置过期时间，避免解锁失败，需要和加锁是原子性的
            //stringRedisTemplate.expire("lock", 30, TimeUnit.SECONDS);
            Map<String, List<Catelog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                String lua_scripts = "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del',KEYS[1]) else return 0 end";
                //解锁保证原子性
                stringRedisTemplate.execute(new DefaultRedisScript<Integer>(lua_scripts, Integer.class), Arrays.asList("lock")
                        , uuid);
            }

            //获取值对比+对比成功删除也应当做成原子操作，lua脚本解锁
            /*String lockValue = stringRedisTemplate.opsForValue().get("lock");
            if (uuid.equals(lockValue)) {
                stringRedisTemplate.delete("lock"); //删除锁
            }*/
            return dataFromDb;
        } else {
            //等待重试
            //可以设定休眠时间
            System.out.println("获取分布式锁失败。。");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisLock(); //自旋
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        //得到锁之后，再去缓存中确定一次
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>(){});
            return result;
        }
        System.out.println("查询了数据库。。。。");

        //否则继续进行下一步
        List<CategoryEntity> selectList = baseMapper.selectList(null);


        //查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个一级分类，查到这个一级分类的二级分类
            List<CategoryEntity> entityList = getParent_cid(selectList, v.getCatId());
            //封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (entityList != null) {
                catelog2Vos = entityList.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //找当前2级分类的3级分类
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return category3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(category3Vos);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        String s = JSON.toJSONString(parent_cid);
        stringRedisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    /**
     *
     * 本地锁
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {

        //如何加锁
        //1、synchronized(this):this是单例的
        //2、方法上加synchronized
        //TODO 本地锁：synchronized，lock，在分布式情况下想要锁住所有必须使用分布式锁

        synchronized (this) {
            //得到锁之后，再去缓存中确定一次
            String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
            if (!StringUtils.isEmpty(catalogJSON)) {
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>(){});
                return result;
            }

            //否则继续进行下一步
            List<CategoryEntity> selectList = baseMapper.selectList(null);


            //查出所有一级分类
            List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

            //封装数据
            Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //每一个一级分类，查到这个一级分类的二级分类
                List<CategoryEntity> entityList = getParent_cid(selectList, v.getCatId());
                //封装上面的结果
                List<Catelog2Vo> catelog2Vos = null;
                if (entityList != null) {
                    catelog2Vos = entityList.stream().map(l2 -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //找当前2级分类的3级分类
                        List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                        if (level3Catelog != null) {
                            List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                                Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return category3Vo;
                            }).collect(Collectors.toList());
                            catelog2Vo.setCatalog3List(category3Vos);
                        }

                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }
                return catelog2Vos;
            }));

            String s = JSON.toJSONString(parent_cid);
            stringRedisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
            return parent_cid;
        }
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity >selectList, Long parent_cid) {
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
        List<CategoryEntity> collect = selectList.stream().filter(item -> {
            return item.getParentCid() == parent_cid;
        }).collect(Collectors.toList());
        return collect;
    }

    private void findParentPath(Long catelogId, List<Long> path) {
        path.add(catelogId);
        CategoryEntity categoryEntity = this.getById(catelogId);
        if(categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), path);
        }
        return ;
    }

}