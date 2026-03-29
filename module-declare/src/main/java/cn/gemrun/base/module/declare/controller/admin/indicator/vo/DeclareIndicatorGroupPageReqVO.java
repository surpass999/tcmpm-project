package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标分组分页 Request VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeclareIndicatorGroupPageReqVO extends PageParam {

    /**
     * 分组编码
     */
    private String groupCode;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组层级：1=一级 2=二级
     */
    private Integer groupLevel;

    /**
     * 关联项目类型（0-6）
     */
    private Integer projectType;

    /**
     * 状态：0-禁用 1=启用
     */
    private Integer status;

}
