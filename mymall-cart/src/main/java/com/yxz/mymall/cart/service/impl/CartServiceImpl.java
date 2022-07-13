package com.yxz.mymall.cart.service.impl;

import com.yxz.mymall.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;
}
