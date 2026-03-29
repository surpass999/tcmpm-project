package cn.gemrun.base.module.declare.controller.admin.project.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目类型分页请求 VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectTypePageReqVO extends PageParam {

    /**
     * 类型编码
     */
    private String typeCode;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

}
