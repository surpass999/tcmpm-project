-- =============================================
-- 用户表增加省市字段 - 用于大屏统计按地区展示
-- 执行日期: 2026-03-21
-- =============================================

-- 为用户表增加省市字段
ALTER TABLE `system_users`
ADD COLUMN `province_id` INT COMMENT '省份ID（关联system_area.id）' AFTER `dept_id`,
ADD COLUMN `city_id` INT COMMENT '城市ID（关联system_area.id）' AFTER `province_id`;

-- =============================================
-- 说明：
-- 1. province_id 和 city_id 关联 system_area 表的 id 字段
-- 2. 地区数据来自框架的 area.csv 文件
-- 3. AreaTypeEnum: 1=国家, 2=省份, 3=城市, 4=地区
-- =============================================

-- 示例数据更新（请根据实际情况调整）
-- UPDATE system_users SET province_id = 440000, city_id = 440100 WHERE id = 1;
