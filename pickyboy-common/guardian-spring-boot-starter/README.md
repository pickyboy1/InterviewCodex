Guardian 访问控制框架 - 官方文档
欢迎使用 Guardian！这是一个为现代 Spring Boot 应用量身打造的、声明式的、高度可扩展的访问控制框架。

1. 简介
   1.1 Guardian 是什么？
   Guardian 是一个轻量级的 Spring Boot Starter，旨在以一种优雅、无侵入的方式，为您的 Web 应用提供强大的反爬虫、接口限流和访问频率控制能力。它不仅仅是一个简单的限流工具，更是一个可配置、可插拔的访问策略执行引擎。

1.2 它解决了什么问题？
反爬虫：通过对用户、IP 等维度的访问频率进行精细化控制，有效防止恶意爬虫大规模抓取数据。

接口防刷：保护核心接口（如短信验证码、登录、抽奖接口）不被恶意高频调用。

资源保护：对核心或昂贵的 API 操作进行速率限制，确保系统稳定性和服务质量（QoS）。

复杂访问策略：支持多级、多维度的访问控制规则，满足复杂的业务安全需求。

2. 核心特性
   声明式使用：通过一个简单的 @GuardianCheck 注解即可保护您的方法，无需编写任何侵入性代码。

高度可扩展：所有核心组件（Key生成、计数、处置）均面向接口设计，允许开发者轻松自定义和扩展。

多级规则引擎：支持通过注解或配置文件定义任意多级的“阈值-动作”规则，并支持“单次触发”和“持续触发”两种模式。

智能 Key 生成：内置多种 Key 生成器（IP, UserID, SpEL），并能根据 key 表达式内容自动选择，极大简化了配置。

双计数器支持：默认提供基于 Redis 的分布式计数器和基于 Caffeine 的高性能本地计数器，可按需配置。

异步处置策略：所有处置动作（如日志、告警、封号）均在独立线程池中异步执行，不阻塞核心业务请求。

企业级设计：内置健壮的错误处理（故障开放）、清晰的日志、可配置的线程池和友好的异常提示。

3. 设计哲学与模式
   Guardian 的设计深度融合了现代软件工程的最佳实践。

约定优于配置 (Convention over Configuration)

智能 KeyProvider 选择：您只需在 @GuardianCheck 中写 key = "ip"，Guardian 就能自动匹配到 IpKeyProvider，无需额外指定 keyProvider。

全局默认配置：您可以在 application.yml 中定义一套全局的 default-rules，所有未特殊指定的 @GuardianCheck 都会自动应用这套规则。

面向切面编程 (AOP)

@GuardianCheck 注解是整个框架的入口。GuardianAspect 切面会拦截所有标记了此注解的方法，将访问控制逻辑像一个“可插拔的模块”一样织入到业务流程中，而对业务代码本身保持零侵入。

策略模式 (Strategy Pattern)

ActionStrategy 接口是策略模式的完美体现。框架定义了“处置动作”的契约，而 LogActionStrategy, RejectActionStrategy 等则是具体的策略实现。开发者可以轻松创建自己的策略 Bean 来扩展框架的行为。

接口回调与委托 (Interface Callback & Delegation)

为了将框架逻辑与业务逻辑解耦，Guardian 定义了 UserIdInterface, UserAuthService, UserBanService 等回调接口。Guardian 框架不关心您如何获取用户ID或如何封禁一个用户，它只负责在需要的时候，去调用您提供的实现了这些接口的 Bean，将具体操作“委托”给您的业务系统。

4. 整体架构与实现细节
   Guardian 的工作流程清晰地划分为几个阶段，由不同的核心组件负责：

请求 -> GuardianAspect -> GuardianService -> KeyProviderManager -> Counter -> ActionStrategyManager -> ActionStrategy

GuardianAspect (切面)：作为守门员，拦截所有带 @GuardianCheck 注解的请求。它的职责非常单一：调用 GuardianService 并处理最终返回的 GuardianCheckResult。

GuardianService (核心服务)：框架的“大脑”。负责编排整个检查流程：

调用 KeyProviderManager 生成用于计数的唯一 Key。

解析注解和配置，构建 GuardianDefinition（防护定义）。

调用 Counter 获取计数值。

根据 GuardianDefinition 中的规则列表，判断是否触发了规则。

如果触发，调用 ActionStrategyManager 执行相应的处置策略。

返回一个包含所有信息的 GuardianCheckResult。

