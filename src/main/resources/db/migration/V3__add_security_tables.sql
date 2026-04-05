-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `permission_code` VARCHAR(128) NOT NULL COMMENT '权限编码',
  `permission_name` VARCHAR(128) NOT NULL COMMENT '权限名称',
  `type` INT DEFAULT 3 COMMENT '权限类型：1-菜单 2-按钮 3-接口',
  `parent_id` VARCHAR(64) DEFAULT NULL COMMENT '父级 ID',
  `path` VARCHAR(255) DEFAULT NULL COMMENT '路径',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户 ID',
  `role_id` VARCHAR(64) NOT NULL COMMENT '角色 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键 ID',
  `role_id` VARCHAR(64) NOT NULL COMMENT '角色 ID',
  `permission_id` VARCHAR(64) NOT NULL COMMENT '权限 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 初始化数据

-- 默认管理员账号（密码：admin123）
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`) VALUES
('1', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'admin@example.com', 1);

-- 角色
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `status`) VALUES
('1', 'ADMIN', '超级管理员', '拥有所有权限', 1),
('2', 'USER', '普通用户', '基础权限', 1);

-- 权限
INSERT INTO `sys_permission` (`id`, `permission_code`, `permission_name`, `type`, `status`) VALUES
('1', 'user:create', '创建用户', 3, 1),
('2', 'user:update', '更新用户', 3, 1),
('3', 'user:delete', '删除用户', 3, 1),
('4', 'user:read', '查看用户', 3, 1),
('5', 'role:create', '创建角色', 3, 1),
('6', 'role:update', '更新角色', 3, 1),
('7', 'role:delete', '删除角色', 3, 1),
('8', 'role:read', '查看角色', 3, 1),
('9', 'agent:execute', '执行 Agent 任务', 3, 1),
('10', 'chat:send', '发送消息', 3, 1);

-- 分配管理员角色
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES
('1', '1', '1');

-- 分配管理员权限
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES
('1', '1', '1'), ('2', '1', '2'), ('3', '1', '3'), ('4', '1', '4'),
('5', '1', '5'), ('6', '1', '6'), ('7', '1', '7'), ('8', '1', '8'),
('9', '1', '9'), ('10', '1', '10');

-- 分配普通用户权限
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES
('11', '2', '9'), ('12', '2', '10');
