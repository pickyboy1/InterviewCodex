package com.pickyboy.guardian.core.counter;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.model.counter.CounterParam;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.IntegerCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Collections;

/**
 * Redis计数器实现
 *
 * @author pickyboy
 */
@Slf4j
@Component
@ConditionalOnClass(RedissonClient.class)
public class RedisCounter implements Counter {

    @Resource
    private RedissonClient redissonClient;

    @Override
    public long incrementAndGet(CounterParam config) {
        if (StrUtil.isBlank(config.getKey())) {
            return 0;
        }

        String redisKey = buildRedisKey(config);
        String luaScript = buildLuaScript();

        RScript script = redissonClient.getScript(IntegerCodec.INSTANCE);
        Object countObj = script.eval(
                RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(redisKey),
                config.getExpiration()
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
            Integer count = (Integer) redissonClient.getBucket(redisKey, IntegerCodec.INSTANCE).get();
            return count != null ? count : 0;
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
     * 构建Redis键
     */
    private String buildRedisKey(CounterParam config) {
        long timeFactor = getTimeFactor(config);
        long timeSlot = Instant.now().getEpochSecond() / config.getWindowSize() / timeFactor;
        return config.getKey() + ":" + timeSlot;
    }

    /**
     * 获取时间因子
     */
    private long getTimeFactor(CounterParam config) {
        switch (config.getTimeUnit()) {
            case SECONDS:
                return 1;
            case MINUTES:
                return 60;
            case HOURS:
                return 60 * 60;
            case DAYS:
                return 60 * 60 * 24;
            default:
                return 1;
        }
    }

    /**
     * 构建Lua脚本
     */
    private String buildLuaScript() {
        return "if redis.call('exists', KEYS[1]) == 1 then " +
               "    return redis.call('incr', KEYS[1]); " +
               "else " +
               "    redis.call('set', KEYS[1], 1); " +
               "    redis.call('expire', KEYS[1], ARGV[1]); " +
               "    return 1; " +
               "end";
    }
}
