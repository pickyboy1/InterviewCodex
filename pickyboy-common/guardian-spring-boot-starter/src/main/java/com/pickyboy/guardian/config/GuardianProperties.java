package com.pickyboy.guardian.config;

import com.pickyboy.guardian.anotation.Rule;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.GuardianDefinition;
import com.pickyboy.guardian.model.rule.GuardianRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Guardian配置属性
 *
 * @author pickyboy
 */
@Data
@ConfigurationProperties(prefix = "guardian")
public class GuardianProperties {

    /**
     * 是否启用 Guardian 功能。
     */
    private boolean enabled = true;

    /**
     * 默认使用的计数器类型。
     * 可选值："redis", "local" (caffeine)
     */
    private String defaultCounterType = GuardianConstants.COUNTER_TYPE_REDIS;

    /**
     * 默认的时间窗口大小。
     */
    private int defaultWindowSize = GuardianConstants.DEFAULT_WINDOW_SIZE;

    /**
     * 默认的时间单位。
     */
    private TimeUnit defaultTimeUnit = TimeUnit.MINUTES;


    /**
     * 全局默认的访问控制规则列表。
     * 当 @GuardianCheck 注解的 rules 属性为空时，将使用此处的配置。
     * <p>
     * 关键改动：
     * 这里的 List 类型是 GuardianRule (我们定义的POJO)，而不是 @Rule (注解)。
     * 这样 Spring 才能正确地将 YAML 中的配置绑定到这个列表上。
     */
    private List<GuardianRule> defaultRules = new ArrayList<>();

    /**
     * 默认的错误消息。
     */
    private String defaultErrorMessage = GuardianConstants.DEFAULT_ERROR_MESSAGE;

    /**
     * 本地计数器（Caffeine）的全局最大缓存条目数。
     */
    private long localCounterMaxSize = 10000L;
}
