package cn.gemrun.base.module.bpm.framework.flowable.core.behavior;

import cn.hutool.core.collection.CollUtil;
import cn.gemrun.base.framework.common.util.collection.SetUtils;
import cn.gemrun.base.module.bpm.enums.definition.BpmChildProcessMultiInstanceSourceTypeEnum;
import cn.gemrun.base.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.FlowableUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

import java.util.List;
import java.util.Set;

/**
 * 自定义的【串行】的【多个】流程任务的 assignee 负责人的分配
 *
 * 本质上，实现和 {@link BpmParallelMultiInstanceBehavior} 一样，只是继承的类不一样
 *
 * @author 芋道源码
 */
@Slf4j
@Setter
public class BpmSequentialMultiInstanceBehavior extends SequentialMultiInstanceBehavior {

    private BpmTaskCandidateInvoker taskCandidateInvoker;

    public BpmSequentialMultiInstanceBehavior(Activity activity, AbstractBpmnActivityBehavior innerActivityBehavior) {
        super(activity, innerActivityBehavior);
        // 关联 Pull Request：https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1483
        // 在解析/构造阶段基于 activityId 初始化与 activity 绑定且不变的字段，避免在运行期修改 Behavior 实例状态
        super.collectionExpression = null; // collectionExpression 和 collectionVariable 是互斥的
        super.collectionVariable = FlowableUtils.formatExecutionCollectionVariable(activity.getId());
        // 从 execution.getVariable() 读取当前所有任务处理的人的 key
        super.collectionElementVariable = FlowableUtils.formatExecutionCollectionElementVariable(activity.getId());

        // 处理会签规则的 completionCondition
        processSignRuleCompletionCondition(activity);
    }

    /**
     * 处理会签规则的 completionCondition
     * 根据 DSL 中的 signRule 动态设置多实例完成条件
     */
    private void processSignRuleCompletionCondition(Activity activity) {
        try {
            // 获取 Activity 中的 FlowElement（UserTask）
            if (!(activity instanceof UserTask)) {
                return;
            }
            UserTask userTask = (UserTask) activity;

            // 解析 DSL 中的 signRule
            String signRule = BpmnModelUtils.parseSignRule(userTask);
            if (signRule == null) {
                log.debug("[processSignRuleCompletionCondition] 节点 {} 未配置 signRule，跳过", userTask.getId());
                return;
            }

            // 根据 signRule 生成 completionCondition
            String completionCondition = BpmnModelUtils.buildSignRuleCompletionCondition(signRule);
            if (completionCondition == null) {
                log.debug("[processSignRuleCompletionCondition] 节点 {} signRule={} 无对应规则，跳过",
                        userTask.getId(), signRule);
                return;
            }

            // 获取或创建多实例配置
            MultiInstanceLoopCharacteristics loopCharacteristics = userTask.getLoopCharacteristics();
            if (loopCharacteristics == null) {
                // 如果没有多实例配置，创建一个（专家会签场景）
                loopCharacteristics = new MultiInstanceLoopCharacteristics();
                loopCharacteristics.setInputDataItem("${coll_userList}");
                loopCharacteristics.setSequential(true); // 串行
                loopCharacteristics.setLoopCardinality("1");
                userTask.setLoopCharacteristics(loopCharacteristics);
                log.info("[processSignRuleCompletionCondition] 节点 {} 创建新的串行多实例配置", userTask.getId());
            }

            // 设置完成条件
            loopCharacteristics.setCompletionCondition(completionCondition);
            log.info("[processSignRuleCompletionCondition] 节点 {} 设置会签规则: signRule={}, completionCondition={}",
                    userTask.getId(), signRule, completionCondition);

        } catch (Exception e) {
            log.warn("[processSignRuleCompletionCondition] 处理会签规则失败: {}", e.getMessage());
        }
    }

    /**
     * 逻辑和 {@link BpmParallelMultiInstanceBehavior#resolveNrOfInstances(DelegateExecution)} 类似
     *
     * 差异的点：是在【第二步】的时候，需要返回 LinkedHashSet 集合！因为它需要有序！
     */
    @Override
    protected int resolveNrOfInstances(DelegateExecution execution) {
        // 情况一：UserTask 节点
        if (execution.getCurrentFlowElement() instanceof UserTask) {
            // 获取任务的所有处理人
            // 不使用 execution.getVariable 原因：目前依次审批任务回退后 collectionVariable 变量没有清理， 如果重新进入该任务不会重新分配审批人
            @SuppressWarnings("unchecked")
            Set<Long> assigneeUserIds = (Set<Long>) execution.getVariableLocal(super.collectionVariable, Set.class);
            if (assigneeUserIds == null) {
                assigneeUserIds = taskCandidateInvoker.calculateUsersByTask(execution);
                if (CollUtil.isEmpty(assigneeUserIds)) {
                    // 特殊：如果没有处理人的情况下，至少有一个 null 空元素，避免自动通过！
                    // 这样，保证在 BpmUserTaskActivityBehavior 至少创建出一个 Task 任务
                    // 用途：1）审批人为空时；2）审批类型为自动通过、自动拒绝时
                    assigneeUserIds = SetUtils.asSet((Long) null);
                }
                execution.setVariableLocal(super.collectionVariable, assigneeUserIds);
            }
            return assigneeUserIds.size();
        }

        // 情况二：CallActivity 节点
        if (execution.getCurrentFlowElement() instanceof CallActivity) {
            FlowElement flowElement = execution.getCurrentFlowElement();
            Integer sourceType = BpmnModelUtils.parseMultiInstanceSourceType(flowElement);
            if (sourceType.equals(BpmChildProcessMultiInstanceSourceTypeEnum.NUMBER_FORM.getType())) {
                return execution.getVariable(super.collectionExpression.getExpressionText(), Integer.class);
            }
            if (sourceType.equals(BpmChildProcessMultiInstanceSourceTypeEnum.MULTIPLE_FORM.getType())) {
                return execution.getVariable(super.collectionExpression.getExpressionText(), List.class).size();
            }
        }

        return super.resolveNrOfInstances(execution);
    }

    @Override
    protected void executeOriginalBehavior(DelegateExecution execution, ExecutionEntity multiInstanceRootExecution, int loopCounter) {
        // 参见 https://t.zsxq.com/53Meo 情况
        if (execution.getCurrentFlowElement() instanceof CallActivity
            || execution.getCurrentFlowElement() instanceof SubProcess) {
            super.executeOriginalBehavior(execution, multiInstanceRootExecution, loopCounter);
            return;
        }
        super.executeOriginalBehavior(execution, multiInstanceRootExecution, loopCounter);
    }

}
