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

    // ==================== 核心标识符 ====================

    /**
     * Guardian 在 Redis 中使用的 Key 的统一前缀，用于避免命名冲突。
     */
    public static final  String  REDIS_KEY_PREFIX = "guardian";


    // ==================== 内置组件名称常量 ====================

    /**
     * 内置 Redis 计数器的名称。
     */
    public static final String COUNTER_TYPE_REDIS = "redis";

    /**
     * 内置本地（Caffeine）计数器的名称。
     */
    public static final String COUNTER_TYPE_LOCAL = "local";

    /**
     * 内置“仅记录日志”策略的名称。
     */
    public static final String STRATEGY_TYPE_LOG_ONLY = "logOnlyStrategy";

    /**
     * 内置“拒绝请求”策略的名称。
     */
    public static final String STRATEGY_TYPE_REJECT = "rejectActionStrategy";

    /**
     * 内置“封禁用户”策略的名称 (依赖 UserAuthService 和 UserBanService)。
     */
    public static final String STRATEGY_TYPE_BAN = "banActionStrategy";

    public static final String STRATEGY_TYPE_EMAIL = "emailActionStrategy";

    /**
     * 内置 SpEL Key 生成器的名称。
     */
    public static final String KEY_PROVIDER_SPEL = "spel";

    /**
     * 内置 IP Key 生成器的名称。
     */
    public static final String KEY_PROVIDER_IP = "ip";

    public static final String KEY_PROVIDER_USER = "userId";


    // ==================== 告警级别常量 ====================

    /**
     * 信息级别
     */
    public static final String ALERT_LEVEL_INFO = "INFO";

    /**
     * 警告级别
     */
    public static final String ALERT_LEVEL_WARNING = "WARN";

    /**
     * 严重级别
     */
    public static final String ALERT_LEVEL_CRITICAL = "CRITICAL";


    // ==================== 默认配置常量 ====================

    /**
     * 默认时间窗口大小 (1分钟)。
     */
    public static final int DEFAULT_WINDOW_SIZE = 1;

    /**
     * 默认的错误消息。
     */
    public static final String DEFAULT_ERROR_MESSAGE = "访问频率过高，请稍后再试";
}