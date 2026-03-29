package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TagCategoryEnum {

    REGION("region", "区域"),
    LEVEL("level", "等级"),
    FEATURE("feature", "特征"),
    ATTRIBUTE("attribute", "属性");

    private final String code;
    private final String name;
}
