package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NationalReportStatusEnum {

    NOT_REPORTED(0, "未上报"),
    AUDITING(1, "国家局审批中"),
    REPORTED(2, "已上报");

    private final Integer status;
    private final String name;
}
