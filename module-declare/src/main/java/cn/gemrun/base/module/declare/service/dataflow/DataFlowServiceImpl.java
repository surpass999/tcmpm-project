package cn.gemrun.base.module.declare.service.dataflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
import cn.gemrun.base.module.declare.controller.admin.dataflow.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.dataflow.DataFlowDO;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.dal.mysql.dataflow.DataFlowMapper;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCreateReqVO;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import org.flowable.engine.repository.ProcessDefinition;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;

/**
 * 数据流通记录 Service 实现类
 *
 * @author
 */
@Service
@Slf4j
public class DataFlowServiceImpl implements DataFlowService {

    @Resource
    private DataFlowMapper dataFlowMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ProjectService projectService;
    @Resource
    private BpmProcessInstanceService bpmProcessInstanceService;
    @Resource
    private BpmProcessDefinitionService bpmProcessDefinitionService;

    @Override
    public Long createDataFlow(DataFlowSaveReqVO createReqVO) {
        // 获取当前登录用户的部门ID
        Long deptId = null;
        Long userId = null;
        try {
            userId = WebFrameworkUtils.getLoginUserId();
            if (userId != null) {
                AdminUserRespDTO user = adminUserApi.getUser(userId);
                if (user != null) {
                    deptId = user.getDeptId();
                }
            }
        } catch (Exception e) {
            log.warn("[createDataFlow] 获取用户部门ID失败: {}", e.getMessage());
        }

        // 转换并插入
        DataFlowDO dataFlow = BeanUtils.toBean(createReqVO, DataFlowDO.class);
        dataFlow.setDeptId(deptId);
        dataFlow.setStatus(0); // 草稿状态

        dataFlowMapper.insert(dataFlow);
        log.info("[createDataFlow] 创建数据流通记录成功, id: {}", dataFlow.getId());

        return dataFlow.getId();
    }

    @Override
    public void updateDataFlow(DataFlowSaveReqVO updateReqVO) {
        // 校验存在
        validateDataFlowExists(updateReqVO.getId());

        // 更新
        DataFlowDO updateObj = BeanUtils.toBean(updateReqVO, DataFlowDO.class);
        dataFlowMapper.updateById(updateObj);

        log.info("[updateDataFlow] 更新数据流通记录, id: {}", updateReqVO.getId());
    }

    @Override
    public void deleteDataFlow(Long id) {
        // 校验存在
        validateDataFlowExists(id);
        // 删除
        dataFlowMapper.deleteById(id);
        log.info("[deleteDataFlow] 删除数据流通记录, id: {}", id);
    }

    @Override
    public DataFlowDO getDataFlow(Long id) {
        return dataFlowMapper.selectById(id);
    }

    @Override
    public PageResult<DataFlowDO> getDataFlowPage(DataFlowPageReqVO pageReqVO) {
        return dataFlowMapper.selectPage(pageReqVO);
    }

    @Override
    public List<DataFlowDO> getDataFlowListByProjectId(Long projectId) {
        return dataFlowMapper.selectByProjectId(projectId);
    }

    @Override
    public String submitDataFlow(Long id, String processDefinitionKey) {
        // 1. 校验数据流通是否存在
        DataFlowDO dataFlow = validateDataFlowExists(id);

        // 2. 校验状态必须是草稿才能提交
        if (dataFlow.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的数据流通才能提交审核，当前状态: " + dataFlow.getStatus());
        }

        // 3. 如果已有流程实例ID，说明已发起流程
        if (dataFlow.getProcessInstanceId() != null && !dataFlow.getProcessInstanceId().isEmpty()) {
            log.warn("[submitDataFlow] 数据流通已存在流程实例: dataFlowId={}, processInstanceId={}",
                id, dataFlow.getProcessInstanceId());
            return dataFlow.getProcessInstanceId();
        }

        // 4. 获取流程定义
        ProcessDefinition processDefinition = bpmProcessDefinitionService.getActiveProcessDefinition(processDefinitionKey);
        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在: " + processDefinitionKey);
        }

        // 5. 构建流程变量
        String businessKey = String.format("declare:dataflow:submit:%d", id);
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessKey", businessKey);
        variables.put("businessType", "dataflow");
        variables.put("title", "数据流通审核 - " + dataFlow.getDataName());

        // 6. 发起流程
        BpmProcessInstanceCreateReqVO createReqVO = new BpmProcessInstanceCreateReqVO();
        createReqVO.setProcessDefinitionId(processDefinition.getId());
        createReqVO.setVariables(variables);

        Long userId = getCurrentUserId();
        String processInstanceId = bpmProcessInstanceService.createProcessInstance(userId, createReqVO);

        // 7. 更新数据流通的流程实例ID和状态
        dataFlow.setProcessInstanceId(processInstanceId);
        dataFlow.setStatus(1); // 已提交
        dataFlowMapper.updateById(dataFlow);

        log.info("[submitDataFlow] 提交审核成功: dataFlowId={}, processInstanceId={}",
            id, processInstanceId);

        return processInstanceId;
    }

    @Override
    public void updateDataFlowStatus(Long id, String bizStatus) {
        DataFlowDO dataFlow = dataFlowMapper.selectById(id);
        if (dataFlow == null) {
            log.warn("[updateDataFlowStatus] 数据流通不存在: id={}", id);
            return;
        }

        // 解析状态码
        Integer status = parseStatus(bizStatus);
        dataFlow.setStatus(status);
        dataFlowMapper.updateById(dataFlow);

        log.info("[updateDataFlowStatus] 更新数据流通状态: id={}, bizStatus={}, status={}", id, bizStatus, status);
    }

    @Override
    public void updateDataFlowProcessInstance(Long id, String processInstanceId, Integer status) {
        DataFlowDO dataFlow = validateDataFlowExists(id);
        dataFlow.setProcessInstanceId(processInstanceId);
        dataFlow.setStatus(status);
        dataFlowMapper.updateById(dataFlow);

        log.info("[updateDataFlowProcessInstance] 更新数据流通流程实例: id={}, processInstanceId={}, status={}",
            id, processInstanceId, status);
    }

    private DataFlowDO validateDataFlowExists(Long id) {
        DataFlowDO dataFlow = dataFlowMapper.selectById(id);
        if (dataFlow == null) {
            throw exception(DATAFLOW_NOT_EXISTS);
        }
        return dataFlow;
    }

    private Integer parseStatus(String bizStatus) {
        if (bizStatus == null) {
            return 2; // 审核中
        }
        // 根据 bizStatus 解析状态
        // 这里简化处理，实际应根据流程配置解析
        if (bizStatus.contains("APPROVED") || bizStatus.contains("通过")) {
            return 3; // 已通过
        } else if (bizStatus.contains("REJECTED") || bizStatus.contains("退回")) {
            return 4; // 退回
        } else if (bizStatus.contains("SUBMITTED") || bizStatus.contains("提交")) {
            return 1; // 已提交
        }
        return 2; // 审核中
    }

    private Long getCurrentUserId() {
        try {
            return SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception e) {
            log.warn("[getCurrentUserId] 获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

}
