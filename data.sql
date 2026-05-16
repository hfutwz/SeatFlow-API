-- SeatFlow 种子数据脚本
-- 版本：v1.0
-- 日期：2026-05-16
-- 说明：初始化 admin 账号、4角色、8权限、系统参数

USE `seatflow`;

-- 1. 插入系统参数（根据 PRD 3.4 章）
INSERT IGNORE INTO `t_system_config` (`config_key`, `config_value`, `description`) VALUES
('max_reservation_hours', '4', '单次最大预约小时数'),
('check_in_remind_before_min', '15', '签到提前提醒（分钟）'),
('check_in_warn_after_min', '10', '签到逾期警告（分钟）'),
('check_in_cancel_after_min', '15', '签到逾期取消（分钟）');

-- 2. 插入权限（根据 PRD 3.2 章 F011）
INSERT IGNORE INTO `t_permission` (`name`, `code`) VALUES
('查看预约记录', 'reservation:view'),
('查看违约记录', 'violation:view'),
('为用户预约和取消预约', 'reservation:manage'),
('座位登记和注销', 'seat:manage'),
('自习室登记和注销', 'room:manage'),
('调整系统参数', 'system:config'),
('角色和权限管理', 'role:manage'),
('用户角色分配', 'user:manage');

-- 3. 插入默认角色（根据 PRD 3.2 章 F010）
INSERT IGNORE INTO `t_role` (`name`, `code`, `description`) VALUES
('超级管理员', 'super_admin', '拥有所有权限，系统最高管理者'),
('自习室管理员', 'room_admin', '管理自习室和座位，查看预约记录'),
('服务管理员', 'service_admin', '查看预约和违约记录，代客预约/取消'),
('查看员', 'viewer', '仅查看预约和违约记录');

-- 4. 为角色分配权限
-- 4.1 super_admin：所有权限
INSERT IGNORE INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `t_role` r, `t_permission` p
WHERE r.code = 'super_admin';

-- 4.2 room_admin：自习室/座位管理 + 查看预约记录
INSERT IGNORE INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `t_role` r, `t_permission` p
WHERE r.code = 'room_admin'
  AND p.code IN ('room:manage', 'seat:manage', 'reservation:view');

-- 4.3 service_admin：查看预约/违约 + 代客预约
INSERT IGNORE INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `t_role` r, `t_permission` p
WHERE r.code = 'service_admin'
  AND p.code IN ('reservation:view', 'violation:view', 'reservation:manage');

-- 4.4 viewer：仅查看预约/违约
INSERT IGNORE INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `t_role` r, `t_permission` p
WHERE r.code = 'viewer'
  AND p.code IN ('reservation:view', 'violation:view');

-- 5. 创建 admin 用户（密码：admin123，BCrypt加密后：$2a$10$...）
-- 使用 Spring Security BCrypt 加密，这里是加密后的密码
INSERT IGNORE INTO `t_user` (`username`, `password`, `real_name`, `email`, `user_type`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iK6roS6zM59ycVcFQ4eFZ8B5WQ1a', '系统管理员', 'admin@seatflow.com', 'ADMIN');

-- 6. 为 admin 用户分配 super_admin 角色
INSERT IGNORE INTO `t_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `t_user` u, `t_role` r
WHERE u.username = 'admin' AND r.code = 'super_admin';

-- 7. 插入示例院系数据
INSERT IGNORE INTO `t_department` (`name`) VALUES
('计算机科学与工程学院'),
('电子信息工程学院'),
('机械与汽车工程学院'),
('经济管理学院'),
('理学院'),
('人文与社会科学学院');

-- 8. 插入示例自习室数据
INSERT IGNORE INTO `t_room` (`name`, `location`, `department_id`, `open_time`, `close_time`) VALUES
('图书馆301', '图书馆三楼东侧', NULL, '07:00:00', '22:00:00'),
('图书馆302', '图书馆三楼西侧', NULL, '07:00:00', '22:00:00'),
('计算机学院实验室', '计算机学院楼301', 1, '08:00:00', '21:00:00'),
('电子学院自习室', '电子学院楼201', 2, '08:00:00', '21:00:00');

-- 9. 插入示例座位数据（以图书馆301为例，5行6列）
INSERT IGNORE INTO `t_seat` (`room_id`, `seat_number`, `row_num`, `col_num`, `socket_type`, `position`, `status`) VALUES
-- 第1行
(1, 'A01', 1, 1, 'FIXED', 'WINDOW', 'AVAILABLE'),
(1, 'A02', 1, 2, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'A03', 1, 3, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'A04', 1, 4, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(1, 'A05', 1, 5, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'A06', 1, 6, 'NONE', 'CORRIDOR', 'AVAILABLE'),
-- 第2行
(1, 'B01', 2, 1, 'FIXED', 'WINDOW', 'AVAILABLE'),
(1, 'B02', 2, 2, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'B03', 2, 3, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'B04', 2, 4, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(1, 'B05', 2, 5, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'B06', 2, 6, 'NONE', 'CORRIDOR', 'AVAILABLE'),
-- 第3行（停用一个座位）
(1, 'C01', 3, 1, 'FIXED', 'WINDOW', 'DISABLED'),
(1, 'C02', 3, 2, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'C03', 3, 3, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'C04', 3, 4, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(1, 'C05', 3, 5, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'C06', 3, 6, 'NONE', 'CORRIDOR', 'AVAILABLE'),
-- 第4行
(1, 'D01', 4, 1, 'FIXED', 'WINDOW', 'AVAILABLE'),
(1, 'D02', 4, 2, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'D03', 4, 3, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'D04', 4, 4, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(1, 'D05', 4, 5, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'D06', 4, 6, 'NONE', 'CORRIDOR', 'AVAILABLE'),
-- 第5行
(1, 'E01', 5, 1, 'FIXED', 'WINDOW', 'AVAILABLE'),
(1, 'E02', 5, 2, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'E03', 5, 3, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'E04', 5, 4, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(1, 'E05', 5, 5, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'E06', 5, 6, 'NONE', 'CORRIDOR', 'AVAILABLE');

-- 10. 插入示例签到编码（今日）
INSERT IGNORE INTO `t_check_in_code` (`room_id`, `code_date`, `code`) VALUES
(1, CURDATE(), 'A1B2C3'),
(2, CURDATE(), 'D4E5F6'),
(3, CURDATE(), 'G7H8I9'),
(4, CURDATE(), 'J0K1L2');

-- 查询验证
SELECT '系统参数' as type, COUNT(*) as count FROM t_system_config
UNION ALL
SELECT '权限', COUNT(*) FROM t_permission
UNION ALL
SELECT '角色', COUNT(*) FROM t_role
UNION ALL
SELECT '角色权限关联', COUNT(*) FROM t_role_permission
UNION ALL
SELECT '用户', COUNT(*) FROM t_user
UNION ALL
SELECT '用户角色关联', COUNT(*) FROM t_user_role
UNION ALL
SELECT '院系', COUNT(*) FROM t_department
UNION ALL
SELECT '自习室', COUNT(*) FROM t_room
UNION ALL
SELECT '座位', COUNT(*) FROM t_seat
UNION ALL
SELECT '签到编码', COUNT(*) FROM t_check_in_code;