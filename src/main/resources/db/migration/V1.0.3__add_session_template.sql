-- =====================================================
-- AI Spring Framework - 会话模板功能
-- 版本：1.0.3
-- =====================================================

USE ai_framework;

-- =====================================================
-- 会话模板表
-- =====================================================

CREATE TABLE IF NOT EXISTS `chat_session_template` (
    `id` VARCHAR(64) NOT NULL COMMENT '模板 ID',
    `name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `category` VARCHAR(64) DEFAULT NULL COMMENT '分类',
    `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标',
    `preset_messages` JSON DEFAULT NULL COMMENT '预设消息',
    `system_prompt` TEXT COMMENT '系统提示词',
    `model_config` JSON DEFAULT NULL COMMENT '模型配置',
    `is_builtin` TINYINT DEFAULT 0 COMMENT '是否内置',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `usage_count` INT DEFAULT 0 COMMENT '使用次数',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话模板表';

-- =====================================================
-- 插入默认模板
-- =====================================================

INSERT INTO `chat_session_template` (`id`, `name`, `description`, `category`, `icon`, `preset_messages`, `system_prompt`, `is_builtin`, `is_enabled`) VALUES
('tpl_general', '通用助手', '日常对话、问题解答、创意写作', 'general', 'ChatDotRound', 
 '[]', 
 '你是一个有帮助的 AI 助手，请友好、专业地回答用户的问题。', 
 1, 1),
('tpl_coding', '编程助手', '代码编写、调试、代码审查', 'coding', 'Code', 
 '[{"role": "system", "content": "你是一个专业的编程助手，擅长多种编程语言。请提供清晰、可执行的代码示例。"}]', 
 '你是一个专业的编程助手，擅长 Python、Java、JavaScript、Go 等多种编程语言。请提供清晰、可执行、有注释的代码示例，并解释关键逻辑。', 
 1, 1),
('tpl_translation', '翻译助手', '多语言翻译、文本润色', 'translation', 'RefreshLeft', 
 '[{"role": "system", "content": "你是一个专业的翻译助手，请准确翻译用户提供的文本。"}]', 
 '你是一个专业的翻译助手，精通中文、英文、日文、韩文等多种语言。请准确翻译用户提供的文本，并保持原文的语气和风格。如有歧义，请说明。', 
 1, 1),
('tpl_writing', '写作助手', '文章写作、润色、改写', 'writing', 'EditPen', 
 '[{"role": "system", "content": "你是一个专业的写作助手，请帮助用户撰写、润色文章。"}]', 
 '你是一个专业的写作助手，请帮助用户撰写、润色、改写文章。注意文章结构、逻辑清晰、语言流畅。', 
 1, 1),
('tpl_analysis', '数据分析', '数据解读、图表建议、洞察分析', 'analysis', 'DataAnalysis', 
 '[{"role": "system", "content": "你是一个数据分析专家，请帮助用户解读数据、提供洞察。"}]', 
 '你是一个数据分析专家，请帮助用户解读数据、提供洞察和建议。如有需要，可以建议使用哪些图表来展示数据。', 
 1, 1);

-- =====================================================
-- 完成提示
-- =====================================================
SELECT '会话模板功能完成！' AS message;
