-- =====================================================
-- 流程配置相关表
-- =====================================================

-- ----------------------------
-- 1. 业务类型与流程关联配置表（declare_business_type）
-- ----------------------------
DROP TABLE IF EXISTS `declare_business_type`;
CREATE TABLE `declare_business_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_type` varchar(128) NOT NULL COMMENT '业务类型（declare:filing:submit）',
  `business_name` varchar(64) NOT NULL COMMENT '业务名称（备案提交）',
  `process_definition_key` varchar(64) NOT NULL COMMENT '流程定义Key',
  `process_category` varchar(32) NOT NULL COMMENT '流程分类（declare_filing）',
  `description` varchar(512) DEFAULT '' COMMENT '描述',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否启用（0=禁用，1=启用）',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_business_type`(`business_type`),
  INDEX `idx_process_key`(`process_definition_key`),
  INDEX `idx_enabled`(`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务类型与流程关联配置表';

-- ----------------------------
-- 2. 业务与流程实例关联表（declare_business_process）
-- ----------------------------
DROP TABLE IF EXISTS `declare_business_process`;
CREATE TABLE `declare_business_process` (
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
  `result` varchar(32) DEFAULT '' COMMENT '流程结果（agree=通过，reject=驳回）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_business`(`business_type`, `business_id`),
  INDEX `idx_process_instance`(`process_instance_id`),
  INDEX `idx_current_node`(`current_node_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务与流程实例关联表';

-- ----------------------------
-- 3. 配置数据示例
-- ----------------------------
INSERT INTO `declare_business_type` (`business_type`, `business_name`, `process_definition_key`, `process_category`, `description`, `enabled`, `sort`) VALUES
-- 备案相关流程
('declare:filing:submit', '备案提交', 'proc_filing', 'declare_filing', '医院提交备案申请', 1, 1),
('declare:filing:audit', '备案审核', 'proc_filing_audit', 'declare_filing', '省局审核备案', 1, 2),
('declare:filing:archive', '备案归档', 'proc_filing_archive', 'declare_filing', '备案归档管理', 0, 3),

-- 项目相关流程
('declare:project:start', '项目立项', 'proc_project', 'declare_project', '项目立项启动', 1, 10),
('declare:project:midterm', '中期评估', 'proc_midterm', 'declare_project', '项目中期评估', 1, 11),
('declare:project:rectification:start', '发起整改', 'proc_rectification', 'declare_rectification', '项目整改流程', 0, 12),
('declare:project:rectification:complete', '整改完成', 'proc_rectification_complete', 'declare_rectification', '整改完成提交', 0, 13),

-- 成果相关流程
('declare:achievement:submit', '成果提交', 'proc_achievement', 'declare_achievement', '提交成果认定', 0, 20),
('declare:achievement:audit', '成果审核', 'proc_achievement_audit', 'declare_achievement', '成果审核流程', 0, 21);

SET FOREIGN_KEY_CHECKS = 1;
