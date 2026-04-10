# AI Spring Framework

基于 Spring Boot 3 + LangChain4j 的企业级 AI 应用框架，集成通义千问、Ollama 本地模型、向量数据库、RAG 记忆检索、多模态处理、Agent 工作流等能力。

## 功能特性

- **多 LLM 支持**: 通义千问 (云端)、小米 MiMo v2 Pro、Ollama (本地开源模型)
- **RAG 检索增强**: 向量相似度 + 关键词 BM25 + RRF 融合 + Cross-Encoder 重排序
- **Agent 系统**: ReAct 推理、工具调用链、多 Agent 协作、链模板管理
- **16+ 内置工具**: 计算器、代码执行、搜索、天气、股票、翻译、邮件等
- **多模态**: 图像识别 (Vision)、OCR、语音识别 (ASR)、语音合成 (TTS)、图像生成
- **会话管理**: CRUD、分享、合并、模板、标签、收藏、摘要、导出/导入
- **工作流引擎**: 可视化工作流定义、条件分支、并行执行
- **知识库**: 文档上传、文本分块、向量化存储、语义检索
- **企业特性**: JWT 认证、Spring Security、Redis 缓存、Druid 连接池、Flyway 迁移、Swagger 文档、Actuator 监控

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 3.2.4 |
| LangChain4j | 0.30.0 |
| MyBatis Plus | 3.5.6 |
| MySQL | 8.0 |
| Redis | 7 |
| Milvus | 向量数据库 |

## 快速开始

### 环境要求

- Java 21+
- Maven 3.8+
- MySQL 8.0
- Redis 7

### 启动

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 运行应用
mvn spring-boot:run

# 3. 访问
# Swagger UI:  http://localhost:8081/swagger-ui.html
# Druid 监控:  http://localhost:8081/druid/ (admin / admin123)
# 健康检查:    http://localhost:8081/actuator/health
```

## 项目结构

项目按业务领域组织，每个领域包含完整的 entity / mapper / repository / service / dto / model 分层。

```
src/main/java/com/example/aiframework/
├── Application.java                        # 启动类
│
├── common/                                 # 公共组件
│   ├── annotation/                         #   自定义注解 (@RateLimit, @AsyncTask, LimitType)
│   ├── exception/                          #   全局异常处理 (BusinessException, GlobalExceptionHandler)
│   ├── interceptor/                        #   HTTP 拦截器 (限流、请求日志)
│   ├── security/                           #   JWT 认证 (JwtFilter, UserDetailsService, JwtUtil)
│   └── util/                               #   工具类 (Result, RateLimiter, PasswordGenerator)
│
├── config/                                 # 配置类
│   ├── AiConfig.java                       #   AI/LLM 多模型配置
│   ├── MilvusConfig.java                   #   Milvus 向量数据库 + Embedding 配置
│   ├── data/                               #   数据层配置
│   │   ├── RedisConfig.java                #     Redis 序列化配置
│   │   ├── MybatisPlusConfig.java          #     MyBatis-Plus 分页 + MapperScan
│   │   └── DruidConfig.java                #     Druid 连接池监控
│   ├── web/                                #   Web 层配置
│   │   ├── WebConfig.java                  #     CORS、拦截器注册
│   │   ├── WebSocketConfig.java            #     STOMP WebSocket 端点
│   │   └── RestTemplateConfig.java         #     HTTP 客户端
│   ├── security/                           #   安全配置
│   │   └── SecurityConfig.java             #     Spring Security 过滤链
│   └── monitor/                            #   监控配置
│       ├── ActuatorConfig.java             #     健康检查指示器
│       └── SchedulerConfig.java            #     定时任务线程池
│
├── controller/                             # REST 控制器 (16 个)
│   ├── AiController.java                   #   /api/ai          LLM 对话/补全
│   ├── AgentController.java                #   /api/agent       Agent 链/工具调用
│   ├── AuthController.java                 #   /api/auth        登录/注册
│   ├── KnowledgeController.java            #   /api/knowledge   知识库 CRUD/检索
│   ├── LocalModelController.java           #   /api/local-model Ollama 模型管理
│   ├── MessageController.java              #   /api/message     消息 CRUD/收藏/搜索
│   ├── MilvusController.java               #   /api/milvus      向量操作
│   ├── MonitorController.java              #   /api/monitor     系统监控
│   ├── MultimediaController.java           #   /api/multimedia  OCR/图像处理
│   ├── SessionController.java              #   /api/session     会话 CRUD/分享/合并
│   ├── SessionWebSocketController.java     #   WebSocket        实时聊天
│   ├── SpeechController.java               #   /api/speech      ASR/TTS
│   ├── SystemController.java               #   /api/system      系统信息
│   ├── TaskController.java                 #   /api/task        异步任务队列
│   ├── VisionController.java               #   /api/vision      图像识别
│   └── WorkflowController.java             #   /api/workflow    工作流定义/执行
│
├── chat/                                   # 聊天/会话领域
│   ├── entity/          (9)                #   ChatSession, ChatMessage, Summary, Tag, Favorite, Share, Template, Multimodal, TokenUsage
│   ├── mapper/          (6)                #   MyBatis Mapper 接口
│   ├── repository/      (7)                #   数据访问仓储
│   ├── dto/             (7)                #   会话导出、合并、统计 DTO
│   ├── model/           (3)                #   ChatRequest, ChatResponse, MessageEditRequest
│   └── service/         (15)               #   ChatService, RagMemoryService, SessionSummary/Merge/Share/Template/Stats, Favorite, Search, TokenUsage, ExportImport, Cleanup 等
│
├── agent/                                  # Agent/工具链领域
│   ├── entity/          (4)                #   AgentTask, AgentTaskLog, ChainTemplate, ChainTemplateNode
│   ├── mapper/          (4)                #   MyBatis Mapper 接口
│   ├── repository/      (1)                #   ChainTemplateRepository
│   ├── dto/             (2)                #   ChainTemplate, TemplateNode
│   ├── service/         (8)                #   ReActAgent, AgentChain, ChainValidation/Optimization/Template, AgentTask, ToolTest
│   └── tool/            (19)               #   Tool 接口 + ToolManager + 16 个内置工具
│
├── knowledge/                              # 知识库领域
│   ├── entity/          (3)                #   KnowledgeBase, KnowledgeDocument, KnowledgeChunk
│   ├── mapper/          (3)                #   MyBatis Mapper 接口
│   ├── model/           (4)                #   VectorRecord, SearchRequest, CollectionRequest, FieldSchema
│   └── service/         (6)                #   KnowledgeBaseService, KnowledgeSearchService, MilvusService, MilvusNativeService, TextSplitterService, PdfParserService
│
├── workflow/                               # 工作流领域
│   ├── entity/          (3)                #   WorkflowDefinition, WorkflowInstance, WorkflowNodeInstance
│   ├── mapper/          (3)                #   MyBatis Mapper 接口
│   └── service/         (2)                #   WorkflowDefinitionService, WorkflowEngineService
│
├── multimodal/                             # 多媒体领域
│   └── service/         (5)                #   VisionService, SpeechRecognitionService, TextToSpeechService, OcrService, ImageGenerationService
│
├── system/                                 # 系统管理领域
│   ├── entity/          (5)                #   User, Role, Permission, UserRole, RolePermission
│   ├── mapper/          (3)                #   UserMapper, RoleMapper, PermissionMapper
│   ├── dto/             (5)                #   LoginRequest, LoginResponse, Ollama DTO
│   └── service/         (4)                #   AuthService, RateLimitService, PerformanceMonitorService, LocalModelService
│
└── task/                                   # 异步任务领域
    ├── entity/          (1)                #   AsyncTaskEntity
    ├── mapper/          (1)                #   AsyncTaskMapper
    └── service/         (2)                #   TaskQueueService, TaskExecutorService
