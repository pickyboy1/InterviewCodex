package com.pickyboy.interviewcodex.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 1. rLock.lock()
 * 代码路径: waitTime 未设置, expireTime 未设置。
 *
 * 锁的特性: 阻塞式、可重入、带看门狗（自动续期）的锁。
 *
 * 讲解:
 *
 * 阻塞式: 如果当前线程来加锁时，锁已经被其他线程持有，那么当前线程会一直等待（阻塞），直到获取到锁为止。
 *
 * 看门狗机制: 这是 Redisson 的核心亮点。一旦加锁成功，Redisson 会启动一个后台线程（看门狗），默认每隔10秒检查一下持有锁的业务线程是否还在运行。如果还在，它会自动将锁的过期时间重置为30秒。这意味着，只要你的业务逻辑没有执行完毕，这个锁就不会因超时而自动释放，极大地避免了因业务执行时间过长导致锁失效的问题。这是最常用、最安全的模式。
 *
 * 2. rLock.lock(expireTime, TimeUnit.MILLISECONDS)
 * 代码路径: waitTime 未设置, expireTime 已设置。
 *
 * 锁的特性: 阻塞式、可重入、有固定过期时间的锁。
 *
 * 讲解:
 *
 * 阻塞式: 和第一种一样，会一直等待直到获取到锁。
 *
 * 固定过期时间: 与第一种不同的是，一旦加锁成功，这个锁会在指定的 expireTime 之后自动释放。并且，这种方式会禁用看门狗机制。这意味着，如果你的业务执行时间超过了 expireTime，锁会被强制释放，其他线程就能获取到锁，可能会导致并发问题。这种方式通常用于那些你能明确预估执行时间，并且希望锁必须在特定时间后释放的场景。
 *
 * 3. rLock.tryLock(waitTime, TimeUnit.MILLISECONDS)
 * 代码路径: waitTime 已设置, expireTime 未设置。
 *
 * 锁的特性: 尝试获取、可重入、带看门狗（自动续期）的锁。
 *
 * 讲解:
 *
 * 尝试获取: 这是非阻塞的。当前线程会尝试在指定的 waitTime 内获取锁。
 *
 * 如果在 waitTime 内成功获取到锁，方法返回 true。
 *
 * 如果在 waitTime 时间结束后仍未获取到锁，方法会立即返回 false，线程不会再继续等待。
 *
 * 看门狗机制: 一旦成功获取到锁，看门狗机制同样会启动，自动为锁续期。
 *
 * 这种方式非常适合那些不希望线程被长时间阻塞，“拿不到锁就干别的”或者“拿不到锁就报错”的业务场景。
 *
 * 4. rLock.tryLock(waitTime, expireTime, TimeUnit.MILLISECONDS)
 * 代码路径: waitTime 已设置, expireTime 已设置。
 *
 * 锁的特性: 尝试获取、可重入、有固定过期时间的锁。
 *
 * 讲解:
 *
 * 尝试获取: 和第三种一样，在 waitTime 内尝试获取锁。
 *
 * 固定过期时间: 和第二种一样，一旦加锁成功，锁会在 expireTime 后自动释放，并且禁用看门狗。
 *
 * 这是四种方式中定制化程度最高的，它结合了“在指定时间内尝试”和“成功后锁定指定时间”两个特性。适用于需要精细控制等待时间和锁定时间的复杂场景。
 */

/**
 * 分布式锁切面
 *
 * @author pickyboy
 */
@Aspect
@Component
@Order(Integer.MIN_VALUE + 1)
public class DistributeLockAspect {

    private RedissonClient redissonClient;

    public DistributeLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DistributeLockAspect.class);

    @Around("@annotation(com.pickyboy.interviewcodex.lock.DistributeLock)")
    public Object process(ProceedingJoinPoint pjp) throws Exception {
        Object response = null;
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        DistributeLock distributeLock = method.getAnnotation(DistributeLock.class);

        // 获取Key
        String key = distributeLock.key();
        if (DistributeLockConstant.NONE_KEY.equals(key)) {
            if (DistributeLockConstant.NONE_KEY.equals(distributeLock.keyExpression())) {
                throw new DistributeLockException("no lock key found...");
            }
            // 解析Spel表达式,计算key
            // region
            SpelExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression(distributeLock.keyExpression());

            EvaluationContext context = new StandardEvaluationContext();
            // 获取参数值
            Object[] args = pjp.getArgs();

            // 获取运行时参数的名称
            StandardReflectionParameterNameDiscoverer discoverer
                    = new StandardReflectionParameterNameDiscoverer();
            String[] parameterNames = discoverer.getParameterNames(method);

            // 将参数绑定到context中
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // 解析表达式，获取结果
            key = String.valueOf(expression.getValue(context));
            // endregion
        }

        String scene = distributeLock.scene();

        // 拼接场景和锁,获得实际锁的键名
        String lockKey = scene + "#" + key;

        int expireTime = distributeLock.expireTime();
        int waitTime = distributeLock.waitTime();
        // 锁对象
        RLock rLock= redissonClient.getLock(lockKey);
        try {
            boolean lockResult = false;
            // 阻塞等待,直到拿到锁
            if (waitTime == DistributeLockConstant.DEFAULT_WAIT_TIME) {
                // 锁一直续期,除非自己释放

                if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {
                    LOG.info(String.format("lock for key : %s", lockKey));
                    rLock.lock();
                } else {
                    LOG.info(String.format("lock for key : %s , expire : %s", lockKey, expireTime));
                    rLock.lock(expireTime, TimeUnit.MILLISECONDS);
                }
                lockResult = true;
            } else
            // 有期限的等待,拿不到锁直接失败
            {
                if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {
                    LOG.info(String.format("try lock for key : %s , wait : %s", lockKey, waitTime));
                    lockResult = rLock.tryLock(waitTime, TimeUnit.MILLISECONDS);
                } else {
                    LOG.info(String.format("try lock for key : %s , expire : %s , wait : %s", lockKey, expireTime, waitTime));
                    lockResult = rLock.tryLock(waitTime, expireTime, TimeUnit.MILLISECONDS);
                }
            }

            // 获取锁失败,直接抛出异常
            if (!lockResult) {
                LOG.warn(String.format("lock failed for key : %s , expire : %s", lockKey, expireTime));
                throw new DistributeLockException("acquire lock failed... key : " + lockKey);
            }


            LOG.info(String.format("lock success for key : %s , expire : %s", lockKey, expireTime));
            // 成功获取锁,进入原方法
            response = pjp.proceed();
        } catch (Throwable e) {
            throw new Exception(e);
        } finally {
            // 锁依然被当前线程持有,才调用方法释放
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                LOG.info(String.format("unlock for key : %s , expire : %s", lockKey, expireTime));
            }
        }
        return response;
    }
}
