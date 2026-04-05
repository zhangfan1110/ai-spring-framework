-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `name` VARCHAR(128) NOT NULL COMMENT '知识库名称',
  `description` TEXT DEFAULT NULL COMMENT '知识库描述',
  `type` VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT '类型：PUBLIC/PRIVATE',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人 ID',
  `document_count` INT DEFAULT 0 COMMENT '文档数量',
  `collection_name` VARCHAR(128) DEFAULT NULL COMMENT 'Milvus 集合名称',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 知识文档表
CREATE TABLE IF NOT EXISTS `knowledge_document` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `knowledge_base_id` VARCHAR(64) NOT NULL COMMENT '知识库 ID',
  `title` VARCHAR(256) DEFAULT NULL COMMENT '文档标题',
  `content` LONGTEXT DEFAULT NULL COMMENT '文档内容',
  `doc_type` VARCHAR(16) DEFAULT NULL COMMENT '文档类型：TEXT/PDF/WORD/MD',
  `file_path` VARCHAR(512) DEFAULT NULL COMMENT '文件路径',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
  `vector_status` VARCHAR(16) DEFAULT 'PENDING' COMMENT '向量化状态',
  `uploaded_by` VARCHAR(64) DEFAULT NULL COMMENT '上传人 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_kb_id` (`knowledge_base_id`),
  KEY `idx_vector_status` (`vector_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文档表';

-- 知识分块表
CREATE TABLE IF NOT EXISTS `knowledge_chunk` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `document_id` VARCHAR(64) NOT NULL COMMENT '文档 ID',
  `knowledge_base_id` VARCHAR(64) NOT NULL COMMENT '知识库 ID',
  `content` TEXT NOT NULL COMMENT '分块内容',
  `chunk_index` INT NOT NULL COMMENT '分块索引',
  `vector_id` VARCHAR(64) DEFAULT NULL COMMENT 'Milvus 向量 ID',
  `metadata` TEXT DEFAULT NULL COMMENT '元数据（JSON）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_kb_id` (`knowledge_base_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识分块表';
