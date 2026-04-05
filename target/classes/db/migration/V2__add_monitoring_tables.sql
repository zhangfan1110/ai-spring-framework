-- Token 使用统计表
CREATE TABLE IF NOT EXISTS `ai_token_usage` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户 ID',
  `session_id` VARCHAR(64) DEFAULT NULL COMMENT '会话 ID',
  `model` VARCHAR(128) DEFAULT NULL COMMENT '模型名称',
  `input_tokens` INT DEFAULT 0 COMMENT '输入 Token 数',
  `output_tokens` INT DEFAULT 0 COMMENT '输出 Token 数',
  `total_tokens` INT DEFAULT 0 COMMENT '总 Token 数',
  `call_type` VARCHAR(32) DEFAULT NULL COMMENT '调用类型：CHAT/COMPLETION/EMBEDDING',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '调用状态：SUCCESS/FAILED',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `response_time` BIGINT DEFAULT 0 COMMENT '响应时间（毫秒）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_model` (`model`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token 使用统计表';

-- 调用链性能统计表
CREATE TABLE IF NOT EXISTS `ai_chain_performance` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `chain_id` VARCHAR(128) NOT NULL COMMENT '调用链 ID',
  `chain_name` VARCHAR(256) DEFAULT NULL COMMENT '调用链名称',
  `execution_time_ms` BIGINT DEFAULT 0 COMMENT '执行时间（毫秒）',
  `node_count` INT DEFAULT 0 COMMENT '节点数量',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '执行状态：SUCCESS/FAILED',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_chain_id` (`chain_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调用链性能统计表';

-- 工具调用统计表
CREATE TABLE IF NOT EXISTS `ai_tool_invocation` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `tool_name` VARCHAR(128) NOT NULL COMMENT '工具名称',
  `chain_id` VARCHAR(128) DEFAULT NULL COMMENT '所属调用链 ID',
  `input_params` TEXT DEFAULT NULL COMMENT '输入参数（JSON）',
  `output_result` TEXT DEFAULT NULL COMMENT '输出结果（JSON）',
  `execution_time_ms` BIGINT DEFAULT 0 COMMENT '执行时间（毫秒）',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '执行状态：SUCCESS/FAILED',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tool_name` (`tool_name`),
  KEY `idx_chain_id` (`chain_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具调用统计表';
