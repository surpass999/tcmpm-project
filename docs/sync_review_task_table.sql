-- ============================================
-- declare_review_task 表结构同步脚本
-- 执行前请先备份数据！
-- ============================================

-- 1. 先查看当前表结构
-- DESC `declare_review_task`;

-- 2. 添加所有可能缺失的列
-- 注意：如果列已存在会报错，可以忽略或者先删除再添加

-- task_definition_key 列（节点Key）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `task_definition_key` varchar(64) DEFAULT '' COMMENT '节点Key（对应DSL的nodeKey）' AFTER `process_instance_id`;

-- task_name 列（任务名称）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `task_name` varchar(128) DEFAULT '' COMMENT '任务名称' AFTER `business_id`;

-- task_type 列（任务类型）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `task_type` tinyint(4) DEFAULT 0 COMMENT '任务类型：1=备案论证，2=中期评估，3=验收评审，4=成果审核' AFTER `task_definition_key`;

-- start_time 列（任务开始时间）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `start_time` datetime DEFAULT NULL COMMENT '任务开始时间';

-- end_time 列（任务截止时间）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `end_time` datetime DEFAULT NULL COMMENT '任务截止时间';

-- expert_ids 列（参与专家ID集合）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `expert_ids` varchar(512) DEFAULT '' COMMENT '参与专家ID集合（逗号分隔）';

-- review_standard 列（评审标准JSON）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `review_standard` text COMMENT '评审标准JSON（关联指标体系）';

-- review_requirement 列（评审要求）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `review_requirement` varchar(1024) DEFAULT '' COMMENT '评审要求';

-- total_score 列（综合评分）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `total_score` decimal(6,2) DEFAULT NULL COMMENT '综合评分';

-- review_conclusion 列（评审结论）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `review_conclusion` varchar(256) DEFAULT '' COMMENT '评审结论';

-- version 列（乐观锁版本号）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `version` int(11) DEFAULT 0 COMMENT '乐观锁版本号';

-- delete_reason 列（删除原因）
ALTER TABLE `declare_review_task`
ADD COLUMN IF NOT EXISTS `delete_reason` varchar(256) DEFAULT '' COMMENT '删除原因';

-- 3. 修改列属性（如果需要）

-- 如果 task_type 是 NOT NULL 但没有默认值，需要先修改
-- ALTER TABLE `declare_review_task` MODIFY COLUMN `task_type` tinyint(4) DEFAULT 0 COMMENT '任务类型';

-- 4. 验证同步结果
-- DESC `declare_review_task`;

-- 5. 同步完成后清理可能存在的无效数据
-- DELETE FROM `declare_review_task` WHERE `task_name` IS NULL AND `business_id` IS NULL;
