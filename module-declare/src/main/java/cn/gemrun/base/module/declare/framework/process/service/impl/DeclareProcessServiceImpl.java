package cn.gemrun.base.module.declare.framework.process.service.impl;

import cn.gemrun.base.module.bpm.api.BpmBusinessTypeApi;
import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeRespDTO;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.gemrun.base.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.gemrun.base.module.declare.dal.dataobject.process.DeclareBusinessProcessDO;
import cn.gemrun.base.module.declare.dal.mysql.process.DeclareBusinessProcessMapper;
import cn.gemrun.base.module.declare.framework.process.service.DeclareProcessService;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil;
import cn.hutool.core.util.*;
import lombok.extern.slf4j.*;
import org.flowable.engine.*;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 流程配置服务实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class DeclareProcessServiceImpl implements DeclareProcessService {

    @Resource
    private BpmBusinessTypeApi bpmBusinessTypeApi;

    @Resource
    private DeclareBusinessProcessMapper businessProcessMapper;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private BpmProcessDefinitionService processDefinitionService;

    @Override
    public BpmBusinessTypeRespDTO getProcessConfig(String businessType) {
        Assert.notNull(businessType, "businessType 不能为空");
        cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeGetReqDTO reqDTO = new cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeGetReqDTO();
        reqDTO.setBusinessType(businessType);
        return bpmBusinessTypeApi.getProcessConfig(reqDTO);
    }

    @Override
    public boolean hasProcessConfig(String businessType) {
        BpmBusinessTypeRespDTO config = getProcessConfig(businessType);
        return config != null && config.getEnabled() == 1;
    }

    @Override
    public String getProcessDefinitionKey(String businessType) {
        // 通过 API 获取
        return bpmBusinessTypeApi.getProcessDefinitionKey(businessType);
    }

    @Override
    @Transactional
    public String startProcessIfConfigured(String businessType, Long businessId, Long userId) {
        // 1. 查询流程配置（通过 API）
        BpmBusinessTypeRespDTO config = getProcessConfig(businessType);
        if (config == null || config.getEnabled() != 1) {
            log.info("未配置流程，跳过: businessType={}", businessType);
            return null;
        }

        // 2. 检查流程定义是否存在
        String processKey = config.getProcessDefinitionKey();
        org.flowable.engine.repository.ProcessDefinition processDefinition =
            processDefinitionService.getActiveProcessDefinition(processKey);
        if (processDefinition == null) {
            log.warn("流程定义不存在，跳过: processKey={}", processKey);
            return null;
        }

        // 3. 构建流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessType", businessType);
        variables.put("businessId", businessId);
        variables.put("initiator", userId);

        // 4. 启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            processKey,
            businessType + ":" + businessId,  // businessKey
            variables
        );

        String processInstanceId = processInstance.getId();
        log.info("流程已启动: businessType={}, businessId={}, processInstanceId={}",
            businessType, businessId, processInstanceId);

        // 5. 创建业务流程关联记录
        createBusinessProcess(businessType, businessId, processInstanceId, processKey, userId);

        return processInstanceId;
    }

    @Override
    @Transactional
    public String startProcess(String businessType, Long businessId, Long userId) {
        BpmBusinessTypeRespDTO config = getProcessConfig(businessType);
        if (config == null || config.getEnabled() != 1) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.PROCESS_CONFIG_NOT_FOUND);
        }
        return startProcessIfConfigured(businessType, businessId, userId);
    }

    @Override
    @Transactional
    public void updateProcessInstance(String processInstanceId, String currentNodeKey, String currentStatus) {
        DeclareBusinessProcessDO businessProcess = businessProcessMapper.selectByProcessInstanceId(processInstanceId);
        if (businessProcess != null) {
            businessProcess.setCurrentNodeKey(currentNodeKey);
            businessProcess.setCurrentStatus(currentStatus);
            businessProcessMapper.updateById(businessProcess);
        }
    }

    @Override
    @Transactional
    public void endProcess(String processInstanceId, String result) {
        DeclareBusinessProcessDO businessProcess = businessProcessMapper.selectByProcessInstanceId(processInstanceId);
        if (businessProcess != null) {
            businessProcess.setEndTime(LocalDateTime.now());
            businessProcess.setResult(result);
            businessProcessMapper.updateById(businessProcess);
        }
    }

    @Override
    public String getProcessInstanceId(String businessType, Long businessId) {
        DeclareBusinessProcessDO businessProcess = businessProcessMapper.selectByBusiness(businessType, businessId);
        if (businessProcess != null) {
            return businessProcess.getProcessInstanceId();
        }
        return null;
    }

    @Override
    public DeclareBusinessProcessDO getBusinessProcess(String businessType, Long businessId) {
        return businessProcessMapper.selectByBusiness(businessType, businessId);
    }

    @Override
    public String parseBusinessType(String methodName, String className) {
        // 解析模块名：取包名第二部分
        String[] packageParts = className.split("\\.");
        String moduleName = packageParts.length > 1 ? packageParts[1] : "declare";

        // 解析业务名：取类名，去掉 Controller 后缀
        String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
        if (simpleClassName.endsWith("Controller")) {
            simpleClassName = simpleClassName.substring(0, simpleClassName.length() - 10);
        } else if (simpleClassName.endsWith("DO")) {
            simpleClassName = simpleClassName.substring(0, simpleClassName.length() - 2);
        }
        String tableName = StrUtil.lowerFirst(simpleClassName);

        // 解析动作名
        String actionName = methodName;
        if (actionName.startsWith("submit")) {
            actionName = "submit";
        } else if (actionName.startsWith("start")) {
            actionName = "start";
        } else if (actionName.startsWith("audit")) {
            actionName = "audit";
        } else if (actionName.startsWith("approve")) {
            actionName = "approve";
        } else if (actionName.startsWith("reject")) {
            actionName = "reject";
        } else if (actionName.startsWith("rectification")) {
            actionName = "rectification";
        } else if (actionName.startsWith("midterm")) {
            actionName = "midterm";
        } else if (actionName.startsWith("annual")) {
            actionName = "annual";
        }

        return moduleName + ":" + tableName + ":" + actionName;
    }

    /**
     * 创建业务流程关联记录
     */
    private void createBusinessProcess(String businessType, Long businessId,
                                       String processInstanceId, String processDefinitionKey, Long userId) {
        DeclareBusinessProcessDO businessProcess = new DeclareBusinessProcessDO();
        businessProcess.setBusinessType(businessType);
        businessProcess.setBusinessId(businessId);
        businessProcess.setProcessInstanceId(processInstanceId);
        businessProcess.setProcessDefinitionKey(processDefinitionKey);
        businessProcess.setInitiatorId(userId);
        businessProcess.setStartTime(LocalDateTime.now());
        businessProcess.setCurrentStatus("STARTED");
        businessProcessMapper.insert(businessProcess);
    }

}
