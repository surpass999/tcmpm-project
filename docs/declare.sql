SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. declare_filing（备案信息表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_filing`;
CREATE TABLE `declare_filing`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '备案主键（自增）',
  `social_credit_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '统一社会信用代码',
  `medical_license_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '医疗机构执业许可证号',
  `org_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构名称',
  `project_type` tinyint(4) NOT NULL COMMENT '项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型',
  `valid_start_time` datetime NOT NULL COMMENT '有效期限开始时间',
  `valid_end_time` datetime NOT NULL COMMENT '有效期限结束时间',
  `construction_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '建设内容（备案方案核心）',
  `filing_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '备案状态：0=草稿，1=已提交，2=省级审核通过，3=专家论证通过，4=已归档，5=退回修改',
  `province_review_opinion` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '省级审核意见',
  `province_review_time` datetime DEFAULT NULL COMMENT '省级审核时间',
  `province_reviewer_id` bigint(20) DEFAULT NULL COMMENT '省级审核人ID（关联system_users.id）',
  `expert_review_opinion` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '专家论证意见',
  `expert_reviewer_ids` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '论证专家ID集合（逗号分隔，关联declare_expert.id）',
  `filing_archive_time` datetime DEFAULT NULL COMMENT '备案归档时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_social_credit_code`(`social_credit_code`) USING BTREE COMMENT '统一社会信用代码唯一索引',
  INDEX `idx_project_type`(`project_type`) USING BTREE COMMENT '项目类型索引',
  INDEX `idx_filing_status`(`filing_status`) USING BTREE COMMENT '备案状态索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目备案核心信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 2. declare_attachment（备案附件表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_attachment`;
CREATE TABLE `declare_attachment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '附件主键（自增）',
  `filing_id` bigint(20) NOT NULL COMMENT '关联备案ID（declare_filing.id）',
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件名称',
  `file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件存储路径（关联infra_file.path）',
  `file_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件类型（如PDF、Word）',
  `file_size` int(11) NOT NULL COMMENT '附件大小（KB）',
  `related_indicator_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '关联指标ID集合（逗号分隔）',
  `upload_time` datetime NOT NULL COMMENT '上传时间',
  `uploader_id` bigint(20) NOT NULL COMMENT '上传人ID（关联system_users.id）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_filing_id`(`filing_id`) USING BTREE COMMENT '关联备案ID索引',
  CONSTRAINT `fk_attachment_filing` FOREIGN KEY (`filing_id`) REFERENCES `declare_filing` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '备案支撑材料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 3. declare_filing_review（备案审核记录表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_filing_review`;
CREATE TABLE `declare_filing_review`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '审核记录主键（自增）',
  `filing_id` bigint(20) NOT NULL COMMENT '关联备案ID（declare_filing.id）',
  `review_type` tinyint(4) NOT NULL COMMENT '审核类型：1=省级审核，2=专家论证',
  `reviewer_id` bigint(20) NOT NULL COMMENT '审核人ID（关联system_users.id或declare_expert.id）',
  `review_opinion` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审核意见',
  `review_result` tinyint(4) NOT NULL COMMENT '审核结果：1=通过，2=退回修改，3=未通过',
  `review_time` datetime NOT NULL COMMENT '审核时间',
  `reject_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '退回原因（审核结果为2时必填）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_filing_id`(`filing_id`) USING BTREE COMMENT '关联备案ID索引',
  INDEX `idx_review_type`(`review_type`) USING BTREE COMMENT '审核类型索引',
  CONSTRAINT `fk_filing_review_filing` FOREIGN KEY (`filing_id`) REFERENCES `declare_filing` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '备案审核轨迹表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 4. declare_project（已立项项目表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_project`;
