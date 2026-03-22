-- ============================================
-- 项目完成标记功能 - 数据库变更
-- ============================================

-- 1. 为 actual_end_time 字段添加索引，提升完成率统计查询性能
ALTER TABLE declare_project ADD INDEX idx_actual_end_time (actual_end_time);

-- ============================================
-- 说明：
-- 1. actual_end_time 字段原本就存在于 declare_project 表中
-- 2. 项目验收通过时，系统会自动设置 actual_end_time = NOW()
-- 3. 完成率统计通过 actual_end_time IS NOT NULL 判断
-- 4. 双保险机制：
--    - ProjectProcessTaskStatusListener: 检测 bizStatus 包含 FINISH
--    - ProjectProcessStatusListener: processType=6 且 status=2(APPROVE) 兜底
-- ============================================
