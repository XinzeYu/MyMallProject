package com.yxz.mymall.seckill.feign;

import com.yxz.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("mymall-coupon")
public interface CouponFeignService {

    /**
     * 查询最近三天需要参加秒杀商品的信息
     * @return
     */
    @GetMapping(value = "/coupon/seckillsession/lates3DaySession")
    R getLates3DaySession();

}

