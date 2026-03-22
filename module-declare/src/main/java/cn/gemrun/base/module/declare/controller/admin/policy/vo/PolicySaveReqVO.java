package cn.gemrun.base.module.declare.controller.admin.policy.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 政策通知保存请求 VO
 *
 * @author Gemini
 */
@Data
public class PolicySaveReqVO {

    /**
     * 主键（为空时为新增）
     */
    private Long id;

    /**
     * 政策/通知标题
     */
    @NotBlank(message = "政策标题不能为空")
    private String policyTitle;

    /**
     * 政策/通知正文
     */
    @NotBlank(message = "政策正文不能为空")
    private String policyContent;

    /**
     * 政策摘要
     */
    private String policySummary;

    /**
     * 发布单位
     */
    @NotBlank(message = "发布单位不能为空")
    private String releaseDept;

    /**
     * 发布时间
     */
    private String releaseTime;

    /**
     * 类型：1=政策文件，2=工作通知
     */
    @NotNull(message = "政策类型不能为空")
    private Integer policyType;

    /**
     * 目标范围：1=全国，2=全省
     */
    @NotNull(message = "目标范围不能为空")
    private Integer targetScope;

    /**
     * 适用项目类型（逗号分隔，如1,2,3）
     */
    private String targetProjectTypes;

    /**
     * 附件URL列表
     */
    private List<String> attachmentUrls;

}
