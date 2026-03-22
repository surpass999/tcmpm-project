-- =====================================================
-- 经验库/案例展示/工具包功能 - 字典数据扩展
-- 执行方式：在数据库中执行此SQL
-- =====================================================

-- 1. 扩展政策类型字典 declare_policy_type
-- 注意：先查询 dict_type_id，然后插入
-- 查询ID: SELECT id FROM system_dict_type WHERE type = 'declare_policy_type';

-- 假设 dict_type_id = 1 (请根据实际查询结果调整)
-- 插入新的字典数据（经验分享、典型案例、工具模板）
INSERT INTO system_dict_data (name, value, sort, status, remark, dict_type_id, dict_type, creator, create_time, updater, update_time, deleted)
VALUES 
('经验分享', '3', 3, 0, '经验库类型', 1, 'declare_policy_type', 'admin', NOW(), 'admin', NOW(), 0),
('典型案例', '4', 4, 0, '案例展示类型', 1, 'declare_policy_type', 'admin', NOW(), 'admin', NOW(), 0),
('工具模板', '5', 5, 0, '工具包类型', 1, 'declare_policy_type', 'admin', NOW(), 'admin', NOW(), 0);

-- =====================================================
-- 菜单配置 - 在"信息发布与公众服务"菜单下添加子菜单
-- =====================================================

-- 先查询父菜单ID
-- SELECT id FROM system_menu WHERE name = '信息发布与公众服务' AND type = 1;

-- 假设父菜单ID = 100 (请根据实际查询结果调整)
-- 插入子菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, visible, keep_alive, always_show, frame_url, frame_app, app_id, remark, creator, create_time, updater, update_time, deleted)
VALUES 
('经验库', '', 1, 10, 100, '/declare/experience', 'mdi:book-open-variant', 'declare/policy/index', 0, 0, 0, 0, '', '', NULL, '', 'admin', NOW(), 'admin', NOW(), 0),
('案例展示', '', 1, 11, 100, '/declare/case', 'mdi:star-box-outline', 'declare/policy/index', 0, 0, 0, 0, '', '', NULL, '', 'admin', NOW(), 'admin', NOW(), 0),
('工具包', '', 1, 12, 100, '/declare/tool-template', 'mdi:toolbox', 'declare/policy/index', 0, 0, 0, 0, '', '', NULL, '', 'admin', NOW(), 'admin', NOW(), 0);
