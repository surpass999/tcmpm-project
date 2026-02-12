-- =====================================================
-- 智慧中医医院试点项目管理平台 - 数据库设计
-- 作者：GemRun
-- 日期：2025.12
-- 说明：
-- 1. 参考 ruoyi-vue-pro 的 infra_file 表设计
-- 2. 合并多个附件表为统一的附件关联表
-- 3. 统一状态枚举规范
-- =====================================================

-- =====================================================
-- 统一状态枚举规范
-- =====================================================
-- 【通用状态】适用于：filing_status, task_status, status 等
-- ----------------------------------------------------------------
-- | 值 | 枚举名       | 说明         | 对应DSL actions           |
-- ----------------------------------------------------------------
-- | 0  | DRAFT        | 草稿         | save, draft              |
-- | 1  | SUBMITTED    | 已提交       | submit                   |
-- | 2  | AUDITING     | 审核中       | agree, reject, back      |
-- | 3  | APPROVED     | 已通过       | -                        |
-- | 4  | REJECTED     | 退回         | -                        |
-- | 5  | ARCHIVED     | 已归档       | archive                  |
-- | 6  | CANCELLED    | 已取消       | cancel                   |
-- ----------------------------------------------------------------

-- 【项目状态】project_status（继承通用状态，扩展）
-- ----------------------------------------------------------------
-- | 值 | 枚举名       | 说明         | 对应DSL bizStatus        |
-- ----------------------------------------------------------------
-- | 0  | FILING       | 立项中       | PRO_FILING               |
-- | 1  | CONSTRUCTION | 建设中       | PRO_CONSTRUCTION        |
-- | 2  | MIDTERM      | 中期评估     | PRO_MIDTERM             |
-- | 3  | RECTIFICATION| 整改中       | PRO_RECTIFICATION       |
-- | 4  | ACCEPTANCE   | 验收中       | PRO_ACCEPTANCE          |
-- | 5  | ACCEPTED     | 已验收       | PRO_ACCEPTED            |
-- | 6  | TERMINATED   | 已终止       | PRO_TERMINATED          |
-- ----------------------------------------------------------------

-- 【评审任务状态】task_status
-- ----------------------------------------------------------------
-- | 值 | 枚举名       | 说明         | 对应Flowable Task       |
-- ----------------------------------------------------------------
-- | 0  | PENDING      | 待分配       | created                 |
-- | 1  | ASSIGNED     | 待接收       | assigned                |
-- | 2  | REVIEWING    | 评审中       | in_progress             |
-- | 3  | COMPLETED    | 已完成       | completed               |
-- ----------------------------------------------------------------

-- 【评审专家任务状态】review_status
-- ----------------------------------------------------------------
-- | 值 | 枚举名       | 说明         | 对应actions              |
-- ----------------------------------------------------------------
-- | 0  | PENDING      | 待评审       | -                       |
-- | 1  | RECEIVED     | 已接收       | receive                 |
-- | 2  | REVIEWING    | 评审中       | reviewing               |
-- | 3  | SUBMITTED    | 已提交       | submit                  |
-- | 4  | TIMEOUT      | 超时         | -                       |
-- ----------------------------------------------------------------

-- 【数据流通状态】circulation_status
-- ----------------------------------------------------------------
-- | 值 | 枚举名       | 说明         |                         |
-- ----------------------------------------------------------------
-- | 0  | PENDING      | 未流通       |                         |
-- | 1  | CIRCULATING  | 已流通       |                         |
-- | 2  | TRADING      | 待交易       |                         |
-- | 3  | TRADED       | 已交易       |                         |
-- ----------------------------------------------------------------

-- 【专家状态】expert_status
-- ----------------------------------------------------------------
-- | 值 | 枚举名       | 说明         |                         |
-- ----------------------------------------------------------------
-- | 1  | ACTIVE       | 在册         |                         |
-- | 2  | SUSPENDED    | 暂停         |                         |
-- | 3  | CANCELLED    | 注销         |                         |
-- ----------------------------------------------------------------

-- 【政策状态】policy_status
-- ----------------------------------------------------------------
-- | 值 | 枚举名       | 说明         |                         |
-- ----------------------------------------------------------------
-- | 1  | PUBLISHED    | 已发布       |                         |
-- | 2  | UNPUBLISHED  | 已下架       |                         |
-- ----------------------------------------------------------------

-- 【推广范围】promotion_scope
-- ----------------------------------------------------------------
-- | 值 | 枚举名       | 说明         |                         |
-- ----------------------------------------------------------------
-- | 1  | REGIONAL     | 区域试点     |                         |
-- | 2  | NATIONAL     | 全国推广     |                         |
-- ----------------------------------------------------------------

