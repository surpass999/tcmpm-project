package cn.gemrun.base.module.declare.framework.process.service;

import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeRespDTO;
import cn.gemrun.base.module.declare.dal.dataobject.process.DeclareBusinessProcessDO;

import java.util.*;

/**
 * 流程配置服务接口
 *
 * @author Gemini
 */
public interface DeclareProcessService {

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
     * @param result           流程结果（agree/reject/back）
     */
    void endProcess(String processInstanceId, String result);

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
    DeclareBusinessProcessDO getBusinessProcess(String businessType, Long businessId);

    /**
     * 解析 businessType（自动解析）
     *
     * @param methodName 方法名
     * @param className  类名
     * @return 业务类型
     */
    String parseBusinessType(String methodName, String className);

}