CREATE TABLE `declare_project`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '项目主键（自增）',
  `filing_id` bigint(20) NOT NULL COMMENT '关联备案ID（declare_filing.id）',
  `project_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称',
  `project_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '项目状态：0=立项中，1=建设中，2=中期评估，3=整改中，4=验收中，5=已验收，6=已终止',
  `total_investment` decimal(18, 2) DEFAULT NULL COMMENT '总投资（万元，关联指标201）',
  `central_fund_arrive` decimal(18, 2) DEFAULT NULL COMMENT '中央转移支付到账金额（万元，关联指标20101）',
  `accumulated_investment` decimal(18, 2) DEFAULT NULL COMMENT '累计完成投资（万元，关联指标202）',
  `central_fund_used` decimal(18, 2) DEFAULT NULL COMMENT '中央转移支付累计使用金额（万元，关联指标20201）',
  `start_time` datetime NOT NULL COMMENT '立项时间',
  `plan_end_time` datetime NOT NULL COMMENT '计划完成时间',
  `actual_progress` int(11) DEFAULT 0 COMMENT '实际进度（%）',
  `leader_user_id` bigint(20) NOT NULL COMMENT '项目负责人ID（关联system_users.id）',
  `leader_mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '负责人手机号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_filing_id`(`filing_id`) USING BTREE COMMENT '关联备案ID唯一索引',
  INDEX `idx_project_status`(`project_status`) USING BTREE COMMENT '项目状态索引',
  CONSTRAINT `fk_project_filing` FOREIGN KEY (`filing_id`) REFERENCES `declare_filing` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '已立项项目核心信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 5. declare_construction（项目建设过程表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_construction`;
CREATE TABLE `declare_construction`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '建设记录主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `report_period` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '报告周期（如2025年上半年）',
  `construction_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '建设情况描述',
  `evidence_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '证据链附件ID集合（逗号分隔，关联infra_file.id）',
  `report_time` datetime NOT NULL COMMENT '报告提交时间',
  `reporter_id` bigint(20) NOT NULL COMMENT '报告人ID（关联system_users.id）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_period`(`project_id`, `report_period`) USING BTREE COMMENT '项目ID+报告周期联合索引',
  CONSTRAINT `fk_construction_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目建设进展数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 6. declare_annual（年度总结表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_annual`;
CREATE TABLE `declare_annual`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '总结主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `year` int(4) NOT NULL COMMENT '年度（如2025）',
  `org_management` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '组织管理情况',
  `construction_status` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '建设情况（含备案方案对比）',
  `achievements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '建设成效（含本院亮点）',
  `problems` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存在问题',
  `solutions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '解决措施',
  `attachment_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '附件ID集合（逗号分隔，关联infra_file.id）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_project_year`(`project_id`, `year`) USING BTREE COMMENT '项目ID+年度唯一索引',
  CONSTRAINT `fk_annual_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目年度总结信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 7. declare_midterm（中期评估表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_midterm`;
CREATE TABLE `declare_midterm`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评估主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `self_evaluation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '自评报告',
  `evidence_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '证据链附件ID集合',
  `expert_evaluation` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '专家评估意见',
  `score` decimal(5, 2) DEFAULT NULL COMMENT '评估得分',
  `rectification_require` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '整改要求',
  `evaluation_time` datetime NOT NULL COMMENT '评估时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '项目ID唯一索引（一个项目一次中期评估）',
  CONSTRAINT `fk_midterm_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '中期评估相关数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 8. declare_rectification（整改记录表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_rectification`;
CREATE TABLE `declare_rectification`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '整改主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `rectification_require` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '整改要求',
  `rectification_measures` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '整改措施',
  `completion_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '整改状态：0=未开始，1=整改中，2=已完成，3=未通过',
  `supporting_materials` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '佐证材料ID集合',
  `plan_complete_time` datetime NOT NULL COMMENT '计划完成时间',
  `actual_complete_time` datetime DEFAULT NULL COMMENT '实际完成时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_status`(`project_id`, `completion_status`) USING BTREE COMMENT '项目ID+整改状态联合索引',
  CONSTRAINT `fk_rectification_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目整改信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 9. declare_acceptance（验收申请表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_acceptance`;
CREATE TABLE `declare_acceptance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '验收主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `apply_time` datetime NOT NULL COMMENT '申请时间',
  `acceptance_materials` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '验收材料清单（附件ID集合）',
  `indicator_achieve` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标达成情况',
  `fund_use_report` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资金使用报告',
  `acceptance_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '验收状态：0=申请中，1=验收中，2=已通过，3=未通过',
  `acceptance_time` datetime DEFAULT NULL COMMENT '验收时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '项目ID唯一索引（一个项目一次验收申请）',
  INDEX `idx_acceptance_status`(`acceptance_status`) USING BTREE COMMENT '验收状态索引',
  CONSTRAINT `fk_acceptance_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目验收申请信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 10. declare_achievement（成果信息表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_achievement`;
