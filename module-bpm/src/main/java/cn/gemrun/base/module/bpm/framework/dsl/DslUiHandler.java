package cn.gemrun.base.module.bpm.framework.dsl;

import cn.gemrun.base.module.bpm.framework.dsl.config.DslConfig;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslUi;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DSL UI 配置处理器
 * 基于 DSL 的 ui 配置处理表单和只读逻辑
 *
 * @author Gemini
 */
@Slf4j
@Component
public class DslUiHandler {

    /**
     * 获取 UI 配置
     *
     * @param dslConfig 节点 DSL 配置
     * @return UI 配置
     */
    public DslUi getUiConfig(DslConfig dslConfig) {
        if (dslConfig == null) {
            return null;
        }
        return dslConfig.getUi();
    }

    /**
     * 获取表单标识
     *
     * @param dslConfig 节点 DSL 配置
     * @return 表单标识
     */
    public String getFormKey(DslConfig dslConfig) {
        DslUi ui = getUiConfig(dslConfig);
        if (ui == null || StrUtil.isBlank(ui.getForm())) {
            return null;
        }
        return ui.getForm();
    }

    /**
     * 判断是否只读
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否只读
     */
    public boolean isReadonly(DslConfig dslConfig) {
        DslUi ui = getUiConfig(dslConfig);
        return ui != null && Boolean.TRUE.equals(ui.getReadonly());
    }

    /**
     * 从 DSL JSON 字符串获取 UI 配置
     *
     * @param dslJson DSL JSON 字符串
     * @return UI 配置
     */
    public DslUi getUiConfigFromJson(String dslJson) {
        if (StrUtil.isBlank(dslJson)) {
            return null;
        }

        try {
            JSONObject jsonObject = JSONUtil.parseObj(dslJson);
            if (!jsonObject.containsKey("ui")) {
                return null;
            }
            JSONObject uiJson = jsonObject.getJSONObject("ui");
            if (uiJson == null) {
                return null;
            }

            DslUi ui = new DslUi();
            ui.setForm(uiJson.getStr("form"));
            ui.setReadonly(uiJson.getBool("readonly"));
            return ui;
        } catch (Exception e) {
            log.warn("[DslUiHandler] 解析 UI 配置失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取表单字段权限
     * 根据 UI 配置确定哪些字段可见/可编辑
     *
     * @param dslConfig 节点 DSL 配置
     * @param allFields 所有字段列表
     * @return 字段权限映射
     */
    public FieldPermissions getFieldPermissions(DslConfig dslConfig, java.util.List<String> allFields) {
        FieldPermissions permissions = new FieldPermissions();

        // 默认所有字段可见
        if (allFields != null) {
            permissions.setVisibleFields(new java.util.HashSet<>(allFields));
        }

        // 如果是只读模式，所有字段都不可编辑
        if (isReadonly(dslConfig)) {
            permissions.setEditableFields(new java.util.HashSet<>());
        }

        return permissions;
    }

    /**
     * 根据节点能力(Cap)推断默认的 UI 配置
     *
     * @param cap 节点能力
     * @return UI 配置
     */
    public DslUi getDefaultUiByCap(String cap) {
        DslUi ui = new DslUi();

        if (cap == null) {
            return ui;
        }

        switch (cap.toUpperCase()) {
            case "FILL":
                // 填报：可编辑
                ui.setReadonly(false);
                break;
            case "MODIFY":
                // 补正：可编辑，但字段可能受限（vars.modifyFields）
                ui.setReadonly(false);
                break;
            case "AUDIT":
            case "COUNTERSIGN":
            case "CONFIRM":
            case "ARCHIVE":
            case "PUBLISH":
                // 审批/会签/确认/归档/发布：只读
                ui.setReadonly(true);
                break;
            default:
                break;
        }

        return ui;
    }

    /**
     * 字段权限
     */
    @Data
    public static class FieldPermissions {
        /**
         * 可见字段
         */
        private java.util.Set<String> visibleFields;

        /**
         * 可编辑字段
         */
        private java.util.Set<String> editableFields;

        /**
         * 必填字段
         */
        private java.util.Set<String> requiredFields;
    }

}
