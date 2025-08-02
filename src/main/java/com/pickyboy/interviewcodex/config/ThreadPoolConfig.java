package com.pickyboy.interviewcodex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置
 *
 * @author pickyboy
 */
@Configuration
public class ThreadPoolConfig {
    /**
     * 创建用于批量处理的线程池 Bean
     *
     * @return ThreadPoolExecutor 实例
     */
    @Bean("batchExecutor") // 修正了原始命名中的拼写错误 "Excutor" -> "Executor"
    public ThreadPoolExecutor batchExecutor() {
        // 最佳实践 1: 使用 ThreadFactoryBuilder 为线程池中的线程命名
        // 这极大地提升了系统的【可观测性】。当出现问题时，通过线程堆栈可以快速定位到是哪个线程池出了问题。
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("batch-executor-thread-%d")
                .build();

        // 获取 CPU 核心数
        int coreSize = Runtime.getRuntime().availableProcessors();

        // 最佳实践 2: 根据任务类型合理配置参数
        // 批量操作是典型的 IO 密集型任务，核心线程数可以设置得比 CPU 核心数大一些，以提高吞吐量。
        // 这里的 coreSize * 2 是一个合理的初始值。
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreSize * 2, // 核心线程数
                coreSize * 4, // 最大线程数
                60L, // 线程空闲后的存活时间
                TimeUnit.SECONDS, // 时间单位
                new ArrayBlockingQueue<>(10000), // 有界工作队列
                threadFactory, // 自定义的线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用者线程执行任务
        );

        return executor;
    }
}