-- 【发布单位】release_dept
-- ----------------------------------------------------------------
-- | 值 | 枚举名       | 说明         |                         |
-- ----------------------------------------------------------------
-- | 1  | NATIONAL     | 国家局       |                         |
-- | 2  | PROVINCE     | 省局         |                         |
-- ----------------------------------------------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 第一部分：业务核心表（declare_ 开头）
-- =====================================================

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

  -- 备案时间
  `valid_start_time` datetime NOT NULL COMMENT '有效期限开始时间',
  `valid_end_time` datetime NOT NULL COMMENT '有效期限结束时间',

  -- 备案内容
  `construction_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '建设内容（备案方案核心）',

  -- 备案状态：0=草稿(DRAFT)，1=已提交(SUBMITTED)，2=省级审核中(AUDITING)，3=已通过(APPROVED)，4=退回(REJECTED)，5=已归档(ARCHIVED)
  `filing_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '备案状态：0=草稿，1=已提交，2=省级审核中，3=已通过，4=退回，5=已归档',
  `status_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '状态变更原因（退回原因等）',

  -- 省级审核信息
  `province_review_opinion` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '省级审核意见',
  `province_review_time` datetime DEFAULT NULL COMMENT '省级审核时间',
  `province_reviewer_id` bigint(20) DEFAULT NULL COMMENT '省级审核人ID（关联system_users.id）',
  `province_review_result` tinyint(4) DEFAULT NULL COMMENT '省级审核结果：1=通过(APPROVED)，4=退回(REJECTED)，6=驳回(CANCELLED)',

  -- 国家局审核信息
  `national_review_opinion` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '国家局审核意见',
  `national_review_time` datetime DEFAULT NULL COMMENT '国家局审核时间',
  `national_reviewer_id` bigint(20) DEFAULT NULL COMMENT '国家局审核人ID（关联system_users.id）',
  `national_review_result` tinyint(4) DEFAULT NULL COMMENT '国家局审核结果：1=通过(APPROVED)，4=退回(REJECTED)，6=驳回(CANCELLED)',

  -- 专家论证信息（可选）
  `expert_review_opinion` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '专家论证意见',
  `expert_reviewer_ids` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '论证专家ID集合（逗号分隔）',

  -- 归档信息
  `filing_archive_time` datetime DEFAULT NULL COMMENT '备案归档时间',
  `project_id` bigint(20) DEFAULT NULL COMMENT '关联立项项目ID（declare_project.id）',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  

  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_social_credit_code`(`social_credit_code`) USING BTREE COMMENT '统一社会信用代码唯一索引',
  INDEX `idx_project_type`(`project_type`) USING BTREE COMMENT '项目类型索引',
  INDEX `idx_filing_status`(`filing_status`) USING BTREE COMMENT '备案状态索引',
  INDEX `idx_org_name`(`org_name`(100)) USING BTREE COMMENT '机构名称索引（模糊搜索）'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目备案核心信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 2. declare_project（已立项项目表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_project`;
CREATE TABLE `declare_project`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '项目主键（自增）',
  `filing_id` bigint(20) NOT NULL COMMENT '关联备案ID（declare_filing.id）',
  `project_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称',

  -- 项目状态：0=立项中(FILING)，1=建设中(CONSTRUCTION)，2=中期评估(MIDTERM)，3=整改中(RECTIFICATION)，4=验收中(ACCEPTANCE)，5=已验收(ACCEPTED)，6=已终止(TERMINATED)
  `project_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '项目状态：0=立项中，1=建设中，2=中期评估，3=整改中，4=验收中，5=已验收，6=已终止',

  -- 核心指标（冗余存储，方便查询）
  `total_investment` decimal(18, 2) DEFAULT NULL COMMENT '总投资（万元，关联指标201）',
  `central_fund_arrive` decimal(18, 2) DEFAULT NULL COMMENT '中央转移支付到账金额（万元，关联指标20101）',
  `accumulated_investment` decimal(18, 2) DEFAULT NULL COMMENT '累计完成投资（万元，关联指标202）',
  `central_fund_used` decimal(18, 2) DEFAULT NULL COMMENT '中央转移支付累计使用金额（万元，关联指标20201）',

  -- 时间信息
  `start_time` datetime NOT NULL COMMENT '立项时间',
  `plan_end_time` datetime NOT NULL COMMENT '计划完成时间',
  `actual_end_time` datetime DEFAULT NULL COMMENT '实际完成时间',

  -- 进度信息
  `actual_progress` int(11) DEFAULT 0 COMMENT '实际进度（%）',

  -- 负责人信息
  `leader_user_id` bigint(20) NOT NULL COMMENT '项目负责人ID（关联system_users.id）',
  `leader_mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '负责人手机号',
  `leader_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '负责人姓名（冗余）',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_filing_id`(`filing_id`) USING BTREE COMMENT '关联备案ID唯一索引',
  INDEX `idx_project_status`(`project_status`) USING BTREE COMMENT '项目状态索引',
  INDEX `idx_leader`(`leader_user_id`) USING BTREE COMMENT '负责人索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '已立项项目核心信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 3. declare_project_process（项目过程记录表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_project_process`;
CREATE TABLE `declare_project_process`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '过程记录主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',

  -- 过程类型：1=建设过程，2=年度总结，3=中期评估，4=整改记录，5=验收申请
  `process_type` tinyint(4) NOT NULL COMMENT '过程类型：1=建设过程，2=年度总结，3=中期评估，4=整改记录，5=验收申请',
  `process_title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '过程标题（如：2025年上半年建设过程）',

  -- 过程数据（JSON格式，灵活存储）
  `process_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '过程数据JSON',
  `indicator_values` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '指标值JSON',

  -- 状态：0=草稿(DRAFT)，1=已提交(SUBMITTED)，2=审核中(AUDITING)，3=通过(APPROVED)，4=退回(REJECTED)
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态：0=草稿，1=已提交，2=审核中，3=通过，4=退回',
  `status_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '状态变更原因',

  -- 时间信息
  `report_period_start` datetime DEFAULT NULL COMMENT '报告周期开始时间',
  `report_period_end` datetime DEFAULT NULL COMMENT '报告周期结束时间',
  `report_time` datetime DEFAULT NULL COMMENT '报告提交时间',

  -- 审核信息
  `review_opinion` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '审核意见',
  `reviewer_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `review_result` tinyint(4) DEFAULT NULL COMMENT '审核结果：1=通过，2=退回',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_process`(`project_id`, `process_type`) USING BTREE COMMENT '项目ID+过程类型联合索引',
  INDEX `idx_status`(`status`) USING BTREE COMMENT '状态索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目过程记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 4. declare_rectification（项目整改跟踪表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_rectification`;
