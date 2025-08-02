package com.pickyboy.interviewcodex.cache;

import com.pickyboy.interviewcodex.model.entity.Question;
import com.pickyboy.interviewcodex.model.entity.QuestionBank;
import com.pickyboy.interviewcodex.model.vo.QuestionBankVO;
import com.pickyboy.interviewcodex.model.vo.QuestionVO;
import com.pickyboy.interviewcodex.service.QuestionBankService;
import com.pickyboy.interviewcodex.service.QuestionService;
import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存系统完整测试类
 *
 * 测试覆盖：
 * 1. 自动缓存注解 (@AutoCache)
 * 2. 缓存清除注解 (@CacheEvict)
 * 3. 批量操作缓存
 * 4. 多状态缓存
 * 5. 并发安全性
 * 6. 缓存一致性
 * 7. 性能测试
 *
 * @author pickyboy
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheSystemTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CacheUtils cacheUtils;

    // 测试数据
    private static Long testQuestionId;
    private static Long testQuestionBankId;

    @BeforeAll
    static void setUpTestData(@Autowired QuestionService questionService,
                             @Autowired QuestionBankService questionBankService) {
        // 创建测试题目
        Question question = new Question();
        question.setTitle("测试题目");
        question.setContent("这是一个测试题目内容");
        question.setAnswer("测试答案");
        question.setUserId(1L);
        questionService.save(question);
        testQuestionId = question.getId();

        // 创建测试题库
        QuestionBank questionBank = new QuestionBank();
        questionBank.setTitle("测试题库");
        questionBank.setDescription("测试题库描述");
        questionBank.setUserId(1L);
        questionBankService.save(questionBank);
        testQuestionBankId = questionBank.getId();
    }

    @AfterAll
    static void cleanUpTestData(@Autowired QuestionService questionService,
                               @Autowired QuestionBankService questionBankService,
                               @Autowired RedissonClient redissonClient) {
        // 清理测试数据
        if (testQuestionId != null) {
            questionService.removeById(testQuestionId);
        }
        if (testQuestionBankId != null) {
            questionBankService.removeById(testQuestionBankId);
        }

        // 清理Redis缓存
        redissonClient.getKeys().flushdb();
    }

    @Test
    @Order(1)
    @DisplayName("测试自动缓存功能 - @AutoCache")
    void testAutoCache() {
        // 第一次调用，数据会被缓存
        long startTime = System.currentTimeMillis();
        QuestionVO questionVO1 = questionService.getCacheQuestionVO(testQuestionId);
        long firstCallTime = System.currentTimeMillis() - startTime;

        assertNotNull(questionVO1);
        assertEquals("测试题目", questionVO1.getTitle());

        // 第二次调用，应该从缓存获取，速度更快
        startTime = System.currentTimeMillis();
        QuestionVO questionVO2 = questionService.getCacheQuestionVO(testQuestionId);
        long secondCallTime = System.currentTimeMillis() - startTime;

        assertNotNull(questionVO2);
        assertEquals("测试题目", questionVO2.getTitle());

        // 验证缓存键是否存在
        String cacheKey = "question_detail::" + testQuestionId;
        assertTrue(redissonClient.getBucket(cacheKey).isExists());

        // 验证第二次调用更快（从缓存获取）
        assertTrue(secondCallTime < firstCallTime);

        System.out.println("第一次调用耗时: " + firstCallTime + "ms");
        System.out.println("第二次调用耗时: " + secondCallTime + "ms");
        System.out.println("缓存加速比: " + (firstCallTime / (double) secondCallTime));
    }

    @Test
    @Order(2)
    @DisplayName("测试多状态缓存功能")
    void testMultiStatusCache() {
        // 测试题库缓存，支持两种状态：带题目列表和不带题目列表

        // 不带题目列表的缓存
        QuestionBankVO bankVO1 = questionBankService.getCachedQuestionBankVO(testQuestionBankId, false);
        assertNotNull(bankVO1);
        assertNull(bankVO1.getQuestionPage());

        // 带题目列表的缓存
        QuestionBankVO bankVO2 = questionBankService.getCachedQuestionBankVO(testQuestionBankId, true);
        assertNotNull(bankVO2);
        assertNotNull(bankVO2.getQuestionPage());

        // 验证两种缓存键都存在
        String cacheKey1 = "bank_detail::" + testQuestionBankId + "_false";
        String cacheKey2 = "bank_detail::" + testQuestionBankId + "_true";

        assertTrue(redissonClient.getBucket(cacheKey1).isExists());
        assertTrue(redissonClient.getBucket(cacheKey2).isExists());

        System.out.println("多状态缓存测试通过");
    }

    @Test
    @Order(3)
    @DisplayName("测试缓存清除功能 - @CacheEvict")
    void testCacheEvict() {
        // 先确保数据被缓存
        questionService.getCacheQuestionVO(testQuestionId);
        String cacheKey = "question_detail::" + testQuestionId;
        assertTrue(redissonClient.getBucket(cacheKey).isExists());

        // 更新数据，应该自动清除缓存
        Question updateQuestion = new Question();
        updateQuestion.setId(testQuestionId);
        updateQuestion.setTitle("更新后的题目");
        questionService.updateQuestionWithCache(updateQuestion);

        // 验证缓存已被清除
        assertFalse(redissonClient.getBucket(cacheKey).isExists());

        // 再次获取数据，应该是更新后的数据
        QuestionVO updatedVO = questionService.getCacheQuestionVO(testQuestionId);
        assertEquals("更新后的题目", updatedVO.getTitle());

        System.out.println("缓存清除测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("测试批量缓存清除功能")
    void testBatchCacheEvict() {
        // 创建测试题目列表
        List<Long> questionIds = Arrays.asList(testQuestionId);

        // 先缓存这些数据
        for (Long id : questionIds) {
            questionService.getCacheQuestionVO(id);
            String cacheKey = "question_detail::" + id;
            assertTrue(redissonClient.getBucket(cacheKey).isExists());
        }

        // 批量删除，应该清除所有相关缓存
        questionService.batchDeleteQuestionsWithCache(questionIds);

        // 验证所有缓存都被清除
        for (Long id : questionIds) {
            String cacheKey = "question_detail::" + id;
            assertFalse(redissonClient.getBucket(cacheKey).isExists());
        }

        System.out.println("批量缓存清除测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("测试并发缓存安全性")
    void testConcurrentCacheSafety() throws Exception {
        // 并发获取相同数据，确保缓存安全
        int threadCount = 10;
        CompletableFuture<QuestionVO>[] futures = new CompletableFuture[threadCount];

        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(() ->
                questionService.getCacheQuestionVO(testQuestionId));
        }

        // 等待所有线程完成
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

        // 验证所有结果一致
        QuestionVO firstResult = futures[0].get();
        for (int i = 1; i < threadCount; i++) {
            QuestionVO result = futures[i].get();
            assertEquals(firstResult.getId(), result.getId());
            assertEquals(firstResult.getTitle(), result.getTitle());
        }

        // 验证缓存只被创建一次
        String cacheKey = "question_detail::" + testQuestionId;
        assertTrue(redissonClient.getBucket(cacheKey).isExists());

        System.out.println("并发缓存安全性测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("测试缓存工具类功能")
    void testCacheUtils() {
        // 测试单个缓存清除
        String testKey = "test_cache_key";
        redissonClient.getBucket("test_scene::" + testKey).set("test_value");

        cacheUtils.evictCache("test_scene", testKey);
        assertFalse(redissonClient.getBucket("test_scene::" + testKey).isExists());

        // 测试多状态缓存清除
        redissonClient.getBucket("test_scene::" + testKey + "_status1").set("value1");
        redissonClient.getBucket("test_scene::" + testKey + "_status2").set("value2");

        cacheUtils.evictCacheWithStatus("test_scene", testKey, Arrays.asList("status1", "status2"));

        assertFalse(redissonClient.getBucket("test_scene::" + testKey + "_status1").isExists());
        assertFalse(redissonClient.getBucket("test_scene::" + testKey + "_status2").isExists());

        // 测试批量缓存清除
        List<Object> keys = Arrays.asList("key1", "key2", "key3");
        for (Object key : keys) {
            redissonClient.getBucket("batch_scene::" + key).set("value");
        }

        cacheUtils.batchEvictCache("batch_scene", keys);

        for (Object key : keys) {
            assertFalse(redissonClient.getBucket("batch_scene::" + key).isExists());
        }

        System.out.println("缓存工具类功能测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("测试缓存性能对比")
    void testCachePerformance() {
        int iterations = 50;

        // 清除可能存在的缓存
        String cacheKey = "question_detail::" + testQuestionId;
        redissonClient.getBucket(cacheKey).delete();

        // 测试无缓存情况下的性能
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            // 每次都清除缓存，模拟无缓存情况
            redissonClient.getBucket(cacheKey).delete();
            questionService.getCacheQuestionVO(testQuestionId);
        }
        long noCacheTime = System.currentTimeMillis() - startTime;

        // 测试有缓存情况下的性能
        questionService.getCacheQuestionVO(testQuestionId); // 预热缓存

        startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            questionService.getCacheQuestionVO(testQuestionId);
        }
        long cacheTime = System.currentTimeMillis() - startTime;

        // 计算性能提升
        double speedupRatio = (double) noCacheTime / Math.max(cacheTime, 1);

        System.out.println("无缓存 " + iterations + " 次调用耗时: " + noCacheTime + "ms");
        System.out.println("有缓存 " + iterations + " 次调用耗时: " + cacheTime + "ms");
        System.out.println("缓存性能提升: " + String.format("%.2f", speedupRatio) + "倍");

        // 缓存应该带来性能提升
        assertTrue(speedupRatio > 1.0);
    }

    @Test
    @Order(8)
    @DisplayName("测试缓存一致性")
    void testCacheConsistency() {
        // 获取初始数据并缓存
        QuestionVO original = questionService.getCacheQuestionVO(testQuestionId);
        assertNotNull(original);

        // 更新数据库中的数据（绕过缓存）
        Question directUpdate = new Question();
        directUpdate.setId(testQuestionId);
        directUpdate.setTitle("直接更新的标题");
        questionService.updateById(directUpdate);

        // 从缓存获取数据，应该还是旧数据
        QuestionVO cached = questionService.getCacheQuestionVO(testQuestionId);
        assertEquals(original.getTitle(), cached.getTitle());

        // 通过带缓存清除的方法更新
        Question cacheUpdate = new Question();
        cacheUpdate.setId(testQuestionId);
        cacheUpdate.setTitle("缓存更新的标题");
        questionService.updateQuestionWithCache(cacheUpdate);

        // 再次获取，应该是新数据
        QuestionVO updated = questionService.getCacheQuestionVO(testQuestionId);
        assertEquals("缓存更新的标题", updated.getTitle());

        System.out.println("缓存一致性测试通过");
    }

    @Test
    @Order(9)
    @DisplayName("测试缓存边界情况")
    void testCacheEdgeCases() {
        // 测试不存在的ID
        QuestionVO nullResult = questionService.getCacheQuestionVO(999999L);
        assertNull(nullResult);

        // 测试空列表缓存清除（不应该抛异常）
        assertDoesNotThrow(() -> {
            cacheUtils.batchEvictCache("test_scene", null);
            cacheUtils.batchEvictCache("test_scene", Arrays.asList());
            cacheUtils.evictCacheWithStatus("test_scene", "key", null);
            cacheUtils.evictCacheWithStatus("test_scene", "key", Arrays.asList());
        });

        System.out.println("缓存边界情况测试通过");
    }
}