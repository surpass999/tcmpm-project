-- 为 declare_progress_report 表添加 dept_id 字段，用于数据权限控制
-- 该字段关联 system_dept.id，使数据权限框架能正确过滤数据

-- 1. 添加 dept_id 字段
ALTER TABLE declare_progress_report
ADD COLUMN `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID（用于数据权限控制，对应 system_dept.id）' AFTER `hospital_id`;

-- 2. 更新现有数据：将 hospital_id=1 的记录设置 dept_id=138
-- 注意：这里假设每条 progress_report 记录对应一个医院，需要通过医院表关联查询其 dept_id
UPDATE declare_progress_report pr
INNER JOIN declare_hospital h ON pr.hospital_id = h.id
SET pr.dept_id = h.dept_id
WHERE pr.dept_id IS NULL AND h.dept_id IS NOT NULL;

-- 3. 添加索引（可选，但建议添加以提高查询性能）
ALTER TABLE declare_progress_report
ADD INDEX idx_dept_id (`dept_id`);
