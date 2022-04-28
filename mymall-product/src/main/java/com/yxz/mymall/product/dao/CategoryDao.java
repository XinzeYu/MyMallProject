package com.yxz.mymall.product.dao;

import com.yxz.mymall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author yuxinze
 * @email xinzeyu@seu.edu.cn
 * @date 2022-04-28 13:13:07
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
