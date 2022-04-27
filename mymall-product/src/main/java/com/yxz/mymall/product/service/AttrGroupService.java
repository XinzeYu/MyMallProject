package com.yxz.mymall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxz.common.utils.PageUtils;
import com.yxz.mymall.product.entity.AttrGroupEntity;

import java.util.Map;

/**
 * 属性分组
 *
 * @author yuxinze
 * @email xinzeyu@seu.edu.cn
 * @date 2022-04-27 14:02:40
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);
}
