package com.yxz.mymall.coupon.dao;

import com.yxz.mymall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author yuxinze
 * @email xinzeyu@seu.edu.cn
 * @date 2022-05-06 14:20:02
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
