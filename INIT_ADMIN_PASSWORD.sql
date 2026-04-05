-- 初始化 admin 用户密码
-- 密码：admin123
-- BCrypt 哈希值

-- 如果用户已存在，更新密码
UPDATE `sys_user` 
SET `password` = '$2a$10$4N8vVxJz9Z6Y5X4W3V2U1T0S9R8Q7P6O5N4M3L2K1J0I9H8G7F6E5' 
WHERE `username` = 'admin';

-- 如果用户不存在，插入新用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`) 
SELECT '1', 'admin', '$2a$10$4N8vVxJz9Z6Y5X4W3V2U1T0S9R8Q7P6O5N4M3L2K1J0I9H8G7F6E5', '系统管理员', 'admin@example.com', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

-- 注意：上面的哈希值是示例，实际值需要通过 BCrypt 生成
-- 使用以下 Java 代码生成正确的哈希：
-- BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
-- String hashed = encoder.encode("admin123");
