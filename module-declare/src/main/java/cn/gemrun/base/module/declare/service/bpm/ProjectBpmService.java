package cn.gemrun.base.module.declare.service.bpm;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 项目BPM流程服务接口
 * 提供定时任务触发的流程启动能力
 *
 * @author Gemini
 */
public interface ProjectBpmService {

    /**
     * 启动年报/半年报流程
     *
     * @param processId 过程记录ID
     * @param projectId 项目ID
     * @param isAnnual true=年报, false=半年报
     * @param reportPeriodStart 报告周期开始时间
     * @param reportPeriodEnd 报告周期结束时间
     * @return 流程实例ID，失败返回null
     */
    String startReportProcess(Long processId, Long projectId, boolean isAnnual,
                              LocalDateTime reportPeriodStart, LocalDateTime reportPeriodEnd);

    /**
     * 启动验收申请流程
     *
     * @param processId 过程记录ID
     * @param projectId 项目ID
     * @return 流程实例ID，失败返回null
     */
    String startAcceptanceProcess(Long processId, Long projectId);

    /**
     * 启动任意项目流程（通用方法）
     *
     * @param processId 过程记录ID
     * @param projectId 项目ID
     * @param processType 过程类型 (2=半年报, 3=年报, 6=验收)
     * @return 流程实例ID，失败返回null
     */
    String startProcess(Long processId, Long projectId, Integer processType);

    /**
     * 发送通知消息
     *
     * @param userId 接收人用户ID
     * @param title 通知标题
     * @param content 通知内容
     */
    void sendNotice(Long userId, String title, String content);

    /**
     * 更新项目过程记录的流程实例ID和状态（由 ProcessStartedEvent 监听器调用）
     *
     * @param processId 过程记录ID
     * @param processInstanceId 流程实例ID
     */
    void updateProjectProcessInstance(Long processId, String processInstanceId);
}
