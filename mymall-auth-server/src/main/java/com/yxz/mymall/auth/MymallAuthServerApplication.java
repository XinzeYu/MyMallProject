package com.yxz.mymall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class MymallAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MymallAuthServerApplication.class, args);
    }

}
