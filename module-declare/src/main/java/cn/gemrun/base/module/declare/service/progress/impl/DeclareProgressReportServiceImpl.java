package cn.gemrun.base.module.declare.service.progress.impl;

import cn.gemrun.base.module.declare.dal.dataobject.hospital.DeclareHospitalDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorGroupDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareProgressReportDO;
import cn.gemrun.base.module.declare.dal.mysql.DeclareHospitalMapper;
import cn.gemrun.base.module.declare.dal.mysql.DeclareProgressReportMapper;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorGroupMapper;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorValueMapper;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import cn.gemrun.base.module.declare.enums.NationalReportStatusEnum;
import cn.gemrun.base.module.declare.enums.ProvinceStatusEnum;
import cn.gemrun.base.module.declare.enums.ReportStatusEnum;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorService;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorValueService;
import cn.gemrun.base.module.declare.service.progress.DeclareProgressReportService;
import cn.gemrun.base.module.declare.service.progress.DeclareReportWindowService;
import cn.gemrun.base.module.declare.service.project.ProjectTypeService;
import cn.gemrun.base.module.declare.vo.progress.*;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.bpm.api.task.BpmProcessInstanceApi;
import cn.gemrun.base.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.gemrun.base.module.bpm.service.business.BpmBusinessTypeService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
public class DeclareProgressReportServiceImpl implements DeclareProgressReportService {

    private final DeclareProgressReportMapper progressReportMapper;
    private final DeclareHospitalMapper hospitalMapper;
    private final DeclareIndicatorValueService indicatorValueService;
    private final DeclareIndicatorValueMapper indicatorValueMapper;
    private final DeclareIndicatorService indicatorService;
    private final DeclareIndicatorGroupMapper indicatorGroupMapper;
    @Lazy
    private final DeclareReportWindowService reportWindowService;
    private final ProjectTypeService projectTypeService;
    private final AdminUserApi adminUserApi;
    private final BpmProcessInstanceApi processInstanceApi;
    private final BpmBusinessTypeService bpmBusinessTypeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(DeclareProgressReportCreateReqVO reqVO) {
        // 从当前登录用户获取 deptId
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        Long deptId = user != null ? user.getDeptId() : null;

        DeclareHospitalDO hospital = hospitalMapper.selectByDeptId(deptId);
        if (hospital == null) {
            throw new RuntimeException("医院信息不存在，请确认当前用户已关联医院部门");
        }

        // 防止重复创建：检查该医院本批次是否已有记录
        DeclareProgressReportDO existing = progressReportMapper.selectByDeptAndPeriod(
                deptId, reqVO.getReportYear(), reqVO.getReportBatch());
        if (existing != null) {
            throw new RuntimeException("该医院本批次已存在填报记录（id=" + existing.getId() + "），请直接编辑现有记录，勿重复创建");
        }

        // 防止超限
        if (isOverLimit(deptId, reqVO.getReportYear())) {
            throw new RuntimeException("该医院本年度填报次数已达上限（4次）");
        }

        DeclareProgressReportDO report = DeclareProgressReportDO.builder()
                .reportYear(reqVO.getReportYear())
                .reportBatch(reqVO.getReportBatch())
                .hospitalId(hospital.getId())
                .deptId(deptId)
                .hospitalName(hospital.getHospitalName())
                .provinceCode(hospital.getProvinceCode())
                .provinceName(hospital.getProvinceName())
                .reportStatus(ReportStatusEnum.DRAFT.getStatus())
                .provinceStatus(ProvinceStatusEnum.NOT_SUBMITTED.getStatus())
                .nationalReportStatus(NationalReportStatusEnum.NOT_REPORTED.getStatus())
                .creator(userId != null ? userId.toString() : null)
                .build();
        progressReportMapper.insert(report);

        // 初始化指标骨架记录（businessType=3：进度填报）
        initIndicatorSkeletons(report.getId(), hospital.getProjectType(), userId);

        return report.getId();
    }

