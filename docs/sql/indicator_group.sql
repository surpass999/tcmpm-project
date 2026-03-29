-- =============================================
-- 指标分组表（支持两级树形结构）
-- =============================================
CREATE TABLE IF NOT EXISTS declare_indicator_group (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id         BIGINT COMMENT '父分组ID（顶级为0）',
    group_code        VARCHAR(32) NOT NULL COMMENT '分组编码（唯一）',
    group_name        VARCHAR(100) NOT NULL COMMENT '分组名称',
    group_level       TINYINT NOT NULL DEFAULT 1 COMMENT '分组层级：1=一级 2=二级',
    project_type      INT COMMENT '关联项目类型（0-6），一级分组必须填写，用于区分分组属于哪个项目类型',
    group_prefix      VARCHAR(50) COMMENT '分组前缀标识（如101、10101），用于描述分组特征',
    description       VARCHAR(500) COMMENT '分组描述',
    sort              INT DEFAULT 0 COMMENT '排序',
    status            TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1=启用',
    creator           VARCHAR(64),
    create_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           VARCHAR(64),
    update_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           BIT NOT NULL DEFAULT FALSE,

    UNIQUE INDEX idx_group_code (group_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_project_type (project_type),
    INDEX idx_status (status)
) COMMENT '指标分组表';

-- =============================================
-- 预置数据：综合型下的分组示例
-- =============================================
-- 一级分组：101基本情况
INSERT INTO declare_indicator_group (group_code, group_name, group_level, project_type, group_prefix, sort) VALUES
('GROUP_101', '101基本情况', 1, 1, '101', 1);

-- 二级分组
INSERT INTO declare_indicator_group (group_code, group_name, group_level, project_type, group_prefix, sort, parent_id) VALUES
('GROUP_10101', '10101基本情况子项1', 2, 1, '10101', 1, (SELECT id FROM (SELECT id FROM declare_indicator_group WHERE group_code='GROUP_101') AS t)),
('GROUP_10102', '10102基本情况子项2', 2, 1, '10102', 2, (SELECT id FROM (SELECT id FROM declare_indicator_group WHERE group_code='GROUP_101') AS t));

-- =============================================
-- 在 declare_indicator 表中增加 group_id 字段（如果不存在）
-- =============================================
-- ALTER TABLE declare_indicator ADD COLUMN group_id BIGINT COMMENT '所属分组ID';
-- CREATE INDEX idx_indicator_group_id ON declare_indicator(group_id);
