# 工作流引擎说明

## ⚙️ 功能概述

可视化工作流引擎，支持：

- ✅ 工作流定义和编排
- ✅ 多种节点类型（开始/结束/任务/条件/并行）
- ✅ 工作流实例执行
- ✅ 执行状态追踪
- ✅ 节点重试机制
- ✅ 并行分支支持

---

## 🏗️ 架构设计

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│ 工作流定义   │ ───> │ 工作流引擎    │ ───> │  节点执行器  │
│  (JSON 配置)  │      │ (调度执行)    │      │ (任务处理)   │
└─────────────┘      └──────────────┘      └─────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │   数据库      │
                     │ (状态持久化)  │
                     └──────────────┘
```

---

## 📝 使用方式

### 1. 创建工作流定义

```bash
POST /api/workflow/definition?workflowCode=customer_onboarding&workflowName=客户入驻流程

{
  "startNodeId": "start",
  "nodes": [
    {
      "id": "start",
      "name": "开始",
      "type": "START"
    },
    {
      "id": "task1",
      "name": "验证客户信息",
      "type": "TASK",
      "config": {
        "taskType": "VALIDATE_CUSTOMER"
      }
    },
    {
      "id": "condition1",
      "name": "验证结果判断",
      "type": "CONDITION"
    },
    {
      "id": "task2",
      "name": "创建账户",
      "type": "TASK",
      "config": {
        "taskType": "CREATE_ACCOUNT"
      }
    },
    {
      "id": "end",
      "name": "结束",
      "type": "END"
    }
  ],
  "edges": [
    {
      "source": "start",
      "target": "task1"
    },
    {
      "source": "task1",
      "target": "condition1"
    },
    {
      "source": "condition1",
      "target": "task2",
      "condition": "validated == true"
    },
    {
      "source": "condition1",
      "target": "end",
      "condition": "validated == false"
    },
    {
      "source": "task2",
      "target": "end"
    }
  ]
}
```

### 2. 发布工作流

```bash
POST /api/workflow/definition/{id}/publish
```

### 3. 启动工作流实例

```bash
POST /api/workflow/definition/{id}/start

{
  "customerId": "CUST_123",
  "customerName": "张三",
  "email": "zhangsan@example.com"
}

# 响应
{
  "code": 200,
  "data": {
    "id": "instance-uuid",
    "instanceNo": "WF20240406001234567",
    "status": "RUNNING"
  }
}
```

### 4. 查询实例状态

```bash
GET /api/workflow/instance/{id}

# 响应
{
  "code": 200,
  "data": {
    "id": "instance-uuid",
    "status": "RUNNING",
    "currentNodeId": "task2",
    "startTime": "2024-04-06T01:00:00"
  }
}
```

### 5. 查询节点执行历史

```bash
GET /api/workflow/instance/{id}/nodes

# 响应
[
  {
    "nodeId": "start",
    "nodeName": "开始",
    "nodeType": "START",
    "status": "COMPLETED",
    "duration": 10
  },
  {
    "nodeId": "task1",
    "nodeName": "验证客户信息",
    "nodeType": "TASK",
    "status": "COMPLETED",
    "duration": 1500
  },
  {
    "nodeId": "task2",
    "nodeName": "创建账户",
    "nodeType": "TASK",
    "status": "RUNNING"
  }
]
```

---

## 🎯 节点类型

### START - 开始节点

工作流的起点，无输入，触发第一个任务。

```json
{
  "id": "start",
  "name": "开始",
  "type": "START"
}
```

### END - 结束节点

工作流的终点，标记工作流完成。

```json
{
  "id": "end",
  "name": "结束",
  "type": "END"
}
```

### TASK - 任务节点

执行具体任务，可配置任务类型。

```json
{
  "id": "task1",
  "name": "发送邮件",
  "type": "TASK",
  "config": {
    "taskType": "SEND_EMAIL",
    "template": "welcome",
    "async": true
  }
}
```

**内置任务类型：**
- `CHAT_RESPONSE` - AI 聊天响应
- `SESSION_SUMMARY` - 会话摘要
- `TITLE_GENERATION` - 标题生成
- `EMAIL_SEND` - 邮件发送
- `FILE_PROCESS` - 文件处理
- `CUSTOM` - 自定义任务

### CONDITION - 条件节点

根据条件表达式选择分支。

```json
{
  "id": "condition1",
  "name": "审批结果判断",
  "type": "CONDITION"
}
```

**边配置条件：**
```json
{
  "source": "condition1",
  "target": "approved_task",
  "condition": "approved == true"
}
```

### PARALLEL - 并行节点

启动多个并行分支，等待所有分支完成。

```json
{
  "id": "parallel1",
  "name": "并行处理",
  "type": "PARALLEL",
  "config": {
    "branches": ["branch1", "branch2", "branch3"],
    "waitForAll": true
  }
}
```

---

## 📊 数据库表

### workflow_definition

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 工作流 ID |
| workflow_code | VARCHAR(64) | 工作流编码 |
| workflow_name | VARCHAR(128) | 工作流名称 |
| version | VARCHAR(32) | 版本号 |
| workflow_config | LONGTEXT | 配置（JSON） |
| status | VARCHAR(16) | 状态 |

### workflow_instance

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 实例 ID |
| workflow_definition_id | VARCHAR(64) | 定义 ID |
| instance_no | VARCHAR(64) | 实例编号 |
| business_data | LONGTEXT | 业务数据 |
| current_node_id | VARCHAR(64) | 当前节点 |
| status | VARCHAR(16) | 状态 |

### workflow_node_instance

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 节点实例 ID |
| workflow_instance_id | VARCHAR(64) | 实例 ID |
| node_id | VARCHAR(64) | 节点 ID |
| node_type | VARCHAR(16) | 节点类型 |
| input_data | LONGTEXT | 输入数据 |
| output_data | LONGTEXT | 输出数据 |
| status | VARCHAR(16) | 状态 |
| duration | BIGINT | 执行时长 |

---

## 🎨 使用场景

### 1. 客户入驻流程

```
开始 → 验证信息 → [条件] → 创建账户 → 发送欢迎邮件 → 结束
                         ↓
                      拒绝通知
