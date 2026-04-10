-- =====================================================
-- AI Spring Framework - 创建数据库
-- 版本：0.0.1
-- 说明：Flyway 无法创建数据库，需单独执行此脚本
-- =====================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS ai_framework 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 授权用户访问（可选，根据实际情况修改）
-- CREATE USER IF NOT EXISTS 'ai_framework'@'localhost' IDENTIFIED BY 'your_password';
-- GRANT ALL PRIVILEGES ON ai_framework.* TO 'ai_framework'@'localhost';
-- FLUSH PRIVILEGES;

SELECT '数据库创建完成！请使用 V1.0.0 及后续迁移脚本创建表结构。' AS message;
