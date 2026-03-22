-- =============================================
-- 培训工作交流模块 - 数据表
-- =============================================

-- ----------------------------
-- 1. 培训交流活动表
-- ----------------------------
DROP TABLE IF EXISTS `declare_training`;
CREATE TABLE `declare_training` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` varchar(200) NOT NULL COMMENT '活动名称',
    `type` tinyint NOT NULL COMMENT '活动类型：1=推进会，2=专题研讨，3=系统演示，4=业务培训',
    `content` text COMMENT '活动详情（富文本）',
    `organizer` varchar(200) COMMENT '组织单位',
    `speaker` varchar(200) COMMENT '主讲人/嘉宾',
    `start_time` datetime NOT NULL COMMENT '开始时间',
    `end_time` datetime NOT NULL COMMENT '结束时间',
    `location` varchar(200) COMMENT '活动地点',
    `online_link` varchar(500) COMMENT '线上参与链接',
    `target_scope` tinyint NOT NULL DEFAULT 1 COMMENT '目标范围：1=全国，2=全省',
    `target_provinces` varchar(500) COMMENT '目标省份（逗号分隔，如：广东省,浙江省）',
    `registration_deadline` datetime COMMENT '报名截止时间',
    `max_participants` int COMMENT '最大参与人数（空表示不限）',
    `current_participants` int DEFAULT 0 COMMENT '当前报名人数',
    `attachments` varchar(1000) COMMENT '附件（多个逗号分隔，存储URL）',
    `meeting_materials` text COMMENT '会议资料/培训材料',
    `poster_url` varchar(500) COMMENT '活动海报URL',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=草稿，2=报名中，3=进行中，4=已结束，5=已取消',
    `publish_time` datetime COMMENT '发布时间',
    `publisher_id` bigint COMMENT '发布人ID',
    `publisher_name` varchar(100) COMMENT '发布人姓名',
    `remark` varchar(500) COMMENT '备注',
    `dept_id` bigint COMMENT '部门ID',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训交流活动表';

-- ----------------------------
-- 2. 活动报名表
-- ----------------------------
DROP TABLE IF EXISTS `declare_training_registration`;
CREATE TABLE `declare_training_registration` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `training_id` bigint NOT NULL COMMENT '活动ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `user_name` varchar(100) NOT NULL COMMENT '报名人姓名',
    `organization` varchar(200) COMMENT '所属单位',
    `position` varchar(100) COMMENT '职位/职称',
    `phone` varchar(20) COMMENT '联系电话',
    `email` varchar(100) COMMENT '邮箱',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=已报名，2=已签到，3=已取消，4=未出席',
    `register_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `sign_in_time` datetime COMMENT '签到时间',
    `cancel_time` datetime COMMENT '取消时间',
    `feedback` text COMMENT '参与反馈',
    `rating` tinyint COMMENT '评分（1-5分）',
    `attendance_certificate` varchar(100) COMMENT '参与证明编号',
    `remark` varchar(500) COMMENT '备注',
    `dept_id` bigint COMMENT '部门ID',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_training_user` (`training_id`, `user_id`, `deleted`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_training_id` (`training_id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动报名表';

-- ----------------------------
-- 3. 初始化字典数据
-- ----------------------------
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '推进会', '1', 'declare_training_type', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(2, '专题研讨', '2', 'declare_training_type', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(3, '系统演示', '3', 'declare_training_type', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(4, '业务培训', '4', 'declare_training_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '全国', '1', 'declare_target_scope', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(2, '全省', '2', 'declare_target_scope', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '草稿', '1', 'declare_training_status', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(2, '报名中', '2', 'declare_training_status', 0, 'processing', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(3, '进行中', '3', 'declare_training_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(4, '已结束', '4', 'declare_training_status', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(5, '已取消', '5', 'declare_training_status', 0, 'error', '', '', 'admin', NOW(), 'admin', NOW(), 0);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '已报名', '1', 'declare_registration_status', 0, 'processing', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(2, '已签到', '2', 'declare_registration_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(3, '已取消', '3', 'declare_registration_status', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), 0),
(4, '未出席', '4', 'declare_registration_status', 0, 'error', '', '', 'admin', NOW(), 'admin', NOW(), 0);

-- ----------------------------
-- 4. 初始化通知模板（复用系统通知模块）
-- ----------------------------
INSERT INTO `system_notify_template` (`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
('培训活动发布通知', 'training_publish', 1, '系统管理员', '【培训活动】您有一条新的培训活动「{trainingName}」，将于 {startTime} 在 {location} 举行，欢迎报名参加！', '["trainingName","startTime","location"]', 0, '培训活动发布时发送给目标用户', 'admin', NOW(), 'admin', NOW(), 0),
('培训活动报名成功通知', 'training_register_success', 1, '系统管理员', '【报名成功】您已成功报名「{trainingName}」，活动将于 {startTime} 开始，请准时参加！', '["trainingName","startTime"]', 0, '用户报名成功后发送', 'admin', NOW(), 'admin', NOW(), 0),
('培训活动开始提醒', 'training_reminder', 1, '系统管理员', '【活动提醒】「{trainingName}」将于 {startTime} 开始，请提前做好准备！', '["trainingName","startTime"]', 0, '活动开始前24小时发送', 'admin', NOW(), 'admin', NOW(), 0),
('培训活动变更通知', 'training_change', 1, '系统管理员', '【活动变更】「{trainingName}」有重要变更：{changeContent}，请注意查看！', '["trainingName","changeContent"]', 0, '活动时间、地点等变更时发送', 'admin', NOW(), 'admin', NOW(), 0),
('培训活动结束通知', 'training_finish', 1, '系统管理员', '【活动结束】「{trainingName}」已结束，感谢您的参与！请填写反馈问卷。', '["trainingName"]', 0, '活动结束后发送', 'admin', NOW(), 'admin', NOW(), 0);