CREATE TABLE `declare_rectification`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '整改主键',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `process_id` bigint(20) NOT NULL COMMENT '关联流程记录ID（declare_project_process.id）',

  -- 整改内容
  `rectify_type` tinyint(4) NOT NULL COMMENT '整改来源：1=中期评估，2=验收申请',
  `rectify_item` varchar(128) DEFAULT '' COMMENT '整改项编号（如：指标601）',
  `rectify_content` varchar(512) NOT NULL COMMENT '整改要求/问题描述',
  `rectify_measures` text COMMENT '整改措施/完成情况',

  -- 时间与责任人
  `deadline` datetime NOT NULL COMMENT '整改期限',
  `responser_id` bigint(20) NOT NULL COMMENT '整改责任人ID（system_users.id）',
  `complete_time` datetime DEFAULT NULL COMMENT '整改完成时间',

  -- 状态：0=待整改，1=已提交待复验，2=复验通过，3=复验驳回
  `complete_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '完成状态：0=待整改，1=已提交待复验，2=复验通过，3=复验驳回',

  -- 佐证材料
  `rectify_evidence` varchar(1024) DEFAULT '' COMMENT '整改佐证材料ID集合（infra_file.id，逗号分隔）',

  -- 省级复验
  `province_review_result` tinyint(4) DEFAULT NULL COMMENT '省级复验结果：1=通过(APPROVED)，4=驳回(REJECTED)',
  `province_review_opinion` varchar(1024) DEFAULT '' COMMENT '省级复验意见',
  `province_reviewer_id` bigint(20) DEFAULT NULL COMMENT '省级复验人ID',
  `province_review_time` datetime DEFAULT NULL COMMENT '省级复验时间',

  -- 国家局复核（可选，如验收环节需要）
  `nation_review_result` tinyint(4) DEFAULT NULL COMMENT '国家局复核结果：1=通过(APPROVED)，4=驳回(REJECTED)',
  `nation_review_opinion` varchar(1024) DEFAULT '' COMMENT '国家局复核意见',
  `nation_reviewer_id` bigint(20) DEFAULT NULL COMMENT '国家局复核人ID',
  `nation_review_time` datetime DEFAULT NULL COMMENT '国家局复核时间',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) DEFAULT '' COMMENT '删除原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',

  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE,
  INDEX `idx_process_id`(`process_id`) USING BTREE,
  INDEX `idx_responser_id`(`responser_id`) USING BTREE,
  INDEX `idx_deadline`(`deadline`) USING BTREE,
  INDEX `idx_complete_status`(`complete_status`) USING BTREE,
  CONSTRAINT `fk_rectification_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_rectification_process` FOREIGN KEY (`process_id`) REFERENCES `declare_project_process` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目整改跟踪表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 5. declare_achievement（成果信息表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_achievement`;
CREATE TABLE `declare_achievement`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成果主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `achievement_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成果名称',

  -- 成果类型：1=系统功能，2=数据集，3=科研成果，4=管理经验
  `achievement_type` tinyint(4) NOT NULL COMMENT '成果类型：1=系统功能，2=数据集，3=科研成果，4=管理经验',
  `application_field` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用领域',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '成果描述',

  -- 审核状态：0=待审核(PENDING)，1=省级通过/待国家局审核(SUBMITTED)，2=国家局审核中(AUDITING)，3=已认定推广(APPROVED)，4=退回(REJECTED)
  `audit_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核，1=省级通过/待国家局审核，2=国家局审核中，3=已认定推广，4=退回',
  `audit_opinion` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '审核意见',
  `auditor_id` bigint(20) DEFAULT NULL COMMENT '审核人ID（system_users.id）',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',

  -- 推荐状态：0=未推荐(PENDING)，1=已推荐至国家局(SUBMITTED)，2=已纳入推广库(APPROVED)
  `recommend_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '推荐状态：0=未推荐，1=已推荐至国家局，2=已纳入推广库',
  `recommend_opinion` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '推荐意见',
  `recommender_id` bigint(20) DEFAULT NULL COMMENT '推荐人ID',
  `recommend_time` datetime DEFAULT NULL COMMENT '推荐时间',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',

  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '关联项目ID索引',
  INDEX `idx_achievement_type`(`achievement_type`) USING BTREE COMMENT '成果类型索引',
  INDEX `idx_audit_status`(`audit_status`) USING BTREE COMMENT '审核状态索引',
  INDEX `idx_recommend_status`(`recommend_status`) USING BTREE COMMENT '推荐状态索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成果核心信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 6. declare_indicator（指标体系表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_indicator`;
CREATE TABLE `declare_indicator`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '指标主键（自增）',
  `indicator_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标代号（如101、20101、604）',
  `indicator_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标名称',
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '计量单位（人、万元、次等）',

  -- 指标分类：1=基本情况，2=项目管理，3=系统功能，4=建设成效，5=数据集建设，6=数据交易，7=信息安全
  `category` tinyint(4) NOT NULL COMMENT '指标分类：1=基本情况，2=项目管理，3=系统功能，4=建设成效，5=数据集建设，6=数据交易，7=信息安全',

  -- 口径关联
  `caliber_id` bigint(20) DEFAULT NULL COMMENT '关联指标口径ID',
  `logic_rule` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '逻辑校验关系（如201>=20101、802>=80201+80202）',
  `calculation_rule` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '计算公式（如401=系统覆盖名老中医工作室数）',

  -- 值类型
  -- | 1=数字 | 2=字符串 | 3=布尔 | 4=日期 | 5=长文本 | 6=单选 | 7=多选 | 8=日期区间 | 9=文件上传 |
  `value_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本，6=单选，7=多选，8=日期区间，9=文件上传',
  `value_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '选项定义（JSON格式）：[{value:1,label:"选项1"},...]，适用于单选/多选/下拉',

  -- 约束信息
  `is_required` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否必填',
  `min_value` decimal(18, 4) DEFAULT NULL COMMENT '最小值',
  `max_value` decimal(18, 4) DEFAULT NULL COMMENT '最大值',

  -- 展示信息
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序（展示顺序）',
  `show_in_list` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否在列表显示',
  `children_indicator_codes` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '子指标代号集合（逗号分隔，如20101,20102）',

  -- 适用类型：0=全部，1-6=各项目类型
  `project_type` tinyint(4) DEFAULT NULL COMMENT '适用项目类型：0=全部，1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型',

  -- 适用业务类型：filing/project/process/achievement/transaction
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '适用业务类型',

    -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_indicator_code`(`indicator_code`) USING BTREE COMMENT '指标代号唯一索引',
  INDEX `idx_category`(`category`) USING BTREE COMMENT '指标分类索引',
  INDEX `idx_project_type`(`project_type`) USING BTREE COMMENT '适用项目类型索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '统一指标体系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 7. declare_indicator_caliber（指标口径定义表）
-- ----------------------------
-- 说明：定义每个指标的统计口径、数据来源、填报要求
-- ----------------------------
DROP TABLE IF EXISTS `declare_indicator_caliber`;
CREATE TABLE `declare_indicator_caliber`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '口径主键（自增）',
  `indicator_id` bigint(20) NOT NULL COMMENT '关联指标ID（declare_indicator.id）',

  -- 口径定义
  `definition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标解释',
  `statistic_scope` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '统计范围',
  `data_source` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据来源',
  `fill_require` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '填报要求',
  `calculation_example` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '计算示例',

    -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',

  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_indicator_id`(`indicator_id`) USING BTREE COMMENT '指标ID索引',
  CONSTRAINT `fk_caliber_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `declare_indicator` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '指标口径定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 8. declare_indicator_joint_rule（多指标联合验证规则表）
