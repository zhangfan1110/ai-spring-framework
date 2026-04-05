# 知识库管理说明

## 📚 功能概述

基于 Milvus 向量数据库的知识库管理系统，支持：

- ✅ 知识库创建和管理
- ✅ 文档上传和解析
- ✅ 文本自动分块
- ✅ 向量化存储
- ✅ 语义搜索
- ✅ 混合搜索（语义 + 关键词）

---

## 🏗️ 架构设计

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│  上传文档    │ ───> │ 文本分块处理  │ ───> │  向量化     │
│  (PDF/Word)  │      │  (Chunking)  │      │ (Embedding) │
└─────────────┘      └──────────────┘      └─────────────┘
                                                 │
                                                 ▼
                                          ┌──────────────┐
                                          │   Milvus     │
                                          │ (向量数据库)  │
                                          └──────────────┘
                                                 │
                                                 ▼
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│  搜索结果    │ <─── │  相似度排序   │ <─── │  向量检索    │
└─────────────┘      └──────────────┘      └─────────────┘
```

---

## 📝 使用方式

### 1. 创建知识库

```bash
POST /api/knowledge/base?name=产品知识库&description=产品相关文档&type=PRIVATE

# 响应
{
  "code": 200,
  "data": {
    "id": "kb-uuid-xxx",
    "name": "产品知识库",
    "type": "PRIVATE",
    "documentCount": 0,
    "collectionName": "kb_kb_uuid_xxx"
  }
}
```

### 2. 上传文档

```bash
POST /api/knowledge/base/{kbId}/document?title=产品手册
Content-Type: multipart/form-data

file: [binary]
```

支持的文件类型：
- `.txt` - 纯文本
- `.md` - Markdown
- `.pdf` - PDF 文档
- `.doc/.docx` - Word 文档

### 3. 搜索知识

```bash
GET /api/knowledge/base/{kbId}/search?query=如何安装产品&topK=10

# 响应
{
  "code": 200,
  "data": [
    {
      "chunkId": "chunk-xxx",
      "content": "安装步骤如下：1. 下载安装包...",
      "score": 0.95,
      "metadata": {
        "documentId": "doc-xxx",
        "title": "产品安装手册"
      }
    }
  ]
}
```

### 4. 查询知识库列表

```bash
GET /api/knowledge/base/list?userId=user-123
```

### 5. 获取知识库统计

```bash
GET /api/knowledge/base/{kbId}/stats

# 响应
{
  "code": 200,
  "data": {
    "knowledgeBase": {...},
    "documentCount": 15
  }
}
```

---

## 🎯 核心概念

### 知识库 (Knowledge Base)

知识库是文档的集合，支持：
- **PUBLIC** - 公开知识库，所有用户可访问
- **PRIVATE** - 私有知识库，仅创建者可访问

### 文档 (Document)

上传到知识库的文档，支持多种格式：
- 文本文件（.txt）
- Markdown（.md）
- PDF 文档
- Word 文档

### 分块 (Chunk)

文档被自动分割成小块，便于向量化和检索：
- 默认块大小：500 字符
- 重叠大小：50 字符
- 每个分块独立向量化

### 向量化 (Embedding)

使用 Embedding 模型将文本转换为向量：
- 模型：text-embedding-ada-002 或本地模型
- 维度：1536 维
- 存储：Milvus 向量数据库

---

## 🔧 配置参数

### 文本分块配置

```yaml
knowledge:
  chunk:
    size: 500        # 块大小（字符）
    overlap: 50      # 重叠大小（字符）
    separators: ["\n\n", "\n", "。", "！", "？"]  # 分隔符
```

### 向量化配置

```yaml
knowledge:
  embedding:
    model: text-embedding-ada-002
    dimensions: 1536
    batch-size: 100
```

### Milvus 配置

```yaml
milvus:
  host: localhost
  port: 19530
  knowledge-collection-prefix: kb_
```

---

## 📊 数据库表

### knowledge_base

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 知识库 ID |
| name | VARCHAR(128) | 知识库名称 |
| description | TEXT | 描述 |
| type | VARCHAR(16) | 类型 |
| created_by | VARCHAR(64) | 创建人 |
| document_count | INT | 文档数量 |
| collection_name | VARCHAR(128) | Milvus 集合名 |

### knowledge_document

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 文档 ID |
| knowledge_base_id | VARCHAR(64) | 知识库 ID |
| title | VARCHAR(256) | 文档标题 |
| content | LONGTEXT | 文档内容 |
| doc_type | VARCHAR(16) | 文档类型 |
| file_path | VARCHAR(512) | 文件路径 |
| file_size | BIGINT | 文件大小 |
| chunk_count | INT | 分块数量 |
| vector_status | VARCHAR(16) | 向量化状态 |

### knowledge_chunk

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 分块 ID |
| document_id | VARCHAR(64) | 文档 ID |
| knowledge_base_id | VARCHAR(64) | 知识库 ID |
| content | TEXT | 分块内容 |
| chunk_index | INT | 分块索引 |
| vector_id | VARCHAR(64) | Milvus 向量 ID |
| metadata | TEXT | 元数据（JSON） |

---

## 🎨 使用场景

### 1. 产品知识库

```bash
# 创建产品知识库
POST /api/knowledge/base?name=产品知识库&type=PRIVATE

# 上传产品文档
POST /api/knowledge/base/{kbId}/document
- 产品手册.pdf
- 安装指南.md
- FAQ.txt

# 搜索
GET /api/knowledge/base/{kbId}/search?query=如何安装
```

### 2. 客服知识库

```bash
# 创建客服知识库
POST /api/knowledge/base?name=客服知识库&type=PUBLIC

# 上传客服文档
- 常见问题.md
- 解决方案库.txt
- 话术模板.docx

# 客服机器人集成
# 用户提问 → 搜索知识库 → 返回答案
```

### 3. 个人笔记

```bash
# 创建个人笔记库
POST /api/knowledge/base?name=我的笔记&type=PRIVATE

# 上传笔记
- 学习笔记.md
- 会议纪要.txt

# 快速检索
GET /api/knowledge/base/{kbId}/search?query=机器学习
```

---

## 🔍 搜索功能

### 语义搜索

基于向量相似度的搜索：
- 理解查询意图
- 返回语义相关的结果
- 不依赖关键词匹配

### 混合搜索（待实现）

结合语义搜索和关键词搜索：
- BM25 关键词匹配
- 向量相似度
- 加权排序

---

## ⚙️ 异步处理流程

文档上传后的处理流程：

```
1. 上传文档 → PENDING 状态
2. 异步任务队列获取文档
3. 解析文档内容
4. 文本分块
5. 向量化处理
6. 存储到 Milvus
7. 更新状态为 DONE
```

---

## ⚠️ 注意事项

1. **文件大小限制**：建议单个文件 < 50MB
2. **向量化耗时**：大文档可能需要较长时间
3. **Milvus 依赖**：确保 Milvus 服务可用
4. **存储空间**：向量数据占用较大空间

---

## 🚀 扩展建议

- [ ] 支持更多文档格式（Excel/PPT）
- [ ] 文档版本管理
- [ ] 知识图谱集成
- [ ] 自动标签生成
- [ ] 文档权限管理
- [ ] 搜索历史记录
- [ ] 热门搜索分析