CREATE TABLE `declare_achievement`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成果主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `achievement_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成果名称',
  `achievement_type` tinyint(4) NOT NULL COMMENT '成果类型：1=系统功能，2=数据集，3=科研成果，4=管理经验',
  `application_field` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用领域',
  `circulation_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '流通状态：0=未流通，1=已流通，2=待交易',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '关联项目ID索引',
  INDEX `idx_achievement_type`(`achievement_type`) USING BTREE COMMENT '成果类型索引',
  CONSTRAINT `fk_achievement_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成果核心信息表' ROW_FORMAT = Dynamic;


-- ----------------------------
-- 10. declare_achievement_attachment（成果附件表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_achievement_attachment`;
CREATE TABLE `declare_achievement_attachment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '附件主键（自增）',
  `achievement_id` bigint(20) NOT NULL COMMENT '关联成果ID（declare_achievement.id）',
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件名称（如证书扫描件、合同）',
  `file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件存储路径',
  `file_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件类型（PDF、JPG等）',
  `upload_time` datetime NOT NULL COMMENT '上传时间',
  `uploader_id` bigint(20) NOT NULL COMMENT '上传人ID（关联system_users.id）',
  `related_indicator_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '关联指标ID集合（逗号分隔）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_achievement_id`(`achievement_id`) USING BTREE COMMENT '关联成果ID索引',
  CONSTRAINT `fk_achievement_attachment_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `declare_achievement` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成果支撑材料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 11. declare_achievement_review（成果审核记录表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_achievement_review`;
CREATE TABLE `declare_achievement_review`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '审核主键（自增）',
  `achievement_id` bigint(20) NOT NULL COMMENT '关联成果ID（declare_achievement.id）',
  `review_opinion` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审核意见',
  `review_status` tinyint(4) NOT NULL COMMENT '审核状态：1=通过，2=退回修改，3=未通过',
  `review_dept` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审核单位（如国家局、省局）',
  `review_time` datetime NOT NULL COMMENT '审核时间',
  `reviewer_id` bigint(20) NOT NULL COMMENT '审核人ID（关联system_users.id）',
  `reject_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '退回原因（审核状态为2时必填）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_achievement_id`(`achievement_id`) USING BTREE COMMENT '关联成果ID索引',
  INDEX `idx_review_status`(`review_status`) USING BTREE COMMENT '审核状态索引',
  CONSTRAINT `fk_achievement_review_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `declare_achievement` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成果审核与推荐轨迹表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 12. declare_transaction（数据流通交易表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_transaction`;
CREATE TABLE `declare_transaction`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '交易主键（自增）',
  `achievement_id` bigint(20) NOT NULL COMMENT '关联成果ID（declare_achievement.id）',
  `certificate_count` int(11) NOT NULL DEFAULT 0 COMMENT '取得数据产品证书数（关联指标601）',
  `property_count` int(11) NOT NULL DEFAULT 0 COMMENT '取得数据产权登记证数（关联指标602）',
  `transaction_count` int(11) NOT NULL DEFAULT 0 COMMENT '完成交易的数据产品数（关联指标603）',
  `transaction_amount` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '累计交易金额（万元，关联指标604）',
  `transaction_object` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交易对象（如数据交易所、医疗机构）',
  `transaction_time` datetime NOT NULL COMMENT '交易完成时间',
  `transaction_contract` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '交易合同路径（关联infra_file.path）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_achievement_id`(`achievement_id`) USING BTREE COMMENT '关联成果ID索引',
  INDEX `idx_transaction_time`(`transaction_time`) USING BTREE COMMENT '交易时间索引',
  CONSTRAINT `fk_transaction_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `declare_achievement` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '数据流通交易指标表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 13. declare_indicator（指标体系表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_indicator`;
