package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProvinceStatusEnum {

    NOT_SUBMITTED(0, "未提交"),
    AUDITING(1, "省级审核中"),
    APPROVED(2, "省级通过"),
    REJECTED(3, "省级驳回");

    private final Integer status;
    private final String name;
}
