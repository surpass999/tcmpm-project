package cn.gemrun.base.module.declare.service.progress;

import cn.gemrun.base.module.declare.vo.progress.*;
import javax.validation.Valid;

import java.util.List;

public interface DeclareProgressReportService {

    /**
     * 创建填报记录
     */
    Long createReport(@Valid DeclareProgressReportCreateReqVO reqVO);

    /**
     * 获取填报详情
     */
    DeclareProgressReportVO getReport(Long id);

    /**
     * 获取填报列表（医院端）
     */
    List<DeclareProgressReportVO> getReportListByHospital(Long hospitalId, Integer reportYear);

    /**
     * 获取填报列表（省级端）
     */
    List<DeclareProgressReportVO> getReportListByProvince(String provinceCode, Integer reportYear);

    /**
     * 获取省级待审核列表
     */
    List<DeclareProgressReportVO> getProvincePendingList(String provinceCode);

    /**
     * 获取省级审核通过待上报列表
     */
    List<DeclareProgressReportVO> getProvinceApprovedList(String provinceCode);

    /**
     * 提交审核（医院内部提交给审核员）
     */
    void submitReport(Long id);

    /**
     * 医院审核员审核
     */
    void hospitalAudit(@Valid DeclareProgressReportAuditReqVO reqVO);

    /**
     * 省级审核
     */
    void provinceAudit(@Valid DeclareProgressReportAuditReqVO reqVO);

    /**
     * 保存填报记录及指标值（一体化接口）
     *
     * @param reqVO 保存请求
     * @return 报告ID
     */
    Long saveReport(DeclareProgressReportSaveReqVO reqVO);

    /**
     * 保存指标值
     *
     * @param reportId 填报记录ID
     * @param indicatorIds 指标ID列表
     * @param values 值列表
     */
    void saveIndicatorValues(Long reportId, List<Long> indicatorIds, List<String> values);

    /**
     * 获取填报历史
     */
    List<DeclareProgressReportVO> getReportHistory(Long hospitalId, Integer reportYear);

    /**
     * 检查医院是否在时间窗口内
     */
    boolean isInReportWindow(Long hospitalId);

    /**
     * 检查医院是否超过填报次数限制
     */
    boolean isOverLimit(Long hospitalId, Integer reportYear);

    /**
     * 删除填报记录（仅 DRAFT/SAVED 状态可删除）
     */
    void deleteReport(Long id);

    /**
     * 获取两条申报记录的数据对比视图
     *
     * @param reportIdA 申报记录A的ID
     * @param reportIdB 申报记录B的ID
     * @return 对比数据（含两条记录的基础信息和指标值对比）
     */
    DeclareCompareDataVO getCompareData(Long reportIdA, Long reportIdB);

    /**
     * 国家局高级搜索（基本信息 + 指标值条件）
     */
    List<DeclareProgressReportVO> nationalSearch(DeclareNationalSearchReqVO reqVO);
}
