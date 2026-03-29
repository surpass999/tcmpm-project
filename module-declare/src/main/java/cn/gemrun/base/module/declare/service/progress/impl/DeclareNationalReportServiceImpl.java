package cn.gemrun.base.module.declare.service.progress.impl;

import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareNationalReportRecordDO;
import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareProgressReportDO;
import cn.gemrun.base.module.declare.dal.mysql.DeclareNationalReportRecordMapper;
import cn.gemrun.base.module.declare.dal.mysql.DeclareProgressReportMapper;
import cn.gemrun.base.module.declare.enums.ProvinceStatusEnum;
import cn.gemrun.base.module.declare.service.progress.DeclareNationalReportService;
import cn.gemrun.base.module.declare.vo.progress.BatchNationalReportReqVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareNationalReportRecordVO;
import cn.gemrun.base.framework.common.util.json.JsonUtils;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.collection.CollUtil;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeclareNationalReportServiceImpl implements DeclareNationalReportService {

    private final DeclareNationalReportRecordMapper recordMapper;
    private final DeclareProgressReportMapper progressReportMapper;
    private final AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long batchReport(BatchNationalReportReqVO reqVO) {
        List<DeclareProgressReportDO> reports = progressReportMapper.selectBatchIds(reqVO.getReportIds());
        for (DeclareProgressReportDO report : reports) {
            if (report.getProvinceStatus() != ProvinceStatusEnum.APPROVED.getStatus()) {
                throw new RuntimeException("存在未通过省级审核的记录：" + report.getId());
            }
            if (report.getNationalReportStatus() == 1) {
                throw new RuntimeException("存在已上报的记录：" + report.getId());
            }
        }

        Long userId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        DeclareNationalReportRecordDO record = DeclareNationalReportRecordDO.builder()
                .reportIds(JsonUtils.toJsonString(reqVO.getReportIds()))
                .reportCount(reqVO.getReportIds().size())
                .provinceCode(reports.get(0).getProvinceCode())
                .provinceName(reports.get(0).getProvinceName())
                .reporterId(userId)
                .reporterName(user != null ? user.getNickname() : null)
                .reportTime(LocalDateTime.now())
                .remark(reqVO.getRemark())
                .build();
        recordMapper.insert(record);

        for (DeclareProgressReportDO report : reports) {
            report.setNationalReportStatus(1);
            report.setNationalReportTime(LocalDateTime.now());
            progressReportMapper.updateById(report);
        }

        return record.getId();
    }

    @Override
    public List<DeclareNationalReportRecordVO> getRecordList(String provinceCode) {
        LambdaQueryWrapperX<DeclareNationalReportRecordDO> wrapper = new LambdaQueryWrapperX<DeclareNationalReportRecordDO>()
                .eq(provinceCode != null, DeclareNationalReportRecordDO::getProvinceCode, provinceCode)
                .orderByDesc(DeclareNationalReportRecordDO::getCreateTime);
        return recordMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public DeclareNationalReportRecordVO getRecord(Long id) {
        DeclareNationalReportRecordDO record = recordMapper.selectById(id);
        if (record == null) {
            return null;
        }
        return convertToVO(record);
    }

    private DeclareNationalReportRecordVO convertToVO(DeclareNationalReportRecordDO record) {
        return DeclareNationalReportRecordVO.builder()
                .id(record.getId())
                .reportIds(record.getReportIds())
                .reportCount(record.getReportCount())
                .provinceName(record.getProvinceName())
                .reporterName(record.getReporterName())
                .reportTime(record.getReportTime())
                .remark(record.getRemark())
                .createTime(record.getCreateTime())
                .build();
    }
}
