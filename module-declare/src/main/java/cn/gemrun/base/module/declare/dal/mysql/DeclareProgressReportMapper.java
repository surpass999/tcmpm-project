package cn.gemrun.base.module.declare.dal.mysql;

import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareProgressReportDO;
import cn.gemrun.base.module.declare.vo.progress.DeclareNationalSearchReqVO;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 进度填报Mapper
 *
 * @author Gemini
 */
@Mapper
public interface DeclareProgressReportMapper extends BaseMapperX<DeclareProgressReportDO> {

    /**
     * 查询医院在某年某批次的填报记录
     */
    default DeclareProgressReportDO selectByHospitalAndPeriod(
            @Param("hospitalId") Long hospitalId,
            @Param("reportYear") Integer reportYear,
            @Param("reportBatch") Integer reportBatch) {
        return selectOne(new LambdaQueryWrapperX<DeclareProgressReportDO>()
                .eq(DeclareProgressReportDO::getHospitalId, hospitalId)
                .eq(DeclareProgressReportDO::getReportYear, reportYear)
                .eq(DeclareProgressReportDO::getReportBatch, reportBatch));
    }

    /**
     * 查询医院某年度已填报的批次数量
     */
    default long countByHospitalAndYear(
            @Param("hospitalId") Long hospitalId,
            @Param("reportYear") Integer reportYear) {
        return selectCount(new LambdaQueryWrapperX<DeclareProgressReportDO>()
                .eq(DeclareProgressReportDO::getHospitalId, hospitalId)
                .eq(DeclareProgressReportDO::getReportYear, reportYear)
                .ne(DeclareProgressReportDO::getReportStatus, 0));
    }

    /**
     * 查询医院在某年某批次的填报记录（按 deptId 查询，前端传入的 hospitalId 实际为 deptId）
     * 使用 selectList + LIMIT 1 替代 selectOne，避免重复数据时抛 TooManyResultsException
     */
    default DeclareProgressReportDO selectByDeptAndPeriod(
            @Param("deptId") Long deptId,
            @Param("reportYear") Integer reportYear,
            @Param("reportBatch") Integer reportBatch) {
        List<DeclareProgressReportDO> list = selectList(new LambdaQueryWrapperX<DeclareProgressReportDO>()
                .eq(DeclareProgressReportDO::getDeptId, deptId)
                .eq(DeclareProgressReportDO::getReportYear, reportYear)
                .eq(DeclareProgressReportDO::getReportBatch, reportBatch)
                .orderByDesc(DeclareProgressReportDO::getCreateTime)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 统计已有填报记录的医院数量（去重）
     */
    int selectReportedHospitalCount();

    /**
     * 统计总填报记录数
     */
    int selectTotalReportCount();

    /**
     * 统计填报状态为 SUBMITTED（待审批）的记录数
     */
    int selectPendingReportCount();

    /**
     * 统计省级审核中（province_status = 1）的记录数
     */
    int selectProvincePendingCount();

    /**
     * 按省份统计省级审核中（province_status = 1）的记录数
     */
    int selectProvincePendingCountByProvince(String provinceCode);

    /**
     * 按省份统计已有填报记录的医院数量（去重）
     */
    int selectReportedHospitalCountByProvince(String provinceCode);

    /**
     * 国家局高级搜索（基本信息 + 指标值条件）
     */
    List<DeclareProgressReportDO> selectNationalSearch(@Param("req") DeclareNationalSearchReqVO reqVO);

}
