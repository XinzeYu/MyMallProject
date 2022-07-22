package com.yxz.mymall.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 整合sentinel
 * 1、导入依赖
 * 2、下载控制台，java -jar直接运行（懒加载模式）
 * 3、应用配置地址信息
 * spring.cloud.sentinel.transport.dashboard=localhost:8080
 * spring.cloud.sentinel.transport.port=8719
 * 4、控制台调制参数
 *
 * 每一个微服务导入审计模块
 */
@EnableRedisHttpSession
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MymallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(MymallSeckillApplication.class, args);
    }

}
