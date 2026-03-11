package cn.gemrun.base.module.bpm.framework.process.service;

import cn.gemrun.base.module.bpm.api.dto.BpmActionRespDTO;
import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeRespDTO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;

import java.util.List;
import java.util.Map;

/**
 * BPM 流程配置服务接口
 *
 * 提供通用的流程启动、管理功能，所有业务模块共用
 *
 * @author Gemini
 */
public interface BpmProcessService {

    // ========== 流程配置查询 ==========

    /**
     * 获取流程配置
     *
     * @param businessType 业务类型
     * @return 流程配置，如果未配置则返回 null
     */
    BpmBusinessTypeRespDTO getProcessConfig(String businessType);

    /**
     * 检查是否配置了流程
     *
     * @param businessType 业务类型
     * @return true=已配置，false=未配置
     */
    boolean hasProcessConfig(String businessType);

    /**
     * 根据业务类型获取流程定义Key
     *
     * @param businessType 业务类型
     * @return 流程定义Key
     */
    String getProcessDefinitionKey(String businessType);

    // ========== 流程启动 ==========

    /**
     * 启动流程（如果有配置）
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param userId       用户ID
     * @return 流程实例ID，如果没有配置则返回 null
     */
    String startProcessIfConfigured(String businessType, Long businessId, Long userId);

    /**
     * 启动流程（必须有配置）
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param userId       用户ID
     * @return 流程实例ID
     * @throws IllegalStateException 如果没有配置流程
     */
    String startProcess(String businessType, Long businessId, Long userId);

    // ========== 流程状态管理 ==========

    /**
     * 更新业务流程记录
     *
     * @param processInstanceId 流程实例ID
     * @param currentNodeKey    当前节点Key
     * @param currentStatus     当前状态
     */
    void updateProcessInstance(String processInstanceId, String currentNodeKey, String currentStatus);

    /**
     * 结束流程
     *
     * @param processInstanceId 流程实例ID
     * @param result            流程结果（agree/reject/back）
     */
    void endProcess(String processInstanceId, String result);

    // ========== 流程查询 ==========

    /**
     * 获取业务的流程实例ID
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 流程实例ID，如果没有流程则返回 null
     */
    String getProcessInstanceId(String businessType, Long businessId);

    /**
     * 获取业务流程关联记录
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 流程关联记录
     */
    BpmBusinessProcessDO getBusinessProcess(String businessType, Long businessId);

    /**
     * 根据流程实例ID获取业务流程关联记录
     *
     * @param processInstanceId 流程实例ID
     * @return 流程关联记录
     */
    BpmBusinessProcessDO getBusinessProcessByProcessInstanceId(String processInstanceId);

    // ========== 流程任务相关 ==========

    /**
     * 更新当前任务分配信息（assign type 和 source）
     * 在任务完成后调用，根据下一个节点的 DSL 配置更新
     *
     * @param processInstanceId 流程实例ID
     * @param assignType        分配类型
     * @param assignSource      分配来源
     */
    void updateCurrentAssign(String processInstanceId, String assignType, String assignSource);

    /**
     * 添加参与者到 initiator_ids
     * 任务完成时调用，将当前处理人添加到参与者列表
     *
     * @param processInstanceId 流程实例ID
     * @param userId            用户ID
     */
    void addParticipant(String processInstanceId, Long userId);

    /**
     * 获取当前用户对指定业务可执行的操作列表（基于 DSL actions）
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param userId       当前用户ID
     * @return 可执行操作列表，未分配任务则返回空列表
     */
    List<BpmActionRespDTO> getAvailableActions(String businessType, Long businessId, Long userId);

    /**
     * 批量获取当前用户对多个业务可执行的操作列表
     *
     * @param businessType 业务类型
     * @param businessIds  业务ID列表
     * @param userId       当前用户ID
     * @return Map<businessId, 操作列表>
     */
    Map<Long, List<BpmActionRespDTO>> getAvailableActionsBatch(String businessType, List<Long> businessIds, Long userId);

    // ========== 业务类型解析 ==========

    /**
     * 解析 businessType（自动解析）
     *
     * @param methodName 方法名
     * @param className  类名
     * @return 业务类型
     */
    String parseBusinessType(String methodName, String className);

    // ========== 流程操作 ==========

    /**
     * 提交操作（完成任务，推进流程）
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param actionKey   操作标识（如 submit, agree, reject, back 等）
     * @param userId      当前用户ID
     * @param reason      审批意见
     * @param expertUserIds 选择的专家用户ID列表（选择专家操作时使用）
     * @param targetNodeKey 退回时的目标节点（退回操作时使用）
     */
    void submitAction(String businessType, Long businessId, String actionKey, Long userId, String reason, List<Long> expertUserIds, String targetNodeKey);

    // ========== 流程撤回/撤销 ==========

    /**
     * 撤回流程（撤销到草稿状态）
     * 删除 Flowable 运行时流程实例，重置业务状态
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param userId       当前用户ID
     */
    void withdrawProcess(String businessType, Long businessId, Long userId);

}
