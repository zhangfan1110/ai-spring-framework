# 异步任务队列说明

## 📨 功能概述

基于 **Redis Stream** 的轻量级异步任务队列系统，支持：

- ✅ 异步任务提交
- ✅ 延迟任务
- ✅ 任务重试
- ✅ 优先级队列
- ✅ 任务状态追踪
- ✅ 超时处理
- ✅ 并发执行

---

## 🏗️ 架构设计

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Client    │ ───> │ Task Queue   │ ───> │  Executor   │
│  (提交任务)  │      │   (Redis)    │      │ (线程池执行) │
└─────────────┘      └──────────────┘      └─────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  Database    │
                     │ (任务状态持久化)│
                     └──────────────┘
```

---

## 📝 使用方式

### 1. 提交异步任务

```bash
POST /api/task/submit?type=SESSION_SUMMARY&priority=3&delay=0&maxRetry=3
Content-Type: application/json

{
  "sessionId": "session_123"
}

# 响应
{
  "code": 200,
  "data": {
    "taskId": "uuid-xxx",
    "status": "PENDING",
    "message": "任务已提交"
  }
}
```

### 2. 查询任务状态

```bash
GET /api/task/status/{taskId}

# 响应
{
  "code": 200,
  "data": {
    "id": "uuid-xxx",
    "taskType": "SESSION_SUMMARY",
    "status": "SUCCESS",
    "result": "Summary generated",
    "createTime": "2024-04-06T01:00:00",
    "endTime": "2024-04-06T01:00:05"
  }
}
```

### 3. 获取任务统计

```bash
GET /api/task/stats

# 响应
{
  "code": 200,
  "data": {
    "pending": 10,
    "running": 5,
    "success": 100,
    "failed": 2,
    "delayQueueSize": 3,
    "defaultQueueSize": 15
  }
}
```

---

## 🎯 任务类型

### 内置任务类型

| 类型 | 说明 | 参数 |
|------|------|------|
| `CHAT_RESPONSE` | 聊天响应处理 | - |
| `SESSION_SUMMARY` | 会话摘要生成 | sessionId |
| `TITLE_GENERATION` | 标题生成 | sessionId |
| `MEMORY_COMPRESSION` | 记忆压缩 | sessionId |
| `EMAIL_SEND` | 邮件发送 | to, subject, content |
| `FILE_PROCESS` | 文件处理 | filePath |

### 自定义任务类型

在 `TaskExecutorService.executeByType()` 中添加新的任务处理器：

```java
private String executeByType(String taskType, Map<String, Object> data) {
    switch (taskType) {
        case "MY_CUSTOM_TASK":
            return handleMyCustomTask(data);
        // ...
    }
}
```

---

## ⚙️ 配置参数

### 提交任务参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | String | 必填 | 任务类型 |
| `data` | Object | {} | 任务数据（JSON） |
| `priority` | int | 3 | 优先级（1-5） |
| `delay` | long | 0 | 延迟执行时间（秒） |
| `maxRetry` | int | 3 | 最大重试次数 |

### 优先级说明

| 优先级 | 值 | 说明 |
|--------|---|------|
| 最高 | 1 | 优先执行 |
| 高 | 2 | - |
| 中 | 3 | 默认 |
| 低 | 4 | - |
| 最低 | 5 | 最后执行 |

---

## 🔧 高级功能

### 1. 延迟任务

```bash
# 10 分钟后执行
POST /api/task/submit?type=EMAIL_SEND&delay=600

{
  "to": "user@example.com",
  "subject": "定时邮件",
  "content": "这是 10 分钟后发送的邮件"
}
```

### 2. 任务重试

```bash
# 手动重试失败任务
POST /api/task/retry/{taskId}
```

### 3. 超时处理

- 任务执行超过 30 分钟自动标记为超时
- 超时任务会自动重试（不超过最大重试次数）
- 每分钟检查一次超时任务

---

## 📊 数据库表

### async_task

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 任务 ID |
| task_type | VARCHAR(64) | 任务类型 |
| task_data | TEXT | 任务数据（JSON） |
| status | VARCHAR(16) | 状态 |
| result | TEXT | 执行结果 |
| error_message | TEXT | 错误信息 |
| retry_count | INT | 重试次数 |
| max_retry | INT | 最大重试次数 |
| priority | INT | 优先级 |
| delay_seconds | BIGINT | 延迟时间 |
| create_time | DATETIME | 创建时间 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 完成时间 |

---

## 🎨 使用场景

### 1. 异步聊天响应

```java
// 提交任务
Map<String, Object> data = Map.of(
    "sessionId", sessionId,
    "message", userMessage
);
taskQueueService.submitTask("CHAT_RESPONSE", data, 2, 0, 3);

// 立即返回
return Result.success(Map.of("taskId", taskId, "status", "processing"));
```

### 2. 定时任务

```java
// 明天上午 9 点发送邮件
long delay = Duration.between(LocalDateTime.now(), tomorrow9am).getSeconds();
taskQueueService.submitTask("EMAIL_SEND", emailData, 3, delay, 3);
```

### 3. 批量处理

```java
// 批量生成会话摘要
for (String sessionId : sessionIds) {
    taskQueueService.submitTask("SESSION_SUMMARY", 
        Map.of("sessionId", sessionId), 3, 0, 1);
}
```

---

## 🔍 监控和维护

### 查看队列状态

```bash
# Redis CLI
LLEN task:queue:default          # 查看队列长度
ZCARD task:delay                 # 查看延迟队列大小
```

### 查看任务统计

```bash
GET /api/task/stats
```

### 清理已完成任务

```sql
-- 删除 7 天前的成功任务
DELETE FROM async_task 
WHERE status = 'SUCCESS' 
  AND end_time < DATE_SUB(NOW(), INTERVAL 7 DAY);
```

---

## ⚠️ 注意事项

1. **Redis 依赖**：确保 Redis 可用
2. **幂等性**：任务处理器应支持幂等执行
3. **任务大小**：task_data 不宜过大（建议 < 1MB）
4. **重试策略**：合理设置 maxRetry，避免无限重试
5. **监控告警**：建议对失败任务设置告警

---

## 🚀 扩展建议

- [ ] 支持任务依赖（DAG）
- [ ] 任务执行日志
- [ ] 分布式任务调度
- [ ] 任务优先级动态调整
- [ ] 任务取消功能
- [ ] 任务执行进度追踪
- [ ] 集成 RabbitMQ/Kafka
