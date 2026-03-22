package cn.gemrun.base.module.declare.controller.admin.policy.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知回执分页请求 VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeReceiptPageReqVO extends PageParam {

    /**
     * 政策ID
     */
    private Long policyId;

    /**
     * 阅读人ID
     */
    private Long readerId;

    /**
     * 机构名称
     */
    private String orgName;

    /**
     * 阅读状态：1=已阅读，2=已过期
     */
    private Integer readStatus;

    /**
     * 回执状态：1=已阅读，2=已反馈，3=已取消
     */
    private Integer receiptStatus;

}
