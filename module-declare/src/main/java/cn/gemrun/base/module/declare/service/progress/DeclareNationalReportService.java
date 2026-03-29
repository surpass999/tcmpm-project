package cn.gemrun.base.module.declare.service.progress;

import cn.gemrun.base.module.declare.vo.progress.*;

import java.util.List;

public interface DeclareNationalReportService {

    /**
     * 批量上报国家局
     */
    Long batchReport(BatchNationalReportReqVO reqVO);

    /**
     * 获取上报记录列表
     */
    List<DeclareNationalReportRecordVO> getRecordList(String provinceCode);

    /**
     * 获取上报记录详情
     */
    DeclareNationalReportRecordVO getRecord(Long id);
}