-- ----------------------------
-- 说明：存储多指标交叉验证规则，支持与业务流程联动，适配填报实时验证、半年报/中期评估等流程提交验证场景
-- 核心逻辑：通过 process_type 绑定流程、trigger_timing 绑定触发时机，实现"在正确流程节点执行正确验证"
-- ----------------------------
DROP TABLE IF EXISTS `declare_indicator_joint_rule`;
CREATE TABLE `declare_indicator_joint_rule`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '规则主键（自增）',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称（需直观描述验证场景，如：中央转移支付金额校验、等保系统数求和验证）',
  `project_type` tinyint(4) DEFAULT 0 COMMENT '适用项目类型：0=全部项目，1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型（与 declare_project.project_type 枚举一致）',
  `business_type` varchar(64) NOT NULL COMMENT '适用业务类型：filing=备案，project=立项，process=过程记录（含半年报/年度总结/中期评估），achievement=成果，transaction=流通交易（需与 declare_indicator.business_type 对应）',
  `process_type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '适用流程类型：0=无流程关联（仅填报实时验证），1=建设过程，2=年度总结（含半年报），3=中期评估，4=整改记录，5=验收申请（与 declare_project_process.process_type 枚举完全一致）',
  `trigger_timing` varchar(32) NOT NULL DEFAULT 'FILL' COMMENT '触发时机：FILL=填报时实时验证（用户录入指标值后即时校验），PROCESS_SUBMIT=流程提交时强制验证（提交流程记录时触发）',
  `process_node` varchar(64) DEFAULT '' COMMENT '适用流程节点（可选，适配多节点流程）：对应 Flowable 流程的 task_definition_key（如 province_audit=省级审核、national_audit=国家局审核），空值表示该流程所有节点均触发',
  `rule_config` text NOT NULL COMMENT '规则配置（JSON格式，结构化存储验证逻辑），支持4类核心场景：
  1. 数值求和验证（如：三级+二级等保系统数=等保备案总数）：
     {"type":"SUM","indicators":["80201","80202"],"targetIndicator":"802","errorMsg":"三级+二级等保系统数必须等于等保备案总数"}
  2. 数值比较验证（如：中央转移支付使用金额≤到账金额）：
     {"type":"COMPARE","indicators":["20101","20201"],"expression":"${20201} ≤ ${20101}","errorMsg":"中央转移支付使用金额不能超过到账金额"}
  3. 项目类型关联必填（如：智慧中药房型项目必须填写中药房系统功能覆盖率）：
     {"type":"REQUIRED_BY_PROJECT","projectType":3,"requiredIndicators":["301"],"errorMsg":"智慧中药房型项目必须填写中药房系统功能覆盖率"}
  4. 逻辑互斥验证（如：未通过成果认定则交易金额必须为0）：
     {"type":"EXCLUDE","conditionIndicator":"audit_status","conditionValue":"3","targetIndicators":["604"],"targetValue":0,"errorMsg":"未通过成果认定的项目，交易金额必须为0"}',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '规则状态：1=启用（生效执行验证），0=禁用（暂不执行验证）',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号：解决高并发场景下规则配置修改冲突',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因：软删除时记录删除背景（如：规则过期、业务调整）',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者（关联 system_users.id）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（规则配置时间）',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者（关联 system_users.id）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（规则最后修改时间）',

  PRIMARY KEY (`id`) USING BTREE,
  -- 核心索引：优化"按业务类型+项目类型查询规则"场景（如：查询立项业务、综合型项目的所有规则）
  INDEX `idx_business_project`(`business_type`, `project_type`) USING BTREE COMMENT '业务类型+项目类型联合索引',
  -- 核心索引：优化"按流程类型+触发时机查询规则"场景（如：查询年度总结流程、提交时触发的规则）
  INDEX `idx_process_trigger`(`process_type`, `trigger_timing`) USING BTREE COMMENT '流程类型+触发时机联合索引',
  -- 辅助索引：优化"按流程节点查询规则"场景（如：查询省级审核节点触发的规则）
  INDEX `idx_process_node`(`process_node`) USING BTREE COMMENT '流程节点索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '多指标联合验证规则表（支持填报实时验证、流程提交验证，与业务流程强联动）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 9. declare_indicator_value（指标值存储表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_indicator_value`;
