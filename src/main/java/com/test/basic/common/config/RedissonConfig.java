package com.test.basic.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 단일 redis
        config.useSingleServer()
                .setAddress("redis://54.180.118.74:36379")  // 실제 redis 주소
                .setPassword("jikim_pwdForRedis_250419")
                .setConnectionMinimumIdleSize(10)  // 최소 연결 수 설정
                .setConnectionPoolSize(64);        // 커넥션 풀 크기 설정
        
        // redis 클러스터
//        config.useClusterServers()
//                .addNodeAddress("redis://127.0.0.1:7000")
//                .addNodeAddress("redis://127.0.0.1:7001")
//                .addNodeAddress("redis://127.0.0.1:7002");
        return Redisson.create(config);
    }
}