CREATE TABLE `declare_indicator`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '指标主键（自增）',
  `indicator_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标代号（如101、20101、604）',
  `indicator_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标名称',
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '计量单位（人、万元、次等）',
  `category` tinyint(4) NOT NULL COMMENT '指标分类：1=基本情况，2=项目管理，3=系统功能，4=建设成效，5=数据集建设，6=数据交易，7=信息安全',
  `caliber_id` bigint(20) DEFAULT NULL COMMENT '关联指标口径ID（declare_indicator_caliber.id）',
  `logic_rule` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '逻辑校验关系（如201>=20101、802>=80201+80202）',
  `is_required` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否必填（0=否，1=是）',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序（展示顺序）',
  `project_type` tinyint(4) DEFAULT NULL COMMENT '适用项目类型：0=全部，1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_indicator_code`(`indicator_code`) USING BTREE COMMENT '指标代号唯一索引',
  INDEX `idx_category`(`category`) USING BTREE COMMENT '指标分类索引',
  INDEX `idx_caliber_id`(`caliber_id`) USING BTREE COMMENT '关联口径ID索引',
  CONSTRAINT `fk_indicator_caliber` FOREIGN KEY (`caliber_id`) REFERENCES `declare_indicator_caliber` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '统一指标体系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 14. declare_indicator_caliber（指标口径表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_indicator_caliber`;
CREATE TABLE `declare_indicator_caliber`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '口径主键（自增）',
  `indicator_id` bigint(20) NOT NULL COMMENT '关联指标ID（declare_indicator.id）',
  `definition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标解释（对应调查表“指标解释”）',
  `statistic_scope` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '统计范围',
  `data_source` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据来源（如系统填报、人工统计）',
  `fill_require` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '填报要求',
  `calculation_rule` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '计算公式（如401=系统覆盖名老中医工作室数）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_indicator_id`(`indicator_id`) USING BTREE COMMENT '关联指标ID索引',
  CONSTRAINT `fk_indicator_caliber_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `declare_indicator` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '指标口径定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 15. declare_expert（专家表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_expert`;
CREATE TABLE `declare_expert`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '专家主键（自增）',
  `expert_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专家姓名',
  `professional_field` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业领域（如中医电子病历、智慧中药房、信息安全）',
  `review_qualification` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评审资质（如国家级专家、省级专家）',
  `avoidance_rules` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '回避规则（如关联机构回避、地域回避）',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话',
  `email` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '邮箱',
  `affiliation` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '所属单位',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1=可用，0=不可用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_professional_field`(`professional_field`) USING BTREE COMMENT '专业领域索引',
  INDEX `idx_status`(`status`) USING BTREE COMMENT '状态索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专家信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 16. declare_review_task（评审任务表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_review_task`;
CREATE TABLE `declare_review_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务主键（自增）',
  `task_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称（如2025年中期评估评审、备案方案论证）',
  `task_type` tinyint(4) NOT NULL COMMENT '任务类型：1=备案论证，2=中期评估，3=验收评审，4=成果审核',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `expert_ids` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参与专家ID集合（逗号分隔，关联declare_expert.id）',
  `start_time` datetime NOT NULL COMMENT '任务开始时间',
  `end_time` datetime NOT NULL COMMENT '任务截止时间',
  `task_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '任务状态：0=未开始，1=进行中，2=已完成',
  `review_standard` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '评审标准（关联指标体系）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '关联项目ID索引',
  INDEX `idx_task_type_status`(`task_type`, `task_status`) USING BTREE COMMENT '任务类型+状态联合索引',
  CONSTRAINT `fk_review_task_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评审任务分配信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 17. declare_review_result（评审结果表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_review_result`;
CREATE TABLE `declare_review_result`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '结果主键（自增）',
  `task_id` bigint(20) NOT NULL COMMENT '关联评审任务ID（declare_review_task.id）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `indicator_id` bigint(20) NOT NULL COMMENT '关联指标ID（declare_indicator.id）',
  `score` decimal(5, 2) NOT NULL COMMENT '指标得分（满分100分）',
  `total_score` decimal(6, 2) NOT NULL COMMENT '总分（所有指标得分汇总）',
  `review_conclusion` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评审结论（如通过、需整改、未通过）',
  `reviewer_id` bigint(20) NOT NULL COMMENT '评审专家ID（declare_expert.id）',
  `review_time` datetime NOT NULL COMMENT '评审时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE COMMENT '关联评审任务ID索引',
  INDEX `idx_indicator_id`(`indicator_id`) USING BTREE COMMENT '关联指标ID索引',
  INDEX `idx_reviewer_id`(`reviewer_id`) USING BTREE COMMENT '评审专家ID索引',
  CONSTRAINT `fk_review_result_task` FOREIGN KEY (`task_id`) REFERENCES `declare_review_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_review_result_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `declare_indicator` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_review_result_expert` FOREIGN KEY (`reviewer_id`) REFERENCES `declare_expert` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专家评审评分结果表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 18. declare_review_opinion（评审意见表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_review_opinion`;
