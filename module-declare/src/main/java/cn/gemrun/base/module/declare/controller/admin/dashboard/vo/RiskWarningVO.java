package cn.gemrun.base.module.declare.controller.admin.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 驾驶舱风险预警响应VO
 *
 * @author Gemini
 */
@Data
public class RiskWarningVO {

    /**
     * 预警总数
     */
    private Integer totalCount;

    /**
     * 高风险预警数
     */
    private Integer highRiskCount;

    /**
     * 中风险预警数
     */
    private Integer mediumRiskCount;

    /**
     * 低风险预警数
     */
    private Integer lowRiskCount;

    /**
     * 预警列表
     */
    private List<WarningItem> warnings;

    @Data
    public static class WarningItem {
        /** 预警ID */
        private Long id;
        /** 预警类型: progress=进度预警, fund=资金预警, quality=质量预警 */
        private String warningType;
        /** 预警级别: high=高, medium=中, low=低 */
        private String level;
        /** 预警标题 */
        private String title;
        /** 预警描述 */
        private String description;
        /** 关联项目ID */
        private Long projectId;
        /** 关联项目名称 */
        private String projectName;
        /** 预警时间 */
        private String warningTime;
        /** 状态: pending=待处理, handled=已处理, ignored=已忽略 */
        private String status;
        /** 跳转链接 */
        private String jumpUrl;
    }
}
