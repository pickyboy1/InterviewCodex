package com.pickyboy.guardian.config;

import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.GuardianDefinition;
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
     * 是否启用Guardian
     */
    private boolean enabled = true;

    /**
     * 默认计数器类型
     */
    private String defaultCounterType = GuardianConstants.COUNTER_TYPE_REDIS;

    /**
     * 默认时间窗口大小
     */
    private int defaultWindowSize = GuardianConstants.DEFAULT_WINDOW_SIZE;

    /**
     * 默认时间单位
     */
    private TimeUnit defaultTimeUnit = TimeUnit.MINUTES;

    /**
     * 默认过期时间（秒）
     */
    private long defaultExpiration = GuardianConstants.DEFAULT_EXPIRATION;

    /**
     * 全局默认的访问控制规则列表。
     * 当 @GuardianCheck 注解的 rules 属性为空时，将使用此处的配置。
     */
    private List<GuardianDefinition> defaultRules = new ArrayList<>();

    /**
     * 默认错误消息
     */
    private String defaultErrorMessage = GuardianConstants.DEFAULT_ERROR_MESSAGE;
}
