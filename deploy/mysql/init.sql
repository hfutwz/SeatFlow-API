-- 创建 seatflow 数据库
CREATE DATABASE IF NOT EXISTS seatflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 设置权限
GRANT ALL PRIVILEGES ON seatflow.* TO 'root'@'%';
FLUSH PRIVILEGES;