CREATE TABLE `declare_indicator_value`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键（自增）',

  -- 业务类型：1=备案，2=立项，3=建设过程，4=年度总结，5=中期评估，6=验收申请，7=成果，8=流通交易
  `business_type` tinyint(4) NOT NULL COMMENT '业务类型：1=备案，2=立项，3=建设过程，4=年度总结，5=中期评估，6=验收申请，7=成果，8=流通交易',
  `business_id` bigint(20) NOT NULL COMMENT '关联业务主键',
  `indicator_id` bigint(20) NOT NULL COMMENT '关联指标ID',
  `indicator_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标代号（冗余）',

  -- 值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本，6=单选，7=多选，8=日期区间，9=文件上传
  `value_type` tinyint(4) NOT NULL COMMENT '值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本，6=单选，7=多选，8=日期区间，9=文件上传',
  `value_num` decimal(18, 4) DEFAULT NULL COMMENT '数字型值',
  `value_str` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '字符串型值（单选/多选值以逗号分隔，如：1,2,3）',
  `value_bool` bit(1) DEFAULT NULL COMMENT '布尔型值',
  `value_date` datetime DEFAULT NULL COMMENT '日期型值',
  `value_date_start` datetime DEFAULT NULL COMMENT '日期区间-开始',
  `value_date_end` datetime DEFAULT NULL COMMENT '日期区间-结束',
  `value_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '长文本型值',

  -- 校验信息
  `is_valid` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否有效（校验通过）',
  `validation_msg` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '校验信息',

  -- 填报信息
  `fill_time` datetime NOT NULL COMMENT '填报时间',
  `filler_id` bigint(20) NOT NULL COMMENT '填报人ID',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_business`(`business_type`, `business_id`) USING BTREE COMMENT '业务类型+业务ID联合索引',
  INDEX `idx_indicator`(`indicator_id`) USING BTREE COMMENT '指标ID索引',
  INDEX `idx_indicator_code`(`indicator_code`) USING BTREE COMMENT '指标代号索引',
  INDEX `idx_filler`(`filler_id`) USING BTREE COMMENT '填报人索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '指标值存储表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 10. declare_expert（专家信息表）
-- ----------------------------
-- 说明：专家信息扩展表，关联系统用户表
-- ----------------------------
DROP TABLE IF EXISTS `declare_expert`;
CREATE TABLE `declare_expert`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键（自增）',

  -- 关联系统用户表
  `user_id` bigint(20) NOT NULL COMMENT '关联系统用户ID（system_users.id）',

  -- 基本信息
  `expert_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专家姓名',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '身份证号',
  `gender` tinyint(4) DEFAULT NULL COMMENT '性别：1=男，2=女',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',

  -- 联系方式（冗余存储，便于查询）
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '联系电话',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '电子邮箱',

  -- 职业信息
  `work_unit` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '工作单位',
  `job_title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '职务',
  `professional_title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '职称',
  `department` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '所在部门/科室',

  -- 专家资质
  `expert_type` tinyint(4) NOT NULL COMMENT '专家类型：1=技术专家，2=财务专家，3=管理专家，4=行业专家',
  `specialties` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '专业领域（逗号分隔，如：中医信息化、医疗大数据）',
  `expertise_areas` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '擅长方向',
  `education_background` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '学历',
  `degree` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '学位',
  `graduated_from` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '毕业院校',

  -- 资质证明
  `qualification_cert` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '资格证书编号',
  `cert_attach_id` bigint(20) DEFAULT NULL COMMENT '资质证书附件ID（infra_file.id）',

  -- 评审信息
  `review_count` int(11) NOT NULL DEFAULT 0 COMMENT '累计评审次数',
  `last_review_time` datetime DEFAULT NULL COMMENT '上次评审时间',
  `review_score` decimal(3, 2) DEFAULT NULL COMMENT '平均评审评分',

  -- 状态：1=在册，2=暂停，3=注销
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1=在册，2=暂停，3=注销',
  `status_remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '状态说明（如暂停原因）',

  -- 备注
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',

  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_id`(`user_id`) USING BTREE COMMENT '用户ID唯一索引',
  INDEX `idx_expert_type`(`expert_type`) USING BTREE COMMENT '专家类型索引',
  INDEX `idx_status`(`status`) USING BTREE COMMENT '状态索引',
  INDEX `idx_work_unit`(`work_unit`(100)) USING BTREE COMMENT '工作单位索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专家信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 11. declare_review_task（评审任务表）
-- ----------------------------
-- 说明：评审任务的领域业务表，关联 Flowable 流程任务
-- ----------------------------
DROP TABLE IF EXISTS `declare_review_task`;
CREATE TABLE `declare_review_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务主键（自增）',

  -- 关联 Flowable 流程
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '流程实例ID',
  `task_definition_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '节点Key（对应DSL的nodeKey）',

  -- 业务关联
  `task_type` tinyint(4) NOT NULL COMMENT '任务类型：1=备案论证，2=中期评估，3=验收评审，4=成果审核',
  `business_type` tinyint(4) NOT NULL COMMENT '业务类型：1=备案，2=项目，3=成果',
  `business_id` bigint(20) NOT NULL COMMENT '关联业务ID',

  -- 任务信息
  `task_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `start_time` datetime DEFAULT NULL COMMENT '任务开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '任务截止时间',

  -- 专家会签（核心：Flowable 多实例任务）
  `expert_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '参与专家ID集合（逗号分隔）',

  -- 评审专业配置
  `review_standard` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '评审标准JSON（关联指标体系）',
  `review_requirement` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '评审要求',

  -- 汇总结果
  `total_score` decimal(6, 2) DEFAULT NULL COMMENT '综合评分',
  `review_conclusion` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '评审结论',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',

  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_process_instance`(`process_instance_id`) USING BTREE COMMENT '流程实例ID索引',
  INDEX `idx_business`(`business_type`, `business_id`) USING BTREE COMMENT '业务类型+业务ID联合索引',
  INDEX `idx_task_type`(`task_type`) USING BTREE COMMENT '任务类型索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评审任务表（关联Flowable）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 12. declare_review_result（评审结果表）
-- ----------------------------
-- 说明：每位专家的评审详情，关联 Flowable 任务
-- ----------------------------
DROP TABLE IF EXISTS `declare_review_result`;
CREATE TABLE `declare_review_result`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '结果主键（自增）',

  -- 关联 Flowable
  `task_id` bigint(20) NOT NULL COMMENT '关联评审任务ID（declare_review_task.id）',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '流程实例ID',
  `flowable_task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Flowable任务ID',

  -- 专家信息
  `expert_id` bigint(20) NOT NULL COMMENT '评审专家ID（declare_expert.id）',

  -- 业务信息冗余
  `business_type` tinyint(4) NOT NULL COMMENT '业务类型',
  `business_id` bigint(20) NOT NULL COMMENT '关联业务ID',

  -- 评审状态：0=待评审(PENDING)，1=已接收(RECEIVED)，2=评审中(REVIEWING)，3=已提交(SUBMITTED)，4=超时(TIMEOUT)
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '评审状态：0=待评审，1=已接收，2=评审中，3=已提交，4=超时',

  -- 利益冲突检测
  `is_conflict` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否存在利益冲突',
  `conflict_check_result` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '冲突检测结果',

  -- 回避申请
  `is_avoid` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否申请回避',
  `avoid_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '回避原因',

  -- 评审内容
  `score_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '评分数据JSON（各指标评分详情）',
  `total_score` decimal(6, 2) DEFAULT NULL COMMENT '总分',
  `conclusion` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '评审结论：通过/需整改/未通过',

  -- 时间信息
  `receive_time` datetime DEFAULT NULL COMMENT '接收任务时间',
  `submit_time` datetime DEFAULT NULL COMMENT '提交评审时间',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',

  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE COMMENT '任务ID索引',
  INDEX `idx_expert_id`(`expert_id`) USING BTREE COMMENT '专家ID索引',
  INDEX `idx_flowable_task`(`flowable_task_id`) USING BTREE COMMENT 'Flowable任务ID索引',
  INDEX `idx_status`(`status`) USING BTREE COMMENT '评审状态索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专家评审结果表（关联Flowable）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 13. declare_policy（政策通知表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_policy`;
