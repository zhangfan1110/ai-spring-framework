# 安全认证配置说明

## 🔐 JWT + RBAC 权限系统

### 1. 数据库初始化

运行数据库迁移脚本：

```sql
-- 1. 先运行基础表结构
source src/main/resources/db/migration/V3__add_security_tables.sql;

-- 2. 如果登录失败，重新生成密码哈希
-- 密码：admin123
-- 需要确保 BCrypt 哈希值正确
```

### 2. 默认账号

- **用户名**: admin
- **密码**: admin123

### 3. 如果登录失败 "Bad credentials"

说明数据库中的密码哈希值不正确。请按以下步骤修复：

#### 方法一：使用在线工具生成 BCrypt 哈希

1. 访问：https://bcrypt-generator.com/
2. 输入密码：`admin123`
3. Rounds: 10
4. 复制生成的哈希值
5. 执行 SQL 更新：

```sql
UPDATE sys_user 
SET password = '复制的哈希值' 
WHERE username = 'admin';
```

#### 方法二：运行 Java 代码生成

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Main {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashed = encoder.encode("admin123");
        System.out.println(hashed);
    }
}
```

#### 方法三：使用 Spring Boot 应用生成

启动应用后访问：
```
GET http://localhost:8081/api/auth/generate-password?password=admin123
```

### 4. 登录流程

```bash
# 1. 登录获取 Token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# 响应示例：
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": "1",
      "username": "admin",
      "nickname": "系统管理员",
      "roles": ["ADMIN"],
      "permissions": ["user:create", "user:update", ...]
    }
  }
}

# 2. 使用 Token 访问受保护接口
curl http://localhost:8081/api/ai/chat \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 5. 权限说明

**角色：**
- `ADMIN` - 超级管理员（所有权限）
- `USER` - 普通用户（基础权限）

**权限编码：**
- `user:*` - 用户管理
- `role:*` - 角色管理
- `agent:execute` - 执行 Agent 任务
- `chat:send` - 发送消息

### 6. 接口保护

**公开接口（无需认证）：**
- `/api/auth/**` - 认证接口
- `/swagger-ui/**` - API 文档
- `/v3/api-docs/**` - OpenAPI 规范
- `/actuator/**` - 监控端点
- `/druid/**` - Druid 监控

**受保护接口（需要 Token）：**
- 其他所有接口

### 7. 常见问题

**Q: 登录失败 "Bad credentials"**
- 检查数据库中密码哈希是否正确
- 确保用户名没有拼写错误
- 检查用户状态是否为 1（正常）

**Q: Token 无效**
- 检查 Token 是否过期（默认 24 小时）
- 确保请求头格式：`Authorization: Bearer <token>`
- 检查 JWT secret 配置是否一致

**Q: 权限不足**
- 检查用户是否有对应角色
- 检查角色是否有对应权限
- 确认权限编码是否正确

---

## 🔧 配置项

`application.yml`:

```yaml
jwt:
  secret: ai-spring-framework-secret-key-2024-very-long-string-for-security
  expiration: 86400000  # 24 小时
  header: Authorization
  prefix: Bearer
```
