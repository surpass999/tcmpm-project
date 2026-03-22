package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 过程填报表单配置 Response VO
 *
 * @author Gemini
 */
@Data
public class ProcessFormConfigRespVO {

    // ========== 过程记录信息 ==========

    /**
     * 过程记录ID
     */
    private Long processId;

    /**
     * 过程类型
     */
    private Integer processType;

    /**
     * 过程类型名称（从字典获取）
     */
    private String processTypeName;

    /**
     * 过程标题
     */
    private String processTitle;

    /**
     * 报告周期开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime reportPeriodStart;

    /**
     * 报告周期结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime reportPeriodEnd;

    // ========== 项目基本信息 ==========

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目类型
     */
    private Integer projectType;

    /**
     * 项目类型名称（从字典获取）
     */
    private String projectTypeName;

    /**
     * 项目负责人
     */
    private String projectLeader;

    /**
     * 建设周期
     */
    private String constructionPeriod;

    /**
     * 项目状态
     */
    private String projectStatus;

    /**
     * 项目状态名称
     */
    private String projectStatusName;

    // ========== 指标配置（动态渲染表单） ==========

    /**
     * 指标配置列表
     */
    private List<IndicatorFormItem> indicators;

    // ========== 文本字段配置 ==========

    /**
     * 文本字段配置列表
     */
    private List<TextFieldConfig> textFields;

    // ========== 填报数据 ==========

    /**
     * 填报状态
     */
    private String status;

    /**
     * 填报状态名称（从字典获取）
     */
    private String statusName;

    /**
     * 填报人姓名
     */
    private String createName;

    /**
     * 填报时间
     */
    private LocalDateTime createTime;

    /**
     * 过程数据JSON（文本填报内容）
     */
    private String processData;

    /**
     * 指标值JSON
     */
    private String indicatorValues;

    /**
     * 附件IDs
     */
    private String attachmentIds;

    /**
     * 指标版本（已同步次数）
     */
    private Integer indicatorVersion;

    // ========== 内部类 ==========

    /**
     * 指标表单项
     */
    @Data
    public static class IndicatorFormItem {

        /**
         * 指标配置ID
         */
        private Long configId;

        /**
         * 指标ID
         */
        private Long indicatorId;

        /**
         * 指标编码
         */
        private String indicatorCode;

        /**
         * 指标名称
         */
        private String indicatorName;

        /**
         * 指标分类
         */
        private Integer category;

        /**
         * 值类型：1=数值，2=文本，3=单选，4=日期，5=多行文本，6=多选，7=年份，8=季度，9=文件
         */
        private Integer valueType;

        /**
         * 单位
         */
        private String unit;

        /**
         * 是否必填
         */
        private Boolean isRequired;

        /**
         * 排序
         */
        private Integer sort;

        /**
         * 填报要求说明
         */
        private String fillRequire;

        /**
         * 当前已填报值
         */
        private Object currentValue;
    }

    /**
     * 文本字段配置
     */
    @Data
    public static class TextFieldConfig {

        /**
         * 字段编码
         */
        private String fieldCode;

        /**
         * 字段名称
         */
        private String fieldName;

        /**
         * 是否必填
         */
        private Boolean isRequired;

        /**
         * 最大长度
         */
        private Integer maxLength;

        /**
         * 字段类型：1=单行文本，2=多行文本，3=日期
         */
        private Integer fieldType;
    }

}
