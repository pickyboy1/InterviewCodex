# Guardian Spring Boot Starter 使用指南

## 简介

Guardian是一个基于策略模式的访问控制和限流组件，支持Redis和本地缓存两种计数器实现，提供灵活的告警处理策略。

## 快速开始

### 1. 添加依赖

在业务项目的`pom.xml`中添加：

```xml
<dependency>
    <groupId>com.pickyboy</groupId>
    <artifactId>guardian-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置文件

在`application.yml`中添加配置：

```yaml
guardian:
  enabled: true
  default-counter-type: redis  # 或 local
  default-warn-threshold: 10
  default-ban-threshold: 20
  default-warn-actions:
    - email
    - log
  default-ban-actions:
    - ban
    - email
    - log
```

### 3. 使用注解

在需要限流的方法上添加`@GuardianCheck`注解：

```java
@RestController
public class QuestionController {

    // 方式1：使用KeyProvider自动获取用户ID（推荐）
    @GuardianCheck(
        scene = "view_question",
        keyProvider = "user",  // 自动获取用户ID
        warnCount = 10,
        banCount = 20
    )
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id) {
        // 不需要修改方法参数，Guardian会自动获取当前用户ID
        return success(questionService.getById(id));
    }

    // 方式2：使用KeyProvider自动获取IP地址
    @GuardianCheck(
        scene = "api_access",
        keyProvider = "ip",   // 自动获取IP地址
        counterType = "local",
        warnCount = 100,
        banCount = 200
    )
    @GetMapping("/list")
    public BaseResponse<List<QuestionVO>> listQuestions() {
        // 不需要HttpServletRequest参数，Guardian会自动获取IP
        return success(questionService.list());
    }

    // 方式3：传统SpEL表达式（兼容旧版本）
    @GuardianCheck(
        scene = "edit_question",
        key = "#loginUser.id",        // SpEL表达式
        keyProvider = "spel",         // 明确指定使用SpEL
        warnCount = 5,
        banCount = 10
    )
    @PostMapping("/edit")
    public BaseResponse<Void> editQuestion(@RequestBody QuestionEditRequest request, User loginUser) {
        // 业务逻辑
        return success();
    }

    // 方式4：自定义KeyProvider
    @GuardianCheck(
        scene = "admin_operation",
        keyProvider = "custom",      // 使用自定义Provider
        key = "user_method",         // 传递给Provider的参数
        warnCount = 3,
        banCount = 5
    )
    @PostMapping("/admin/delete")
    public BaseResponse<Void> deleteQuestion(@RequestParam Long id) {
        // 业务逻辑
        return success();
    }
}
```

## 配置说明

### 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|---------|------|
| scene | String | 必填 | 业务场景名称 |
| key | String | 必填 | Key表达式，具体含义取决于keyProvider |
| keyProvider | String | "" | KeyProvider名称（user/ip/spel/custom/auto） |
| counterType | String | "" | 计数器类型（redis/local） |
| windowSize | int | 1 | 时间窗口大小 |
| timeUnit | TimeUnit | MINUTES | 时间单位 |
| expiration | long | 180 | 过期时间（秒） |
| warnCount | int | -1 | 警告阈值（-1使用默认值） |
| banCount | int | -1 | 封禁阈值（-1使用默认值） |
| warnActions | String[] | {} | 警告时执行的动作 |
| banActions | String[] | {} | 封禁时执行的动作 |
| failFast | boolean | true | 是否快速失败 |
| errorMessage | String | "" | 自定义错误消息 |

### KeyProvider说明

| Provider | 描述 | key参数示例 | 说明 |
|----------|------|-------------|------|
| user | 自动获取用户ID | 可为空或"user" | 从Sa-Token、方法参数等自动提取用户ID |
| ip | 自动获取IP地址 | 可为空或"ip" | 从Request、代理头部等自动提取真实IP |
| spel | SpEL表达式解析 | "#userId"、"#request.remoteAddr" | 传统SpEL表达式，向后兼容 |
| custom | 自定义逻辑 | "method"、"user_method" | 自定义业务逻辑，支持多种组合策略 |
| auto | 自动选择 | 任意 | 根据key表达式自动选择合适的Provider |

### SpEL表达式示例

```java
// 使用用户ID
key = "#loginUser.id"

// 使用IP地址
key = "#request.remoteAddr"

// 使用方法参数
key = "#userId"

// 组合表达式
key = "'user:' + #loginUser.id + ':action:' + #action"
```

### 动作类型

- `email`: 邮件告警（模拟实现，记录日志）
- `log`: 日志记录
- `ban`: 封禁用户（需要sa-token依赖）

### 计数器类型

- `redis`: Redis分布式计数器（需要Redis和Redisson）
- `local`: 本地Caffeine缓存计数器

## 扩展自定义策略

### 自定义KeyProvider

```java
@Component
public class BusinessKeyProvider implements KeyProvider {

    @Override
    public String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        // 自定义Key生成逻辑
        if ("tenant".equals(keyExpression)) {
            // 多租户场景：租户ID + 用户ID
            String tenantId = getCurrentTenantId();
            String userId = getCurrentUserId();
            return "tenant:" + tenantId + ":user:" + userId;
        }

        if ("department".equals(keyExpression)) {
            // 部门级别限流
            String deptId = getCurrentUserDepartment();
            return "dept:" + deptId;
        }

        // 默认逻辑
        return "business:" + keyExpression;
    }

    @Override
    public String getName() {
        return "business";  // Provider名称
    }

    @Override
    public String getDescription() {
        return "业务相关的Key生成器";
    }

    @Override
    public boolean supports(String keyExpression) {
        return "tenant".equals(keyExpression) ||
               "department".equals(keyExpression);
    }

    private String getCurrentTenantId() {
        // 实现租户ID获取逻辑
        return "tenant_123";
    }

    private String getCurrentUserId() {
        // 实现用户ID获取逻辑
        return "user_456";
    }

    private String getCurrentUserDepartment() {
        // 实现部门ID获取逻辑
        return "dept_789";
    }
}
```

使用自定义KeyProvider：

```java
@GuardianCheck(
    scene = "tenant_operation",
    keyProvider = "business",  // 使用自定义Provider
    key = "tenant",           // 传递给Provider的参数
    warnCount = 50,
    banCount = 100
)
@PostMapping("/data/export")
public BaseResponse<Void> exportData() {
    // 业务逻辑
    return success();
}
```

### 自定义动作策略

```java
@Component
public class CustomActionStrategy implements ActionStrategy {

    @Override
    public void execute(ActionContext context) {
        // 自定义处理逻辑
        // 例如：发送钉钉通知、记录数据库等
    }

    @Override
    public ActionType getType() {
        return ActionType.CUSTOM;
    }

    @Override
    public boolean supports(AlertLevel level) {
        return true;
    }
}
```

### 异常处理

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GuardianException.class)
    public BaseResponse<Void> handleGuardianException(GuardianException e) {
        return BaseResponse.error(ErrorCode.RATE_LIMIT_ERROR, e.getMessage());
    }
}
```

## 注意事项

1. 如果使用Redis计数器，需要确保Redis和Redisson配置正确
2. 如果使用Ban策略，需要添加sa-token依赖
3. SpEL表达式中的变量名要与方法参数名匹配
4. 建议在生产环境中调整合适的阈值
5. 异常情况下会自动降级，不影响业务功能

## 依赖要求

### 必需依赖
- Spring Boot 2.x+
- Spring AOP

### 可选依赖
- Redisson（使用Redis计数器时）
- Sa-Token（使用Ban策略时）

## 示例项目

参考 `InterviewCodex` 项目中的使用示例。