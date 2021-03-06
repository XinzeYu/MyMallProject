package com.yxz.mymall.ware;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableRabbit
@EnableTransactionManagement
@MapperScan("com.yxz.mymall.ware.dao")
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class MymallWareApplication {

    public static void main(String[] args) {
        SpringApplication.run(MymallWareApplication.class, args);
    }

}
