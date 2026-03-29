package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportStatusEnum {

    DRAFT("DRAFT", "草稿"),
    SAVED("SAVED", "已保存"),
    SUBMITTED("SUBMITTED", "待审批");

    private final String status;
    private final String name;

    /**
     * 根据状态值获取枚举
     */
    public static ReportStatusEnum getByStatus(String status) {
        if (status == null) {
            return null;
        }
        for (ReportStatusEnum enumValue : values()) {
            if (enumValue.status.equals(status)) {
                return enumValue;
            }
        }
        return null;
    }
}