```

**资源文件**

```
src/main/resources/
├── application.yml                         # 主配置文件
├── mapper/                                 # MyBatis XML (8 个)
│   ├── ChatSessionMapper.xml
│   ├── ChatMessageMapper.xml
│   ├── ChatSessionSummaryMapper.xml
│   ├── ChatSessionTagMapper.xml
│   ├── ChatMessageFavoriteMapper.xml
│   ├── AgentTaskMapper.xml
│   ├── AgentTaskLogMapper.xml
│   └── ChainTemplateMapper.xml
└── db/
    └── migration/                          # Flyway 数据库迁移 (5 个版本)
```

## API 接口一览

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | `/api/auth` | 登录/注册/Token |
| AI 聊天 | `/api/ai` | 对话/补全/流式 |
| 会话管理 | `/api/session` | 会话 CRUD/分享/合并/模板 |
| 消息 | `/api/message` | 消息 CRUD/收藏/搜索 |
| 知识库 | `/api/knowledge` | 知识库 CRUD/语义检索 |
| Agent | `/api/agent` | ReAct/工具链/多 Agent 协作 |
| 工作流 | `/api/workflow` | 工作流定义/执行 |
| Milvus | `/api/milvus` | 向量 CRUD/搜索 |
| 多媒体 | `/api/multimedia` | OCR/图像处理 |
| 语音 | `/api/speech` | ASR/TTS |
| 视觉 | `/api/vision` | 图像识别 |
| 本地模型 | `/api/local-model` | Ollama 模型管理 |
| 任务 | `/api/task` | 异步任务队列 |
| 监控 | `/api/monitor` | Token 用量/性能监控 |
| 系统 | `/api/system` | 系统信息/会话统计 |

**WebSocket**

```
ws://localhost:8081/ws/session/{sessionId}
```

## 外部依赖

### 必需

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存/会话 |

### 可选

| 服务 | 端口 | 说明 | 启用方式 |
|------|------|------|----------|
| Milvus | 19530 | 向量数据库 | `milvus.enabled: true` |
| Ollama | 11434 | 本地 LLM | `langchain4j.ollama.enabled: true` |
| Tesseract | - | OCR 引擎 | `ocr.enabled: true` |
| Whisper | - | 语音识别 | `asr.enabled: true` |
| Edge TTS | - | 语音合成 | `tts.enabled: true` |

## License

MIT License
