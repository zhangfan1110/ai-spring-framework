# 数据库目录说明

## 📁 目录结构

```
db/
├── README.md                    # 本说明文件
├── init.sh                      # 快速初始化脚本（Linux/Mac）
├── init.bat                     # 快速初始化脚本（Windows）
└── migration/                   # Flyway 迁移脚本
    ├── README.md               # 迁移脚本详细说明
    ├── V0.0.1__create_database.sql
    ├── V1.0.0__init_database.sql
    ├── V1.0.1__add_indexes.sql
    ├── V1.0.2__add_chat_session_tags.sql
    └── V1.0.3__add_session_template.sql
```

## 🚀 快速开始

### 方式一：使用初始化脚本（推荐）

**Linux/Mac**:
```bash
cd src/main/resources/db
bash init.sh localhost root your_password
```

**Windows**:
```batch
cd src\main\resources\db
init.bat localhost root your_password
```

### 方式二：手动执行

1. 创建数据库：
```bash
mysql -h localhost -u root -p < db/migration/V0.0.1__create_database.sql
```

2. 启动应用，Flyway 自动执行后续迁移：
```bash
mvn spring-boot:run
```

### 方式三：Docker

如果使用 Docker Compose 启动 MySQL，数据库会自动创建：

```bash
docker-compose up -d mysql
```

然后启动应用即可。

## 📋 迁移脚本列表

| 版本 | 文件名 | 说明 |
|------|--------|------|
| V0.0.1 | `create_database.sql` | 创建数据库（需手动执行） |
| V1.0.0 | `init_database.sql` | 初始化所有表结构 + 初始数据 |
| V1.0.1 | `add_indexes.sql` | 添加索引优化查询性能 |
| V1.0.2 | `add_chat_session_tags.sql` | 会话标签功能增强 |
| V1.0.3 | `add_session_template.sql` | 会话模板功能 |

## 🔐 默认账号

初始化后，默认管理员账号：

- **用户名**: `admin`
- **密码**: `admin123`

⚠️ 首次登录后请立即修改密码！

## 📊 数据库信息

- **数据库名**: `ai_framework`
- **字符集**: `utf8mb4`
- **排序规则**: `utf8mb4_unicode_ci`
- **表数量**: 20+

## 🔍 验证安装

启动应用后，检查以下内容：

1. **Flyway 迁移历史**:
```sql
USE ai_framework;
SELECT * FROM flyway_schema_history ORDER BY installed_on DESC;
```

2. **表列表**:
```sql
SHOW TABLES;
```

应该看到 20+ 张表。

3. **默认用户**:
```sql
SELECT id, username, nickname, email FROM sys_user;
```

应该看到 admin 用户。

## ⚠️ 注意事项

1. **生产环境**:
   - 修改默认密码
   - 禁止执行 `flyway clean`
   - 定期备份数据库

2. **开发环境**:
   - 如需重置数据库，可删除 `ai_framework` 库重新创建
   - 或设置 `spring.flyway.clean-disabled=false` 后执行 `flyway clean`

3. **字符集问题**:
   - 确保 MySQL 服务器配置了 `utf8mb4`
   - 检查 `my.cnf` 中的 `character-set-server` 配置

## 🛠️ 故障排查

### Flyway 迁移失败

1. 检查错误日志
2. 确认数据库连接配置正确
3. 确认有足够的权限执行 DDL/DML

### 表已存在错误

如果手动创建过表，可能需要：

```sql
-- 删除 flyway 历史记录
DELETE FROM flyway_schema_history WHERE version LIKE '1%';

-- 或者删除整个数据库重新迁移
DROP DATABASE ai_framework;
```

### 字符集不正确

```sql
-- 修改数据库字符集
ALTER DATABASE ai_framework CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修改表字符集
ALTER TABLE chat_messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 📞 相关文档

- [迁移脚本详细说明](migration/README.md)
- [application.yml 配置](../application.yml)
- [后端 README](../../README.md)

---

最后更新：2026-04-09
