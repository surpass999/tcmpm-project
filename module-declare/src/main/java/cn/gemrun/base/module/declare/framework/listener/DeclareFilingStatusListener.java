package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmProcessInstanceStatusEventListenerV2;
import cn.gemrun.base.module.declare.service.filing.FilingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 备案流程状态监听器
 * 监听备案流程事件，更新备案状态
 *
 * @author Gemini
 */
@Component
@Slf4j
public class DeclareFilingStatusListener extends BpmProcessInstanceStatusEventListenerV2 {

    @Resource
    private FilingService filingService;

    @Override
    protected String getBusinessKeyPrefix() {
        return "declare:filing";
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        String businessKey = event.getBusinessKey();
        if (businessKey == null) {
            return;
        }

        // 解析 businessKey: declare:filing:create:1 -> 业务ID = 1
        String[] parts = businessKey.split(":");
        if (parts.length < 4) {
            log.warn("[DeclareFilingStatusListener] businessKey 格式不正确: {}", businessKey);
            return;
        }

        try {
            Long filingId = Long.parseLong(parts[3]);

            // 优先使用 DSL 中定义的 bizStatus
            String bizStatus = event.getBizStatus();
            if (bizStatus != null) {
                Integer status = convertBizStatusToFilingStatus(bizStatus);
                if (status != null) {
                    filingService.updateFilingStatus(filingId, status);
                    log.info("[DeclareFilingStatusListener] 更新备案状态: filingId={}, bizStatus={}, status={}",
                            filingId, bizStatus, status);
                }
            } else {
                // 兜底：使用 BPM 状态转换
                Integer status = convertBpmStatusToFilingStatus(event.getStatus());
                if (status != null) {
                    filingService.updateFilingStatus(filingId, status);
                    log.info("[DeclareFilingStatusListener] 更新备案状态(兜底): filingId={}, bpmStatus={}, status={}",
                            filingId, event.getStatus(), status);
                }
            }
        } catch (NumberFormatException e) {
            log.warn("[DeclareFilingStatusListener] 解析备案ID失败: businessKey={}", businessKey, e);
        }
    }

    /**
     * 将 DSL 中定义的 bizStatus 转换为备案状态
     * DSL bizStatus 值对应 FilingStatusEnum 枚举
     */
    private Integer convertBizStatusToFilingStatus(String bizStatus) {
        if (bizStatus == null) {
            return null;
        }
        // 根据 DSL actions 中定义的 bizStatus 进行映射
        // SUBMITTED -> 1, APPROVED -> 2, EXPERT_APPROVED -> 3, ARCHIVED -> 4, RETURNED -> 5, REJECTED -> 5
        switch (bizStatus) {
            case "SUBMITTED":
                return 1; // 已提交
            case "APPROVED":
            case "EXPERT_REVIEWING":  // 专家评审中，状态保持进行中
            case "EXPERT_APPROVED":
                return 2; // 省级审核通过（待专家评审）
            case "ARCHIVED":
                return 4; // 已归档
            case "RETURNED":
            case "REJECTED":
            case "MODIFYING":
                return 5; // 退回修改
            default:
                log.warn("[DeclareFilingStatusListener] 未知的 bizStatus: {}", bizStatus);
                return null;
        }
    }

    /**
     * 将 BPM 流程状态转换为备案状态（兜底方案）
     * BPM 状态: 1=进行中, 2=完成, 3=拒绝
     * 备案状态: 0=草稿，1=已提交，2=省级审核通过，3=专家论证通过，4=已归档，5=退回修改
     */
    private Integer convertBpmStatusToFilingStatus(Integer bpmStatus) {
        if (bpmStatus == null) {
            return null;
        }
        switch (bpmStatus) {
            case 1:
                // 流程进行中，更新为已提交
                return 1; // 已提交
            case 2:
                // 流程完成，更新为省级审核通过
                return 2; // 省级审核通过
            case 3:
                // 流程拒绝
                return 5; // 退回修改
            default:
                return null;
        }
    }

}
