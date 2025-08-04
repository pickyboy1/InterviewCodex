package com.pickyboy.guardian.core.key.impl;

import cn.hutool.core.util.StrUtil;
import com.pickyboy.guardian.core.key.KeyProvider;
import com.pickyboy.guardian.exception.ParseKeyException;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * SpEL表达式 Key生成器 (集成错误处理)
 * <p>
 * 支持Spring Expression Language表达式解析。
 *
 * @author pickyboy
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE) // 赋予一个最低的优先级，使其作为默认的回退选项
public class SpelKeyProvider implements KeyProvider {

    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    @Override
    public String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (StrUtil.isBlank(keyExpression)) {
            // 对于 SpEL 来说，一个空的表达式是无效的
            throw new ParseKeyException("SpEL key expression cannot be blank.");
        }

        try {
            // 如果不是一个看起来像 SpEL 的表达式，直接返回原始字符串
            // 这使得 SpEL Provider 也能处理简单的静态字符串 Key
            if (!isSpelExpression(keyExpression)) {
                return keyExpression;
            }

            // 解析SpEL表达式
            Expression expression = spelParser.parseExpression(keyExpression);
            EvaluationContext context = buildEvaluationContext(joinPoint);
            Object result = expression.getValue(context);

            // 关键改动：如果 SpEL 表达式的计算结果为 null，也视为一种失败
            if (result == null) {
                throw new ParseKeyException("SpEL expression '" + keyExpression + "' evaluated to null.");
            }

            return result.toString();

        } catch (Exception e) {
            // 关键改动：将所有解析和执行过程中的异常，统一包装成我们自定义的 ParseKeyException
            log.error("SpelKeyProvider解析表达式 '{}' 失败", keyExpression, e);
            if (e instanceof ParseKeyException) {
                throw e; // 如果已经是我们的异常，直接抛出
            }
            throw new ParseKeyException("Failed to parse SpEL expression: " + keyExpression, e);
        }
    }

    @Override
    public String getName() {
        return GuardianConstants.KEY_PROVIDER_SPEL;
    }

    @Override
    public String getDescription() {
        return "SpEL表达式 Key生成器，支持Spring表达式语言";
    }

    @Override
    public boolean supports(String keyExpression) {
        // SpelKeyProvider 作为默认的回退选项，理论上支持所有字符串。
        // KeyProviderManager 会确保它在最后被调用。
        return true;
    }

    /**
     * 简单地、启发式地判断一个字符串是否可能是SpEL表达式。
     */
    private boolean isSpelExpression(String keyExpression) {
        // 这是一个简单的检查，可以根据需要进行扩展
        return StrUtil.isNotBlank(keyExpression) &&
                (keyExpression.contains("#") ||
                        keyExpression.contains(".") ||
                        keyExpression.contains("'") ||
                        keyExpression.contains("+"));
    }

    /**
     * 构建SpEL表达式的求值上下文，将方法参数放入其中。
     */
    private EvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length && i < args.length; i++) {
            context.setVariable(parameters[i].getName(), args[i]);
            // 为了方便，也为 request 和 user 对象设置别名
            if (args[i] instanceof HttpServletRequest) {
                context.setVariable("request", args[i]);
            }
            if (args[i] != null && args[i].getClass().getSimpleName().toLowerCase().contains("user")) {
                context.setVariable("loginUser", args[i]);
                context.setVariable("user", args[i]);
            }
        }
        return context;
    }
}