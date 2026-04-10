-- =====================================================
-- AI Spring Framework - 数据库初始化脚本
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4
-- =====================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS ai_framework 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_framework;

-- =====================================================
-- 1. 系统管理模块
-- =====================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` VARCHAR(64) NOT NULL COMMENT '用户 ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` VARCHAR(64) NOT NULL COMMENT '角色 ID',
    `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` VARCHAR(64) NOT NULL COMMENT '权限 ID',
    `permission_code` VARCHAR(128) NOT NULL COMMENT '权限编码',
    `permission_name` VARCHAR(128) NOT NULL COMMENT '权限名称',
    `resource_type` VARCHAR(32) DEFAULT NULL COMMENT '资源类型：MENU/BUTTON/API',
    `parent_id` VARCHAR(64) DEFAULT NULL COMMENT '父权限 ID',
    `path` VARCHAR(256) DEFAULT NULL COMMENT '资源路径',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` VARCHAR(64) NOT NULL COMMENT 'ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户 ID',
    `role_id` VARCHAR(64) NOT NULL COMMENT '角色 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` VARCHAR(64) NOT NULL COMMENT 'ID',
    `role_id` VARCHAR(64) NOT NULL COMMENT '角色 ID',
    `permission_id` VARCHAR(64) NOT NULL COMMENT '权限 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- =====================================================
-- 2. AI 聊天模块
-- =====================================================

