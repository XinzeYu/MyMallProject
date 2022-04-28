package com.yxz.mymall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.yxz.mymall.product.dao")
@SpringBootApplication
public class MymallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MymallProductApplication.class, args);
    }

}
