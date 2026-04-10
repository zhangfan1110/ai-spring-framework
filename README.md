# AI Spring Framework 🦐

基于 Spring Boot 3 + LangChain4j 的企业级 AI 应用框架，集成通义千问、Ollama 本地模型、向量数据库、RAG 记忆检索、多模态处理等能力。

## 📋 目录

- [功能特性](#-功能特性)
- [技术栈](#-技术栈)
- [快速开始](#-快速开始)
- [外部依赖配置](#-外部依赖配置)
- [项目结构](#-项目结构)
- [API 文档](#-api-文档)
- [常见问题](#-常见问题)

---

## ✨ 功能特性

### 核心能力
- **LLM 集成**: 通义千问 (云端) + Ollama (本地开源模型)
- **RAG 记忆检索**: 向量相似度检索 + 关键词检索 + Cross-Encoder 重排序
- **会话管理**: 聊天会话、消息收藏、会话分享、会话合并、会话模板
- **Agent 工作流**: 可配置的任务链、ReAct 模式、工具调用
- **多模态处理**: 图像识别、OCR、语音识别 (ASR)、语音合成 (TTS)

### 工具系统
内置 16+ 工具：
- `CalculatorTool` - 数学计算
- `CodeExecutorTool` - 代码执行
- `DatabaseTool` - 数据库操作
- `DateTimeTool` - 日期时间
- `EmailTool` - 邮件发送
- `FileTool` - 文件操作
- `HttpTool` - HTTP 请求
- `ImageTool` - 图片处理
- `NewsTool` - 新闻查询
- `PdfTool` - PDF 解析
- `SearchTool` - 网络搜索
- `ShellTool` - Shell 命令
- `StockTool` - 股票查询
- `TranslationTool` - 翻译
- `WeatherTool` - 天气查询
- `ToolManager` - 工具管理器

### 企业特性
- JWT 认证 + Spring Security
- Redis 会话缓存
- Druid 数据库连接池监控
- MyBatis Plus ORM
- Swagger/OpenAPI 文档
- Actuator 监控端点

---

## 🛠️ 技术栈

| 组件 | 版本/技术 |
|------|-----------|
| Java | 21 |
| Spring Boot | 3.2.4 |
| LangChain4j | 0.30.0 |
| MyBatis Plus | 3.5.6 |
| MySQL | 8.0 |
| Redis | 7 |
| Milvus | 向量数据库 |
| Lombok | 1.18.34 |

---

## 🚀 快速开始

### 1. 环境要求

```bash
# Java 21
java -version

# Maven 3.8+
mvn -version

# Docker & Docker Compose (可选，用于容器化部署)
docker --version
docker-compose --version
```

### 2. 启动基础设施

```bash
# 使用 Docker Compose 启动 MySQL + Redis
cd /Users/zhangfan/Desktop/ai-spring-framework
docker-compose up -d mysql redis
```

### 3. 配置环境变量

```bash
# 复制配置模板
cp src/main/resources/application.yml src/main/resources/application-local.yml

# 编辑配置 (可选，默认配置已可用)
vim src/main/resources/application-local.yml
```

### 4. 运行应用

```bash
# 方式一：Maven 运行
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package -DskipTests
java -jar target/ai-spring-framework-1.0.0-SNAPSHOT.jar

# 方式三：IDE 直接运行 Application.java
```

### 5. 验证启动

访问：
- API 文档：http://localhost:8081/swagger-ui.html
- Druid 监控：http://localhost:8081/druid/ (账号：admin / 密码：admin123)
- 健康检查：http://localhost:8081/actuator/health

---

## ⚙️ 外部依赖配置

### 必需服务

| 服务 | 端口 | 说明 | 配置项 |
|------|------|------|--------|
| MySQL | 3306 | 数据库 | `spring.datasource.*` |
| Redis | 6379 | 缓存/会话 | `spring.data.redis.*` |

### 可选服务

| 服务 | 端口 | 说明 | 配置项 |
|------|------|------|--------|
| Milvus | 19530 | 向量数据库 | `milvus.*` |
| Ollama | 11434 | 本地 LLM | `langchain4j.ollama.*` |

### 🔧 功能模块依赖

#### 1. OCR 文字识别

**支持引擎**: Tesseract (默认) / PaddleOCR / EasyOCR / Azure

```yaml
ocr:
  enabled: true
  provider: tesseract  # tesseract/paddle/easyocr/azure
  lang: chi_sim+eng    # 语言包
```

**安装步骤**:

```bash
# macOS - 安装 Tesseract
brew install tesseract
brew install tesseract-lang  # 多语言支持

# 验证安装
tesseract --version
tesseract --list-langs

# Linux (Ubuntu/Debian)
sudo apt install tesseract-ocr
sudo apt install tesseract-ocr-chi-sim tesseract-ocr-chi-tra tesseract-ocr-eng

# 可选：安装 PaddleOCR (中文效果更好)
pip install paddlepaddle paddleocr

# 可选：安装 EasyOCR
pip install easyocr
```

**语言包对照表**:

| 语言 | Tesseract | PaddleOCR | EasyOCR |
|------|-----------|-----------|---------|
| 简体中文 | `chi_sim` | `ch` | `ch_sim` |
| 繁体中文 | `chi_tra` | `ch` | `ch_tra` |
| 英文 | `eng` | `en` | `en` |
| 日文 | `jpn` | `ja` | `ja` |
| 韩文 | `kor` | `ko` | `ko` |

---

#### 2. 语音识别 (ASR)

**支持引擎**: Whisper (本地) / Whisper API

```yaml
asr:
  enabled: true
  provider: whisper-local  # whisper-local / whisper-api
  model: tiny              # tiny/base/small/medium/large
  language: zh             # zh/en/ja/ko 等
```

**安装步骤**:

```bash
# 安装 OpenAI Whisper
pip install openai-whisper

# 验证安装
whisper --version

# 测试识别
whisper audio.mp3 --model tiny --language zh
```

**模型大小参考**:

| 模型 | 大小 | 速度 | 准确率 |
|------|------|------|--------|
| tiny | 39M | 最快 | 一般 |
| base | 74M | 快 | 较好 |
| small | 244M | 中等 | 好 |
| medium | 769M | 慢 | 很好 |
| large | 1550M | 最慢 | 最佳 |

---

#### 3. 语音合成 (TTS)

**支持引擎**: Edge TTS (免费) / Azure / Google / 百度

```yaml
tts:
  enabled: true
  provider: edge  # edge/azure/google/baidu
  voice: zh-CN-XiaoxiaoNeural
  rate: 1.0       # 语速 0.5-2.0
  pitch: 1.0      # 音调 0.5-2.0
```

**安装步骤**:

```bash
# 安装 Edge TTS (推荐，免费)
pip install edge-tts

# 验证安装
edge-tts --version

# 查看支持的语音
edge-tts --list-voices

# 测试合成
edge-tts --voice zh-CN-XiaoxiaoNeural --text "你好，世界" --write-media test.mp3
```

**常用中文语音**:

| 语音 ID | 性别 | 特点 |
|---------|------|------|
| `zh-CN-XiaoxiaoNeural` | 女 | 温暖、自然 |
| `zh-CN-YunxiNeural` | 男 | 沉稳、专业 |
| `zh-CN-YunyangNeural` | 男 | 新闻播报风格 |
| `zh-CN-XiaoyiNeural` | 女 | 活泼 |
| `zh-CN-Liaoning-XiaobeiNeural` | 女 | 东北话 |
| `zh-CN-Shaanxi-XiaoniNeural` | 女 | 陕西话 |
| `zh-HK-HiuMaanNeural` | 女 | 粤语 |

---

#### 4. 图像生成

```yaml
image-gen:
  enabled: true
  provider: dall-e-3  # dall-e-3/stable-diffusion/midjourney
  size: 1024x1024
  quality: standard
  style: vivid
```

**配置说明**:

- **DALL-E 3**: 使用通义千问兼容接口，无需额外配置
- **Stable Diffusion**: 需本地部署 SD WebUI 或 API
- **Midjourney**: 需 Discord Bot 集成 (待实现)

---

#### 5. 视觉识别 (Vision)

```yaml
langchain4j:
  qwen:
    vision-model: qwen-vl-max
    vision-base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  
  ollama:
    vision-model: llava  # 本地视觉模型
```

**本地视觉模型**:

```bash
# 拉取 LLaVA 模型
ollama pull llava

# 或使用 qwen-vl
ollama pull qwen-vl
```

---

#### 6. 向量数据库 (Milvus)

```yaml
milvus:
  enabled: true
  host: localhost
  port: 19530
  username: admin
  password: sdlkg007
```

**启动 Milvus**:

```bash
# Docker 方式
docker run -d \
  --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  -v $(pwd)/milvus:/var/lib/milvus \
  milvusdb/milvus:latest \
  milvus run standalone
```

---

#### 7. Ollama 本地模型

```yaml
langchain4j:
  ollama:
    enabled: true
    base-url: http://localhost:11434
    model-name: deepseek-r1  # 文本模型
    vision-model: llava      # 视觉模型
```

**安装 Ollama**:

```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.com/install.sh | sh

# 启动服务
ollama serve

# 拉取模型
ollama pull deepseek-r1      # 推理模型
ollama pull qwen2.5          # 通用模型
ollama pull llava            # 视觉模型
ollama pull nomic-embed-text # Embedding 模型
```

---

## 📁 项目结构

```
ai-spring-framework/
├── src/main/java/com/example/aiframework/
│   ├── Application.java          # 启动类
│   ├── annotation/               # 自定义注解
│   ├── config/                   # 配置类
│   │   ├── DruidConfig.java
│   │   ├── SecurityConfig.java
│   │   └── ...
│   ├── controller/               # REST 控制器
│   │   ├── AgentController.java
│   │   ├── AiController.java
│   │   ├── AuthController.java
│   │   ├── KnowledgeController.java
│   │   ├── LocalModelController.java
│   │   ├── MessageController.java
│   │   ├── MilvusController.java
│   │   ├── MonitorController.java
│   │   ├── MultimediaController.java  # 多媒体 (OCR/ASR/TTS)
│   │   ├── SessionController.java
│   │   ├── SessionWebSocketController.java
│   │   ├── SpeechController.java      # 语音相关
│   │   ├── SystemController.java
│   │   ├── TaskController.java
│   │   ├── VisionController.java      # 视觉识别
│   │   └── WorkflowController.java
│   ├── dto/                      # 数据传输对象
│   ├── entity/                   # 实体类
│   ├── exception/                # 异常处理
│   ├── interceptor/              # 拦截器
│   ├── mapper/                   # MyBatis Mapper
│   ├── model/                    # AI 模型封装
│   ├── repository/               # 数据访问层
│   ├── security/                 # 安全相关
│   ├── service/                  # 业务逻辑层
│   │   ├── OcrService.java           # OCR 服务
│   │   ├── SpeechRecognitionService.java  # ASR 服务
│   │   ├── TextToSpeechService.java     # TTS 服务
│   │   ├── VisionService.java          # 视觉服务
│   │   ├── ImageGenerationService.java # 图像生成
│   │   ├── KnowledgeBaseService.java   # 知识库
│   │   ├── RagMemoryService.java       # RAG 记忆
│   │   ├── MilvusService.java          # 向量操作
│   │   ├── ChatService.java            # 聊天
│   │   ├── AuthService.java            # 认证
│   │   └── ... (40+ 服务类)
│   ├── tool/                     # Agent 工具
│   │   ├── Tool.java
│   │   ├── ToolManager.java
│   │   ├── CalculatorTool.java
│   │   ├── SearchTool.java
│   │   ├── WeatherTool.java
│   │   └── ... (16+ 工具)
│   └── util/                     # 工具类
│       ├── JwtUtil.java
│       ├── Result.java
│       └── ...
├── src/main/resources/
│   ├── application.yml           # 主配置文件
│   └── mapper/**/*.xml           # MyBatis XML
├── uploads/                      # 上传文件目录
│   ├── audio/
│   ├── ocr/
│   ├── images/
│   └── generated/
├── docs/                         # 文档
├── docker-compose.yml            # Docker 编排
├── Dockerfile                    # Docker 镜像
├── pom.xml                       # Maven 配置
└── README.md                     # 本文件
```

---

## 📖 API 文档

### 主要接口

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证 | `/api/auth` | 登录/注册/Token |
| AI 聊天 | `/api/ai` | 对话/补全 |
| 会话管理 | `/api/session` | 会话 CRUD/分享/合并 |
| 消息 | `/api/message` | 消息 CRUD/收藏/搜索 |
| 知识库 | `/api/knowledge` | 知识库/检索/RAG |
| 本地模型 | `/api/local-model` | Ollama 模型管理 |
| Milvus | `/api/milvus` | 向量操作 |
| 多媒体 | `/api/multimedia` | OCR/图片处理 |
| 语音 | `/api/speech` | ASR/TTS |
| 视觉 | `/api/vision` | 图像识别 |
| 工作流 | `/api/workflow` | 工作流定义/执行 |
| 任务 | `/api/task` | 任务队列 |
| Agent | `/api/agent` | Agent 链/工具 |
| 监控 | `/api/monitor` | 系统监控 |
| 系统 | `/api/system` | 系统信息 |

### WebSocket

```
ws://localhost:8081/ws/session/{sessionId}
```

实时聊天消息推送。

---

## ❓ 常见问题

### Q1: OCR 识别失败，提示"Tesseract 未安装"

```bash
# macOS
brew install tesseract tesseract-lang

# 验证
tesseract --version

# 安装中文语言包
# tesseract-lang 已包含所有语言
```

### Q2: 语音识别报错"Whisper 未安装"

```bash
pip install openai-whisper

# 如果 pip 找不到命令
pip3 install openai-whisper

# 或使用 conda
conda install -c conda-forge openai-whisper
```

### Q3: TTS 合成失败

```bash
# 安装 Edge TTS
pip install edge-tts

# 测试
edge-tts --voice zh-CN-XiaoxiaoNeural --text "测试" --write-media test.mp3
```

### Q4: Milvus 连接失败

```bash
# 检查 Milvus 是否运行
docker ps | grep milvus

# 重启 Milvus
docker restart milvus-standalone

# 检查防火墙
telnet localhost 19530
```

### Q5: Ollama 模型拉取慢

```bash
# 使用国内镜像
export OLLAMA_MIRROR=https://ollama.ainav.cn

# 或手动下载模型文件后导入
```

### Q6: 数据库连接失败

```bash
# 检查 MySQL 是否运行
docker ps | grep mysql

# 检查配置
# application.yml 中的数据库密码是否匹配

# 创建数据库
docker exec -it ai-framework-mysql mysql -uroot -pMyNewPass123! -e "CREATE DATABASE IF NOT EXISTS ai_framework;"
```

---

## 📝 开发笔记

### 添加新工具

1. 实现 `Tool` 接口
2. 在 `ToolManager` 中注册
3. 在 Agent 链中配置使用

### 添加新 LLM 提供商

1. 在 `application.yml` 添加配置
2. 创建对应的 `AiService` 实现
3. 更新 `LangChain4jConfig`

---

## 📄 License

MIT License

---

## 🙏 致谢

- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [通义千问](https://dashscope.aliyun.com/)
- [Ollama](https://ollama.com/)
- [Milvus](https://milvus.io/)

---

_最后更新：2026-04-09_
