-- SeatFlow 数据库建表脚本
-- 版本：v1.0
-- 日期：2026-05-16
-- 说明：根据 Plan v1.2 第3章设计，共11张表，禁止外键约束

-- 创建数据库 seatflow（如果不存在）
CREATE DATABASE IF NOT EXISTS `seatflow` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用 seatflow 数据库
USE `seatflow`;

-- 1. t_user 用户表
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱（推送用）',
  `department_id` BIGINT DEFAULT NULL COMMENT '院系ID（为空=全校共享）',
  `user_type` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '用户类型：STUDENT / ADMIN',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. t_department 院系表
CREATE TABLE IF NOT EXISTS `t_department` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '院系名称',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院系表';

-- 3. t_room 自习室表
CREATE TABLE IF NOT EXISTS `t_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '自习室名称',
  `location` VARCHAR(200) DEFAULT NULL COMMENT '位置描述',
  `department_id` BIGINT DEFAULT NULL COMMENT '所属院系（为空=全校共享）',
  `open_time` TIME NOT NULL DEFAULT '07:00:00' COMMENT '每日开放开始时间',
  `close_time` TIME NOT NULL DEFAULT '22:00:00' COMMENT '每日开放结束时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN / CLOSED',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自习室表';

-- 4. t_seat 座位表
CREATE TABLE IF NOT EXISTS `t_seat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_id` BIGINT NOT NULL COMMENT '所属自习室',
  `seat_number` VARCHAR(20) NOT NULL COMMENT '座位编号（自习室内唯一）',
  `row_num` INT NOT NULL COMMENT '行号',
  `col_num` INT NOT NULL COMMENT '列号',
  `socket_type` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '插座类型：NONE / FIXED / TRACK',
  `position` VARCHAR(20) NOT NULL DEFAULT 'MIDDLE' COMMENT '位置标记：WINDOW / CORRIDOR / MIDDLE',
  `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE / DISABLED',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_seat` (`room_id`, `seat_number`) COMMENT '同一自习室内座位编号唯一',
  KEY `idx_room_id` (`room_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位表';

-- 5. t_reservation 预约表
CREATE TABLE IF NOT EXISTS `t_reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '预约用户',
  `seat_id` BIGINT NOT NULL COMMENT '预约座位',
  `room_id` BIGINT NOT NULL COMMENT '所属自习室（冗余）',
  `date` DATE NOT NULL COMMENT '预约日期',
  `start_time` TIME NOT NULL COMMENT '开始时间（整点）',
  `end_time` TIME NOT NULL COMMENT '结束时间（整点）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING / CHECKED_IN / COMPLETED / CANCELLED',
  `cancelled_by` VARCHAR(20) DEFAULT NULL COMMENT '取消方：STUDENT / ADMIN / SYSTEM',
  `reminded_before` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送提前15min提醒：0=否，1=是',
  `warned_late` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送逾期10min催促：0=否，1=是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_seat_date_time` (`seat_id`, `date`, `start_time`, `end_time`) COMMENT '座位时段冲突检测',
  KEY `idx_user_date_time` (`user_id`, `date`, `start_time`) COMMENT '用户同时段检查',
  KEY `idx_status_date_time` (`status`, `date`, `start_time`) COMMENT '定时任务扫描',
  KEY `idx_room_id` (`room_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约表';

-- 6. t_check_in_code 签到编码表
CREATE TABLE IF NOT EXISTS `t_check_in_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_id` BIGINT NOT NULL COMMENT '所属教室',
  `code_date` DATE NOT NULL COMMENT '编码日期',
  `code` VARCHAR(10) NOT NULL COMMENT '6位动态编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_code_date` (`room_id`, `code_date`) COMMENT '同一教室每日只能有一个编码',
  KEY `idx_code_date` (`code_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到编码表';

-- 7. t_violation 违约表
CREATE TABLE IF NOT EXISTS `t_violation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '违约用户',
  `reservation_id` BIGINT NOT NULL COMMENT '关联预约',
  `type` VARCHAR(30) NOT NULL COMMENT '违约类型：CHECK_IN_TIMEOUT',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_reservation_id` (`reservation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='违约表';

-- 8. t_role 角色表
CREATE TABLE IF NOT EXISTS `t_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码（如 super_admin）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 9. t_permission 权限表
CREATE TABLE IF NOT EXISTS `t_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `code` VARCHAR(50) NOT NULL COMMENT '权限编码（如 reservation:view）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 10. t_user_role 用户-角色关联表
CREATE TABLE IF NOT EXISTS `t_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户',
  `role_id` BIGINT NOT NULL COMMENT '角色',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`) COMMENT '用户-角色唯一约束',
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- 11. t_role_permission 角色-权限关联表
CREATE TABLE IF NOT EXISTS `t_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` BIGINT NOT NULL COMMENT '角色',
  `permission_id` BIGINT NOT NULL COMMENT '权限',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`) COMMENT '角色-权限唯一约束',
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- 12. t_system_config 系统参数表
CREATE TABLE IF NOT EXISTS `t_system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` VARCHAR(100) NOT NULL COMMENT '参数键',
  `config_value` VARCHAR(200) NOT NULL COMMENT '参数值',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '说明',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数表';

-- 显示创建的表
SHOW TABLES;