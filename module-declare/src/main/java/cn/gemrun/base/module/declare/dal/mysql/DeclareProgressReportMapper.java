package cn.gemrun.base.module.declare.dal.mysql;

import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareProgressReportDO;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

}
