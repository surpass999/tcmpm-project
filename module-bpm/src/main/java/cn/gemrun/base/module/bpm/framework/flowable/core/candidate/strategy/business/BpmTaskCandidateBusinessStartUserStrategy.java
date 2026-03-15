package cn.gemrun.base.module.bpm.framework.flowable.core.candidate.strategy.business;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.gemrun.base.framework.common.util.number.NumberUtils;
import cn.gemrun.base.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.system.api.dept.DeptApi;
import cn.gemrun.base.module.system.api.dept.dto.DeptRespDTO;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.gemrun.base.framework.common.util.collection.SetUtils.asSet;

/**
 * 业务发起人策略
 * 通过流程变量中的业务创建人ID，获取其所在部门的用户
 *
 * 适用于整改流程：在项目列表或备案列表页面，由省级/国家级发起
 * 流程变量中需要包含：businessCreatorId（业务创建人ID）
 *
 * 参数格式：
 * - 不传参数：获取业务创建人所在部门的负责人
 * - level数字：获取业务创建人N级上级部门的负责人
 *
 * @author Gemini
 */
@Component
public class BpmTaskCandidateBusinessStartUserStrategy extends AbstractBpmTaskCandidateBusinessStrategy {

    @Resource
    @Lazy
    private BpmProcessInstanceService processInstanceService;

    @Resource
    private DeptApi deptApi;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.BUSINESS_START_USER;
    }

    @Override
    public void validateParam(String param) {
        // 参数可选：
        // - 空：获取业务创建人所在部门的负责人
        // - 数字：获取业务创建人N级上级部门的负责人
        if (StrUtil.isNotBlank(param)) {
            try {
                Integer level = Integer.parseInt(param);
                Assert.isTrue(level > 0, "部门层级必须大于0");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("参数格式错误，应为数字或空，如：2 表示第2级上级部门负责人");
            }
        }
    }

    @Override
    public Set<Long> calculateUsersByTask(DelegateExecution execution, String param) {
        // 1. 从流程变量中获取业务创建人ID
        Long businessCreatorId = getBusinessCreatorId(execution);
        if (businessCreatorId == null) {
            return new HashSet<>();
        }

        // 2. 获取业务创建人的部门
        AdminUserRespDTO businessCreator = adminUserApi.getUser(businessCreatorId);
        if (businessCreator == null || businessCreator.getDeptId() == null) {
            return new HashSet<>();
        }

        DeptRespDTO dept = deptApi.getDept(businessCreator.getDeptId());
        if (dept == null) {
            return new HashSet<>();
        }

        // 3. 根据参数获取部门负责人
        return getDeptLeaderUsers(dept, param);
    }

    @Override
    public Set<Long> calculateUsersByActivity(BpmnModel bpmnModel, String activityId, String param,
                                               Long startUserId, String processDefinitionId, Map<String, Object> processVariables) {
        // 1. 从流程变量中获取业务创建人ID
        Long businessCreatorId = getBusinessCreatorId(processVariables);
        if (businessCreatorId == null) {
            return new HashSet<>();
        }

        // 2. 获取业务创建人的部门
        AdminUserRespDTO businessCreator = adminUserApi.getUser(businessCreatorId);
        if (businessCreator == null || businessCreator.getDeptId() == null) {
            return new HashSet<>();
        }

        DeptRespDTO dept = deptApi.getDept(businessCreator.getDeptId());
        if (dept == null) {
            return new HashSet<>();
        }

        // 3. 根据参数获取部门负责人
        return getDeptLeaderUsers(dept, param);
    }

    /**
     * 从执行对象中获取业务创建人ID
     */
    private Long getBusinessCreatorId(DelegateExecution execution) {
        ProcessInstance processInstance = processInstanceService.getProcessInstance(execution.getProcessInstanceId());
        if (processInstance == null) {
            return null;
        }

        // 优先从流程变量中获取
        Object businessCreatorIdVar = execution.getVariable("businessCreatorId");
        if (businessCreatorIdVar != null) {
            return NumberUtils.parseLong(businessCreatorIdVar.toString());
        }

        // 尝试从 businessKey 解析（格式：tableName:id）
        String businessKey = processInstance.getBusinessKey();
        if (StrUtil.isNotBlank(businessKey)) {
            // 如果业务方在发起流程时传入了业务创建人ID
            Object startUserIdVar = execution.getVariable("startUserId");
            if (startUserIdVar != null) {
                return NumberUtils.parseLong(startUserIdVar.toString());
            }
        }

        // 默认使用流程发起人
        return NumberUtils.parseLong(processInstance.getStartUserId());
    }

    /**
     * 从流程变量中获取业务创建人ID
     */
    private Long getBusinessCreatorId(Map<String, Object> processVariables) {
        if (CollUtil.isEmpty(processVariables)) {
            return null;
        }

        // 优先从流程变量中获取
        Object businessCreatorIdVar = processVariables.get("businessCreatorId");
        if (businessCreatorIdVar != null) {
            return NumberUtils.parseLong(businessCreatorIdVar.toString());
        }

        // 使用发起人作为兜底
        Object startUserIdVar = processVariables.get("startUserId");
        if (startUserIdVar != null) {
            return NumberUtils.parseLong(startUserIdVar.toString());
        }

        return null;
    }

    /**
     * 获取部门负责人用户
     *
     * @param dept  起始部门
     * @param param 参数：空或数字（层级）
     * @return 用户ID集合
     */
    private Set<Long> getDeptLeaderUsers(DeptRespDTO dept, String param) {
        if (dept == null) {
            return new HashSet<>();
        }

        // 无参数：返回当前部门负责人
        if (StrUtil.isBlank(param)) {
            return dept.getLeaderUserId() != null ? asSet(dept.getLeaderUserId()) : new HashSet<>();
        }

        // 有参数：查找指定层级的部门负责人
        int level = Integer.parseInt(param);
        DeptRespDTO targetDept = getTargetLevelDept(dept, level);
        if (targetDept == null || targetDept.getLeaderUserId() == null) {
            return new HashSet<>();
        }

        return asSet(targetDept.getLeaderUserId());
    }

    /**
     * 获取指定层级的部门
     */
    private DeptRespDTO getTargetLevelDept(DeptRespDTO dept, Integer level) {
        if (dept == null || level <= 0) {
            return null;
        }

        DeptRespDTO currentDept = dept;
        for (int i = 1; i < level; i++) {
            if (currentDept.getParentId() == null || currentDept.getParentId() == 0L) {
                return currentDept;
            }
            DeptRespDTO parentDept = deptApi.getDept(currentDept.getParentId());
            if (parentDept == null) {
                return currentDept;
            }
            currentDept = parentDept;
        }
        return currentDept;
    }

}
