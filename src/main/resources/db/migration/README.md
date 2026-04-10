# 数据库迁移脚本说明

本目录包含 AI Spring Framework 的 Flyway 数据库迁移脚本。

## 📁 目录结构

```
db/migration/
├── README.md                          # 本说明文件
├── V1.0.0__init_database.sql         # 数据库初始化（所有表结构 + 初始数据）
├── V1.0.1__add_indexes.sql           # 索引优化
├── V1.0.2__add_chat_session_tags.sql # 会话标签功能增强
└── V1.0.3__add_session_template.sql  # 会话模板功能
```

## 📋 迁移脚本说明

### V1.0.0 - 数据库初始化

**文件名**: `V1.0.0__init_database.sql`

创建所有必需的表结构和初始数据：

#### 系统管理模块
- `sys_user` - 用户表
- `sys_role` - 角色表
- `sys_permission` - 权限表
- `sys_user_role` - 用户角色关联表
- `sys_role_permission` - 角色权限关联表

#### AI 聊天模块
- `chat_sessions` - 聊天会话表
- `chat_messages` - 聊天消息表
- `chat_message_favorites` - 消息收藏表
- `chat_session_shares` - 会话分享表
- `chat_session_summaries` - 会话摘要表
- `chat_session_tags` - 会话标签表
- `token_usage` - Token 使用统计表

#### 知识库模块
- `knowledge_base` - 知识库表
- `knowledge_document` - 知识文档表
- `knowledge_chunk` - 知识分块表

#### Agent/工作流模块
- `chain_templates` - Agent 链模板表
- `chain_template_nodes` - Agent 链模板节点表
- `agent_task` - Agent 任务表
- `agent_task_log` - Agent 任务日志表
- `workflow_definition` - 工作流定义表
- `workflow_instance` - 工作流实例表
- `workflow_node_instance` - 工作流节点实例表

#### 多模态模块
- `chat_session_multimodal` - 会话多模态记录表

#### 异步任务模块
- `async_task` - 异步任务表

**初始数据**:
- 默认管理员用户：`admin` / `admin123`
- 默认角色：`ADMIN`（超级管理员）、`USER`（普通用户）
- 默认权限：7 个基础权限

---

### V1.0.1 - 索引优化

**文件名**: `V1.0.1__add_indexes.sql`

添加复合索引和全文索引，优化查询性能：

- 消息表复合索引（会话 + 时间、会话 + 角色）
- 会话表复合索引（用户 + 状态、用户 + 活跃时间）
- 知识文档表复合索引
- 知识分块表全文索引（用于关键词检索）
- Agent 任务表复合索引
- 工作流实例表复合索引
- Token 使用表复合索引

---

### V1.0.2 - 会话标签功能增强

**文件名**: `V1.0.2__add_chat_session_tags.sql`

增强会话标签功能：

- 在 `chat_sessions` 表添加 `tags_json` 字段（JSON 格式，冗余存储）
- 在 `chat_session_tags` 表添加唯一索引（防止重复标签）
- 自动迁移现有标签数据到 JSON 字段

---

### V1.0.3 - 会话模板功能

**文件名**: `V1.0.3__add_session_template.sql`

添加会话模板功能：

- 创建 `chat_session_template` 表
- 插入 5 个默认模板：
  - 通用助手
  - 编程助手
  - 翻译助手
  - 写作助手
  - 数据分析

---

## 🔧 Flyway 配置

在 `application.yml` 中的配置：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    clean-disabled: true  # 生产环境禁止清理
```

## 📝 命名规范

Flyway 迁移脚本遵循以下命名规范：

```
V<版本>__<描述>.sql
```

- `V` - 版本标识（大写）
- `<版本>` - 版本号（如 1.0.0、1.0.1）
- `__` - 双下划线分隔符
- `<描述>` - 描述性名称（小写，下划线分隔）

示例：
- `V1.0.0__init_database.sql`
- `V1.0.1__add_indexes.sql`
- `V1.1.0__add_user_preferences.sql`

## 🚀 使用方法

### 方式一：自动迁移（推荐）

应用启动时自动执行迁移：

```bash
mvn spring-boot:run
```

### 方式二：手动迁移

使用 Flyway 命令行工具：

```bash
flyway migrate -url=jdbc:mysql://localhost:3306/ai_framework -user=root -password=xxx
```

### 方式三：Docker 初始化

首次启动时，Flyway 会自动检查并执行迁移。

## 📊 表关系图

```
sys_user ─┬─ sys_user_role ── sys_role ── sys_role_permission ── sys_permission
          └─ chat_sessions ──┬─ chat_messages
                             ├─ chat_session_shares
                             ├─ chat_session_summaries
                             ├─ chat_session_tags
                             └─ chat_session_template

knowledge_base ── knowledge_document ── knowledge_chunk

chain_templates ── chain_template_nodes ── agent_task ── agent_task_log

workflow_definition ── workflow_instance ── workflow_node_instance
```

## ⚠️ 注意事项

1. **生产环境**:
   - 禁止执行 `flyway clean`
   - 备份数据库后再执行迁移
   - 在低峰期执行迁移

2. **回滚**:
   - Flyway 不支持自动回滚
   - 需要手动编写回滚脚本（放在 `db/rollback` 目录）

3. **测试**:
   - 新迁移脚本先在测试环境验证
   - 确认无错误后再应用到生产环境

4. **字符集**:
   - 所有表使用 `utf8mb4` 字符集
   - 排序规则：`utf8mb4_unicode_ci`

## 🔍 查询迁移历史

```sql
-- 查看已执行的迁移
SELECT * FROM flyway_schema_history ORDER BY installed_on DESC;

-- 查看迁移状态
SELECT version, description, type, state, installed_by, installed_on 
FROM flyway_schema_history;
```

## 📞 问题排查

### 迁移失败

1. 检查错误日志
2. 确认数据库连接正常
3. 确认有足够的权限执行 DDL/DML
4. 检查 SQL 语法是否正确

### 版本冲突

如果手动修改了数据库结构，可能需要：

```sql
-- 更新 Flyway 历史记录
UPDATE flyway_schema_history 
SET version_match = true, 
    checksum = NULL 
WHERE version = 'x.x.x';
```

或者删除 `flyway_schema_history` 表重新迁移（仅开发环境）。

---

最后更新：2026-04-09
