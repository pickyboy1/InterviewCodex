package com.pickyboy.interviewcodex.blackfilter;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;



@Slf4j
@Component
public class NacosListener{
    @NacosInjected
    /**
     Spring Boot 需要在启动的极早期阶段（称为 bootstrap 引导阶段）就去连接 Nacos，拉取配置。

     只有拉取到配置后，才能用这些配置去初始化后续的 Bean（比如 DataSource）。

     而此时，完整的 Spring IoC 容器可能还没有完全准备好，标准的 @Autowired 机制可能还无法工作或无法找到对应的 Bean。
     */
    private ConfigService configService;

    @Value("${nacos.config.data-id}")
    private String dataId;

    @Value("${nacos.config.group}")
    private String group;

    /**
     * 使用 @PostConstruct 注解，确保在应用启动后，
     * Bean 初始化完成时，立即执行一次配置加载。
     * 这解决了 @NacosConfigListener 不会在首次启动时加载配置的问题。
     */
    @PostConstruct
    public void initialLoadBlacklist() {
        try {
            log.info("NacosListener: Initializing blacklist on startup...");
            String initialConfig = configService.getConfig(dataId, group, 5000L);
            log.info("NacosListener: Fetched initial config: {}", initialConfig);
            BlackIpUtils.rebuildBlackIp(initialConfig);
        } catch (NacosException e) {
            log.error("NacosListener: Failed to fetch initial config on startup.", e);
            // 在这里可以根据业务需求决定是否抛出异常中断启动，
            // 或者使用一个空的黑名单继续运行。
        }
    }
    /**
     * 监听指定的 dataId 和 group 的配置变更。
     * 此方法会在应用启动时自动执行一次以获取初始配置，
     * 之后每当 Nacos 中的配置发生变化时，都会被自动调用。
     *
     * @param config ahe latest configuration content.
     */
    @NacosConfigListener(dataId = "${nacos.config.data-id}", groupId = "${nacos.config.group}")
    public void onConfigChange(String config) {
        log.info("监听到配置变化 (via @NacosConfigListener): {}", config);
        // 调用工具类，使用最新的配置内容重建 IP 黑名单
        BlackIpUtils.rebuildBlackIp(config);
    }
}
