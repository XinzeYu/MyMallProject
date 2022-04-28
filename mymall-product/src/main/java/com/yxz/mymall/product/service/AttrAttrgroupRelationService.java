package com.yxz.mymall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxz.common.utils.PageUtils;
import com.yxz.mymall.product.entity.AttrAttrgroupRelationEntity;

import java.util.Map;

/**
 * 属性&属性分组关联
 *
 * @author yuxinze
 * @email xinzeyu@seu.edu.cn
 * @date 2022-04-28 13:13:07
 */
public interface AttrAttrgroupRelationService extends IService<AttrAttrgroupRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

