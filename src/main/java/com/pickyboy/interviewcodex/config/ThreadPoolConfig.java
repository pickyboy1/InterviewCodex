package com.pickyboy.interviewcodex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {
    @Bean("batchExcutor")
    public ThreadPoolExecutor batchExcutor() {
        int coreSize = Runtime.getRuntime().availableProcessors() * 2;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreSize, coreSize*2, 60,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000), new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
