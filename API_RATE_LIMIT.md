# API 限流配置说明

## 🚦 限流功能

### 已实现
- ✅ 基于 Redis 的分布式限流
- ✅ 自定义 `@RateLimit` 注解
- ✅ 支持多种限流类型：IP/USER/GLOBAL/API
- ✅ 限流拦截器自动拦截
- ✅ 响应头返回剩余请求次数

### 限流注解参数

```java
@RateLimit(
    key = "",                    // 限流 key，支持 SpEL
    time = 60,                   // 时间窗口（默认 60 秒）
    timeUnit = TimeUnit.SECONDS, // 时间单位
    maxRequests = 100,           // 最大请求数
    limitType = LimitType.IP,    // 限流类型
    message = "请求过于频繁"      // 提示信息
)
```

### 限流类型

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| `LimitType.IP` | 按 IP 限流 | 防止单 IP 刷接口 |
| `LimitType.USER` | 按用户限流 | 防止单用户滥用 |
| `LimitType.GLOBAL` | 全局限流 | 控制总流量 |
| `LimitType.API` | 按接口限流 | 保护特定接口 |

## 📝 使用示例

### 1. 聊天接口限流

```java
@PostMapping("/chat")
@RateLimit(time = 60, maxRequests = 20, limitType = LimitType.USER)
public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
    // 每分钟最多 20 次请求/用户
}
```

### 2. Agent 任务限流

```java
@PostMapping("/react/execute")
@RateLimit(time = 60, maxRequests = 10, limitType = LimitType.USER)
public Result<?> executeTask(@RequestBody Map<String, Object> request) {
    // 每分钟最多 10 次任务/用户
}
```

### 3. 短信发送限流

```java
@PostMapping("/sms/send")
@RateLimit(time = 60, maxRequests = 5, limitType = LimitType.IP)
public Result<?> sendSms(@RequestParam String phone) {
    // 每分钟最多 5 次/IP
}
```

### 4. 全局限流

```java
@PostMapping("/expensive-operation")
@RateLimit(time = 60, maxRequests = 100, limitType = LimitType.GLOBAL)
public Result<?> expensiveOperation() {
    // 全系统每分钟最多 100 次
}
```

## 🔧 配置

### Redis 配置

确保 Redis 可用：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### 限流配置（可选）

```yaml
rate-limit:
  enabled: true
  default-max-requests: 100
  default-time-window: 60
```

## 📊 响应头

限流响应包含以下头部：

```
X-RateLimit-Limit: 100          # 最大请求数
X-RateLimit-Remaining: 95       # 剩余请求数
```

超过限制时返回：

```json
{
  "code": 500,
  "message": "请求过于频繁，请稍后再试"
}
```

HTTP 状态码：`429 Too Many Requests`

## 🎯 默认限流规则

以下接口已配置限流：

| 接口 | 限流规则 | 说明 |
|------|----------|------|
| `/api/ai/chat` | 20 次/分钟/用户 | 聊天接口 |
| `/api/agent/react/execute` | 10 次/分钟/用户 | Agent 任务 |
| 其他 `/api/**` | 100 次/分钟/IP | 默认规则 |

**不限流的接口：**
- `/api/auth/**` - 认证接口
- `/swagger-ui/**` - 文档
- `/actuator/**` - 监控
- `/druid/**` - Druid 监控

## 🔍 Redis Key 格式

限流数据存储格式：

```
rate:limit:api:ai:chat:ip:192.168.1.100
rate:limit:api:ai:chat:user:admin
rate:limit:api:agent:react:execute:user:admin
```

## 🛠️ 手动管理限流

### 查看限流状态

```bash
# Redis CLI
GET rate:limit:api:ai:chat:ip:192.168.1.100
```

### 重置限流

```bash
# Redis CLI
DEL rate:limit:api:ai:chat:ip:192.168.1.100
```

### 通过 API 重置（管理员）

```java
@Autowired
private RateLimitService rateLimitService;

// 重置指定 key 的限流
rateLimitService.reset("api:ai:chat:ip:192.168.1.100");
```

## ⚠️ 注意事项

1. **Redis 依赖**：限流功能依赖 Redis，确保 Redis 可用
2. **时间同步**：分布式环境下确保服务器时间同步
3. **限流粒度**：根据业务选择合适的限流类型
4. **用户体验**：设置合理的限流阈值，避免影响正常用户

## 📈 监控建议

1. 记录限流日志
2. 监控限流触发频率
3. 分析被限流的用户/IP
4. 根据监控数据调整限流策略

---

## 🚀 扩展建议

- [ ] 添加限流白名单
- [ ] 支持动态调整限流配置
- [ ] 限流数据统计和可视化
- [ ] 分级限流（普通用户/VIP 用户）
- [ ] 滑动窗口限流算法
