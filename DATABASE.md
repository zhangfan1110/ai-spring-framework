# 数据库配置说明 🗄️

本文档说明 AI Spring Framework 的数据库配置和初始化流程。

## 📋 目录

- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [数据库结构](#数据库结构)
- [Flyway 迁移](#flyway 迁移)
- [表结构说明](#表结构说明)
- [常见问题](#常见问题)

---

## 环境要求

- **MySQL**: 8.0+
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci

---

## 快速开始

### 方式一：使用 Docker（推荐）

```bash
# 启动 MySQL
cd /Users/zhangfan/Desktop/ai-spring-framework
docker-compose up -d mysql

# 启动应用（Flyway 自动迁移）
mvn spring-boot:run
```

### 方式二：本地 MySQL

1. **创建数据库**:

```bash
# Linux/Mac
cd src/main/resources/db
bash init.sh localhost root your_password

# Windows
cd src\main\resources\db
init.bat localhost root your_password
```

2. **启动应用**:

```bash
mvn spring-boot:run
```

Flyway 会自动执行迁移脚本创建所有表结构。

### 方式三：手动执行

```bash
# 1. 创建数据库
mysql -h localhost -u root -p -e "CREATE DATABASE ai_framework DEFAULT CHARACTER SET utf8mb4"

# 2. 启动应用，Flyway 自动迁移
mvn spring-boot:run
```

---

## 数据库结构

### 模块划分

| 模块 | 表数量 | 说明 |
|------|--------|------|
| 系统管理 | 5 | 用户、角色、权限 |
| AI 聊天 | 7 | 会话、消息、分享、收藏、摘要、标签、Token 统计 |
| 知识库 | 3 | 知识库、文档、分块 |
| Agent/工作流 | 7 | 模板、节点、任务、日志、工作流定义/实例/节点实例 |
| 多模态 | 1 | OCR/语音/视觉记录 |
| 异步任务 | 1 | 异步任务队列 |
| Flyway | 1 | 迁移历史记录 |

**总计**: 25 张表

### 表关系图

```
┌─────────────────────────────────────────────────────────────┐
│                      系统管理模块                            │
├─────────────────────────────────────────────────────────────┤
│  sys_user ──┬── sys_user_role ── sys_role                  │
│             └── sys_role_permission ── sys_permission       │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      AI 聊天模块                             │
├─────────────────────────────────────────────────────────────┤
│  chat_sessions ──┬── chat_messages                          │
│                  ├── chat_session_shares                    │
│                  ├── chat_session_summaries                 │
│                  ├── chat_session_tags                      │
│                  ├── chat_message_favorites                 │
│                  └── chat_session_template                  │
│                                                            │
│  token_usage (统计)                                         │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      知识库模块                              │
├─────────────────────────────────────────────────────────────┤
│  knowledge_base ── knowledge_document ── knowledge_chunk    │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Agent/工作流模块                          │
├─────────────────────────────────────────────────────────────┤
│  chain_templates ── chain_template_nodes                    │
│         │                                                   │
│         ▼                                                   │
│  agent_task ── agent_task_log                               │
│                                                            │
│  workflow_definition ── workflow_instance                   │
│                           └── workflow_node_instance        │
└─────────────────────────────────────────────────────────────┘
```

---

## Flyway 迁移

### 配置

在 `application.yml` 中：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    clean-disabled: true  # 生产环境禁止清理
    encoding: UTF-8
```

### 迁移脚本位置

```
src/main/resources/db/migration/
├── V0.0.1__create_database.sql         # 创建数据库
├── V1.0.0__init_database.sql           # 初始化表结构 + 数据
├── V1.0.1__add_indexes.sql             # 索引优化
├── V1.0.2__add_chat_session_tags.sql   # 标签增强
└── V1.0.3__add_session_template.sql    # 会话模板
```

### 查看迁移历史

```sql
USE ai_framework;

-- 查看已执行的迁移
SELECT version, description, type, state, installed_by, installed_on 
FROM flyway_schema_history 
ORDER BY installed_on DESC;
```

---

## 表结构说明

### 系统管理模块

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 用户表 | id, username, password, email, phone |
| `sys_role` | 角色表 | id, role_code, role_name |
| `sys_permission` | 权限表 | id, permission_code, permission_name |
| `sys_user_role` | 用户角色关联 | user_id, role_id |
| `sys_role_permission` | 角色权限关联 | role_id, permission_id |

### AI 聊天模块

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `chat_sessions` | 会话表 | session_id, user_id, title, message_count |
| `chat_messages` | 消息表 | id, session_id, role, content, model |
| `chat_session_shares` | 会话分享 | share_id, session_id, share_token |
| `chat_session_summaries` | 会话摘要 | id, session_id, summary |
| `chat_session_tags` | 会话标签 | session_id, tag_name |
| `chat_message_favorites` | 消息收藏 | user_id, message_id |
| `chat_session_template` | 会话模板 | id, name, preset_messages |
| `token_usage` | Token 统计 | user_id, model_name, total_tokens |

### 知识库模块

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `knowledge_base` | 知识库 | id, name, collection_name |
| `knowledge_document` | 文档 | id, knowledge_id, title, file_path |
| `knowledge_chunk` | 分块 | id, document_id, content, vector_id |

### Agent/工作流模块

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `chain_templates` | Agent 模板 | id, name, category |
| `chain_template_nodes` | 模板节点 | template_id, node_type, node_config |
| `agent_task` | Agent 任务 | id, template_id, status, output |
| `agent_task_log` | 任务日志 | task_id, action, input, output |
| `workflow_definition` | 工作流定义 | id, workflow_code, workflow_config |
| `workflow_instance` | 工作流实例 | workflow_id, status, output_data |
| `workflow_node_instance` | 节点实例 | instance_id, node_id, status |

---

## 常见问题

### Q1: Flyway 迁移失败

**错误**: `Flyway migration failed`

**解决**:
1. 检查数据库连接配置
2. 确认 MySQL 服务运行
3. 检查用户权限

### Q2: 表已存在

**错误**: `Table 'xxx' already exists`

**解决**:
```sql
-- 删除 Flyway 历史记录
DELETE FROM flyway_schema_history WHERE version LIKE '1%';

-- 或删除整个数据库重新迁移
DROP DATABASE ai_framework;
```

### Q3: 字符集问题

**错误**: `Incorrect string value`

**解决**:
```sql
-- 修改数据库字符集
ALTER DATABASE ai_framework CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修改表字符集
ALTER TABLE chat_messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Q4: 默认密码错误

**问题**: admin 用户无法登录

**解决**:
```sql
-- 重置 admin 密码为 admin123
UPDATE sys_user 
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS' 
WHERE username = 'admin';
```

### Q5: 如何添加新用户

```sql
-- 插入新用户（密码需要 BCrypt 加密）
INSERT INTO sys_user (id, username, password, nickname, email, status)
VALUES ('2', 'user1', '$2a$10$...', '用户 1', 'user1@example.com', 1);

-- 分配角色
INSERT INTO sys_user_role (id, user_id, role_id)
VALUES ('2', '2', '2');  -- USER 角色
```

---

## 相关文档

- [迁移脚本详细说明](src/main/resources/db/migration/README.md)
- [数据库目录说明](src/main/resources/db/README.md)
- [后端 README](README.md)

---

最后更新：2026-04-09