CREATE TABLE `declare_review_opinion`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '意见主键（自增）',
  `result_id` bigint(20) NOT NULL COMMENT '关联评审结果ID（declare_review_result.id）',
  `core_conclusion` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '核心结论',
  `specific_basis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '具体依据（结合指标完成情况）',
  `rectification_suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '整改建议（针对未达标指标）',
  `attachment_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '支撑材料ID集合（关联infra_file.id）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_result_id`(`result_id`) USING BTREE COMMENT '关联评审结果ID索引',
  CONSTRAINT `fk_review_opinion_result` FOREIGN KEY (`result_id`) REFERENCES `declare_review_result` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专家评审详细意见表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 19. declare_policy（政策通知表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_policy`;
CREATE TABLE `declare_policy`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '政策主键（自增）',
  `policy_title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '政策/通知标题',
  `policy_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '政策/通知正文',
  `release_dept` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发布单位（如国家中医药管理局、省中医药管理局）',
  `release_time` datetime NOT NULL COMMENT '发布时间',
  `attachment_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '附件ID集合（如政策原文PDF、通知附件）',
  `policy_type` tinyint(4) NOT NULL COMMENT '类型：1=政策文件，2=工作通知',
  `target_project_types` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '适用项目类型（逗号分隔，如1,2,3=综合型+电子病历型+智慧中药房型）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_policy_type`(`policy_type`) USING BTREE COMMENT '政策类型索引',
  INDEX `idx_release_time`(`release_time`) USING BTREE COMMENT '发布时间索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '中医项目专属政策/通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 20. declare_notice_receipt（通知回执表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_notice_receipt`;
CREATE TABLE `declare_notice_receipt`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '回执主键（自增）',
  `policy_id` bigint(20) NOT NULL COMMENT '关联政策/通知ID（declare_policy.id）',
  `receive_dept` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接收单位（如XX中医医院、XX省中医药管理局）',
  `read_time` datetime NOT NULL COMMENT '阅读时间',
  `feedback_content` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '反馈意见',
  `feedback_time` datetime DEFAULT NULL COMMENT '反馈时间',
  `receiver_id` bigint(20) NOT NULL COMMENT '接收人ID（关联system_users.id）',
  `receive_status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '接收状态：1=已阅读，2=已反馈，3=未阅读',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_policy_id`(`policy_id`) USING BTREE COMMENT '关联政策ID索引',
  INDEX `idx_receive_dept`(`receive_dept`) USING BTREE COMMENT '接收单位索引',
  INDEX `idx_receive_status`(`receive_status`) USING BTREE COMMENT '接收状态索引',
  CONSTRAINT `fk_notice_receipt_policy` FOREIGN KEY (`policy_id`) REFERENCES `declare_policy` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通知阅读状态与反馈意见表' ROW_FORMAT = Dynamic;


-- ----------------------------
-- 21. declare_process_config（流程配置表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_process_config`;
CREATE TABLE `declare_process_config`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置主键（自增）',
  `process_type` tinyint(4) NOT NULL COMMENT '流程类型：1=备案流程，2=评估流程，3=验收流程',
  `node_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点名称（如省级审核、专家论证）',
  `flow_rule` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流转规则（如审核通过→下一节点，退回→修改重提）',
  `handle_role_id` bigint(20) NOT NULL COMMENT '处理角色ID（关联system_role.id）',
  `timeout_remind` int(11) NOT NULL DEFAULT 24 COMMENT '超时提醒（小时）',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '节点排序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_process_type`(`process_type`) USING BTREE COMMENT '流程类型索引',
  INDEX `idx_sort`(`sort`) USING BTREE COMMENT '节点排序索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '中医项目核心业务流程配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 22. declare_dataset（数据集表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_dataset`;
CREATE TABLE `declare_dataset`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '数据集主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `dataset_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据集名称',
  `sample_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '样本数量（条）',
  `is_cleaned` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否清洗标注：1=是，0=否',
  `is_verified` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否质量验证：1=是，0=否',
  `is_archived` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否交付归档：1=是，0=否',
  `is_trained` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否训练应用：1=是，0=否',
  `is_agent` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否形成智能体：1=是，0=否',
  `application_effect` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '应用领域与成效（150字内）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '关联项目ID索引',
  INDEX `idx_is_archived`(`is_archived`) USING BTREE COMMENT '是否归档索引',
  CONSTRAINT `fk_dataset_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '中医药高质量数据集信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 23. declare_dataset_attachment（数据集附件表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_dataset_attachment`;