CREATE TABLE `declare_policy`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '政策主键（自增）',
  `policy_title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '政策/通知标题',
  `policy_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '政策/通知正文',
  `policy_summary` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '政策摘要',

  -- 发布单位：1=国家局，2=省局
  `release_dept` tinyint(4) NOT NULL COMMENT '发布单位：1=国家局，2=省局',
  `release_time` datetime NOT NULL COMMENT '发布时间',

  -- 类型：1=政策文件，2=工作通知
  `policy_type` tinyint(4) NOT NULL COMMENT '类型：1=政策文件，2=工作通知',

  -- 目标范围：1=全国，2=全省
  `target_scope` tinyint(4) NOT NULL DEFAULT 1 COMMENT '目标范围：1=全国，2=全省',
  `target_project_types` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '适用项目类型（逗号分隔，如1,2,3）',

  -- 附件（关联infra_file.id，多个用逗号分隔）
  `attachment_ids` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '附件ID集合',

  -- 状态：1=已发布(PUBLISHED)，2=已下架(UNPUBLISHED)
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1=已发布，2=已下架',
  `publisher_id` bigint(20) DEFAULT NULL COMMENT '发布人ID',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_policy_type`(`policy_type`) USING BTREE COMMENT '政策类型索引',
  INDEX `idx_release_time`(`release_time`) USING BTREE COMMENT '发布时间索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '政策通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- =====================================================
-- 第二部分：关联表设计
-- =====================================================

-- ----------------------------
-- 14. declare_attachment（附件关联表）
-- ----------------------------
-- 说明：参考 infra_file 表设计，存储附件与业务的关联关系
-- 实际文件存储在 infra_file 表中，这里只存储关联关系
-- ----------------------------
DROP TABLE IF EXISTS `declare_attachment`;
CREATE TABLE `declare_attachment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联主键（自增）',

  -- 业务类型：1=备案，2=项目，3=成果，4=评审任务，5=评审结果，6=政策通知
  `business_type` tinyint(4) NOT NULL COMMENT '业务类型：1=备案，2=项目，3=成果，4=评审任务，5=评审结果，6=政策通知',
  `business_id` bigint(20) NOT NULL COMMENT '关联业务ID',

  -- 关联到 infra_file 表的文件编号
  `file_id` bigint(20) NOT NULL COMMENT '文件编号（关联infra_file.id）',

  -- 关联类型：1=主附件，2=支撑材料，3=评审材料，4=成果附件
  `attach_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '关联类型：1=主附件，2=支撑材料，3=评审材料，4=成果附件',

  -- 关联指标ID（可选，用于追溯附件对应的指标）
  `related_indicator_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '关联指标ID集合（逗号分隔）',

  -- 排序
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_business`(`business_type`, `business_id`) USING BTREE COMMENT '业务类型+业务ID联合索引',
  INDEX `idx_file_id`(`file_id`) USING BTREE COMMENT '文件ID索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '附件关联表（关联infra_file使用）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 15. declare_review_log（评审操作日志表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_review_log`;
CREATE TABLE `declare_review_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键（自增）',

  -- 关联信息
  `task_id` bigint(20) NOT NULL COMMENT '评审任务ID',
  `result_id` bigint(20) DEFAULT NULL COMMENT '评审结果ID',
  `expert_id` bigint(20) DEFAULT NULL COMMENT '专家ID',

  -- 操作类型：1=接收任务，2=提交评审，3=申请回避，4=完成评审
  `action_type` tinyint(4) NOT NULL COMMENT '操作类型：1=接收任务，2=提交评审，3=申请回避，4=完成评审',
  `action_content` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作内容（评审意见等）',

  -- 时间
  `action_time` datetime NOT NULL COMMENT '操作时间',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE COMMENT '任务ID索引',
  INDEX `idx_expert_id`(`expert_id`) USING BTREE COMMENT '专家ID索引',
  INDEX `idx_action_time`(`action_time`) USING BTREE COMMENT '操作时间索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评审操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 16. declare_process_log（流程执行日志表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_process_log`;
CREATE TABLE `declare_process_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键（自增）',

  -- 流程类型：1=备案，2=评估，3=验收，4=成果
  `process_type` tinyint(4) NOT NULL COMMENT '流程类型：1=备案，2=评估，3=验收，4=成果',

  -- 业务类型：1=备案，2=项目，3=成果
  `business_type` tinyint(4) NOT NULL COMMENT '业务类型：1=备案，2=项目，3=成果',
  `business_id` bigint(20) NOT NULL COMMENT '关联业务ID',

  -- 操作类型：1=提交，2=通过，3=退回，4=撤回，5=转办
  `action_type` tinyint(4) NOT NULL COMMENT '操作类型：1=提交，2=通过，3=退回，4=撤回，5=转办',
  `action_time` datetime NOT NULL COMMENT '操作时间',

  -- 操作人信息
  `operator_id` bigint(20) NOT NULL COMMENT '操作人ID',
  `operator_type` tinyint(4) NOT NULL COMMENT '操作人类型：1=系统用户，2=专家',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作人姓名（冗余）',

  -- Flowable关联
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Flowable任务ID',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Flowable流程实例ID',

  -- 备注
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_business`(`business_type`, `business_id`) USING BTREE COMMENT '业务类型+业务ID联合索引',
  INDEX `idx_operator`(`operator_id`) USING BTREE COMMENT '操作人ID索引',
  INDEX `idx_action_time`(`action_time`) USING BTREE COMMENT '操作时间索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '流程执行日志表' ROW_FORMAT = Dynamic;

-- =====================================================
-- 第三部分：扩展表（医共体、数据集等）
-- =====================================================

