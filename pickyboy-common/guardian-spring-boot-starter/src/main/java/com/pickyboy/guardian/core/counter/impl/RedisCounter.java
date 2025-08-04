package com.pickyboy.guardian.core.counter.impl;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.core.counter.Counter;
import com.pickyboy.guardian.model.counter.CounterParam;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * Redis计数器实现
 *
 * @author pickyboy
 */
@Slf4j
@Component
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(
        name = "guardian.default-counter-type",
        havingValue = GuardianConstants.COUNTER_TYPE_REDIS,
        matchIfMissing = true // 关键点：当用户未配置时，默认创建此 Bean
)
public class RedisCounter implements Counter {

    @Resource
    private RedissonClient redissonClient;

    // 将 Lua 脚本定义为常量，避免重复构建
    private static final String INCR_AND_EXPIRE_SCRIPT =
            "local current = redis.call('incr', KEYS[1]) " +
                    "if current == 1 then " +
                    "    redis.call('expire', KEYS[1], ARGV[1]) " +
                    "end " +
                    "return current";

    @Override
    public long incrementAndGet(CounterParam config) {
        if (StrUtil.isBlank(config.getKey())) {
            return 0;
        }

        String redisKey = buildRedisKey(config);
        RScript script = redissonClient.getScript(LongCodec.INSTANCE);

        // 使用 evalSha, Redisson 会自动缓存脚本的 SHA 摘要，更高效
        // ARGV[1] 应该是时间窗口的秒数
        long expirationInSeconds = config.getTimeUnit().toSeconds(config.getWindowSize());

        Object countObj = script.eval(
                RScript.Mode.READ_WRITE,
                INCR_AND_EXPIRE_SCRIPT,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(redisKey),
                expirationInSeconds
        );

        return (long) countObj;
    }

    @Override
    public long getCurrentCount(CounterParam config) {
        if (StrUtil.isBlank(config.getKey())) {
            return 0;
        }

        String redisKey = buildRedisKey(config);
        try {
            // 使用 getBucket 并指定 Codec
            Object count = redissonClient.getBucket(redisKey, LongCodec.INSTANCE).get();
            return count != null ? (long) count : 0;
        } catch (Exception e) {
            log.error("获取Redis计数失败: {}", redisKey, e);
            return 0;
        }
    }

    @Override
    public void reset(CounterParam config) {
        if (StrUtil.isBlank(config.getKey())) {
            return;
        }

        String redisKey = buildRedisKey(config);
        try {
            redissonClient.getBucket(redisKey).delete();
        } catch (Exception e) {
            log.error("重置Redis计数失败: {}", redisKey, e);
        }
    }

    @Override
    public String getType() {
        return GuardianConstants.COUNTER_TYPE_REDIS;
    }

    /**
     * 构建 Redis Key，确保每个场景下的每个 key 都是唯一的。
     *   防御性编程: 加上特定前缀,使用我们自己的命名空间,避免与业务的key冲突
     *
     * @param config 计数器参数
     * @return 唯一的 Redis Key，格式如：guardian:counter:view_question:user_123
     */
    private String buildRedisKey(CounterParam config) {
        // 移除了手动计算时间槽的逻辑
        return String.join(":",
                GuardianConstants.REDIS_KEY_PREFIX,
                "counter",
                config.getScene(),
                config.getKey()
        );
    }

}