```

### 2. 订单处理流程

```
开始 → 订单审核 → [条件] → 库存检查 → 支付处理 → 发货 → 结束
                          ↓
                       取消订单
```

### 3. 内容审批流程

```
开始 → 提交审核 → 一级审批 → [条件] → 二级审批 → 发布 → 结束
                                  ↓
                               拒绝
```

### 4. 并行数据处理

```
开始 → 并行分支 → [分支 1: 数据清洗] → 合并结果 → 结束
                [分支 2: 数据验证]
                [分支 3: 数据分析]
```

---

## ⚙️ 工作流配置示例

### 简单审批流程

```json
{
  "startNodeId": "start",
  "nodes": [
    {"id": "start", "name": "开始", "type": "START"},
    {"id": "submit", "name": "提交申请", "type": "TASK", 
     "config": {"taskType": "SUBMIT_APPLICATION"}},
    {"id": "manager_approve", "name": "经理审批", "type": "TASK",
     "config": {"taskType": "MANAGER_APPROVAL"}},
    {"id": "condition", "name": "审批判断", "type": "CONDITION"},
    {"id": "hr_approve", "name": "HR 审批", "type": "TASK"},
    {"id": "notify", "name": "发送通知", "type": "TASK",
     "config": {"taskType": "SEND_NOTIFICATION"}},
    {"id": "end", "name": "结束", "type": "END"}
  ],
  "edges": [
    {"source": "start", "target": "submit"},
    {"source": "submit", "target": "manager_approve"},
    {"source": "manager_approve", "target": "condition"},
    {"source": "condition", "target": "hr_approve", "condition": "approved == true"},
    {"source": "condition", "target": "notify", "condition": "approved == false"},
    {"source": "hr_approve", "target": "notify"},
    {"source": "notify", "target": "end"}
  ]
}
```

---

## 🔍 监控和管理

### 查看运行中实例

```bash
GET /api/workflow/definition/{id}/instances?limit=20
```

### 获取工作流统计

```bash
GET /api/workflow/definition/{id}/stats

# 响应
{
  "running": 5,
  "completed": 100,
  "failed": 2,
  "cancelled": 1
}
```

### 取消实例

```bash
POST /api/workflow/instance/{id}/cancel
```

### 重试失败节点

```bash
POST /api/workflow/instance/{instanceId}/node/{nodeId}/retry
```

---

## ⚠️ 注意事项

1. **版本管理**：每次发布自动增加版本号
2. **状态持久化**：每个节点执行状态都记录到数据库
3. **异常处理**：节点失败可配置重试次数
4. **并发控制**：使用线程池并发执行多个实例
5. **超时处理**：建议配置节点超时时间

---

## 🚀 扩展建议

- [ ] 可视化工作流编辑器
- [ ] 节点超时配置
- [ ] 定时触发器
- [ ] 子工作流支持
- [ ] 事件驱动架构
- [ ] 工作流模板市场
- [ ] 执行日志审计
- [ ] 性能监控和告警
