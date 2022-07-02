package com.yxz.mymall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxz.common.utils.PageUtils;
import com.yxz.mymall.ware.entity.WareSkuEntity;

import java.util.Map;

/**
 * 商品库存
 *
 * @author yuxinze
 * @email xinzeyu@seu.edu.cn
 * @date 2022-05-06 14:46:30
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);
}

