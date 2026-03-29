package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 申报模块权限常量
 */
@Getter
@AllArgsConstructor
public enum DeclarePermissionConstants {

    // 医院标签
    HOSPITAL_TAG_READ("declare:hospital-tag:read", "查看医院标签"),
    HOSPITAL_TAG_CREATE("declare:hospital-tag:create", "创建医院标签"),
    HOSPITAL_TAG_UPDATE("declare:hospital-tag:update", "更新医院标签"),
    HOSPITAL_TAG_DELETE("declare:hospital-tag:delete", "删除医院标签"),
    HOSPITAL_TAG_ASSIGN("declare:hospital-tag:assign", "分配医院标签"),

    // 医院管理
    HOSPITAL_CONFIG_TAG("declare:hospital:config-tag", "配置医院标签"),

    // 进度填报
    PROGRESS_REPORT_READ("declare:progress-report:read", "查看进度填报"),
    PROGRESS_REPORT_CREATE("declare:progress-report:create", "创建进度填报"),
    PROGRESS_REPORT_SUBMIT("declare:progress-report:submit", "提交进度填报"),
    PROGRESS_REPORT_HOSPITAL_AUDIT("declare:progress-report:hospital-audit", "医院审核进度填报"),
    PROGRESS_REPORT_PROVINCE_AUDIT("declare:progress-report:province-audit", "省级审核进度填报"),

    // 国家局上报
    NATIONAL_REPORT_BATCH("declare:national-report:batch", "批量上报国家局"),

    // 时间窗口
    REPORT_WINDOW_READ("declare:report-window:read", "查看填报时间窗口"),
    REPORT_WINDOW_CREATE("declare:report-window:create", "创建填报时间窗口"),
    REPORT_WINDOW_UPDATE("declare:report-window:update", "更新填报时间窗口"),
    REPORT_WINDOW_DELETE("declare:report-window:delete", "删除填报时间窗口"),
    ;

    private final String permission;
    private final String name;
}
