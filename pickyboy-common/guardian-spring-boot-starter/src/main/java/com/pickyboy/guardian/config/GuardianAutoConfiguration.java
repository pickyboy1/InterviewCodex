package com.pickyboy.guardian.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Guardian自动配置类
 *
 * @author pickyboy
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(GuardianProperties.class)
@ConditionalOnProperty(prefix = "guardian", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("com.pickyboy.guardian")
public class GuardianAutoConfiguration {

    public GuardianAutoConfiguration() {
        log.info("Guardian Spring Boot Starter 自动配置启动");
    }
}
