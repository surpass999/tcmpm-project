package cn.gemrun.base.module.bpm.framework.flowable.core.behavior;

import cn.hutool.core.collection.CollUtil;
import cn.gemrun.base.framework.common.util.collection.SetUtils;
import cn.gemrun.base.module.bpm.enums.definition.BpmChildProcessMultiInstanceSourceTypeEnum;
import cn.gemrun.base.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmnModelConstants;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.FlowableUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.common.engine.api.delegate.Expression;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自定义的【并行】的【多个】流程任务的 assignee 负责人的分配
 * 第一步，基于分配规则，计算出分配任务的【多个】候选人们。
 * 第二步，将【多个】任务候选人们，设置到 DelegateExecution 的 collectionVariable 变量中，以便 BpmUserTaskActivityBehavior 使用它
 *
 * @author kemengkai
 * @since 2022-04-21 16:57
 */
@Slf4j
@Setter
public class BpmParallelMultiInstanceBehavior extends ParallelMultiInstanceBehavior {

    private BpmTaskCandidateInvoker taskCandidateInvoker;

    public BpmParallelMultiInstanceBehavior(Activity activity,
                                            AbstractBpmnActivityBehavior innerActivityBehavior) {
        super(activity, innerActivityBehavior);
        // 关联 Pull Request：https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1483
        // 在解析/构造阶段基于 activityId 初始化与 activity 绑定且不变的字段，避免在运行期修改 Behavior 实例状态
        super.collectionExpression = null; // collectionExpression 和 collectionVariable 是互斥的
        super.collectionVariable = FlowableUtils.formatExecutionCollectionVariable(activity.getId());
        // 从 execution.getVariable() 读取当前所有任务处理的人的 key
        super.collectionElementVariable = FlowableUtils.formatExecutionCollectionElementVariable(activity.getId());
    }

    /**
     * 重写该方法，主要实现两个功能：
     * 1. 忽略原有的 collectionVariable、collectionElementVariable 表达式，而是采用自己定义的
     * 2. 获得任务的处理人，并设置到 collectionVariable 中，用于 BpmUserTaskActivityBehavior 从中可以获取任务的处理人
     *
     * 注意，多个任务实例，每个任务实例对应一个处理人，所以返回的数量就是任务处理人的数量
     *
     * @param execution 执行任务
     * @return 数量
     */
    @Override
    protected int resolveNrOfInstances(DelegateExecution execution) {
        // 情况一：UserTask 节点
        if (execution.getCurrentFlowElement() instanceof UserTask) {
            // 获取任务的所有处理人
            @SuppressWarnings("unchecked")
            Set<Long> assigneeUserIds = (Set<Long>) execution.getVariable(super.collectionVariable, Set.class);
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

    // ========== 屏蔽解析器覆写 ==========

    @Override
    public void setCollectionExpression(Expression collectionExpression) {
        // 保持自定义变量名，忽略解析器写入的 collection 表达式
    }

    @Override
    public void setCollectionVariable(String collectionVariable) {
        // 保持自定义变量名，忽略解析器写入的 collection 变量名
    }

    @Override
    public void setCollectionElementVariable(String collectionElementVariable) {
        // 保持自定义变量名，忽略解析器写入的单元素变量名
    }

    /**
     * 评审（按总分）完成判断 - 通过设置 completionCondition 表达式实现
     * 此方法在所有实例完成后被调用
     */
    public void verifySequentialCompletion(DelegateExecution execution) {
        // 获取审批方式
        Integer approveMethod = getApproveMethod(execution);

        // 如果是"评审（按总分）"模式
        if (approveMethod != null && approveMethod == 5) {
            evaluateScoreCompletion(execution);
        }
    }

    /**
     * 获取审批方式
     */
    private Integer getApproveMethod(DelegateExecution execution) {
        FlowElement flowElement = execution.getCurrentFlowElement();
        if (flowElement instanceof UserTask) {
            UserTask userTask = (UserTask) flowElement;
            List<ExtensionElement> elements = userTask.getExtensionElements().get(BpmnModelConstants.USER_TASK_APPROVE_METHOD);
            if (CollUtil.isNotEmpty(elements)) {
                return Integer.parseInt(elements.get(0).getElementText());
            }
        }
        return null;
    }

    /**
     * 获取通过分数
     */
    private Integer getPassScore(DelegateExecution execution) {
        FlowElement flowElement = execution.getCurrentFlowElement();
        if (flowElement instanceof UserTask) {
            UserTask userTask = (UserTask) flowElement;
            List<ExtensionElement> elements = userTask.getExtensionElements().get(BpmnModelConstants.USER_TASK_PASS_SCORE);
            if (CollUtil.isNotEmpty(elements)) {
                return Integer.parseInt(elements.get(0).getElementText());
            }
        }
        return null;
    }

    /**
     * 评审（按总分）完成判断
     */
    @SuppressWarnings("unchecked")
    private void evaluateScoreCompletion(DelegateExecution execution) {
        // 1. 获取 passScore 配置
        Integer passScore = getPassScore(execution);
        if (passScore == null) {
            log.warn("[evaluateScoreCompletion] passScore 未配置");
            return;
        }

        // 2. 从变量获取评分列表
        List<Map<String, Object>> scores = getScoresFromVariables(execution);

        // 3. 计算平均分
        BigDecimal averageScore = calculateAverage(scores);

        // 4. 判断是否达标
        if (averageScore.compareTo(BigDecimal.valueOf(passScore)) >= 0) {
            // 达标：设置标记，允许流转
            execution.setVariable("reviewPass", true);
            execution.setVariable("reviewAverageScore", averageScore);
            log.info("[evaluateScoreCompletion] 评分达标: 平均分={}, 通过分数={}", averageScore, passScore);
        } else {
            // 不达标：设置标记，阻止流转
            execution.setVariable("reviewPass", false);
            execution.setVariable("reviewAverageScore", averageScore);
            log.info("[evaluateScoreCompletion] 评分未达标: 平均分={}, 通过分数={}", averageScore, passScore);
        }
    }

    /**
     * 从 Flowable 变量获取评分列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getScoresFromVariables(DelegateExecution execution) {
        Object scoresObj = execution.getVariable("reviewScores");
        if (scoresObj == null) {
            return new ArrayList<>();
        }
        return (List<Map<String, Object>>) scoresObj;
    }

    /**
     * 计算平均分
     */
    private BigDecimal calculateAverage(List<Map<String, Object>> scores) {
        if (scores == null || scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = scores.stream()
            .filter(s -> s.get("score") != null)
            .map(s -> new BigDecimal(s.get("score").toString()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

}
