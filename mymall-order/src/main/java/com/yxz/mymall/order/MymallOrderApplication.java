package com.yxz.mymall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;



@EnableRabbit
@EnableRedisHttpSession
@EnableDiscoveryClient
@SpringBootApplication
public class MymallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MymallOrderApplication.class, args);
    }

}
