-- =====================================================
-- AI Spring Framework - 会话标签功能增强
-- 版本：1.0.2
-- =====================================================

USE ai_framework;

-- =====================================================
-- 会话表添加标签字段（冗余存储，便于查询）
-- =====================================================

ALTER TABLE `chat_sessions` 
ADD COLUMN `tags_json` JSON DEFAULT NULL COMMENT '标签（JSON 格式）' AFTER `status`;

-- 更新现有会话的 tags_json（从关联表聚合）
UPDATE `chat_sessions` cs
SET cs.tags_json = (
    SELECT JSON_ARRAYAGG(tag_name)
    FROM `chat_session_tags` cst
    WHERE cst.session_id = cs.session_id
)
WHERE EXISTS (
    SELECT 1 FROM `chat_session_tags` cst WHERE cst.session_id = cs.session_id
);

-- =====================================================
-- 标签表添加唯一索引（防止重复标签）
-- 使用存储过程检查索引是否存在
-- =====================================================

DROP PROCEDURE IF EXISTS create_uk_session_tag;
DELIMITER $$
CREATE PROCEDURE create_uk_session_tag()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS 
                   WHERE table_schema = 'ai_framework' AND table_name = 'chat_session_tags' AND index_name = 'uk_session_tag') THEN
        ALTER TABLE `chat_session_tags`
        ADD UNIQUE INDEX `uk_session_tag` (`session_id`, `tag_name`);
    END IF;
END$$
DELIMITER ;
CALL create_uk_session_tag();
DROP PROCEDURE IF EXISTS create_uk_session_tag;

-- =====================================================
-- 完成提示
-- =====================================================
SELECT '会话标签功能增强完成！' AS message;