-- ----------------------------
-- 17. declare_dataset（数据集表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_dataset`;
CREATE TABLE `declare_dataset`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '数据集主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID',
  `dataset_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据集名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '数据集描述',
  `sample_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '样本数量',
  `data_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '数据类型',
  `data_format` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '数据格式',

  -- 状态字段
  `is_cleaned` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否清洗标注：0=否，1=是',
  `is_verified` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否质量验证：0=否，1=是',
  `is_archived` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否交付归档：0=否，1=是',
  `is_trained` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否训练应用：0=否，1=是',
  `is_agent` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否形成智能体：0=否，1=是',

  `application_effect` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '应用领域与成效',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '项目ID索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '数据集信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 18. declare_alliance（医共体信息表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_alliance`;
CREATE TABLE `declare_alliance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '医共体主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID',
  `lead_hospital` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '牵头医院名称',
  `member_count` int(11) NOT NULL DEFAULT 0 COMMENT '成员单位数量',
  `platform_architecture` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '平台架构描述',
  `is_unified_patient_index` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否建设统一患者主索引：0=否，1=是',
  `data_resource_lib` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '数据资源库情况（多选拼接）',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '项目ID唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '医共体核心信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 19. declare_alliance_member（医共体成员表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_alliance_member`;
CREATE TABLE `declare_alliance_member`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成员主键（自增）',
  `alliance_id` bigint(20) NOT NULL COMMENT '关联医共体ID',
  `member_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成员单位名称',
  `region` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属地区',

  -- 接入状态：0=未接入(PENDING)，1=已接入(CIRCULATING)
  `access_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '接入状态：0=未接入，1=已接入',
  `access_time` datetime DEFAULT NULL COMMENT '接入时间',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alliance_id`(`alliance_id`) USING BTREE COMMENT '医共体ID索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '医共体成员单位信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 20. declare_security（安全备案表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_security`;
CREATE TABLE `declare_security`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '安全主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID',

  -- 安全指标
  `system_count` int(11) NOT NULL DEFAULT 0 COMMENT '已备案建设方案中所建系统数（关联指标801）',
  `level_protection_count` int(11) NOT NULL DEFAULT 0 COMMENT '网络安全等级保护备案系统数（关联指标802）',
  `level_three_count` int(11) NOT NULL DEFAULT 0 COMMENT '三级等级保护备案系统数（关联指标80201）',
  `level_two_count` int(11) NOT NULL DEFAULT 0 COMMENT '二级等级保护备案系统数（关联指标80202）',
  `domestic_meet_count` int(11) NOT NULL DEFAULT 0 COMMENT '满足国产化要求系统数（关联指标803）',
  `security_filing_time` datetime NOT NULL COMMENT '安全备案时间',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',
  
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '项目ID唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '信息安全备案数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 21. declare_data_flow（数据流通记录表）
-- ----------------------------
-- 说明：记录医院数据流通信息，成果展示需要先有数据流通记录
-- ----------------------------
DROP TABLE IF EXISTS `declare_data_flow`;
CREATE TABLE `declare_data_flow`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键（自增）',
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',

  -- 基本信息
  `data_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据名称',
  `data_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '数据描述',
  `data_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据类型（如：患者数据、诊疗数据）',
  `data_source` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '数据来源',

  -- 流通信息
  `flow_type` tinyint(4) NOT NULL COMMENT '流通类型：1=内部使用，2=对外共享，3=交易',
  `flow_object` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '流通对象',
  `flow_purpose` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '流通目的',

  -- 安全备案
  `security_filing_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '安全备案编号',
  `security_filing_time` datetime DEFAULT NULL COMMENT '安全备案时间',

  -- 证书信息
  `certificate_count` int(11) NOT NULL DEFAULT 0 COMMENT '取得数据产品证书数（关联指标601）',
  `property_count` int(11) NOT NULL DEFAULT 0 COMMENT '取得数据产权登记证数（关联指标602）',

  -- 状态：0=草稿(DRAFT)，1=已提交(SUBMITTED)，2=审核中(AUDITING)，3=已通过(APPROVED)，4=退回(REJECTED)
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态：0=草稿，1=已提交，2=审核中，3=已通过，4=退回',
  `audit_opinion` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '审核意见',
  `auditor_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',

  -- 是否已生成成果展示
  `has_achievement` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否已生成成果展示：0=否，1=是',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',

  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '项目ID索引',
  INDEX `idx_status`(`status`) USING BTREE COMMENT '状态索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '数据流通记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 22. declare_achievement_display（成果展示表）
-- ----------------------------
-- 说明：展示数据流通的成果，关联 declare_achievement
-- 流程：declare_achievement -> declare_data_flow -> declare_achievement_display
-- ----------------------------
DROP TABLE IF EXISTS `declare_achievement_display`;
CREATE TABLE `declare_achievement_display`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键（自增）',

  -- 关联关系
  `project_id` bigint(20) NOT NULL COMMENT '关联项目ID（declare_project.id）',
  `data_flow_id` bigint(20) NOT NULL COMMENT '关联数据流通ID（declare_data_flow.id）',
  `achievement_id` bigint(20) NOT NULL COMMENT '关联成果认定ID（declare_achievement.id）',

  -- 成果信息（冗余存储，便于查询）
  `achievement_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成果名称',
  `achievement_type` tinyint(4) NOT NULL COMMENT '成果类型：1=数据产品，2=数据服务，3=解决方案',

  -- 展示内容
  `achievement_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '成果描述',
  `application_field` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用领域',

  -- 交易信息
  `transaction_count` int(11) NOT NULL DEFAULT 0 COMMENT '完成交易的数据产品数（关联指标603）',
  `transaction_amount` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '累计交易金额（万元，关联指标604）',
  `transaction_object` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '交易对象',
  `transaction_time` datetime DEFAULT NULL COMMENT '交易完成时间',
  `transaction_contract` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '交易合同路径',

  -- 推广信息
  `promotion_scope` tinyint(4) DEFAULT NULL COMMENT '推广范围：1=区域试点(REGIONAL)，2=全国推广(NATIONAL)',
  `promotion_count` int(11) NOT NULL DEFAULT 0 COMMENT '推广次数',

  -- 状态：0=草稿(DRAFT)，1=已提交(SUBMITTED)，2=审核中(AUDITING)，3=已通过(APPROVED)，4=退回(REJECTED)
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态：0=草稿，1=已提交，2=审核中，3=已通过，4=退回',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',

  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id`) USING BTREE COMMENT '项目ID索引',
  INDEX `idx_data_flow_id`(`data_flow_id`) USING BTREE COMMENT '数据流通ID索引',
  INDEX `idx_achievement_id`(`achievement_id`) USING BTREE COMMENT '成果ID索引',
  INDEX `idx_status`(`status`) USING BTREE COMMENT '状态索引',
  CONSTRAINT `fk_display_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `declare_achievement` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成果展示表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 23. declare_exchange（交流活动表）
