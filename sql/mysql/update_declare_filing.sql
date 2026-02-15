-- =====================================================
-- 备案表更新 SQL
-- 根据 docs/declare_optimized.sql 生成
-- 数据库：gemrun_base
-- 执行前请备份数据！
-- =====================================================

USE gemrun_base;

-- =====================================================
-- 1. 更新 declare_filing 表结构
-- =====================================================

-- 检查表是否存在，如果存在则更新结构
-- 注意：如果是新环境，直接执行 CREATE TABLE 部分即可

-- 备份原表数据（如果需要保留）
-- CREATE TABLE declare_filing_backup_20260215 AS SELECT * FROM declare_filing;

-- 1.1 添加新字段（如果不存在）
-- 省级审核信息
ALTER TABLE `declare_filing` ADD COLUMN `national_review_opinion` varchar(1024) DEFAULT '' COMMENT '国家局审核意见' AFTER `province_review_result`;
ALTER TABLE `declare_filing` ADD COLUMN `national_review_time` datetime DEFAULT NULL COMMENT '国家局审核时间' AFTER `national_review_opinion`;
ALTER TABLE `declare_filing` ADD COLUMN `national_reviewer_id` bigint(20) DEFAULT NULL COMMENT '国家局审核人ID' AFTER `national_review_time`;
ALTER TABLE `declare_filing` ADD COLUMN `national_review_result` tinyint(4) DEFAULT NULL COMMENT '国家局审核结果' AFTER `national_reviewer_id`;

-- 专家论证信息
ALTER TABLE `declare_filing` ADD COLUMN `expert_review_opinion` varchar(1024) DEFAULT '' COMMENT '专家论证意见' AFTER `national_review_result`;
ALTER TABLE `declare_filing` ADD COLUMN `expert_reviewer_ids` varchar(256) DEFAULT '' COMMENT '论证专家ID集合' AFTER `expert_review_opinion`;

-- 归档信息
ALTER TABLE `declare_filing` ADD COLUMN `filing_archive_time` datetime DEFAULT NULL COMMENT '备案归档时间' AFTER `expert_reviewer_ids`;
ALTER TABLE `declare_filing` ADD COLUMN `project_id` bigint(20) DEFAULT NULL COMMENT '关联立项项目ID' AFTER `filing_archive_time`;

-- 通用字段
ALTER TABLE `declare_filing` ADD COLUMN `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `project_id`;
ALTER TABLE `declare_filing` ADD COLUMN `delete_reason` varchar(512) DEFAULT '' COMMENT '删除原因' AFTER `version`;

-- 1.2 修改字段注释或类型（如需要）
ALTER TABLE `declare_filing` MODIFY COLUMN `filing_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '备案状态：0=草稿，1=已提交，2=省级审核中，3=已通过，4=退回，5=已归档';

-- 1.3 添加索引（如果不存在）
-- ALTER TABLE `declare_filing` ADD INDEX `idx_org_name`(`org_name`(100));

-- =====================================================
-- 2. 创建流程配置相关表
-- =====================================================

-- 2.1 业务类型与流程关联配置表
CREATE TABLE IF NOT EXISTS `declare_business_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_type` varchar(128) NOT NULL COMMENT '业务类型（declare:filing:submit）',
  `business_name` varchar(64) NOT NULL COMMENT '业务名称',
  `process_definition_key` varchar(64) NOT NULL COMMENT '流程定义Key',
  `process_category` varchar(32) NOT NULL COMMENT '流程分类',
  `description` varchar(512) DEFAULT '' COMMENT '描述',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否启用（0=禁用，1=启用）',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_business_type`(`business_type`),
  INDEX `idx_process_key`(`process_definition_key`),
  INDEX `idx_enabled`(`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务类型与流程关联配置表';

-- 2.2 业务与流程实例关联表
CREATE TABLE IF NOT EXISTS `declare_business_process` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_type` varchar(128) NOT NULL COMMENT '业务类型',
  `business_id` bigint(20) NOT NULL COMMENT '业务ID',
  `process_instance_id` varchar(64) NOT NULL COMMENT '流程实例ID',
  `process_definition_key` varchar(64) NOT NULL COMMENT '流程定义Key',
  `current_node_key` varchar(64) DEFAULT '' COMMENT '当前节点Key',
  `current_status` varchar(32) DEFAULT '' COMMENT '当前流程状态',
  `initiator_id` bigint(20) DEFAULT NULL COMMENT '发起人ID',
  `start_time` datetime NOT NULL COMMENT '流程开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '流程结束时间',
  `result` varchar(32) DEFAULT '' COMMENT '流程结果',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_business`(`business_type`, `business_id`),
  INDEX `idx_process_instance`(`process_instance_id`),
  INDEX `idx_current_node`(`current_node_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务与流程实例关联表';

-- =====================================================
-- 3. 初始化流程配置数据
-- =====================================================

INSERT INTO `declare_business_type` (`business_type`, `business_name`, `process_definition_key`, `process_category`, `description`, `enabled`, `sort`) VALUES
-- 备案相关流程
('declare:filing:submit', '备案提交', 'proc_filing', 'declare_filing', '医院提交备案申请', 1, 1),
('declare:filing:audit', '备案审核', 'proc_filing_audit', 'declare_filing', '省局审核备案', 1, 2),
('declare:filing:withdraw', '备案撤回', 'proc_filing_withdraw', 'declare_filing', '医院撤回备案', 1, 3),
('declare:filing:resubmit', '重新提交', 'proc_filing_resubmit', 'declare_filing', '重新提交备案', 1, 4),
('declare:filing:archive', '备案归档', 'proc_filing_archive', 'declare_filing', '备案归档', 0, 5),

-- 项目相关流程
('declare:project:start', '项目立项', 'proc_project', 'declare_project', '项目立项', 1, 10),
('declare:project:midterm', '中期评估', 'proc_midterm', 'declare_project', '项目中期评估', 1, 11),
('declare:project:rectification:start', '发起整改', 'proc_rectification', 'declare_rectification', '项目整改', 0, 12),
('declare:project:rectification:complete', '整改完成', 'proc_rectification_complete', 'declare_rectification', '整改完成', 0, 13),
('declare:project:acceptance', '验收申请', 'proc_acceptance', 'declare_project', '项目验收', 1, 14),

-- 成果相关流程
('declare:achievement:submit', '成果提交', 'proc_achievement', 'declare_achievement', '提交成果认定', 0, 20),
('declare:achievement:audit', '成果审核', 'proc_achievement_audit', 'declare_achievement', '成果审核', 0, 21),

-- 评审任务流程
('declare:review:task', '评审任务', 'proc_review', 'declare_review', '评审任务分发', 0, 30),
('declare:review:result', '评审结果', 'proc_review_result', 'declare_review', '评审结果汇总', 0, 31)

ON DUPLICATE KEY UPDATE `updater` = VALUES(`updater`), `update_time` = CURRENT_TIMESTAMP;

-- =====================================================
-- 4. 验证结果
-- =====================================================

-- 查看表结构
-- DESCRIBE declare_filing;
-- DESCRIBE declare_business_type;
-- DESCRIBE declare_business_process;

-- 查看配置数据
-- SELECT * FROM declare_business_type;
