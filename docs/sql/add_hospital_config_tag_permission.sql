-- 在医院管理菜单(parent_id=100115)下新增"配置标签"按钮权限
-- system_menu 表结构: id, name, permission, type, sort, parent_id, path, icon, component, componentName, status, visible, keepAlive, alwaysShow, creator, create_time, updater, update_time, deleted
INSERT INTO system_menu
(name, permission, type, sort, parent_id, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
('配置标签', 'declare:hospital:config-tag', 3, 6, 100115, 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0');