CREATE TABLE `declare_dataset_attachment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '附件主键（自增）',
  `dataset_id` bigint(20) NOT NULL COMMENT '关联数据集ID（declare_dataset.id）',
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件名称（如质量验证报告）',
  `file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件存储路径',
  `file_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件类型',
  `upload_time` datetime NOT NULL COMMENT '上传时间',
  `uploader_id` bigint(20) NOT NULL COMMENT '上传人ID',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dataset_id`(`dataset_id`) USING BTREE COMMENT '关联数据集ID索引',
  CONSTRAINT `fk_dataset_attachment` FOREIGN KEY (`dataset_id`) REFERENCES `declare_dataset` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '数据集支撑材料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 24. declare_alliance（医共体信息表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_alliance`;
CREATE TABLE `declare_alliance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '医共体主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `lead_hospital` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '牵头医院名称',
  `member_count` int(11) NOT NULL DEFAULT 0 COMMENT '成员单位数量（关联指标101）',
  `platform_architecture` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '平台架构描述',
  `is_unified_patient_index` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否建设统一患者主索引：1=是，0=否',
  `data_resource_lib` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '数据资源库情况（多选拼接，如基础数据库、临床数据库）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '项目ID唯一索引（一个项目对应一个医共体）',
  CONSTRAINT `fk_alliance_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '中医智慧医共体核心信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 25. declare_alliance_member（医共体成员表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_alliance_member`;
CREATE TABLE `declare_alliance_member`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成员主键（自增）',
  `alliance_id` bigint(20) NOT NULL COMMENT '关联医共体ID（declare_alliance.id）',
  `member_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成员单位名称',
  `region` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属地区',
  `access_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '接入状态：1=已接入，0=未接入',
  `access_time` datetime DEFAULT NULL COMMENT '接入时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alliance_id`(`alliance_id`) USING BTREE COMMENT '关联医共体ID索引',
  INDEX `idx_access_status`(`access_status`) USING BTREE COMMENT '接入状态索引',
  CONSTRAINT `fk_alliance_member` FOREIGN KEY (`alliance_id`) REFERENCES `declare_alliance` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '医共体成员单位信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 26. declare_security（安全备案表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_security`;
CREATE TABLE `declare_security`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '安全主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `system_count` int(11) NOT NULL DEFAULT 0 COMMENT '已备案建设方案中所建系统数（关联指标801）',
  `level_protection_count` int(11) NOT NULL DEFAULT 0 COMMENT '网络安全等级保护备案系统数（关联指标802）',
  `level_three_count` int(11) NOT NULL DEFAULT 0 COMMENT '三级等级保护备案系统数（关联指标80201）',
  `level_two_count` int(11) NOT NULL DEFAULT 0 COMMENT '二级等级保护备案系统数（关联指标80202）',
  `国产化_require_count` int(11) NOT NULL DEFAULT 0 COMMENT '满足国产化要求系统数（关联指标803）',
  `security_filing_time` datetime NOT NULL COMMENT '安全备案时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '项目ID唯一索引',
  CONSTRAINT `fk_security_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目信息安全备案数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 27. declare_exchange（交流活动表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_exchange`;