-- ----------------------------
DROP TABLE IF EXISTS `declare_exchange`;
CREATE TABLE `declare_exchange`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '活动主键（自增）',
  `activity_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',

  -- 活动类型：1=交流会，2=培训，3=研讨会，4=系统演示
  `activity_type` tinyint(4) NOT NULL COMMENT '活动类型：1=交流会，2=培训，3=研讨会，4=系统演示',
  `activity_time` datetime NOT NULL COMMENT '活动时间',
  `activity_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动内容描述',
  `activity_effect` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '活动效果',
  `participant_depts` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参与单位（逗号分隔）',
  `participant_count` int(11) DEFAULT 0 COMMENT '参与人数',
  `activity_materials` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '活动材料ID集合（关联infra_file.id，多个用逗号分隔）',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',

  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_activity_type`(`activity_type`) USING BTREE COMMENT '活动类型索引',
  INDEX `idx_activity_time`(`activity_time`) USING BTREE COMMENT '活动时间索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '交流活动信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 24. declare_notice_receipt（政策通知回执表）
-- ----------------------------
-- 说明：记录政策通知的触达和阅读情况，适配政务类通知追溯需求
-- ----------------------------
DROP TABLE IF EXISTS `declare_notice_receipt`;
CREATE TABLE `declare_notice_receipt`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '回执主键（自增）',

  -- 关联政策
  `policy_id` bigint(20) NOT NULL COMMENT '关联政策ID（declare_policy.id）',

  -- 接收方信息
  `org_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接收机构名称',
  `reader_id` bigint(20) NOT NULL COMMENT '阅读人ID（关联system_users.id）',
  `reader_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '阅读人姓名（冗余）',

  -- 阅读信息
  `read_time` datetime NOT NULL COMMENT '阅读时间',
  `read_status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '阅读状态：1=已阅读，2=已过期',

  -- 反馈信息
  `feedback_content` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '反馈意见',
  `feedback_time` datetime DEFAULT NULL COMMENT '反馈时间',

  -- 回执状态：1=已阅读，2=已反馈，3=已取消
  `receipt_status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '回执状态：1=已阅读，2=已反馈，3=已取消',

  -- 通用字段
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因',

  -- 审计字段
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）',

  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_policy_id`(`policy_id`) USING BTREE COMMENT '政策ID索引',
  INDEX `idx_reader_id`(`reader_id`) USING BTREE COMMENT '阅读人ID索引',
  INDEX `idx_read_time`(`read_time`) USING BTREE COMMENT '阅读时间索引',
  CONSTRAINT `fk_notice_receipt_policy` FOREIGN KEY (`policy_id`) REFERENCES `declare_policy` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '政策通知回执表' ROW_FORMAT = Dynamic;


-- =====================================================
-- 外键约束（如需要可取消注释）
-- =====================================================

-- 专家表关联
-- ALTER TABLE `declare_expert` ADD CONSTRAINT `fk_expert_user` FOREIGN KEY (`user_id`) REFERENCES `system_users` (`id`);

-- 附件表关联
-- ALTER TABLE `declare_attachment` ADD CONSTRAINT `fk_attachment_file` FOREIGN KEY (`file_id`) REFERENCES `infra_file` (`id`);

-- 备案项目关联
-- ALTER TABLE `declare_filing` ADD CONSTRAINT `fk_filing_creator` FOREIGN KEY (`creator`) REFERENCES `system_users` (`id`);
-- ALTER TABLE `declare_project` ADD CONSTRAINT `fk_project_filing` FOREIGN KEY (`filing_id`) REFERENCES `declare_filing` (`id`);
-- ALTER TABLE `declare_project` ADD CONSTRAINT `fk_project_leader` FOREIGN KEY (`leader_user_id`) REFERENCES `system_users` (`id`);
-- ALTER TABLE `declare_project_process` ADD CONSTRAINT `fk_process_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`);

-- 成果关联
-- ALTER TABLE `declare_achievement` ADD CONSTRAINT `fk_achievement_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`);

-- 指标口径关联
-- ALTER TABLE `declare_indicator_caliber` ADD CONSTRAINT `fk_caliber_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `declare_indicator` (`id`) ON DELETE CASCADE;

-- 数据流通与成果展示关联
-- ALTER TABLE `declare_data_flow` ADD CONSTRAINT `fk_data_flow_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`);
-- ALTER TABLE `declare_achievement_display` ADD CONSTRAINT `fk_display_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`);
-- ALTER TABLE `declare_achievement_display` ADD CONSTRAINT `fk_display_data_flow` FOREIGN KEY (`data_flow_id`) REFERENCES `declare_data_flow` (`id`);
-- ALTER TABLE `declare_achievement_display` ADD CONSTRAINT `fk_display_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `declare_achievement` (`id`) ON DELETE CASCADE;

-- 评审关联（注意：expert_ids 是逗号分隔的ID集合，不设外键）
-- ALTER TABLE `declare_review_task` ADD CONSTRAINT `fk_task_project` FOREIGN KEY (`business_id`) REFERENCES `declare_project` (`id`);
-- ALTER TABLE `declare_review_result` ADD CONSTRAINT `fk_result_task` FOREIGN KEY (`task_id`) REFERENCES `declare_review_task` (`id`);
-- ALTER TABLE `declare_review_result` ADD CONSTRAINT `fk_result_expert` FOREIGN KEY (`expert_id`) REFERENCES `declare_expert` (`id`);

-- 数据集与联盟关联
-- ALTER TABLE `declare_dataset` ADD CONSTRAINT `fk_dataset_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`);
-- ALTER TABLE `declare_alliance` ADD CONSTRAINT `fk_alliance_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`);
-- ALTER TABLE `declare_alliance_member` ADD CONSTRAINT `fk_member_alliance` FOREIGN KEY (`alliance_id`) REFERENCES `declare_alliance` (`id`);

-- 安全备案关联
-- ALTER TABLE `declare_security` ADD CONSTRAINT `fk_security_project` FOREIGN KEY (`project_id`) REFERENCES `declare_project` (`id`);

-- 流程日志关联
-- ALTER TABLE `declare_process_log` ADD CONSTRAINT `fk_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `system_users` (`id`);

-- 政策通知回执关联
-- ALTER TABLE `declare_notice_receipt` ADD CONSTRAINT `fk_notice_receipt_policy` FOREIGN KEY (`policy_id`) REFERENCES `declare_policy` (`id`) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;