    /**
     * 为新报告初始化所有指标的骨架记录
     */
    private void initIndicatorSkeletons(Long reportId, Integer projectType, Long userId) {
        List<DeclareIndicatorDO> indicators = indicatorService.getIndicators(projectType, "process");
        if (indicators == null || indicators.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<DeclareIndicatorValueDO> skeletons = indicators.stream()
                .map(ind -> {
                    DeclareIndicatorValueDO skeleton = new DeclareIndicatorValueDO();
                    skeleton.setBusinessType(3);
                    skeleton.setBusinessId(reportId);
                    skeleton.setIndicatorId(ind.getId());
                    skeleton.setIndicatorCode(ind.getIndicatorCode());
                    skeleton.setValueType(ind.getValueType());
                    skeleton.setFillerId(userId);
                    skeleton.setFillTime(now);
                    skeleton.setIsValid(true);
                    return skeleton;
                })
                .collect(Collectors.toList());
        indicatorValueMapper.insertBatch(skeletons);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveReport(DeclareProgressReportSaveReqVO reqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Long reportId;

        if (reqVO.getId() == null) {
            // 新建报告 —— 直接内联创建，复用 createReport 的完整去重逻辑
            AdminUserRespDTO currentUser = adminUserApi.getUser(userId);
            Long deptId = currentUser != null ? currentUser.getDeptId() : null;

            DeclareHospitalDO hospital = hospitalMapper.selectByDeptId(deptId);
            if (hospital == null) {
                throw new RuntimeException("医院信息不存在，请确认当前用户已关联医院部门");
            }

            // 防止重复创建：检查该医院本批次是否已有记录
            DeclareProgressReportDO existing = progressReportMapper.selectByDeptAndPeriod(
                    deptId, reqVO.getReportYear(), reqVO.getReportBatch());
            if (existing != null) {
                throw new RuntimeException("该医院本批次已存在填报记录（id=" + existing.getId() + "），请直接编辑现有记录，勿重复创建");
            }

            // 防止超限
            if (isOverLimit(deptId, reqVO.getReportYear())) {
                throw new RuntimeException("该医院本年度填报次数已达上限（4次）");
            }

            DeclareProgressReportDO report = DeclareProgressReportDO.builder()
                    .reportYear(reqVO.getReportYear())
                    .reportBatch(reqVO.getReportBatch())
                    .hospitalId(hospital.getId())
                    .deptId(deptId)
                    .hospitalName(hospital.getHospitalName())
                    .provinceCode(hospital.getProvinceCode())
                    .provinceName(hospital.getProvinceName())
                    .reportStatus(reqVO.getReportStatus() != null ? reqVO.getReportStatus() : ReportStatusEnum.DRAFT.getStatus())
                    .reportUserName(reqVO.getReportUserName())
                    .provinceStatus(ProvinceStatusEnum.NOT_SUBMITTED.getStatus())
                    .nationalReportStatus(NationalReportStatusEnum.NOT_REPORTED.getStatus())
                    .creator(userId != null ? userId.toString() : null)
                    .build();
            progressReportMapper.insert(report);
            reportId = report.getId();
            initIndicatorSkeletons(reportId, hospital.getProjectType(), userId);
        } else {
            // 更新报告状态
            reportId = reqVO.getId();
            DeclareProgressReportDO report = progressReportMapper.selectById(reportId);
            if (report == null) {
                throw new RuntimeException("填报记录不存在");
            }
            if (reqVO.getReportStatus() != null) {
                report.setReportStatus(reqVO.getReportStatus());
            }
            if (reqVO.getReportUserName() != null) {
                report.setReportUserName(reqVO.getReportUserName());
            }
            progressReportMapper.updateById(report);
        }

        // 保存指标值（businessType=3：进度填报）
        if (reqVO.getValues() != null && !reqVO.getValues().isEmpty()) {
            List<DeclareIndicatorValueDO> valueDOs = new ArrayList<>();
            for (DeclareProgressReportSaveReqVO.IndicatorValueItem item : reqVO.getValues()) {
                DeclareIndicatorValueDO valueDO = new DeclareIndicatorValueDO();
                valueDO.setIndicatorId(item.getIndicatorId());
                valueDO.setIndicatorCode(item.getIndicatorCode());
                valueDO.setValueType(item.getValueType());

                // type=8 日期区间：前端发的是 valueDateStart/valueDateEnd，需特殊处理
                if (Integer.valueOf(8).equals(item.getValueType())) {
                    boolean hasStart = item.getValueDateStart() != null && !item.getValueDateStart().trim().isEmpty();
                    boolean hasEnd = item.getValueDateEnd() != null && !item.getValueDateEnd().trim().isEmpty();
                    if (!hasStart && !hasEnd) {
                        continue; // 日期区间两端都为空，跳过
                    }
                    if (hasStart) {
                        try {
                            valueDO.setValueDateStart(java.time.LocalDateTime.parse(item.getValueDateStart().trim()));
                        } catch (Exception e) {
                            try {
                                valueDO.setValueDateStart(java.time.LocalDateTime.of(
                                    java.time.LocalDate.parse(item.getValueDateStart().trim().substring(0, 10)),
                                    java.time.LocalTime.of(0, 0, 0)));
                            } catch (Exception ex) { /* ignore */ }
                        }
                    }
                    if (hasEnd) {
                        try {
                            valueDO.setValueDateEnd(java.time.LocalDateTime.parse(item.getValueDateEnd().trim()));
                        } catch (Exception e) {
                            try {
                                valueDO.setValueDateEnd(java.time.LocalDateTime.of(
                                    java.time.LocalDate.parse(item.getValueDateEnd().trim().substring(0, 10)),
                                    java.time.LocalTime.of(23, 59, 59)));
                            } catch (Exception ex) { /* ignore */ }
                        }
                    }
                    valueDOs.add(valueDO);
                    continue;
                }

                if (item.getValue() == null || item.getValue().toString().trim().isEmpty()) {
                    // 数字类型显式设置为 null，让数据库空值能够被更新
                    if (Integer.valueOf(1).equals(item.getValueType())) {
                        valueDO.setValueNum(null);
                        valueDOs.add(valueDO);
                    }
                    // 其他类型跳过
                    continue;
                }
                setValueByType(valueDO, item.getValueType(), item.getValue());
                valueDOs.add(valueDO);
            }
            if (!valueDOs.isEmpty()) {
                indicatorValueService.batchSaveIndicatorValues(3, reportId, valueDOs, userId);
            }
        }

        return reportId;
    }

    /**
     * 根据值类型设置值
     * 值类型枚举：
     * 1=数字, 2=字符串, 3=布尔, 4=日期, 5=长文本,
     * 6=单选, 7=多选, 8=日期区间, 9=文件上传,
     * 10=单选下拉, 11=多选下拉, 12=动态容器
     */
    private void setValueByType(DeclareIndicatorValueDO valueDO, Integer valueType, Object value) {
        if (value == null) return;
        String strValue = value.toString();
        if (strValue.trim().isEmpty()) return;

        switch (valueType != null ? valueType : 0) {
            case 1: // 数字
                try {
                    valueDO.setValueNum(new java.math.BigDecimal(strValue));
                } catch (NumberFormatException e) {
                    valueDO.setValueStr(strValue);
                }
                break;
            case 2: // 字符串
            case 6: // 单选
            case 10: // 单选下拉
                valueDO.setValueStr(strValue);
                break;
            case 3: // 布尔
                valueDO.setValueBool(Boolean.parseBoolean(strValue));
                break;
            case 4: // 日期
                try {
                    valueDO.setValueDate(LocalDateTime.parse(strValue));
                } catch (Exception e) {
                    // 兼容只有日期部分（无时间）的格式
                    try {
                        valueDO.setValueDate(java.time.LocalDateTime.of(
                            java.time.LocalDate.parse(strValue.substring(0, 10)),
                            java.time.LocalTime.of(0, 0, 0)));
                    } catch (Exception ex) {
                        valueDO.setValueStr(strValue);
                    }
                }
                break;
            case 5: // 长文本
                valueDO.setValueText(strValue);
                break;
            case 7: // 多选
            case 11: // 多选下拉
            case 9: // 文件上传
                valueDO.setValueStr(strValue);
                break;
            case 8: // 日期区间（格式: "startDate,endDate"）
                String[] parts = strValue.split(",");
                if (parts.length >= 1 && !parts[0].trim().isEmpty()) {
                    try {
                        valueDO.setValueDateStart(LocalDateTime.parse(parts[0].trim()));
                    } catch (Exception e) { /* ignore */ }
                }
                if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                    try {
                        valueDO.setValueDateEnd(LocalDateTime.parse(parts[1].trim()));
                    } catch (Exception e) { /* ignore */ }
                }
                break;
            case 12: // 动态容器 - JSON 字符串直接保存
                valueDO.setValueStr(strValue);
                break;
            default:
                // 未知类型也尝试保存为字符串
                valueDO.setValueStr(strValue);
                break;
            case 0:
                break;
        }
    }

    @Override
    public DeclareProgressReportVO getReport(Long id) {
        DeclareProgressReportDO report = progressReportMapper.selectById(id);
        if (report == null) {
            return null;
        }
        return convertToVO(report);
    }

    @Override
    public List<DeclareProgressReportVO> getReportListByHospital(Long hospitalId, Integer reportYear) {
        // 说明：前端传入的 "hospitalId" 实际是用户的 deptId（部门ID）
        // 因为 hospital 与 dept 是 1:1 关联，医院创建记录时同时设置了 hospitalId（真实医院ID）和 deptId（部门ID）
        // 此处按 deptId 过滤，确保医院用户只能看到自己部门的填报记录 
        // 此处注释所有 hospitalId 与 deptId 相关的查询条件 因为框架会自动根据 deptId 过滤
        // Long deptId = hospitalId; // 参数名保持兼容，实际语义为 deptId
        LambdaQueryWrapperX<DeclareProgressReportDO> wrapper = new LambdaQueryWrapperX<DeclareProgressReportDO>()
                // .eq(deptId != null, DeclareProgressReportDO::getDeptId, deptId)
                // .eq(reportYear != null, DeclareProgressReportDO::getReportYear, reportYear)
                .orderByDesc(DeclareProgressReportDO::getId);
        List<DeclareProgressReportDO> reports = progressReportMapper.selectList(wrapper);
        if (reports.isEmpty()) return Collections.emptyList();

        // 批量预加载医院信息，消除 N+1 查询
        Set<Long> hospitalIds = reports.stream()
                .map(DeclareProgressReportDO::getHospitalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeclareHospitalDO> hospitalMap = hospitalIds.isEmpty()
                ? Collections.emptyMap()
                : hospitalMapper.selectBatchIds(hospitalIds).stream()
                        .collect(Collectors.toMap(DeclareHospitalDO::getId, h -> h));

        return reports.stream()
                .map(report -> convertToVOWithHospital(report, hospitalMap.get(report.getHospitalId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<DeclareProgressReportVO> getReportListByProvince(String provinceCode, Integer reportYear) {
        LambdaQueryWrapperX<DeclareProgressReportDO> wrapper = new LambdaQueryWrapperX<>();
        // wrapper.eq(DeclareProgressReportDO::getProvinceCode, provinceCode);
        wrapper.eq(reportYear != null, DeclareProgressReportDO::getReportYear, reportYear);
        // 只查询医院已提交审批的记录（hospitalProcessInstanceId 有值）
        wrapper.isNotNull(DeclareProgressReportDO::getHospitalProcessInstanceId);
        wrapper.ne(DeclareProgressReportDO::getReportStatus, ReportStatusEnum.DRAFT.getStatus());
        wrapper.ne(DeclareProgressReportDO::getReportStatus, ReportStatusEnum.SAVED.getStatus());
        wrapper.orderByDesc(DeclareProgressReportDO::getCreateTime);
        List<DeclareProgressReportDO> reports = progressReportMapper.selectList(wrapper);
        if (reports.isEmpty()) return Collections.emptyList();

        // 批量预加载医院信息，消除 N+1 查询
        Set<Long> hospitalIds = reports.stream()
                .map(DeclareProgressReportDO::getHospitalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeclareHospitalDO> hospitalMap = hospitalIds.isEmpty()
                ? Collections.emptyMap()
                : hospitalMapper.selectBatchIds(hospitalIds).stream()
                        .collect(Collectors.toMap(DeclareHospitalDO::getId, h -> h));

        return reports.stream()
                .map(report -> convertToVOWithHospital(report, hospitalMap.get(report.getHospitalId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<DeclareProgressReportVO> getProvincePendingList(String provinceCode) {
        LambdaQueryWrapperX<DeclareProgressReportDO> wrapper = new LambdaQueryWrapperX<DeclareProgressReportDO>()
                .eq(DeclareProgressReportDO::getProvinceCode, provinceCode)
                .eq(DeclareProgressReportDO::getProvinceStatus, ProvinceStatusEnum.AUDITING.getStatus())
                .orderByDesc(DeclareProgressReportDO::getCreateTime);
        List<DeclareProgressReportDO> reports = progressReportMapper.selectList(wrapper);
        if (reports.isEmpty()) return Collections.emptyList();

        Set<Long> hospitalIds = reports.stream()
                .map(DeclareProgressReportDO::getHospitalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeclareHospitalDO> hospitalMap = hospitalIds.isEmpty()
                ? Collections.emptyMap()
                : hospitalMapper.selectBatchIds(hospitalIds).stream()
                        .collect(Collectors.toMap(DeclareHospitalDO::getId, h -> h));

        return reports.stream()
                .map(report -> convertToVOWithHospital(report, hospitalMap.get(report.getHospitalId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<DeclareProgressReportVO> getProvinceApprovedList(String provinceCode) {
        LambdaQueryWrapperX<DeclareProgressReportDO> wrapper = new LambdaQueryWrapperX<DeclareProgressReportDO>()
                .eq(DeclareProgressReportDO::getProvinceCode, provinceCode)
                .eq(DeclareProgressReportDO::getProvinceStatus, ProvinceStatusEnum.APPROVED.getStatus())
                .eq(DeclareProgressReportDO::getNationalReportStatus, NationalReportStatusEnum.NOT_REPORTED.getStatus())
                .orderByDesc(DeclareProgressReportDO::getCreateTime);
        List<DeclareProgressReportDO> reports = progressReportMapper.selectList(wrapper);
        if (reports.isEmpty()) return Collections.emptyList();

        Set<Long> hospitalIds = reports.stream()
                .map(DeclareProgressReportDO::getHospitalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeclareHospitalDO> hospitalMap = hospitalIds.isEmpty()
                ? Collections.emptyMap()
                : hospitalMapper.selectBatchIds(hospitalIds).stream()
                        .collect(Collectors.toMap(DeclareHospitalDO::getId, h -> h));

        return reports.stream()
                .map(report -> convertToVOWithHospital(report, hospitalMap.get(report.getHospitalId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReport(Long id, String auditUserName) {
        DeclareProgressReportDO report = progressReportMapper.selectById(id);
        if (report == null) {
            throw new RuntimeException("填报记录不存在");
        }
        // 前置校验：只有 SAVED 状态才允许提交
        if (!ReportStatusEnum.SAVED.getStatus().equals(report.getReportStatus())) {
            throw new RuntimeException("请先保存后再提交审核");
        }
        if (StrUtil.isNotBlank(report.getHospitalProcessInstanceId())) {
            throw new RuntimeException("该填报记录已提交审核，请勿重复提交");
        }

        // 从 bpm_business_type 表动态获取流程定义 Key
        String businessType = "progress_report:submit";
        String processDefinitionKey = bpmBusinessTypeService.getProcessDefinitionKey(businessType);
        if (StrUtil.isBlank(processDefinitionKey)) {
            // 兜底：如果未配置，走硬编码（兼容旧逻辑）
            processDefinitionKey = "progressReportApprove";
        }

        BpmProcessInstanceCreateReqDTO createReqDTO = new BpmProcessInstanceCreateReqDTO();
        createReqDTO.setProcessDefinitionKey(processDefinitionKey);
        createReqDTO.setBusinessKey(processDefinitionKey + "_" + id);

        Map<String, Object> variables = new HashMap<>();
        variables.put("businessType", businessType);
        variables.put("businessCreatorId", SecurityFrameworkUtils.getLoginUserId());
        variables.put("title", String.format("%s - %d年第%d期进度填报",
                report.getHospitalName(), report.getReportYear(), report.getReportBatch()));
        variables.put("reportId", report.getId());
        variables.put("hospitalId", report.getHospitalId());
        variables.put("hospitalName", report.getHospitalName());
        variables.put("provinceCode", report.getProvinceCode());
        variables.put("bizStatus", "HOSPITAL_SUBMITTED");
        createReqDTO.setVariables(variables);

        String processInstanceId = processInstanceApi.createProcessInstance(
                SecurityFrameworkUtils.getLoginUserId(), createReqDTO);

        // 保存审核人姓名
        if (StrUtil.isNotBlank(auditUserName)) {
            report.setAuditUserName(auditUserName);
        }
        report.setHospitalProcessInstanceId(processInstanceId);
        report.setReportStatus(ReportStatusEnum.SUBMITTED.getStatus());
        progressReportMapper.updateById(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hospitalAudit(DeclareProgressReportAuditReqVO reqVO) {
        // 此方法已废弃，医院审核统一通过 BPM 任务审批接口处理
        // 审核结果由 DeclareProgressReportTaskStatusListener 监听 BPM 事件后自动更新
        DeclareProgressReportDO report = progressReportMapper.selectById(reqVO.getId());
        if (report == null) {
            throw new RuntimeException("填报记录不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void provinceAudit(DeclareProgressReportAuditReqVO reqVO) {
        // 此方法已废弃，省级审核统一通过 BPM 任务审批接口处理
        // 审核结果由 DeclareProgressReportTaskStatusListener 监听 BPM 事件后自动更新
        DeclareProgressReportDO report = progressReportMapper.selectById(reqVO.getId());
        if (report == null) {
            throw new RuntimeException("填报记录不存在");
        }
    }

    @Override
    public void saveIndicatorValues(Long reportId, List<Long> indicatorIds, List<String> values) {
        if (indicatorIds == null || indicatorIds.isEmpty()) {
            return;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        List<DeclareIndicatorValueDO> valueDOs = new ArrayList<>();
        for (int i = 0; i < indicatorIds.size(); i++) {
            Long indicatorId = indicatorIds.get(i);
            String value = (values != null && i < values.size()) ? values.get(i) : null;
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            DeclareIndicatorValueDO valueDO = new DeclareIndicatorValueDO();
            valueDO.setIndicatorId(indicatorId);
            // indicatorCode 为空，由 batchSaveIndicatorValues 根据 existing 数据补全
            valueDO.setValueType(1); // 默认数字类型
            valueDO.setValueStr(value);
            valueDOs.add(valueDO);
        }
        if (!valueDOs.isEmpty()) {
            indicatorValueService.batchSaveIndicatorValues(3, reportId, valueDOs, userId);
        }
    }

    @Override
    public List<DeclareProgressReportVO> getReportHistory(Long hospitalId, Integer reportYear) {
        return getReportListByHospital(hospitalId, reportYear);
    }

    @Override
    public boolean isInReportWindow(Long hospitalId) {
        // 1. 是否有开放的窗口
        ReportWindowVO openWindow = reportWindowService.getLatestOpenWindow();
        if (openWindow == null) {
            return false;
        }
        // 2. 该医院本批次是否已填报（按 deptId 查询）
        DeclareProgressReportDO existing = progressReportMapper.selectByDeptAndPeriod(
                hospitalId, openWindow.getReportYear(), openWindow.getReportBatch());
        return existing == null;
    }

    @Override
    public boolean isOverLimit(Long hospitalId, Integer reportYear) {
        if (hospitalId == null) {
            return false;
        }
        long count = progressReportMapper.countByHospitalAndYear(hospitalId, reportYear);
        return count >= 4;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long id) {
        DeclareProgressReportDO report = progressReportMapper.selectById(id);
        if (report == null) {
            throw new RuntimeException("填报记录不存在");
        }
        // 仅允许删除草稿或已保存状态
        if (!ReportStatusEnum.DRAFT.getStatus().equals(report.getReportStatus())
                && !ReportStatusEnum.SAVED.getStatus().equals(report.getReportStatus())) {
            throw new RuntimeException("仅草稿和已保存状态可删除");
        }
        // 先删除关联的指标值（进度填报的 businessType 为 3）
        indicatorValueService.deleteIndicatorValues(3, id);
        // 再删除主记录
        progressReportMapper.deleteById(id);
    }

    private DeclareProgressReportVO convertToVO(DeclareProgressReportDO report) {
        // 查询医院信息以获取 projectType 和其他字段
        DeclareHospitalDO hospital = report.getHospitalId() != null
                ? hospitalMapper.selectById(report.getHospitalId())
                : null;
        return convertToVOWithHospital(report, hospital);
    }

    private DeclareProgressReportVO convertToVOWithHospital(DeclareProgressReportDO report, DeclareHospitalDO hospital) {
        Integer projectType = hospital != null ? hospital.getProjectType() : null;
        String projectTypeName = projectTypeService.getProjectTypeTitle(projectType);
        String projectTypeShortName = projectTypeService.getProjectTypeName(projectType);
        String unifiedSocialCreditCode = hospital != null ? hospital.getUnifiedSocialCreditCode() : null;
        String medicalLicenseNo = hospital != null ? hospital.getMedicalLicenseNo() : null;

        // 查询填报窗口信息
        LocalDateTime ws = null;
        LocalDateTime we = null;
        if (report.getReportYear() != null && report.getReportBatch() != null) {
            List<ReportWindowVO> windows = reportWindowService.getWindowList(report.getReportYear(), null);
            if (windows != null) {
                for (ReportWindowVO w : windows) {
                    if (w.getReportBatch() != null && w.getReportBatch().equals(report.getReportBatch())) {
                        ws = w.getWindowStart();
                        we = w.getWindowEnd();
                        break;
                    }
                }
            }
        }

        return DeclareProgressReportVO.builder()
                .id(report.getId())
                .hospitalProcessInstanceId(report.getHospitalProcessInstanceId())
                .reportYear(report.getReportYear())
                .reportBatch(report.getReportBatch())
                .hospitalId(report.getHospitalId())
                .deptId(report.getDeptId())
                .hospitalName(report.getHospitalName())
                .provinceCode(report.getProvinceCode())
                .provinceName(report.getProvinceName())
                .reportStatus(report.getReportStatus())
                .reportStatusName(getReportStatusName(report.getReportStatus()))
                .provinceStatus(report.getProvinceStatus())
                .provinceStatusName(getProvinceStatusName(report.getProvinceStatus()))
                .nationalReportStatus(report.getNationalReportStatus())
                .nationalReportStatusName(getNationalReportStatusName(report.getNationalReportStatus()))
                .nationalReportTime(report.getNationalReportTime())
                .nationalReporterName(report.getNationalReporterName())
                .auditUserName(report.getAuditUserName())
                .reportUserName(report.getReportUserName())
                .projectType(projectType)
                .projectTypeName(projectTypeName)
                .projectTypeShortName(projectTypeShortName)
                .windowStart(ws)
                .windowEnd(we)
                .unifiedSocialCreditCode(unifiedSocialCreditCode)
                .medicalLicenseNo(medicalLicenseNo)
                .creator(report.getCreator())
                .createTime(report.getCreateTime())
                .updateTime(report.getUpdateTime())
                .build();
    }

    /**
     * 从字典获取填报状态的中文展示文本（已废弃，请使用前端枚举渲染）
     * @deprecated 前端通过 ReportStatusEnum 自行渲染
     */
    @Deprecated
    private String getReportStatusName(String status) {
        if (status == null) return "";
        ReportStatusEnum enumValue = ReportStatusEnum.getByStatus(status);
        if (enumValue != null) {
            return enumValue.getName();
        }
        return status;
    }

    private String getProvinceStatusName(Integer status) {
        if (status == null) return "";
        for (ProvinceStatusEnum e : ProvinceStatusEnum.values()) {
            if (e.getStatus().equals(status)) {
                return e.getName();
            }
        }
        return "";
    }

    private String getNationalReportStatusName(Integer status) {
        if (status == null) return "未上报";
        if (status == 1) return "国家局审批中";
        if (status == 2) return "已上报";
        return "未上报";
    }

    /**
     * 将 deptId 转换为 hospitalId
     * 前端传入的 hospitalId 实际上是用户的 deptId
     */

    @Override
    public DeclareCompareDataVO getCompareData(Long reportIdA, Long reportIdB) {
        // 1. 获取两条申报记录的基础信息
        DeclareProgressReportDO reportA = progressReportMapper.selectById(reportIdA);
        DeclareProgressReportDO reportB = progressReportMapper.selectById(reportIdB);

        if (reportA == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.PROGRESS_REPORT_A_NOT_EXISTS);
        }
        if (reportB == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.PROGRESS_REPORT_B_NOT_EXISTS);
        }

        // 2. 校验项目类型一致，不一致则抛业务异常
        Integer projectTypeA = null;
        Integer projectTypeB = null;

        DeclareHospitalDO hospitalA = hospitalMapper.selectById(reportA.getHospitalId());
        DeclareHospitalDO hospitalB = hospitalMapper.selectById(reportB.getHospitalId());

        if (hospitalA != null) {
            projectTypeA = hospitalA.getProjectType();
        }
        if (hospitalB != null) {
            projectTypeB = hospitalB.getProjectType();
        }

        if (!java.util.Objects.equals(projectTypeA, projectTypeB)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.PROGRESS_REPORT_PROJECT_TYPE_NOT_MATCH);
        }

        // 3. 获取该项目的指标定义列表（按一级分组 + 二级分组 + 指标代号排序）
        List<DeclareIndicatorDO> indicatorList =
                indicatorService.getIndicators(projectTypeA, "process");

        // 4. 获取两条记录的指标值映射 (businessType=3=进度填报, businessId=reportId)
        Map<String, DeclareIndicatorValueDO> valueMapA =
                indicatorValueService.getIndicatorValueMap(3, reportIdA);
        Map<String, DeclareIndicatorValueDO> valueMapB =
                indicatorValueService.getIndicatorValueMap(3, reportIdB);

        // 5. 构建 groupId -> DeclareIndicatorGroupDO 映射表（含一级和二级分组）
        Map<Long, DeclareIndicatorGroupDO> groupMap = new HashMap<>();
        if (projectTypeA != null) {
            List<DeclareIndicatorGroupDO> allGroups = indicatorGroupMapper.selectByProjectType(projectTypeA);
            for (DeclareIndicatorGroupDO g : allGroups) {
                groupMap.put(g.getId(), g);
            }
        }

        // 6. 遍历指标定义，组装每行的 A/B 值和差异类型
        List<DeclareCompareIndicatorRowVO> rows = new ArrayList<>();
        for (DeclareIndicatorDO indicator : indicatorList) {
            DeclareCompareIndicatorRowVO row = DeclareCompareIndicatorRowVO.builder()
                    .indicatorId(indicator.getId())
                    .indicatorCode(indicator.getIndicatorCode())
                    .indicatorName(indicator.getIndicatorName())
                    .unit(indicator.getUnit())
                    .valueType(indicator.getValueType())
                    .valueOptions(indicator.getValueOptions())
                    .groupId(indicator.getGroupId())
                    .extraConfig(indicator.getExtraConfig())
                    .build();

            // 通过 groupId 查找二级分组，再找一级分组名称和排序号
            DeclareIndicatorGroupDO group2 = groupMap.get(indicator.getGroupId());
            if (group2 != null) {
                row.setGroupName(group2.getGroupName());
                row.setGroupSort(group2.getSort());
                DeclareIndicatorGroupDO group1 = groupMap.get(group2.getParentId());
                if (group1 != null) {
                    row.setParentGroupName(group1.getGroupName());
                    row.setParentGroupSort(group1.getSort());
                }
            }
            row.setIndicatorSort(indicator.getSort());

            DeclareIndicatorValueDO valA = valueMapA.get(indicator.getIndicatorCode());
            DeclareIndicatorValueDO valB = valueMapB.get(indicator.getIndicatorCode());

            row.setValueA(extractValue(valA, indicator.getValueType()));
            row.setValueB(extractValue(valB, indicator.getValueType()));
            row.setDiffType(computeDiffType(row.getValueA(), row.getValueB(), indicator.getValueType()));

            rows.add(row);
        }

        // 7. 按一级分组排序号 + 二级分组排序号 + 指标排序号排序，确保顺序与配置一致
        // 一级分组 parentGroupSort=null，使用 groupSort；二级分组 parentGroupSort=父分组 sort
        rows.sort(java.util.Comparator
                // 一级分组排在前（parentGroupSort 为 null 的行）
                .comparingInt((DeclareCompareIndicatorRowVO r) -> r.getParentGroupSort() == null ? r.getGroupSort() : r.getParentGroupSort())
                // 二级分组排在对应一级分组之后（同一个一级分组下的二级分组按 sort 排序）
                .thenComparingInt(DeclareCompareIndicatorRowVO::getGroupSort)
                // 同组内指标按 sort 排序
                .thenComparingInt(DeclareCompareIndicatorRowVO::getIndicatorSort));

        // 8. 组装返回 VO
        DeclareProgressReportVO voA = convertToVO(reportA);
        DeclareProgressReportVO voB = convertToVO(reportB);

        DeclareCompareDataVO result = new DeclareCompareDataVO();
        result.setReportA(voA);
        result.setReportB(voB);
        result.setIndicators(rows);
        return result;
    }

    /**
     * 从 IndicatorValueDO 中提取对应类型的值对象
     */
    private Object extractValue(DeclareIndicatorValueDO val, Integer valueType) {
        if (val == null) {
            return null;
        }
        switch (valueType != null ? valueType : 0) {
            case 1: // 数字
                if (val.getValueNum() != null) {
                    return val.getValueNum();
                }
                if (val.getValueStr() != null && !val.getValueStr().isEmpty()) {
                    try {
                        return new java.math.BigDecimal(val.getValueStr());
                    } catch (NumberFormatException e) {
                        return val.getValueStr();
                    }
                }
                return null;
            case 2: // 字符串
            case 6: // 单选
            case 9: // 文件上传
            case 10: // 单选下拉
                return val.getValueStr() != null && !val.getValueStr().isEmpty() ? val.getValueStr() : null;
            case 3: // 布尔
                if (val.getValueBool() != null) {
                    return val.getValueBool();
                }
                if ("true".equals(val.getValueStr())) return true;
                if ("false".equals(val.getValueStr())) return false;
                return null;
            case 4: // 日期
                return val.getValueDate();
            case 5: // 长文本
                return val.getValueText();
            case 7: // 多选
            case 11: // 多选下拉
                return val.getValueStr() != null && !val.getValueStr().isEmpty()
                    ? val.getValueStr().split(",")
                    : null;
            case 8: // 日期区间
                if (val.getValueDateStart() == null && val.getValueDateEnd() == null) {
                    return null;
                }
                Map<String, Object> range = new HashMap<>();
                range.put("start", val.getValueDateStart());
                range.put("end", val.getValueDateEnd());
                return range;
            case 12: // 动态容器
                return val.getValueStr();
            default:
                return val.getValueStr();
        }
    }

    /**
     * 计算差异类型
     */
    private String computeDiffType(Object valA, Object valB, Integer valueType) {
        boolean hasA = valA != null && !"".equals(String.valueOf(valA));
        boolean hasB = valB != null && !"".equals(String.valueOf(valB));
        if (!hasA && !hasB) return "none";
        if (!hasA || !hasB) return "different";

        switch (valueType != null ? valueType : 0) {
            case 1: // 数字: 比较大小
                try {
                    double a = Double.parseDouble(String.valueOf(valA));
                    double b = Double.parseDouble(String.valueOf(valB));
                    if (b > a) return "up";
                    if (b < a) return "down";
                    return "equal";
                } catch (NumberFormatException e) {
                    return String.valueOf(valA).equals(String.valueOf(valB)) ? "equal" : "different";
                }
            case 8: { // 日期区间: 比较开始日期
                Map<String, Object> rangeA = (Map<String, Object>) valA;
                Map<String, Object> rangeB = (Map<String, Object>) valB;
                Object startA = rangeA != null ? rangeA.get("start") : null;
                Object startB = rangeB != null ? rangeB.get("start") : null;
                if (startA == null && startB == null) return "equal";
                if (startA == null || startB == null) return "different";
                return startA.toString().compareTo(startB.toString()) == 0 ? "equal" : "different";
            }
            case 12: // 动态容器: JSON 字符串整体比较
            default: // 其他类型: 比较字符串相等
                return String.valueOf(valA).equals(String.valueOf(valB)) ? "equal" : "different";
        }
    }

    @Override
    public List<DeclareProgressReportVO> nationalSearch(DeclareNationalSearchReqVO reqVO) {
        List<DeclareProgressReportDO> list = progressReportMapper.selectNationalSearch(reqVO);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<DeclareProgressReportVO> getReportListByNational() {
        LambdaQueryWrapperX<DeclareProgressReportDO> wrapper = new LambdaQueryWrapperX<DeclareProgressReportDO>()
                .isNotNull(DeclareProgressReportDO::getHospitalProcessInstanceId)
                .ne(DeclareProgressReportDO::getReportStatus, ReportStatusEnum.DRAFT.getStatus())
                .ne(DeclareProgressReportDO::getReportStatus, ReportStatusEnum.SAVED.getStatus())
                .orderByDesc(DeclareProgressReportDO::getCreateTime);
        List<DeclareProgressReportDO> reports = progressReportMapper.selectList(wrapper);
        if (reports.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> hospitalIds = reports.stream()
                .map(DeclareProgressReportDO::getHospitalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeclareHospitalDO> hospitalMap = hospitalIds.isEmpty()
                ? Collections.emptyMap()
                : hospitalMapper.selectBatchIds(hospitalIds).stream()
                        .collect(Collectors.toMap(DeclareHospitalDO::getId, h -> h));

        return reports.stream()
                .map(report -> convertToVOWithHospital(report, hospitalMap.get(report.getHospitalId())))
                .collect(Collectors.toList());
    }
}
