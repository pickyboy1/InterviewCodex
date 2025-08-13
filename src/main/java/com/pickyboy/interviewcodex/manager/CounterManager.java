package com.pickyboy.interviewcodex.manager;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.IntegerCodec;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的通用计数器
 * 已经该用自定义注解
 *
 * @author pickyboy
 */
/*@Slf4j
@Service
public class CounterManager {
    @Resource
    private RedissonClient redissonClient;

    *//**
     *
     * @param key 键
     * @param timeInterval 时间间隔
     * @param timeUnit 时间间隔单位
     * @param expirationTimeInSeconds 过期时间
     * @return
     *//*
    public long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit,long expirationTimeInSeconds){
        if(StrUtil.isBlank( key)){
            return 0;
        }
        // 根据时间粒度生成Redis Key
        long timeFactor;
        switch (timeUnit){
            case SECONDS:
                timeFactor = 1;
                break;
            case MINUTES:
                timeFactor = 60;
                break;
            case HOURS:
                timeFactor = 60 * 60;
                break;
            case DAYS:
                timeFactor = 60 * 60 * 24;
                break;
            default:
                timeFactor = 1;
        }
        String redisKey = key + ":" + Instant.now().getEpochSecond() /timeInterval / timeFactor;

        // lua 脚本
        String luaScript =
                "if redis.call('exists',KEYS[1])==1 then " +
                        " return redis.call('incr',KEYS[1]); "+
                        "else "+
                        " redis.call('set',KEYS[1],1); "+
                        " redis.call('expire',KEYS[1],ARGV[1]); "+
                        " return 1; "+
                        " end";
        RScript script = redissonClient.getScript(IntegerCodec.INSTANCE);
        Object countObj = script.eval(RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(redisKey), expirationTimeInSeconds);
        return (long)countObj;
    }

    // 封装多种默认参数方法,供简化调用
    *//**
     * 按秒计数，并指定key的过期时间。
     * @param key 计数器的键
     * @param expirationTimeInSeconds key的过期时间（秒）
     * @return 当前计数值
     *//*
    public long incrAndGetCounterPerSecond(String key, long expirationTimeInSeconds) {
        return incrAndGetCounter(key, 1, TimeUnit.SECONDS, expirationTimeInSeconds);
    }

    private static final long ONE_DAY_IN_SECONDS = 24 * 60 * 60;

    *//**
     * 按分钟计数，使用默认的1天过期时间。
     * @param key 计数器的键
     * @return 当前计数值
     *//*
    public long incrAndGetCounterPerMinute(String key) {
        return incrAndGetCounter(key, 1, TimeUnit.MINUTES, ONE_DAY_IN_SECONDS);
    }


    *//**
     * 按秒计数，使用默认的1天过期时间。
     * @param key 计数器的键
     * @return 当前计数值
     *//*
    public long incrAndGetCounter(String key) {
        return incrAndGetCounter(key, 1, TimeUnit.SECONDS, ONE_DAY_IN_SECONDS);
    }
}*/
