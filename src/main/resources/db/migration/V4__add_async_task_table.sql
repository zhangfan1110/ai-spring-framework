-- 异步任务表
CREATE TABLE IF NOT EXISTS `async_task` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `task_type` VARCHAR(64) NOT NULL COMMENT '任务类型',
  `task_data` TEXT DEFAULT NULL COMMENT '任务数据（JSON）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/RUNNING/SUCCESS/FAILED',
  `result` TEXT DEFAULT NULL COMMENT '执行结果',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `max_retry` INT DEFAULT 3 COMMENT '最大重试次数',
  `priority` INT DEFAULT 3 COMMENT '优先级：1-最高 5-最低',
  `delay_seconds` BIGINT DEFAULT NULL COMMENT '延迟执行时间（秒）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始执行时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务表';
