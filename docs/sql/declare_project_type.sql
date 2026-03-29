-- =============================================
-- 项目类型管理模块 - 数据库脚本
-- =============================================

-- ----------------------------
-- 1. 新建表：declare_project_type
-- ----------------------------
DROP TABLE IF EXISTS `declare_project_type`;
CREATE TABLE `declare_project_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_code` varchar(32) NOT NULL COMMENT '类型编码（唯一标识，如 PROJECT_01）',
  `type_value` int(11) NOT NULL COMMENT '类型值（业务使用：1=综合型，2=示范型等）',
  `name` varchar(64) NOT NULL COMMENT '类型名称（如 综合型）',
  `title` varchar(128) NOT NULL COMMENT '显示标题（如 综合型医院）',
  `description` varchar(512) DEFAULT NULL COMMENT '类型描述',
  `icon` varchar(128) DEFAULT NULL COMMENT '图标名称或URL',
  `color` varchar(32) DEFAULT NULL COMMENT '主题颜色（如 #1890ff）',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序（升序）',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：0=禁用，1=启用',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`),
  UNIQUE KEY `uk_type_value` (`type_value`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='项目类型定义表';

-- ----------------------------
-- 2. 插入初始数据
-- ----------------------------
INSERT INTO `declare_project_type` (`id`, `type_code`, `type_value`, `name`, `title`, `description`, `icon`, `color`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'PROJECT_01', 1, '综合型', '综合型医院', '具备完整中医信息化建设的综合医院', NULL, '#1890ff', 1, 1, 'system', NOW(), NULL, NOW(), b'0'),
(2, 'PROJECT_02', 2, '示范型', '示范型医院', '具有示范引领作用的医院', NULL, '#52c41a', 2, 1, 'system', NOW(), NULL, NOW(), b'0'),
(3, 'PROJECT_03', 3, '培育型', '培育型医院', '处于培育发展阶段的医院', NULL, '#faad14', 3, 1, 'system', NOW(), NULL, NOW(), b'0'),
(4, 'PROJECT_04', 4, '协作型', '协作型医院', '参与医共体/医联体协作的医院', NULL, '#722ed1', 4, 1, 'system', NOW(), NULL, NOW(), b'0'),
(5, 'PROJECT_05', 5, '县域医共体', '县域医共体', '县域医疗服务共同体牵头医院', NULL, '#13c2c2', 5, 1, 'system', NOW(), NULL, NOW(), b'0'),
(6, 'PROJECT_06', 6, '城市医疗集团', '城市医疗集团', '城市医疗集团牵头医院', NULL, '#eb2f96', 6, 1, 'system', NOW(), NULL, NOW(), b'0');

-- ----------------------------
-- 3. 配置菜单（可选，按需执行）
-- ----------------------------
-- 注意：需要根据现有菜单 ID 结构调整 parent_id
-- 假设 5047 是 "申报配置管理" 父菜单
-- INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `menu_type`, `status`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
-- VALUES
-- (99998, '项目类型管理', '', 1, 10, 5047, 'project-type', 'ri:database-line', 'declare/project-type/index', NULL, 1, 0, 1, 1, 'system', NOW(), NULL, NOW(), b'0');
