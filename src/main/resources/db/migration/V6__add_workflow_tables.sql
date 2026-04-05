-- 工作流定义表
CREATE TABLE IF NOT EXISTS `workflow_definition` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `workflow_code` VARCHAR(64) NOT NULL COMMENT '工作流编码',
  `workflow_name` VARCHAR(128) NOT NULL COMMENT '工作流名称',
  `description` TEXT DEFAULT NULL COMMENT '工作流描述',
  `version` VARCHAR(32) DEFAULT NULL COMMENT '版本号',
  `workflow_config` LONGTEXT DEFAULT NULL COMMENT '工作流配置（JSON）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人 ID',
  `published_by` VARCHAR(64) DEFAULT NULL COMMENT '发布人 ID',
  `published_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_code_version` (`workflow_code`, `version`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义表';

-- 工作流实例表
CREATE TABLE IF NOT EXISTS `workflow_instance` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `workflow_definition_id` VARCHAR(64) NOT NULL COMMENT '工作流定义 ID',
  `instance_no` VARCHAR(64) NOT NULL COMMENT '实例编号',
  `business_data` LONGTEXT DEFAULT NULL COMMENT '业务数据（JSON）',
  `current_node_id` VARCHAR(64) DEFAULT NULL COMMENT '当前节点 ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING/COMPLETED/FAILED/CANCELLED',
  `result` LONGTEXT DEFAULT NULL COMMENT '执行结果',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_definition_id` (`workflow_definition_id`),
  KEY `idx_status` (`status`),
  KEY `idx_instance_no` (`instance_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流实例表';

-- 工作流节点实例表
CREATE TABLE IF NOT EXISTS `workflow_node_instance` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `workflow_instance_id` VARCHAR(64) NOT NULL COMMENT '工作流实例 ID',
  `node_id` VARCHAR(64) NOT NULL COMMENT '节点 ID',
  `node_name` VARCHAR(128) DEFAULT NULL COMMENT '节点名称',
  `node_type` VARCHAR(16) DEFAULT NULL COMMENT '节点类型：START/END/TASK/CONDITION/PARALLEL',
  `input_data` LONGTEXT DEFAULT NULL COMMENT '输入数据（JSON）',
  `output_data` LONGTEXT DEFAULT NULL COMMENT '输出数据（JSON）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/COMPLETED/FAILED/SKIPPED',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `duration` BIGINT DEFAULT 0 COMMENT '执行时长（毫秒）',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`workflow_instance_id`),
  KEY `idx_node_id` (`node_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点实例表';
