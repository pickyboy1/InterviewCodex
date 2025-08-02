package com.pickyboy.interviewcodex.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;

import java.util.List;

/**
 * 缓存清理工具类
 *
 * @author pickyboy
 */
@Slf4j
public class CacheUtils {

    private final RedissonClient redissonClient;

    /**
     * 构造函数注入
     * @param redissonClient Redis客户端
     */
    public CacheUtils(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 批量清除缓存
     * @param scene 缓存场景
     * @param ids ID列表
     */
    public void batchEvictCache(String scene, List<Object> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        RBatch batch = redissonClient.createBatch();
        for (Object id : ids) {
            if (id != null) {
                String cacheKey = scene + "::" + id.toString();
                batch.getBucket(cacheKey).deleteAsync();
            }
        }
        batch.execute();

        log.info("Batch Cache Evicted: {} keys for scene: {}", ids.size(), scene);
    }

    /**
     * 批量清除缓存（支持多状态）
     * @param scene 缓存场景
     * @param ids ID列表
     * @param statuses 状态列表，如果为空则只清除基础key
     */
    public void batchEvictCacheWithStatus(String scene, List<Object> ids, List<String> statuses) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        RBatch batch = redissonClient.createBatch();
        int keyCount = 0;

        for (Object id : ids) {
            if (id != null) {
                if (statuses == null || statuses.isEmpty()) {
                    // 只清除基础key
                    String cacheKey = scene + "::" + id.toString();
                    batch.getBucket(cacheKey).deleteAsync();
                    keyCount++;
                } else {
                    // 清除所有状态的key
                    for (String status : statuses) {
                        String cacheKey = scene + "::" + id.toString() + "_" + status;
                        batch.getBucket(cacheKey).deleteAsync();
                        keyCount++;
                    }
                }
            }
        }

        batch.execute();
        log.info("Batch Cache Evicted: {} keys for scene: {}", keyCount, scene);
    }

    /**
     * 单个清除缓存
     * @param scene 缓存场景
     * @param id ID
     */
    public void evictCache(String scene, Object id) {
        if (id != null) {
            String cacheKey = scene + "::" + id.toString();
            boolean deleted = redissonClient.getBucket(cacheKey).delete();

            if (deleted) {
                log.info("Cache Evicted: {}", cacheKey);
            } else {
                log.warn("Cache Evict Failed or Key Not Found: {}", cacheKey);
            }
        }
    }

    /**
     * 单个清除缓存（支持多状态）
     * @param scene 缓存场景
     * @param id ID
     * @param statuses 状态列表，如果为空则只清除基础key
     */
    public void evictCacheWithStatus(String scene, Object id, List<String> statuses) {
        if (id == null) {
            return;
        }

        if (statuses == null || statuses.isEmpty()) {
            // 只清除基础key
            evictCache(scene, id);
        } else {
            // 清除所有状态的key
            RBatch batch = redissonClient.createBatch();
            for (String status : statuses) {
                String cacheKey = scene + "::" + id.toString() + "_" + status;
                batch.getBucket(cacheKey).deleteAsync();
            }
            batch.execute();
            log.info("Cache Evicted: {} keys for id {} in scene: {}", statuses.size(), id, scene);
        }
    }
}