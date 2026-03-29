package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEventListener;
import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareProgressReportDO;
import cn.gemrun.base.module.declare.dal.mysql.DeclareProgressReportMapper;
import cn.gemrun.base.module.declare.enums.ProvinceStatusEnum;
import cn.gemrun.base.module.declare.enums.ReportStatusEnum;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 进度填报 BPM 任务状态监听器
 * <p>
 * 监听 progressReportApprove 流程的任务完成事件，根据 bizStatus 更新业务状态。
 *
 * @author jason
 */
@Slf4j
@Component
public class DeclareProgressReportTaskStatusListener extends BpmTaskStatusEventListener {

    @Resource
    private DeclareProgressReportMapper progressReportMapper;

    @Override
    protected String getProcessDefinitionKey() {
        return "progressReportApprove";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected void onEvent(BpmTaskStatusEvent event) {
        Long reportId = parseReportId(event);
        if (reportId == null) {
            log.warn("[DeclareProgressReportTaskStatusListener] 解析填报记录ID失败: businessKey={}",
                event.getBusinessKey());
            return;
        }

        DeclareProgressReportDO report = progressReportMapper.selectById(reportId);
        if (report == null) {
            log.warn("[DeclareProgressReportTaskStatusListener] 填报记录不存在: reportId={}", reportId);
            return;
        }

        String bizStatus = event.getBizStatus();
        // 如果 bizStatus 为空（自动审批场景），尝试从流程变量中读取
        if (bizStatus == null && event.getVariables() != null) {
            Object bizStatusObj = event.getVariables().get("bizStatus");
            if (bizStatusObj != null) {
                bizStatus = bizStatusObj.toString();
            }
        }

        if (bizStatus == null) {
            log.warn("[DeclareProgressReportTaskStatusListener] 无法确定业务状态，跳过处理: reportId={}", reportId);
            return;
        }

        log.info("[DeclareProgressReportTaskStatusListener] 收到任务状态事件: " +
                "reportId={}, taskDefinitionKey={}, bizStatus={}",
            reportId, event.getTaskDefinitionKey(), bizStatus);

        // 根据 bizStatus 更新业务状态
        String opinion = event.getReason();
        if ("HOSPITAL_SUBMITTED".equals(bizStatus)) {
            // 提交员提交 或 重新提交
            report.setReportStatus(ReportStatusEnum.SUBMITTED.getStatus());
            progressReportMapper.updateById(report);

        } else if ("HOSPITAL_APPROVED".equals(bizStatus)) {
            // 医院审核通过，进入省级审核
            report.setReportStatus("HOSPITAL_APPROVED");
            report.setProvinceStatus(ProvinceStatusEnum.AUDITING.getStatus());
            progressReportMapper.updateById(report);

        } else if ("HOSPITAL_REJECTED".equals(bizStatus)) {
            // 医院审核驳回，提交员可重新编辑并提交
            report.setReportStatus("HOSPITAL_REJECTED");
            progressReportMapper.updateById(report);
        } else if ("HOSPITAL_RETURNED".equals(bizStatus)) {
            // 医院退回，提交员可重新编辑并提交
            report.setReportStatus("HOSPITAL_RETURNED");
            progressReportMapper.updateById(report);

        } else if ("PROVINCE_APPROVED".equals(bizStatus)) {
            // 省级审核通过
            report.setReportStatus("PROVINCE_APPROVED");
            report.setProvinceStatus(ProvinceStatusEnum.APPROVED.getStatus());
            if (StrUtil.isNotBlank(opinion)) {
                report.setProvinceOpinion(opinion);
            }
            report.setProvinceAuditTime(LocalDateTime.now());
            progressReportMapper.updateById(report);

        } else if ("PROVINCE_REJECTED".equals(bizStatus)) {
            // 省级审核驳回，提交员可重新编辑并提交
            report.setReportStatus("PROVINCE_REJECTED");
            report.setProvinceStatus(ProvinceStatusEnum.REJECTED.getStatus());
            if (StrUtil.isNotBlank(opinion)) {
                report.setProvinceOpinion(opinion);
            }
            report.setProvinceAuditTime(LocalDateTime.now());
            progressReportMapper.updateById(report);

        } else if ("PROVINCE_RETURNED".equals(bizStatus)) {
            // 省级退回（打回修正），提交员可重新编辑并提交
            report.setReportStatus(bizStatus);
            progressReportMapper.updateById(report);

        } else if ("DRAFT".equals(bizStatus)) {
            // 保存草稿（不推进流程，仅保存）
            report.setReportStatus(ReportStatusEnum.SAVED.getStatus());
            progressReportMapper.updateById(report);
        } else if (StrUtil.isNotEmpty(bizStatus)) {
            report.setReportStatus(bizStatus);
            progressReportMapper.updateById(report);
        }
    }

    /**
     * 从 businessKey 中解析填报记录ID
     * 格式: progressReportApprove_{reportId}
     */
    private Long parseReportId(BpmTaskStatusEvent event) {
        String businessKey = event.getBusinessKey();
        if (StrUtil.isBlank(businessKey)) {
            return null;
        }
        // 优先尝试标准格式: progressReportApprove_{reportId}
        if (businessKey.startsWith("progressReportApprove_")) {
            String idStr = businessKey.substring("progressReportApprove_".length());
            try {
                return Long.parseLong(idStr);
            } catch (NumberFormatException e) {
                log.warn("[parseReportId] 解析标准格式ID失败: businessKey={}", businessKey);
            }
        }
        // 兜底：直接尝试解析为数字
        try {
            return Long.parseLong(businessKey);
        } catch (NumberFormatException e) {
            log.warn("[parseReportId] 解析ID失败: businessKey={}", businessKey);
            return null;
        }
    }
}
