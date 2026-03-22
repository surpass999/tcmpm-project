package cn.gemrun.base.module.declare.controller.admin.policy.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知回执响应 VO
 *
 * @author Gemini
 */
@Data
public class NoticeReceiptRespVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 政策ID
     */
    private Long policyId;

    /**
     * 政策标题（冗余）
     */
    private String policyTitle;

    /**
     * 接收机构名称
     */
    private String orgName;

    /**
     * 阅读人ID
     */
    private Long readerId;

    /**
     * 阅读人姓名
     */
    private String readerName;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 阅读状态：1=已阅读，2=已过期
     */
    private Integer readStatus;

    /**
     * 反馈意见
     */
    private String feedbackContent;

    /**
     * 反馈时间
     */
    private LocalDateTime feedbackTime;

    /**
     * 回执状态：1=已阅读，2=已反馈，3=已取消
     */
    private Integer receiptStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