-- 会话表
CREATE TABLE IF NOT EXISTS `chat_sessions` (
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户 ID',
    `title` VARCHAR(256) DEFAULT NULL COMMENT '会话标题',
    `message_count` INT DEFAULT 0 COMMENT '消息数量',
    `last_active_time` DATETIME DEFAULT NULL COMMENT '最后活跃时间',
    `parent_id` VARCHAR(64) DEFAULT NULL COMMENT '父会话 ID（用于分支）',
    `template_id` VARCHAR(64) DEFAULT NULL COMMENT '使用的模板 ID',
    `model_name` VARCHAR(64) DEFAULT NULL COMMENT '使用的模型名称',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-已删除 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`session_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_last_active` (`last_active_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS `chat_messages` (
    `id` VARCHAR(64) NOT NULL COMMENT '消息 ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
    `role` VARCHAR(32) NOT NULL COMMENT '消息角色：USER/AI/SYSTEM',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `model` VARCHAR(64) DEFAULT NULL COMMENT '模型名称',
    `tokens` INT DEFAULT NULL COMMENT '令牌数',
    `parent_id` VARCHAR(64) DEFAULT NULL COMMENT '父消息 ID（用于多轮对话）',
    `tool_calls` JSON DEFAULT NULL COMMENT '工具调用记录',
    `tool_call_results` JSON DEFAULT NULL COMMENT '工具调用结果',
    `edited` TINYINT DEFAULT 0 COMMENT '是否被编辑过',
    `edited_at` DATETIME DEFAULT NULL COMMENT '编辑时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否已删除（软删除）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- 消息收藏表
CREATE TABLE IF NOT EXISTS `chat_message_favorites` (
    `id` VARCHAR(64) NOT NULL COMMENT 'ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户 ID',
    `message_id` VARCHAR(64) NOT NULL COMMENT '消息 ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
    `note` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_message` (`user_id`, `message_id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息收藏表';

-- 会话分享表
CREATE TABLE IF NOT EXISTS `chat_session_shares` (
    `share_id` VARCHAR(64) NOT NULL COMMENT '分享 ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
    `share_token` VARCHAR(128) NOT NULL COMMENT '分享令牌',
    `share_title` VARCHAR(256) DEFAULT NULL COMMENT '分享标题',
    `share_description` VARCHAR(512) DEFAULT NULL COMMENT '分享描述',
    `is_public` TINYINT DEFAULT 0 COMMENT '是否公开',
    `access_password` VARCHAR(64) DEFAULT NULL COMMENT '访问密码',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
    `view_count` INT DEFAULT 0 COMMENT '访问次数',
    `max_views` INT DEFAULT NULL COMMENT '最大访问次数',
    `creator_id` VARCHAR(64) DEFAULT NULL COMMENT '创建者 ID',
    `disabled` TINYINT DEFAULT 0 COMMENT '是否已禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`share_id`),
    UNIQUE KEY `uk_share_token` (`share_token`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话分享表';

-- 会话摘要表
CREATE TABLE IF NOT EXISTS `chat_session_summaries` (
    `id` VARCHAR(64) NOT NULL COMMENT '摘要 ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
    `summary` TEXT NOT NULL COMMENT '摘要内容',
    `key_points` JSON DEFAULT NULL COMMENT '关键信息',
    `start_message_id` VARCHAR(64) DEFAULT NULL COMMENT '起始消息 ID',
    `end_message_id` VARCHAR(64) DEFAULT NULL COMMENT '结束消息 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话摘要表';

-- 会话标签表
CREATE TABLE IF NOT EXISTS `chat_session_tags` (
    `id` VARCHAR(64) NOT NULL COMMENT 'ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
    `tag_name` VARCHAR(64) NOT NULL COMMENT '标签名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话标签表';

-- Token 使用统计表
CREATE TABLE IF NOT EXISTS `token_usage` (
    `id` VARCHAR(64) NOT NULL COMMENT 'ID',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户 ID',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '会话 ID',
    `model_name` VARCHAR(64) NOT NULL COMMENT '模型名称',
    `prompt_tokens` INT DEFAULT 0 COMMENT '输入令牌数',
    `completion_tokens` INT DEFAULT 0 COMMENT '输出令牌数',
    `total_tokens` INT DEFAULT 0 COMMENT '总令牌数',
    `usage_date` DATE NOT NULL COMMENT '使用日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_usage_date` (`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token 使用统计表';

-- =====================================================
-- 3. 知识库模块
-- =====================================================

-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` VARCHAR(64) NOT NULL COMMENT '知识库 ID',
    `name` VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `type` VARCHAR(32) DEFAULT 'PRIVATE' COMMENT '类型：PUBLIC/PRIVATE',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人 ID',
    `document_count` INT DEFAULT 0 COMMENT '文档数量',
    `collection_name` VARCHAR(128) DEFAULT NULL COMMENT '向量集合名称',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 知识文档表
CREATE TABLE IF NOT EXISTS `knowledge_document` (
    `id` VARCHAR(64) NOT NULL COMMENT '文档 ID',
    `knowledge_id` VARCHAR(64) NOT NULL COMMENT '知识库 ID',
    `title` VARCHAR(256) NOT NULL COMMENT '文档标题',
    `content` LONGTEXT COMMENT '文档内容',
    `file_path` VARCHAR(512) DEFAULT NULL COMMENT '文件路径',
    `file_type` VARCHAR(32) DEFAULT NULL COMMENT '文件类型：PDF/WORD/EXCEL/TXT',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_knowledge_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文档表';

-- 知识分块表（用于 RAG 检索）
CREATE TABLE IF NOT EXISTS `knowledge_chunk` (
    `id` VARCHAR(64) NOT NULL COMMENT '分块 ID',
    `document_id` VARCHAR(64) NOT NULL COMMENT '文档 ID',
    `knowledge_id` VARCHAR(64) NOT NULL COMMENT '知识库 ID',
    `content` TEXT NOT NULL COMMENT '分块内容',
    `chunk_index` INT NOT NULL COMMENT '分块索引',
    `vector_id` VARCHAR(128) DEFAULT NULL COMMENT '向量 ID（Milvus）',
    `metadata` JSON DEFAULT NULL COMMENT '元数据',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_knowledge_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识分块表';

-- =====================================================
-- 4. Agent/工作流模块
-- =====================================================

-- Agent 链模板表
CREATE TABLE IF NOT EXISTS `chain_templates` (
    `id` VARCHAR(64) NOT NULL COMMENT '模板 ID',
    `name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `category` VARCHAR(64) DEFAULT NULL COMMENT '分类',
    `estimated_duration` INT DEFAULT NULL COMMENT '预计执行时长（秒）',
    `difficulty` INT DEFAULT 1 COMMENT '难度等级 1-5',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `is_builtin` TINYINT DEFAULT 0 COMMENT '是否内置',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人 ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 链模板表';

-- Agent 链模板节点表
CREATE TABLE IF NOT EXISTS `chain_template_nodes` (
    `id` VARCHAR(64) NOT NULL COMMENT '节点 ID',
    `template_id` VARCHAR(64) NOT NULL COMMENT '模板 ID',
    `node_id` VARCHAR(64) NOT NULL COMMENT '节点标识',
    `node_type` VARCHAR(64) NOT NULL COMMENT '节点类型',
    `node_name` VARCHAR(128) DEFAULT NULL COMMENT '节点名称',
    `node_config` JSON NOT NULL COMMENT '节点配置',
    `parent_node_id` VARCHAR(64) DEFAULT NULL COMMENT '父节点 ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 链模板节点表';

-- Agent 任务表
CREATE TABLE IF NOT EXISTS `agent_task` (
    `id` VARCHAR(64) NOT NULL COMMENT '任务 ID',
    `template_id` VARCHAR(64) DEFAULT NULL COMMENT '模板 ID',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '会话 ID',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户 ID',
    `input` TEXT COMMENT '输入内容',
    `output` TEXT COMMENT '输出结果',
    `status` VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/SUCCESS/FAILED/CANCELLED',
    `progress` INT DEFAULT 0 COMMENT '进度百分比',
    `error_message` TEXT COMMENT '错误信息',
    `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 任务表';

-- Agent 任务日志表
CREATE TABLE IF NOT EXISTS `agent_task_log` (
    `id` VARCHAR(64) NOT NULL COMMENT '日志 ID',
    `task_id` VARCHAR(64) NOT NULL COMMENT '任务 ID',
    `node_id` VARCHAR(64) DEFAULT NULL COMMENT '节点 ID',
    `action` VARCHAR(64) DEFAULT NULL COMMENT '动作',
    `input` TEXT COMMENT '输入',
    `output` TEXT COMMENT '输出',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '耗时（毫秒）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 任务日志表';

-- 工作流定义表
CREATE TABLE IF NOT EXISTS `workflow_definition` (
    `id` VARCHAR(64) NOT NULL COMMENT 'ID',
    `workflow_code` VARCHAR(64) NOT NULL COMMENT '工作流编码',
    `workflow_name` VARCHAR(128) NOT NULL COMMENT '工作流名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `version` VARCHAR(32) DEFAULT '1.0.0' COMMENT '版本号',
    `workflow_config` JSON NOT NULL COMMENT '工作流配置',
    `status` VARCHAR(32) DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人 ID',
    `published_by` VARCHAR(64) DEFAULT NULL COMMENT '发布人 ID',
    `published_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_code_version` (`workflow_code`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流定义表';

-- 工作流实例表
CREATE TABLE IF NOT EXISTS `workflow_instance` (
    `id` VARCHAR(64) NOT NULL COMMENT '实例 ID',
    `workflow_id` VARCHAR(64) NOT NULL COMMENT '工作流 ID',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '会话 ID',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户 ID',
    `input_data` JSON COMMENT '输入数据',
    `output_data` JSON COMMENT '输出数据',
    `status` VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/SUCCESS/FAILED',
    `current_node` VARCHAR(64) DEFAULT NULL COMMENT '当前节点',
    `error_message` TEXT COMMENT '错误信息',
    `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流实例表';

-- 工作流节点实例表
CREATE TABLE IF NOT EXISTS `workflow_node_instance` (
    `id` VARCHAR(64) NOT NULL COMMENT 'ID',
    `instance_id` VARCHAR(64) NOT NULL COMMENT '实例 ID',
    `node_id` VARCHAR(64) NOT NULL COMMENT '节点 ID',
    `node_type` VARCHAR(64) DEFAULT NULL COMMENT '节点类型',
    `input_data` JSON COMMENT '输入数据',
    `output_data` JSON COMMENT '输出数据',
    `status` VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态',
    `error_message` TEXT COMMENT '错误信息',
    `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '耗时（毫秒）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流节点实例表';

-- =====================================================
-- 5. 多模态模块
-- =====================================================

-- 会话多模态记录表（OCR/语音/视觉）
CREATE TABLE IF NOT EXISTS `chat_session_multimodal` (
    `id` VARCHAR(64) NOT NULL COMMENT 'ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
    `message_id` VARCHAR(64) DEFAULT NULL COMMENT '消息 ID',
    `modality_type` VARCHAR(32) NOT NULL COMMENT '类型：OCR/ASR/TTS/VISION',
    `original_file` VARCHAR(512) DEFAULT NULL COMMENT '原始文件路径',
    `result_file` VARCHAR(512) DEFAULT NULL COMMENT '结果文件路径',
    `result_text` TEXT COMMENT '识别结果文本',
    `metadata` JSON DEFAULT NULL COMMENT '元数据',
    `status` VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态',
    `error_message` TEXT COMMENT '错误信息',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '耗时（毫秒）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话多模态记录表';

-- =====================================================
-- 6. 异步任务模块
-- =====================================================

-- 异步任务表
CREATE TABLE IF NOT EXISTS `async_task` (
    `id` VARCHAR(64) NOT NULL COMMENT '任务 ID',
    `task_type` VARCHAR(64) NOT NULL COMMENT '任务类型',
    `task_data` JSON NOT NULL COMMENT '任务数据',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `max_retries` INT DEFAULT 3 COMMENT '最大重试次数',
    `status` VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/SUCCESS/FAILED',
    `result` JSON DEFAULT NULL COMMENT '执行结果',
    `error_message` TEXT COMMENT '错误信息',
    `scheduled_time` DATETIME DEFAULT NULL COMMENT '计划执行时间',
    `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_scheduled_time` (`scheduled_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异步任务表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入默认管理员用户（密码：admin123，BCrypt 加密）
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`) 
VALUES ('1', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', '管理员', 'admin@example.com', 1)
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);

-- 插入默认角色
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`) VALUES
('1', 'ADMIN', '超级管理员', '系统超级管理员'),
('2', 'USER', '普通用户', '普通用户角色')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

-- 关联管理员角色
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) 
VALUES ('1', '1', '1')
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- 插入默认权限
INSERT INTO `sys_permission` (`id`, `permission_code`, `permission_name`, `resource_type`) VALUES
('1', 'chat:create', '创建会话', 'API'),
('2', 'chat:read', '查看会话', 'API'),
('3', 'chat:delete', '删除会话', 'API'),
('4', 'knowledge:manage', '管理知识库', 'API'),
('5', 'workflow:manage', '管理工作流', 'API'),
('6', 'agent:execute', '执行 Agent 任务', 'API'),
('7', 'system:monitor', '系统监控', 'MENU')
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- 关联管理员权限
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) 
SELECT CONCAT('rp', i, '1'), '1', id 
FROM (SELECT 1 as i UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) t, `sys_permission`
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);

-- =====================================================
-- 完成提示
-- =====================================================
SELECT '数据库初始化完成！' AS message;
