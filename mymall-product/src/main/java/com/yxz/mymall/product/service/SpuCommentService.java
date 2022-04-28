package com.yxz.mymall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxz.common.utils.PageUtils;
import com.yxz.mymall.product.entity.SpuCommentEntity;

import java.util.Map;

/**
 * 商品评价
 *
 * @author yuxinze
 * @email xinzeyu@seu.edu.cn
 * @date 2022-04-28 13:13:07
 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

