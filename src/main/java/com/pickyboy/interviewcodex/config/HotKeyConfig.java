package com.pickyboy.interviewcodex.config;

import com.jd.platform.hotkey.client.ClientStarter;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * hotkey热点发现配置
 * @author pickyboy
 */
@Configuration
@ConfigurationProperties(prefix = "hotkey")
@Data
public class HotKeyConfig {
    /**
     * etcd 地址
     */
    private String etcdServer = "http://127.0.0.1:2379";

    /**
     * 应用名称
     */
    private String appName = "interviewCodex";

    /**
     * 本地缓存最大数量
     */
    private int caffeineSize = 10000;

    /**
     * 批量推送key的间隔时间
     */
    private long batchPushInterval = 1000L;

    @Bean
    public void initHotKey(){
        ClientStarter.Builder builder = new ClientStarter.Builder();
        ClientStarter clientStarter = builder.setAppName(appName)
                .setEtcdServer(etcdServer)
                .setCaffeineSize(caffeineSize)
                .setPushPeriod(batchPushInterval)
                .build();
                clientStarter.startPipeline();
    }
}

