package cn.gemrun.base.module.declare.controller.admin.policy.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 政策通知详情响应 VO
 *
 * @author Gemini
 */
@Data
public class PolicyRespVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 政策/通知标题
     */
    private String policyTitle;

    /**
     * 政策/通知正文
     */
    private String policyContent;

    /**
     * 政策摘要
     */
    private String policySummary;

    /**
     * 发布单位
     */
    private String releaseDept;

    /**
     * 发布时间
     */
    private LocalDateTime releaseTime;

    /**
     * 类型：1=政策文件，2=工作通知
     */
    private Integer policyType;

    /**
     * 目标范围：1=全国，2=全省
     */
    private Integer targetScope;

    /**
     * 适用项目类型（逗号分隔，如1,2,3）
     */
    private String targetProjectTypes;

    /**
     * 附件URL列表
     */
    private List<String> attachmentUrls;

    /**
     * 状态：1=已发布，2=已下架
     */
    private Integer status;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 发布人名称
     */
    private String publisherName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
