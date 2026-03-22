-- 为 declare_process_indicator_config 表添加审计字段
-- 修复错误：Unknown column 'deleted' in 'where clause'

ALTER TABLE `declare_process_indicator_config` 
ADD COLUMN `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除（0=否，1=是）' AFTER `sort`,
ADD COLUMN `delete_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '删除原因' AFTER `deleted`,
ADD COLUMN `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者' AFTER `delete_reason`,
ADD COLUMN `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者' AFTER `creator`;

-- 调整 create_time 和 update_time 字段位置（如果需要）
-- ALTER TABLE `declare_process_indicator_config` MODIFY COLUMN `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
-- ALTER TABLE `declare_process_indicator_config` MODIFY COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 添加版本号字段（乐观锁）
ALTER TABLE `declare_process_indicator_config`
ADD COLUMN `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updater`;
