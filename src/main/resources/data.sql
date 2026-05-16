-- SeatFlow Seed Data
-- Password: admin123 (BCrypt hash)
-- NOTE: Run this after schema.sql

-- Departments
INSERT INTO t_department (id, name) VALUES (1, '计算机学院');
INSERT INTO t_department (id, name) VALUES (2, '电子工程学院');

-- Users (password = admin123 / student123, BCrypt hash)
INSERT INTO t_user (id, username, password, real_name, email, department_id, user_type) VALUES
(1, 'admin', '$2a$10$XiJZwcfX1LTFisLwC3LtD.vu9Q745J1dgom5nkR8CR3RQsKbUEUFK', '系统管理员', 'admin@seatflow.edu', NULL, 'ADMIN'),
(2, 'student1', '$2a$10$r6lwVY8aIlDsahPHO7CC6OlU0MjkRbpK3dRwYB42czS2tAVaDD21G', '张三', 'zhangsan@seatflow.edu', 1, 'STUDENT'),
(3, 'student2', '$2a$10$r6lwVY8aIlDsahPHO7CC6OlU0MjkRbpK3dRwYB42czS2tAVaDD21G', '李四', 'lisi@seatflow.edu', 2, 'STUDENT');

-- Roles
INSERT INTO t_role (id, name, code, description) VALUES
(1, '超级管理员', 'super_admin', '拥有全部权限'),
(2, '自习室管理员', 'room_admin', '管理自习室和座位'),
(3, '服务管理员', 'service_admin', '查看预约/违约记录，代客预约'),
(4, '只读用户', 'viewer', '仅查看预约/违约记录');

-- Permissions
INSERT INTO t_permission (id, name, code) VALUES
(1, '查看预约记录', 'reservation:view'),
(2, '查看违约记录', 'violation:view'),
(3, '代客预约和取消', 'reservation:manage'),
(4, '座位登记和注销', 'seat:manage'),
(5, '自习室登记和注销', 'room:manage'),
(6, '调整系统参数', 'system:config'),
(7, '角色和权限管理', 'role:manage'),
(8, '用户角色分配', 'user:manage');

-- Role-Permission mapping
-- super_admin: all permissions
INSERT INTO t_role_permission (role_id, permission_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8);
-- room_admin: reservation:view, violation:view, seat:manage, room:manage
INSERT INTO t_role_permission (role_id, permission_id) VALUES
(2,1),(2,2),(2,4),(2,5);
-- service_admin: reservation:view, violation:view, reservation:manage
INSERT INTO t_role_permission (role_id, permission_id) VALUES
(3,1),(3,2),(3,3);
-- viewer: reservation:view, violation:view
INSERT INTO t_role_permission (role_id, permission_id) VALUES
(4,1),(4,2);

-- User-Role mapping
-- admin -> super_admin
INSERT INTO t_user_role (user_id, role_id) VALUES (1, 1);

-- System config
INSERT INTO t_system_config (config_key, config_value, description) VALUES
('max_reservation_hours', '4', '单次最大预约小时数'),
('check_in_remind_before_min', '15', '签到提前提醒（分钟）'),
('check_in_warn_after_min', '10', '签到逾期警告（分钟）'),
('check_in_cancel_after_min', '15', '签到逾期取消（分钟）');

-- Rooms
INSERT INTO t_room (id, name, location, department_id, open_time, close_time, status) VALUES
(1, '图书馆301', '图书馆3楼东侧', NULL, '07:00:00', '22:00:00', 'OPEN'),
(2, '计算机学院机房', '计算机楼B201', 1, '08:00:00', '21:00:00', 'OPEN');

-- Seats for Room 1 (图书馆301) - 4x5 grid
INSERT INTO t_seat (room_id, seat_number, row_num, col_num, socket_type, position, status) VALUES
(1, 'A1', 1, 1, 'NONE', 'WINDOW', 'AVAILABLE'),
(1, 'A2', 1, 2, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'A3', 1, 3, 'FIXED', 'MIDDLE', 'AVAILABLE'),
(1, 'A4', 1, 4, 'FIXED', 'MIDDLE', 'AVAILABLE'),
(1, 'A5', 1, 5, 'NONE', 'CORRIDOR', 'AVAILABLE'),
(1, 'B1', 2, 1, 'NONE', 'WINDOW', 'AVAILABLE'),
(1, 'B2', 2, 2, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(1, 'B3', 2, 3, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(1, 'B4', 2, 4, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'B5', 2, 5, 'NONE', 'CORRIDOR', 'AVAILABLE'),
(1, 'C1', 3, 1, 'FIXED', 'WINDOW', 'AVAILABLE'),
(1, 'C2', 3, 2, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'C3', 3, 3, 'NONE', 'MIDDLE', 'DISABLED'),
(1, 'C4', 3, 4, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(1, 'C5', 3, 5, 'FIXED', 'CORRIDOR', 'AVAILABLE'),
(1, 'D1', 4, 1, 'NONE', 'WINDOW', 'AVAILABLE'),
(1, 'D2', 4, 2, 'FIXED', 'MIDDLE', 'AVAILABLE'),
(1, 'D3', 4, 3, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'D4', 4, 4, 'NONE', 'MIDDLE', 'AVAILABLE'),
(1, 'D5', 4, 5, 'NONE', 'CORRIDOR', 'AVAILABLE');

-- Seats for Room 2 (计算机学院机房) - 3x4 grid
INSERT INTO t_seat (room_id, seat_number, row_num, col_num, socket_type, position, status) VALUES
(2, 'A1', 1, 1, 'FIXED', 'WINDOW', 'AVAILABLE'),
(2, 'A2', 1, 2, 'FIXED', 'MIDDLE', 'AVAILABLE'),
(2, 'A3', 1, 3, 'FIXED', 'MIDDLE', 'AVAILABLE'),
(2, 'A4', 1, 4, 'FIXED', 'CORRIDOR', 'AVAILABLE'),
(2, 'B1', 2, 1, 'TRACK', 'WINDOW', 'AVAILABLE'),
(2, 'B2', 2, 2, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(2, 'B3', 2, 3, 'TRACK', 'MIDDLE', 'AVAILABLE'),
(2, 'B4', 2, 4, 'TRACK', 'CORRIDOR', 'AVAILABLE'),
(2, 'C1', 3, 1, 'FIXED', 'WINDOW', 'AVAILABLE'),
(2, 'C2', 3, 2, 'FIXED', 'MIDDLE', 'AVAILABLE'),
(2, 'C3', 3, 3, 'FIXED', 'MIDDLE', 'AVAILABLE'),
(2, 'C4', 3, 4, 'FIXED', 'CORRIDOR', 'AVAILABLE');
