-- =====================================================
-- AI Spring Framework - 索引优化脚本
-- 版本：1.0.1
-- =====================================================

USE ai_framework;

-- =====================================================
-- 聊天模块索引优化
-- =====================================================

-- 消息表复合索引（常用查询组合）
-- 使用存储过程检查索引是否存在
DROP PROCEDURE IF EXISTS create_idx_session_create;
DELIMITER $$
CREATE PROCEDURE create_idx_session_create()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'chat_messages' AND index_name = 'idx_session_create') THEN
        CREATE INDEX `idx_session_create` ON `chat_messages` (`session_id`, `create_time`);
    END IF;
END$$
DELIMITER ;
CALL create_idx_session_create();
DROP PROCEDURE IF EXISTS create_idx_session_create;

DROP PROCEDURE IF EXISTS create_idx_session_role;
DELIMITER $$
CREATE PROCEDURE create_idx_session_role()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'chat_messages' AND index_name = 'idx_session_role') THEN
        CREATE INDEX `idx_session_role` ON `chat_messages` (`session_id`, `role`);
    END IF;
END$$
DELIMITER ;
CALL create_idx_session_role();
DROP PROCEDURE IF EXISTS create_idx_session_role;

-- 会话表复合索引
DROP PROCEDURE IF EXISTS create_idx_user_status;
DELIMITER $$
CREATE PROCEDURE create_idx_user_status()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'chat_sessions' AND index_name = 'idx_user_status') THEN
        CREATE INDEX `idx_user_status` ON `chat_sessions` (`user_id`, `status`);
    END IF;
END$$
DELIMITER ;
CALL create_idx_user_status();
DROP PROCEDURE IF EXISTS create_idx_user_status;

DROP PROCEDURE IF EXISTS create_idx_user_active;
DELIMITER $$
CREATE PROCEDURE create_idx_user_active()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'chat_sessions' AND index_name = 'idx_user_active') THEN
        CREATE INDEX `idx_user_active` ON `chat_sessions` (`user_id`, `last_active_time`);
    END IF;
END$$
DELIMITER ;
CALL create_idx_user_active();
DROP PROCEDURE IF EXISTS create_idx_user_active;

-- =====================================================
-- 知识库模块索引优化
-- =====================================================

-- 知识文档表复合索引
DROP PROCEDURE IF EXISTS create_idx_knowledge_status;
DELIMITER $$
CREATE PROCEDURE create_idx_knowledge_status()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'knowledge_document' AND index_name = 'idx_knowledge_status') THEN
        CREATE INDEX `idx_knowledge_status` ON `knowledge_document` (`knowledge_id`, `status`);
    END IF;
END$$
DELIMITER ;
CALL create_idx_knowledge_status();
DROP PROCEDURE IF EXISTS create_idx_knowledge_status;

-- 知识分块表全文索引（用于关键词检索）
DROP PROCEDURE IF EXISTS create_ftx_content;
DELIMITER $$
CREATE PROCEDURE create_ftx_content()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'knowledge_chunk' AND index_name = 'ftx_content') THEN
        CREATE FULLTEXT INDEX `ftx_content` ON `knowledge_chunk` (`content`);
    END IF;
END$$
DELIMITER ;
CALL create_ftx_content();
DROP PROCEDURE IF EXISTS create_ftx_content;

-- =====================================================
-- Agent/工作流模块索引优化
-- =====================================================

-- Agent 任务表复合索引
DROP PROCEDURE IF EXISTS create_idx_task_status_user;
DELIMITER $$
CREATE PROCEDURE create_idx_task_status_user()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'agent_task' AND index_name = 'idx_task_status_user') THEN
        CREATE INDEX `idx_task_status_user` ON `agent_task` (`user_id`, `status`);
    END IF;
END$$
DELIMITER ;
CALL create_idx_task_status_user();
DROP PROCEDURE IF EXISTS create_idx_task_status_user;

DROP PROCEDURE IF EXISTS create_idx_task_created;
DELIMITER $$
CREATE PROCEDURE create_idx_task_created()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'agent_task' AND index_name = 'idx_task_created') THEN
        CREATE INDEX `idx_task_created` ON `agent_task` (`create_time`);
    END IF;
END$$
DELIMITER ;
CALL create_idx_task_created();
DROP PROCEDURE IF EXISTS create_idx_task_created;

-- 工作流实例表复合索引
DROP PROCEDURE IF EXISTS create_idx_workflow_status;
DELIMITER $$
CREATE PROCEDURE create_idx_workflow_status()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'workflow_instance' AND index_name = 'idx_workflow_status') THEN
        CREATE INDEX `idx_workflow_status` ON `workflow_instance` (`workflow_id`, `status`);
    END IF;
END$$
DELIMITER ;
CALL create_idx_workflow_status();
DROP PROCEDURE IF EXISTS create_idx_workflow_status;

-- =====================================================
-- 系统模块索引优化
-- =====================================================

-- Token 使用表复合索引
DROP PROCEDURE IF EXISTS create_idx_user_date;
DELIMITER $$
CREATE PROCEDURE create_idx_user_date()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'token_usage' AND index_name = 'idx_user_date') THEN
        CREATE INDEX `idx_user_date` ON `token_usage` (`user_id`, `usage_date`);
    END IF;
END$$
DELIMITER ;
CALL create_idx_user_date();
DROP PROCEDURE IF EXISTS create_idx_user_date;

-- =====================================================
-- 完成提示
-- =====================================================
SELECT '索引优化完成！' AS message;
