package cn.gemrun.base.module.declare.dal.dataobject.policy;

import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 政策通知回执 DO
 *
 * @author Gemini
 */
@TableName("declare_notice_receipt")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeReceiptDO extends BaseDO {

    /**
     * 主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 关联政策ID（declare_policy.id）
     */
    private Long policyId;

    /**
     * 接收机构名称
     */
    private String orgName;

    /**
     * 阅读人ID（关联system_users.id）
     */
    private Long readerId;

    /**
     * 阅读人姓名（冗余）
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

}
