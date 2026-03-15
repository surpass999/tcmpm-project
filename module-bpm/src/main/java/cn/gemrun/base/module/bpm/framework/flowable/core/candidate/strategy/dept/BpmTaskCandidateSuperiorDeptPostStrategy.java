package cn.gemrun.base.module.bpm.framework.flowable.core.candidate.strategy.dept;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.gemrun.base.framework.common.util.number.NumberUtils;
import cn.gemrun.base.framework.common.util.string.StrUtils;
import cn.gemrun.base.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.system.api.dept.DeptApi;
import cn.gemrun.base.module.system.api.dept.PostApi;
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
import java.util.stream.Collectors;

import static cn.gemrun.base.framework.common.util.collection.SetUtils.asSet;

/**
 * 上级部门+岗位策略
 * 查找发起人的上级部门，并获取该部门下指定岗位的用户
 *
 * 参数格式：岗位ID,部门层级（如：100,2 表示查找发起人第2级上级部门下的岗位用户）
 *
 * @author Gemini
 */
@Component
public class BpmTaskCandidateSuperiorDeptPostStrategy extends AbstractBpmTaskCandidateDeptLeaderStrategy {

    @Resource
    @Lazy
    private BpmProcessInstanceService processInstanceService;

    @Resource
    private PostApi postApi;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.SUPERIOR_DEPT_POST;
    }

    @Override
    public void validateParam(String param) {
        Assert.isTrue(StrUtil.isNotBlank(param), "上级部门+岗位策略参数不能为空");
        String[] parts = param.split(",");
        Assert.isTrue(parts.length == 2, "参数格式错误，应为：岗位ID,部门层级，如：100,2");

        // 验证岗位ID
        Long postId = NumberUtils.parseLong(parts[0]);
        Assert.notNull(postId, "岗位ID格式错误");
        postApi.validPostList(Collections.singletonList(postId));

        // 验证部门层级
        Integer level = NumberUtils.parseInt(parts[1]);
        Assert.isTrue(level > 0, "部门层级必须大于0");
    }

    @Override
    public Set<Long> calculateUsersByTask(DelegateExecution execution, String param) {
        // 获取流程发起人
        ProcessInstance processInstance = processInstanceService.getProcessInstance(execution.getProcessInstanceId());
        Long startUserId = NumberUtils.parseLong(processInstance.getStartUserId());

        // 获取上级部门的岗位用户
        return getSuperiorDeptPostUsers(startUserId, param);
    }

    @Override
    public Set<Long> calculateUsersByActivity(BpmnModel bpmnModel, String activityId, String param,
                                               Long startUserId, String processDefinitionId, Map<String, Object> processVariables) {
        // 获取上级部门的岗位用户
        return getSuperiorDeptPostUsers(startUserId, param);
    }

    /**
     * 获取发起人上级部门指定岗位的用户
     *
     * @param startUserId 发起人ID
     * @param param       参数格式：岗位ID,部门层级
     * @return 用户ID集合
     */
    private Set<Long> getSuperiorDeptPostUsers(Long startUserId, String param) {
        String[] parts = param.split(",");
        Long postId = NumberUtils.parseLong(parts[0]);
        Integer level = NumberUtils.parseInt(parts[1]);

        // 1. 获取发起人所在部门
        DeptRespDTO startDept = super.getStartUserDept(startUserId);
        if (startDept == null) {
            return new HashSet<>();
        }

        // 2. 查找指定层级的上级部门
        DeptRespDTO targetDept = getTargetLevelDept(startDept, level);
        if (targetDept == null) {
            return new HashSet<>();
        }

        // 3. 获取该部门及其子部门下指定岗位的用户
        // 先获取部门下的所有用户，再过滤岗位
        Set<Long> userIds = new HashSet<>();

        // 获取指定部门的用户
        List<AdminUserRespDTO> deptUsers = adminUserApi.getUserListByDeptIds(Collections.singletonList(targetDept.getId()));
        if (CollUtil.isEmpty(deptUsers)) {
            return userIds;
        }

        // 获取岗位下的所有用户
        List<AdminUserRespDTO> postUsers = adminUserApi.getUserListByPostIds(Collections.singletonList(postId));
        if (CollUtil.isEmpty(postUsers)) {
            return userIds;
        }

        // 取交集：同时属于该部门且拥有该岗位的用户
        Set<Long> postUserIds = new HashSet<>(postUsers.stream().map(AdminUserRespDTO::getId).collect(Collectors.toList()));
        for (AdminUserRespDTO user : deptUsers) {
            if (postUserIds.contains(user.getId())) {
                userIds.add(user.getId());
            }
        }

        return userIds;
    }

    /**
     * 获取指定层级的部门
     *
     * @param dept   起始部门
     * @param level  目标层级（从1开始，1表示上一级部门）
     * @return 目标部门，如果不存在返回null
     */
    private DeptRespDTO getTargetLevelDept(DeptRespDTO dept, Integer level) {
        if (dept == null || level <= 0) {
            return null;
        }

        DeptRespDTO currentDept = dept;
        for (int i = 0; i < level; i++) {
            if (currentDept.getParentId() == null || currentDept.getParentId() == 0L) {
                // 已到达顶级部门，返回当前部门
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
