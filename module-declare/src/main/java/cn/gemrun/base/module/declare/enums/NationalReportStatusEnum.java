package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NationalReportStatusEnum {

    NOT_REPORTED(0, "未上报"),
    REPORTED(1, "已上报");

    private final Integer status;
    private final String name;
}
