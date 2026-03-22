package cn.gemrun.base.module.declare.dal.dataobject.policy;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * 政策通知 DO
 *
 * @author
 */
@TableName("declare_policy")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDO extends BaseDO {

    /**
     * 主键（自增）
     */
    @TableId
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
     * 附件集合（多个附件，用逗号分隔）
     */
    private String attachments;

    /**
     * 状态：1=已发布，2=已下架
     */
    private Integer status;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 发布人名称（非数据库字段，用于展示）
     */
    @TableField(exist = false)
    private String publisherName;

}
