# AI Spring Framework 🦐

> 基于 Spring Boot + LangChain4j 的企业级 AI 应用开发框架

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.30.0-orange.svg)](https://github.com/langchain4j/langchain4j)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📖 项目简介

AI Spring Framework 是一个功能完整的企业级 AI 应用开发框架，集成了 LangChain4j、通义千问、Ollama 本地模型、Milvus 向量数据库等主流
AI 技术栈。

### 核心特性

- 🤖 **ReAct Agent** - 智能代理，支持工具调用和链式执行
- 🔗 **调用链管理** - 可视化编排 AI 工作流
- 🧠 **RAG 记忆** - 基于 Milvus 的长期记忆存储
- 🛠️ **工具系统** - 20+ 内置工具（搜索、文件、数据库等）
- 💬 **多模型支持** - 通义千问、Ollama 本地模型
- 📊 **监控统计** - Token 使用追踪、性能监控
- 🔐 **安全认证** - JWT 认证、RBAC 权限（待实现）
- 🐳 **容器化部署** - Docker + Docker Compose 一键启动

---

## 🚀 快速开始

### 环境要求

- Java 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- Milvus 2.4.0+（可选，用于 RAG）

### 方式一：Docker Compose（推荐）

```bash
# 克隆项目
git clone <repository-url>
cd ai-spring-framework

# 一键启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f app
```

启动后访问：

- API 服务：http://localhost:8081
- Swagger 文档：http://localhost:8081/swagger-ui.html
- Druid 监控：http://localhost:8081/druid

### 方式二：本地运行

1. **配置数据库**

```sql
CREATE
DATABASE ai_framework DEFAULT CHARACTER SET utf8mb4;
```

2. **修改配置文件**

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_framework
    username: root
    password: your_password
```

3. **运行应用**

```bash
mvn clean install
mvn spring-boot:run
```

---

## 📁 项目结构

```
ai-spring-framework/
├── src/main/java/com/example/aiframework/
│   ├── config/              # 配置类
│   ├── controller/          # REST API 控制器
│   ├── service/             # 业务逻辑层
│   ├── repository/          # 数据访问层
│   ├── entity/              # 实体类
│   ├── dto/                 # 数据传输对象
│   ├── model/               # 数据模型
│   ├── tool/                # AI 工具实现
│   ├── util/                # 工具类
│   └── Application.java     # 启动类
├── src/main/resources/
│   ├── application.yml      # 应用配置
│   ├── db/migration/        # 数据库迁移脚本
│   └── template/            # 模板文件
├── docker-compose.yml       # Docker 编排
├── Dockerfile               # Docker 镜像
└── pom.xml                  # Maven 配置
```

---

## 🎯 核心功能

### 1. ReAct Agent 智能代理

```java
// 调用示例
POST /api/agent/

chat {
    "task":"帮我查询北京天气并发送邮件提醒",
            "role":"助手"
}
```

Agent 会自动：

1. 分析任务意图
2. 选择合适的工具
3. 执行工具调用
4. 返回最终结果

### 2. 调用链管理

内置调用链模板：

- `download_process` - 下载并处理文件
- `weather_notify` - 天气查询与提醒
- `search_translate` - 搜索并翻译
- `pdf_analysis` - PDF 解析与分析
- `news_digest` - 新闻聚合

```bash
# 获取所有模板
GET /api/chain-template/templates

# 执行调用链
POST /api/chain/execute
{
  "templateId": "weather_notify",
  "params": {
    "city": "北京",
    "email": "user@example.com"
  }
}
```

### 3. RAG 记忆服务

基于 Milvus 向量数据库，支持：

- 对话历史存储
- 语义搜索
- 长期记忆检索

```bash
# 搜索相关记忆
GET /api/rag/memory/search?query=上次讨论的项目&sessionId=xxx
```

### 4. 工具系统

内置 20+ 工具：

| 工具               | 功能      |
|------------------|---------|
| SearchTool       | 网络搜索    |
| WeatherTool      | 天气查询    |
| NewsTool         | 新闻获取    |
| StockTool        | 股票查询    |
| TranslationTool  | 翻译      |
| CodeExecutorTool | 代码执行    |
| DatabaseTool     | 数据库操作   |
| FileTool         | 文件操作    |
| HttpTool         | HTTP 请求 |
| PdfTool          | PDF 处理  |
| ImageTool        | 图像处理    |
| ...              | ...     |

### 5. 监控统计

```bash
# 今日 Token 使用
GET /api/monitor/token/today

# 调用链性能统计
GET /api/monitor/performance/chains

# 工具调用统计
GET /api/monitor/performance/tools
```

---

## 📊 API 文档

启动应用后访问 Swagger UI：

```
http://localhost:8081/swagger-ui.html
```

主要接口分类：

- AI 对话接口
- Agent 接口
- 调用链接口
- 会话管理
- RAG 记忆
- 多媒体处理
- 监控统计

---

## ⚙️ 配置说明

### 模型配置

```yaml
# 通义千问
dashscope:
  api-key: your_api_key
  base-url: https://dashscope.aliyuncs.com

# Ollama 本地模型
langchain4j:
  ollama:
    base-url: http://localhost:11434
    model-name: deepseek-r1
    enabled: true
```

### Milvus 配置

```yaml
milvus:
  host: localhost
  port: 19530
  default-collection: ai_memory
```

---

## 🛠️ 开发指南

### 添加自定义工具

1. 实现 `Tool` 接口：

```java

@ToolComponent
public class MyCustomTool implements Tool {
    @Override
    public String getName() {
        return "my_custom_tool";
    }

    @Override
    public String getDescription() {
        return "我的自定义工具";
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> params) {
        // 实现工具逻辑
        return ToolExecutionResult.success(result);
    }
}
```

2. 注册到 `ToolManager`

### 创建调用链模板

```java
ChainTemplate template = new ChainTemplate();
template.

setId("my_template");
template.

setName("我的模板");
template.

setNodes(Arrays.asList(
        createNode("step1", "http",...),

createNode("step2","code_executor",...)
));
        chainTemplateService.

saveTemplateToDatabase(template);
```

---

## 📈 监控与运维

### 健康检查

```bash
GET /actuator/health
```

### 性能监控

- Druid 监控：`/druid`
- Token 使用统计：`/api/monitor/token/*`
- 性能指标：`/api/monitor/performance/*`

### 日志配置

```yaml
logging:
  level:
    com.example.aiframework: DEBUG
    dev.langchain4j: INFO
```

---

## 🚀 部署

### 生产环境部署

1. 修改 `application-prod.yml`
2. 构建 Docker 镜像：

```bash
docker build -t ai-spring-framework:latest .
```

3. 部署到 Kubernetes：

```bash
kubectl apply -f k8s/
```

---

## 📝 更新日志

### v1.0.0 (2024-04-05)

- ✅ 完成内部类迁移（22 个内部类 → 独立文件）
- ✅ 添加 Token 使用统计
- ✅ 添加性能监控服务
- ✅ 添加 Docker 容器化支持
- ✅ 完善 API 文档

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📄 许可证

MIT License

---

## 👤 作者

**皮皮虾** 🦐

---

## 🔗 相关链接

- [LangChain4j 文档](https://docs.langchain4j.dev/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [Milvus 文档](https://milvus.io/docs)
- [通义千问 API](https://help.aliyun.com/zh/dashscope/)
