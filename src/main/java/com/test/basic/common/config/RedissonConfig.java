package com.test.basic.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private String redisPort;
    @Value("${spring.data.redis.password}")
    private String redisPassword;

    // 분산락을 위한 RedissonClient
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 단일 redis
        var serverConfig = config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort) // 실제 redis 주소
                .setConnectionMinimumIdleSize(10) // 최소 연결 수 설정
                .setConnectionPoolSize(64); // 커넥션 풀 크기 설정

        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }

        // redis 클러스터
        // config.useClusterServers()
        // .addNodeAddress("redis://127.0.0.1:7000")
        // .addNodeAddress("redis://127.0.0.1:7001")
        // .addNodeAddress("redis://127.0.0.1:7002");
        return Redisson.create(config);
    }
}
