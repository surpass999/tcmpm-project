package cn.gemrun.base.module.declare.controller.admin.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 驾驶舱项目进度响应VO
 *
 * @author Gemini
 */
@Data
public class ProjectProgressVO {

    /**
     * 项目总数
     */
    private Integer totalProjectCount;

    /**
     * 平均进度(%)
     */
    private Integer averageProgress;

    /**
     * 按时完成项目数
     */
    private Integer onTimeCount;

    /**
     * 延期项目数
     */
    private Integer delayedCount;

    /**
     * 各阶段项目分布
     */
    private List<StageItem> stageDistribution;

    /**
     * 各省进度对比
     */
    private List<ProvinceProgress> provinceProgress;

    @Data
    public static class StageItem {
        /** 阶段名称 */
        private String stageName;
        /** 阶段值: 1=立项, 2=设计, 3=实施, 4=验收 */
        private Integer stageValue;
        /** 项目数量 */
        private Integer count;
        /** 占比(%) */
        private Integer percentage;
    }

    @Data
    public static class ProvinceProgress {
        /** 省份名称 */
        private String provinceName;
        /** 省份ID */
        private Long provinceId;
        /** 平均进度(%) */
        private Integer progress;
        /** 项目数量 */
        private Integer projectCount;
    }
}
