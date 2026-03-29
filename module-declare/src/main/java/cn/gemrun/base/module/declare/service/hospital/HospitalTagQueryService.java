package cn.gemrun.base.module.declare.service.hospital;

import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DimensionStatVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareProgressReportVO;

import java.util.List;
import java.util.Map;

public interface HospitalTagQueryService {

    /**
     * 多维度标签查询
     * @param tagIds 标签ID列表（AND条件）
     * @return 符合条件的医院编码列表
     */
    List<String> queryHospitalCodesByTags(List<Long> tagIds);

    /**
     * 统计各维度医院分布
     */
    Map<String, Long> statisticsByCategory(String category);

    /**
     * 跨维度统计
     */
    List<DimensionStatVO> crossStatistics(String category1, String category2);

    /**
     * 按标签筛选进度填报列表
     */
    List<DeclareProgressReportVO> queryReportByTags(List<Long> tagIds, Integer reportYear, Integer reportBatch);
}
