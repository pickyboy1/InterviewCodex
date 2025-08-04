package com.pickyboy.guardian.model.constant;

/**
 * Guardian常量类
 * 记录Guardian提供的默认实现类型
 *
 * @author pickyboy
 */
public final class GuardianConstants {

    private GuardianConstants() {
        // 工具类，禁止实例化
    }

    // ==================== 计数器类型常量 ====================

    /**
     * Redis计数器类型
     * 基于Redis的分布式计数器，使用固定窗口算法
     */
    public static final String COUNTER_TYPE_REDIS = "redis";

    /**
     * 本地缓存计数器类型
     * 基于Caffeine的本地缓存计数器，使用固定窗口算法
     */
    public static final String COUNTER_TYPE_LOCAL = "local";

    // ==================== 动作类型常量 ====================

    /**
     * 邮件告警动作类型
     * 模拟邮件发送，实际记录日志
     */
    public static final String ACTION_TYPE_EMAIL = "email";

    /**
     * 日志记录动作类型
     * 记录告警日志
     */
    public static final String ACTION_TYPE_LOG = "log";

    /**
     * 封禁用户动作类型
     * 踢下线并封禁用户（需要sa-token依赖）
     */
    public static final String ACTION_TYPE_BAN = "ban";

    // ==================== KeyProvider类型常量 ====================

    /**
     * 用户ID KeyProvider
     * 自动获取用户ID
     */
    public static final String KEY_PROVIDER_USER = "user";

    /**
     * IP地址 KeyProvider
     * 自动获取客户端IP地址
     */
    public static final String KEY_PROVIDER_IP = "ip";

    /**
     * SpEL表达式 KeyProvider
     * 支持Spring表达式语言
     */
    public static final String KEY_PROVIDER_SPEL = "spel";

    /**
     * 自动选择 KeyProvider
     * 根据key表达式自动选择
     */
    public static final String KEY_PROVIDER_AUTO = "auto";

    // ==================== 告警级别常量 ====================

    /**
     * 信息级别
     */
    public static final String ALERT_LEVEL_INFO = "info";

    /**
     * 警告级别
     */
    public static final String ALERT_LEVEL_WARNING = "warning";

    /**
     * 错误级别
     */
    public static final String ALERT_LEVEL_ERROR = "error";

    /**
     * 严重级别
     */
    public static final String ALERT_LEVEL_CRITICAL = "critical";

    // ==================== 默认配置常量 ====================

    /**
     * 默认时间窗口大小
     */
    public static final int DEFAULT_WINDOW_SIZE = 1;

    /**
     * 默认过期时间（秒）
     */
    public static final long DEFAULT_EXPIRATION = 180;

    /**
     * 默认警告阈值
     */
    public static final int DEFAULT_WARN_THRESHOLD = 10;

    /**
     * 默认封禁阈值
     */
    public static final int DEFAULT_BAN_THRESHOLD = 20;

    /**
     * 默认错误消息
     */
    public static final String DEFAULT_ERROR_MESSAGE = "访问频率过高，请稍后再试";
}