KeyProviderManager (Key管理器)：一个智能调度器。它会自动发现系统中所有 KeyProvider 的实现，并根据用户在 key 属性中写入的内容（如 "ip", "#userId"），通过 supports() 方法智能地选择最合适的 Provider 来生成 Key。

Counter (计数器)：负责执行原子计数。我们提供了两种实现：

RedisCounter：使用 Lua 脚本保证“递增”和“设置过期时间”的原子性，实现了高效的滚动时间窗口。

LocalCounter：基于 Caffeine，通过一个 Map 为每种不同的时间窗口动态创建专属的 LoadingCache 实例，实现了高性能的本地计数。

ActionStrategyManager (策略管理器)：一个策略注册中心。它会自动发现并注册系统中所有 ActionStrategy 的实现，供 GuardianService 根据规则配置来调用。

ActionStrategy (处置策略)：具体的“动作”执行者。所有策略都在独立的线程池中异步执行，并通过 failFast() 方法来告知框架是否需要中断当前请求。

5. 使用教程
   步骤 1: 添加依赖
   将 guardian-spring-boot-starter 添加到您项目的 pom.xml 中。

<dependency>
    <groupId>com.pickyboy</groupId>
    <artifactId>guardian-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

步骤 2: 配置文件 (application.yml)
根据您的需要，配置 Guardian。以下是一个全面的示例：

guardian:
enabled: true
default-counter-type: redis
default-window-size: 1
default-time-unit: MINUTES
default-error-message: "您的操作过于频繁，请稍后再试。"
local-counter-max-size: 10000
default-rules:
- count: 20
level: "WARN"
strategies:
- "logOnlyStrategy"
- count: 50
level: "CRITICAL"
strategies:
- "rejectActionStrategy"
- "emailActionStrategy"
continuous: true

步骤 3: 使用 @GuardianCheck 注解
在任何您想保护的 Controller 方法上，添加 @GuardianCheck 注解。

示例 1：简单的 IP 限流

@GuardianCheck(scene = "view_article", key = "ip", rules = {
@Rule(count = 100, strategies = "rejectActionStrategy", continuous = true)
})
@GetMapping("/article/{id}")
public Article getArticle(@PathVariable String id) {
// ...
}

含义：同一个 IP，在 1 分钟内（由全局配置决定）访问文章超过 100 次后，后续所有请求都将被拒绝。

示例 2：复杂的多级用户行为控制

@GuardianCheck(
scene = "post_comment",
key = "#loginUser.id",
windowSize = 1,
timeUnit = TimeUnit.HOURS,
rules = {
@Rule(count = 10, strategies = "logOnlyStrategy", level = "INFO", description = "用户一小时内发帖10次"),
@Rule(count = 20, strategies = "rejectActionStrategy", level = "WARN", description = "拒绝发帖", continuous = true),
@Rule(count = 50, strategies = {"rejectActionStrategy", "banUserStrategy"}, level = "CRITICAL", description = "封禁用户", continuous = true)
}
)
@PostMapping("/comment")
public Result postComment(@RequestBody Comment comment, @LoginUser User loginUser) {
// ...
}

含义：同一个用户，在 1 小时内：

发第 10 条评论时，会触发一次日志记录。

从第 20 条评论开始，后续所有发帖请求都将被拒绝。

从第 50 条评论开始，不仅拒绝请求，还会触发封号策略。

6. 进阶：如何扩展 Guardian？
   a. 自定义处置策略
   只需创建一个实现 ActionStrategy 接口的 Spring Bean 即可。

@Component
public class MyCustomStrategy implements ActionStrategy {
@Override
public void execute(ActionContext context) {
// 您的自定义逻辑，比如发送钉钉消息
System.out.println("自定义策略被执行！Key: " + context.getKey());
}

    @Override
    public String getType() {
        return "myCustomStrategy"; // 策略的唯一名称
    }
}

b. 自定义用户ID获取
如果 Guardian 的默认用户ID解析逻辑不满足您的需求，只需创建一个实现 UserIdInterface 的 Bean。

@Component
public class MyUserIdProvider implements UserIdInterface {
@Override
public String getUserId(ProceedingJoinPoint joinPoint, String keyExpression) {
// 从您自己的登录上下文中获取用户ID
return SecurityContextHolder.getContext().getAuthentication().getName();
}
}

c. 配置专用线程池
为了隔离 Guardian 的异步任务，强烈建议在您的项目中配置一个专用的线程池 Bean，并命名为 guardianExecutor。

@Configuration
public class MyThreadPoolConfig {
@Bean("guardianExecutor")
public Executor guardianExecutor() {
return Executors.newFixedThreadPool(10);
}
}

感谢您使用 Guardian！