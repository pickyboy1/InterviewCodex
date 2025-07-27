package com.pickyboy.interviewcodex.blackfilter;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Component
public class NacosListener implements InitializingBean {

    @NacosInjected
    private ConfigService configService;

    @Value("${nacos.config.data-id}")
    private String dataId;

    @Value("${nacos.config.group}")
    private String group;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("NacosListener init start...");
        String config= configService.getConfigAndSignListener(dataId, group, 3000L, new Listener() {
           final ThreadFactory threadFactory = new ThreadFactory() {

               private final AtomicInteger poolNumber = new AtomicInteger(1);
               @Override
               public Thread newThread(Runnable r){
                   if(r == null){
                        return new Thread(() -> {log.info("未指定任务");});
                   }
                   Thread thread = new Thread(r);
                   thread.setName("refresh-ThreadPool" + poolNumber.getAndIncrement());
                   return thread;
               }

           };
           final ExecutorService executorService = Executors.newFixedThreadPool(1, threadFactory);

           @Override
            public Executor getExecutor() {
                return executorService;
            }
            // 监听配置变更逻辑
            @Override
            public void receiveConfigInfo(String s) {
                log.info("监听到配置变化:{}", s);
                BlackIpUtils.rebuildBlackIp(s);
            }
        });
        BlackIpUtils.rebuildBlackIp(config);
    }
}