CREATE TABLE `declare_exchange`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '活动主键（自增）',
  `activity_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `activity_type` tinyint(4) NOT NULL COMMENT '活动类型：1=交流会，2=培训，3=研讨会，4=系统演示',
  `activity_time` datetime NOT NULL COMMENT '活动时间',
  `participant_depts` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参与单位（逗号分隔）',
  `activity_materials` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '活动材料ID集合（如会议纪要、培训课件）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_activity_type`(`activity_type`) USING BTREE COMMENT '活动类型索引',
  INDEX `idx_activity_time`(`activity_time`) USING BTREE COMMENT '活动时间索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '中医项目交流培训活动信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 28. declare_experience（经验库表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_experience`;
CREATE TABLE `declare_experience`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '经验主键（自增）',
  `case_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '案例名称',
  `background` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目背景',
  `implementation_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实施路径',
  `reference_points` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '可借鉴要点',
  `related_project_id` bigint(20) DEFAULT NULL COMMENT '关联项目ID（declare_project.id）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_related_project`(`related_project_id`) USING BTREE COMMENT '关联项目ID索引',
  CONSTRAINT `fk_experience_project` FOREIGN KEY (`related_project_id`) REFERENCES `declare_project` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '优秀成果与典型案例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 29. declare_toolkit（工具包表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_toolkit`;
CREATE TABLE `declare_toolkit`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '工具包主键（自增）',
  `toolkit_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具包名称',
  `toolkit_type` tinyint(4) NOT NULL COMMENT '类型：1=模板，2=方法论，3=操作手册',
  `toolkit_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具包内容（或存储路径）',
  `applicable_scene` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '适用场景（如备案方案编制、年度总结填报）',
  `download_count` int(11) NOT NULL DEFAULT 0 COMMENT '下载次数',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_toolkit_type`(`toolkit_type`) USING BTREE COMMENT '工具包类型索引',
  INDEX `idx_applicable_scene`(`applicable_scene`) USING BTREE COMMENT '适用场景索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '可复用工具包信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 30. declare_indicator_value（指标值存储表，补充完整）
-- ----------------------------
DROP TABLE IF EXISTS `declare_indicator_value`;
CREATE TABLE `declare_indicator_value`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键（自增），唯一标识1条指标值记录',
  `business_type` tinyint(4) NOT NULL COMMENT '业务类型：1=备案（declare_filing），2=立项（declare_project），3=年度总结（declare_annual），4=中期评估（declare_midterm），5=验收（declare_acceptance）',
  `business_id` bigint(20) NOT NULL COMMENT '关联业务主键（对应业务表ID）',
  `indicator_id` bigint(20) NOT NULL COMMENT '关联指标ID（declare_indicator.id）',
  `indicator_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标代号（冗余，方便快速查询）',
  `value_type` tinyint(4) NOT NULL COMMENT '值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本',
  `value_num` decimal(18, 4) DEFAULT NULL COMMENT '数字型值（金额、数量等）',
  `value_str` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '字符串型值（短文本、选项等）',
  `value_bool` bit(1) DEFAULT NULL COMMENT '布尔型值（0=否，1=是）',
  `value_date` datetime DEFAULT NULL COMMENT '日期型值',
  `value_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '长文本型值（超1024字符的描述、备注等）',
  `fill_time` datetime NOT NULL COMMENT '指标值填报时间',
  `filler_id` bigint(20) NOT NULL COMMENT '填写人ID（关联system_users.id）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号（默认0）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_business_type_id`(`business_type`, `business_id`) USING BTREE COMMENT '业务类型+业务ID联合索引',
  INDEX `idx_indicator_code`(`indicator_code`) USING BTREE COMMENT '指标代号索引',
  INDEX `idx_indicator_id`(`indicator_id`) USING BTREE COMMENT '关联指标ID索引',
  CONSTRAINT `fk_indicator_value_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `declare_indicator` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '指标值存储表' ROW_FORMAT = Dynamic;