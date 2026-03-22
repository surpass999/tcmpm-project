package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmTaskCreatedEvent;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEventListener;
import cn.gemrun.base.module.bpm.framework.core.util.BusinessKeyParser;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.gemrun.base.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.gemrun.base.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.gemrun.base.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.task.BpmTaskService;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewResultDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewTaskDO;
import cn.gemrun.base.module.declare.dal.dataobject.expert.ExpertDO;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewResultMapper;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewTaskMapper;
import cn.gemrun.base.module.declare.service.expert.ExpertService;
import cn.gemrun.base.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评审任务状态监听器
 * 监听BPM任务事件，创建/更新评审任务和评审结果
 *
 * @author Gemini
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewTaskStatusListener extends BpmTaskStatusEventListener {

    private final ReviewTaskMapper reviewTaskMapper;
    private final ReviewResultMapper reviewResultMapper;
    private final BpmTaskService bpmTaskService;
    private final BpmProcessDefinitionService bpmProcessDefinitionService;
    private final BpmProcessInstanceService bpmProcessInstanceService;
    private final ExpertService expertService;

    /**
     * 驳回原因模板
     */
    private static final String REJECT_REASON_TEMPLATE = "评审分数未达标，系统自动驳回。平均分：%s，通过分数：%s";

    /**
     * 监听专家评审任务创建事件
     * 当 Flowable 创建专家评审任务时，更新 declare_review_task 表的 expertIds 和状态
     */
    @EventListener
    public void onTaskCreated(BpmTaskCreatedEvent event) {
        String taskDefinitionKey = event.getTaskDefinitionKey();
        if (taskDefinitionKey == null) {
            return;
        }

        // 判断是否是专家评审任务：1）通过 bizStatus == "EXPERT_REVIEWING" 判断（select-expert 按钮特有）
        // 2）通过 taskDefinitionKey 包含 "expert_review" 判断（不区分大小写）
        boolean isExpertReview = "EXPERT_REVIEWING".equals(event.getBizStatus())
                || (taskDefinitionKey != null && taskDefinitionKey.toLowerCase().contains("expert_review"));
        if (!isExpertReview) {
            return;
        }

        String businessKey = event.getBusinessKey();
        log.info("[ReviewTaskStatusListener] 收到专家任务创建事件: businessKey={}, taskKey={}, taskId={}",
                businessKey, taskDefinitionKey, event.getTaskId());

        try {
            Long businessId = BusinessKeyParser.parseBusinessId(event.getBusinessKey(), event.getProcessDefinitionKey());
            Integer businessType = getBusinessType(businessKey);
            if (businessType == null) {
                log.warn("[ReviewTaskStatusListener] 无法识别业务类型: businessKey={}", businessKey);
                return;
            }

            // 查找对应的评审任务记录
            List<ReviewTaskDO> tasks = reviewTaskMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewTaskDO>()
                            .eq(ReviewTaskDO::getBusinessId, businessId)
                            .eq(ReviewTaskDO::getBusinessType, businessType)
                            .eq(ReviewTaskDO::getTaskDefinitionKey, taskDefinitionKey)
            );

            ReviewTaskDO reviewTask;
            if (tasks.isEmpty()) {
                // 记录不存在（选派专家按钮节点和专家评审节点是分开的），
                // 在 Flowable 创建专家子任务时创建记录
                reviewTask = new ReviewTaskDO();
                reviewTask.setProcessInstanceId(event.getProcessInstanceId());
                reviewTask.setTaskDefinitionKey(taskDefinitionKey);
                reviewTask.setTaskName(event.getTaskName());
                reviewTask.setBusinessType(businessType);
                reviewTask.setBusinessId(businessId);
                reviewTask.setStatus(0); // 待分配
                reviewTask.setStartTime(LocalDateTime.now());
                // 根据业务类型设置任务类型：1=备案论证，2=项目论证，3=验收评审，4=成果审核
                reviewTask.setTaskType(getTaskTypeFromBusinessType(businessType, taskDefinitionKey));
                reviewTaskMapper.insert(reviewTask);
                log.info("[ReviewTaskStatusListener] onTaskCreated 创建评审任务: taskId={}, businessId={}, businessType={}",
                        reviewTask.getId(), businessId, businessType);
            } else {
                reviewTask = tasks.get(0);
            }

            // 如果 assignee 存在，说明专家任务被创建了，需要更新专家ID
            if (StringUtils.hasText(event.getAssignee())) {
                Long userId = Long.parseLong(event.getAssignee());
                // 通过 userId 查找专家记录
                ExpertDO expert = expertService.getExpertByUserId(userId);
                if (expert != null) {
                    // 追加专家ID到 expertIds 字段
                    String existingIds = reviewTask.getExpertIds();
                    String newIds;
                    if (StringUtils.hasText(existingIds)) {
                        // 检查是否已存在
                        if (!existingIds.contains(String.valueOf(expert.getId()))) {
                            newIds = existingIds + "," + expert.getId();
                        } else {
                            newIds = existingIds;
                        }
                    } else {
                        newIds = String.valueOf(expert.getId());
                    }
                    reviewTask.setExpertIds(newIds);
                    reviewTask.setStatus(1); // 评审中
                    reviewTaskMapper.updateById(reviewTask);
                    log.info("[ReviewTaskStatusListener] 更新评审任务专家: taskId={}, expertId={}, expertIds={}",
                            reviewTask.getId(), expert.getId(), newIds);

                    // 为该专家创建评审结果记录
                    createReviewResultForExpert(reviewTask, expert.getId(), businessType, event);
                } else {
                    log.warn("[ReviewTaskStatusListener] assignee 对应的专家不存在: userId={}", userId);
                }
            } else {
                // 无审批人，可能是会签场景，更新状态为评审中
                if (reviewTask.getStatus() == 0) {
                    reviewTask.setStatus(1);
                    reviewTaskMapper.updateById(reviewTask);
                    log.info("[ReviewTaskStatusListener] 更新评审任务状态为评审中: taskId={}", reviewTask.getId());
                }
            }

        } catch (Exception e) {
            log.error("[ReviewTaskStatusListener] 处理专家任务创建事件失败: businessKey={}", businessKey, e);
        }
    }

    /**
     * 为专家创建评审结果记录
     */
    private void createReviewResultForExpert(ReviewTaskDO reviewTask, Long expertId,
                                             Integer businessType, BpmTaskCreatedEvent event) {
        // 检查是否已存在该专家的评审结果
        List<ReviewResultDO> existing = reviewResultMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewResultDO>()
                        .eq(ReviewResultDO::getTaskId, reviewTask.getId())
                        .eq(ReviewResultDO::getExpertId, expertId)
        );

        if (!existing.isEmpty()) {
            log.info("[ReviewTaskStatusListener] 评审结果已存在，跳过创建: taskId={}, expertId={}",
                    reviewTask.getId(), expertId);
            return;
        }

        ReviewResultDO result = new ReviewResultDO();
        result.setTaskId(reviewTask.getId());
        result.setProcessInstanceId(event.getProcessInstanceId());
        result.setFlowableTaskId(event.getTaskId());
        result.setExpertId(expertId);
        result.setBusinessType(businessType);
        result.setBusinessId(reviewTask.getBusinessId());
        result.setStatus(0); // 待评审
        result.setReceiveTime(LocalDateTime.now());
        reviewResultMapper.insert(result);
        log.info("[ReviewTaskStatusListener] 创建评审结果: resultId={}, expertId={}, taskId={}",
                result.getId(), expertId, reviewTask.getId());
    }

    @Override
    protected String getProcessDefinitionKey() {
        // 监听所有申报相关流程
        return null; // null 表示监听所有流程，通过业务Key判断
    }

    @Override
    protected void onEvent(BpmTaskStatusEvent event) {
        String taskDefinitionKey = event.getTaskDefinitionKey();
        String businessKey = event.getBusinessKey();

        if (taskDefinitionKey == null || businessKey == null) {
            return;
        }

        // 判断是否是专家评审任务：1）通过 bizStatus == "EXPERT_REVIEWING" 判断（select-expert 按钮特有）
        // 2）通过 taskDefinitionKey 包含 "expert_review" 判断（不区分大小写）
        boolean isExpertReview = "EXPERT_REVIEWING".equals(event.getBizStatus())
                || (taskDefinitionKey != null && taskDefinitionKey.toLowerCase().contains("expert_review"));
        if (!isExpertReview) {
            return;
        }

        log.info("[ReviewTaskStatusListener] 收到专家评审节点事件: businessKey={}, taskKey={}, status={}",
                businessKey, taskDefinitionKey, event.getStatus());

        try {
            // 获取业务ID
            Long businessId = BusinessKeyParser.parseBusinessId(event.getBusinessKey(), event.getProcessDefinitionKey());
            // 获取业务类型
            Integer businessType = getBusinessType(businessKey);

            // 根据业务类型处理
            if (businessType != null) {
                switch (businessType) {
                    case 1:
                        handleFilingReview(event, businessId);
                        break;
                    case 2:
                        handleProjectReview(event, businessId);
                        break;
                    case 3:
                        handleAchievementReview(event, businessId);
                        break;
                }
            }

            // 处理评审通过判断逻辑
            if (BpmTaskStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
                handleReviewPassCheck(event);
            }
        } catch (Exception e) {
            log.error("[ReviewTaskStatusListener] 处理评审事件失败: businessKey={}", businessKey, e);
        }
    }

    /**
     * 处理评审通过判断逻辑
     * 当专家评分提交完成后，检查是否所有专家都已提交，计算平均分，判断是否通过
     */
    private void handleReviewPassCheck(BpmTaskStatusEvent event) {
        String businessKey = event.getBusinessKey();
        Integer businessType = getBusinessType(businessKey);
        if (businessType == null) {
            log.warn("[ReviewTaskStatusListener] 无法识别业务类型: businessKey={}", businessKey);
            return;
        }

        Long businessId;
        try {
            businessId = BusinessKeyParser.parseBusinessId(event.getBusinessKey(), event.getProcessDefinitionKey());
        } catch (Exception e) {
            log.warn("[ReviewTaskStatusListener] 解析业务ID失败: businessKey={}", businessKey, e);
            return;
        }

        // 查询该业务下的评审任务
        List<ReviewTaskDO> tasks = reviewTaskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewTaskDO>()
                        .eq(ReviewTaskDO::getBusinessId, businessId)
                        .eq(ReviewTaskDO::getBusinessType, businessType)
                        .eq(ReviewTaskDO::getTaskDefinitionKey, event.getTaskDefinitionKey())
        );

        if (tasks.isEmpty()) {
            return;
        }

        ReviewTaskDO reviewTask = tasks.get(0);

        // 查询所有已提交的评审结果
        List<ReviewResultDO> submittedResults = reviewResultMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewResultDO>()
                        .eq(ReviewResultDO::getTaskId, reviewTask.getId())
                        .eq(ReviewResultDO::getStatus, 2) // 已提交
        );

        // 查询所有评审结果（包括未提交的）
        List<ReviewResultDO> allResults = reviewResultMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewResultDO>()
                        .eq(ReviewResultDO::getTaskId, reviewTask.getId())
        );

        int submittedCount = submittedResults.size();
        int totalExpertCount = allResults.size();

        log.info("[ReviewTaskStatusListener] 评审进度: taskId={}, 已提交专家数={}, 专家总数={}",
                reviewTask.getId(), submittedCount, totalExpertCount);

        // 如果专家总数为0（异常情况），直接返回
        if (totalExpertCount == 0) {
            log.warn("[ReviewTaskStatusListener] 专家数量为0，跳过判断: taskId={}", reviewTask.getId());
            return;
        }

        // 如果不是全部专家都提交了，则不进行判断
        if (submittedCount < totalExpertCount) {
            log.info("[ReviewTaskStatusListener] 专家评分未全部提交，跳过自动判断: 已提交={}, 总数={}",
                    submittedCount, totalExpertCount);
            return;
        }

        // 全部专家都提交后，计算平均分
        BigDecimal totalScore = submittedResults.stream()
                .map(r -> r.getTotalScore() != null ? r.getTotalScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageScore = totalScore.divide(
                BigDecimal.valueOf(submittedResults.size()),
                2,
                RoundingMode.HALF_UP
        );

        log.info("[ReviewTaskStatusListener] 评审分数计算: taskId={}, 专家数={}, 平均分={}",
                reviewTask.getId(), submittedResults.size(), averageScore);

        // 获取流程节点配置的 passScore，判断是否通过
        Integer passScore = getPassScoreFromNodeConfig(event.getTaskDefinitionKey(), event.getProcessInstanceId());
        if (passScore == null) {
            log.info("[ReviewTaskStatusListener] 未配置通过分数，跳过自动判断: taskKey={}", event.getTaskDefinitionKey());
            return;
        }

        log.info("[ReviewTaskStatusListener] 评审分数对比: 平均分={}, 通过分数={}, 结果={}",
                averageScore, passScore, averageScore.compareTo(BigDecimal.valueOf(passScore)) >= 0);

        if (averageScore.compareTo(BigDecimal.valueOf(passScore)) >= 0) {
            // 达到通过分数，流程继续（已经是 APPROVE 状态，无需额外处理）
            log.info("[ReviewTaskStatusListener] 分数达标，流程继续: taskId={}, 平均分={}, 通过分数={}",
                    reviewTask.getId(), averageScore, passScore);
        } else {
            // 分数未达标，触发驳回
            log.info("[ReviewTaskStatusListener] 分数未达标，触发驳回: taskId={}, 平均分={}, 通过分数={}",
                    reviewTask.getId(), averageScore, passScore);
            triggerReject(event, averageScore, passScore);
        }
    }

    /**
     * 从业务Key中解析业务类型
     * <p>
     * 新格式: {processDefinitionKey}_{businessId}
     * 例如: declare_filing_1 -> 1, declare_project_2 -> 2, declare_achievement_3 -> 3
     */
    private Integer getBusinessType(String businessKey) {
        if (businessKey == null) {
            return null;
        }
        if (businessKey.startsWith("declare_filing")) {
            return 1;
        } else if (businessKey.startsWith("declare_project")) {
            return 2;
        } else if (businessKey.startsWith("declare_achievement")) {
            return 3;
        }
        // 兼容旧格式
        if (businessKey.startsWith("filing_")) {
            return 1;
        } else if (businessKey.startsWith("project_")) {
            return 2;
        } else if (businessKey.startsWith("achievement_")) {
            return 3;
        }
        return null;
    }

    /**
     * 处理备案评审
     */
    private void handleFilingReview(BpmTaskStatusEvent event, Long businessId) {
        // 创建或更新评审任务
        createOrUpdateReviewTask(
                businessId,
                1, // 业务类型：1=备案
                event.getProcessInstanceId(),
                event.getTaskDefinitionKey(),
                event.getTaskName(),
                1 // 任务类型：1=备案论证
        );
    }

    /**
     * 处理项目评审
     */
    private void handleProjectReview(BpmTaskStatusEvent event, Long businessId) {
        // 判断任务类型
        Integer taskType = getProjectTaskType(event.getTaskDefinitionKey());

        // 创建或更新评审任务
        createOrUpdateReviewTask(
                businessId,
                2, // 业务类型：2=项目
                event.getProcessInstanceId(),
                event.getTaskDefinitionKey(),
                event.getTaskName(),
                taskType
        );
    }

    /**
     * 处理成果评审
     */
    private void handleAchievementReview(BpmTaskStatusEvent event, Long businessId) {
        // 创建或更新评审任务
        createOrUpdateReviewTask(
                businessId,
                3, // 业务类型：3=成果
                event.getProcessInstanceId(),
                event.getTaskDefinitionKey(),
                event.getTaskName(),
                4 // 任务类型：4=成果审核
        );
    }

    /**
     * 创建或更新评审任务
     */
    private void createOrUpdateReviewTask(Long businessId, Integer businessType,
                                          String processInstanceId, String taskDefinitionKey,
                                          String taskName, Integer taskType) {
        // 检查是否已存在评审任务
        List<ReviewTaskDO> existingTasks = reviewTaskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewTaskDO>()
                        .eq(ReviewTaskDO::getBusinessId, businessId)
                        .eq(ReviewTaskDO::getBusinessType, businessType)
                        .eq(ReviewTaskDO::getTaskDefinitionKey, taskDefinitionKey)
        );

        if (existingTasks.isEmpty()) {
            // 创建新的评审任务
            ReviewTaskDO task = new ReviewTaskDO();
            task.setProcessInstanceId(processInstanceId);
            task.setTaskDefinitionKey(taskDefinitionKey);
            task.setTaskName(taskName);
            task.setTaskType(taskType);
            task.setBusinessType(businessType);
            task.setBusinessId(businessId);
            task.setStatus(0); // 待分配
            task.setStartTime(LocalDateTime.now());
            reviewTaskMapper.insert(task);

            log.info("[ReviewTaskStatusListener] 创建评审任务: taskId={}, businessId={}, businessType={}",
                    task.getId(), businessId, businessType);
        } else {
            // 更新现有任务
            ReviewTaskDO task = existingTasks.get(0);
            task.setStatus(1); // 评审中
            task.setProcessInstanceId(processInstanceId);
            reviewTaskMapper.updateById(task);

            log.info("[ReviewTaskStatusListener] 更新评审任务: taskId={}, status=评审中", task.getId());
        }
    }

    /**
     * 根据任务定义Key判断项目任务类型
     */
    private Integer getProjectTaskType(String taskDefinitionKey) {
        if (taskDefinitionKey == null) {
            return 2; // 默认中期评估
        }
        if (taskDefinitionKey.contains("mid_review") || taskDefinitionKey.contains("midterm")) {
            return 2; // 中期评估
        } else if (taskDefinitionKey.contains("final_review") || taskDefinitionKey.contains("acceptance")) {
            return 3; // 验收评审
        }
        return 2; // 默认中期评估
    }

    /**
     * 根据业务类型获取任务类型
     * taskType: 1=备案论证，2=中期评估，3=验收评审，4=成果审核
     */
    private Integer getTaskTypeFromBusinessType(Integer businessType, String taskDefinitionKey) {
        if (businessType == null) {
            return 2;
        }
        switch (businessType) {
            case 1:
                return 1; // 备案论证
            case 2:
                return getProjectTaskType(taskDefinitionKey); // 中期评估或验收评审
            case 3:
                return 4; // 成果审核
            default:
                return 2;
        }
    }

    /**
     * 从流程节点配置中获取通过分数
     *
     * @param taskDefinitionKey 任务定义Key（节点ID）
     * @param processInstanceId  流程实例ID
     * @return 通过分数，如果未配置则返回 null
     */
    private Integer getPassScoreFromNodeConfig(String taskDefinitionKey, String processInstanceId) {
        try {
            // 获取流程实例信息
            org.flowable.engine.runtime.ProcessInstance processInstance = bpmProcessInstanceService.getProcessInstance(processInstanceId);
            if (processInstance == null) {
                log.warn("[ReviewTaskStatusListener] 未找到流程实例: processInstanceId={}", processInstanceId);
                return null;
            }

            // 获取流程定义信息
            String processDefinitionId = processInstance.getProcessDefinitionId();
            BpmProcessDefinitionInfoDO processDefinitionInfo = bpmProcessDefinitionService.getProcessDefinitionInfo(processDefinitionId);
            if (processDefinitionInfo == null || !StringUtils.hasText(processDefinitionInfo.getSimpleModel())) {
                log.warn("[ReviewTaskStatusListener] 未找到流程定义信息: processDefinitionId={}", processDefinitionId);
                return null;
            }

            // 解析 simpleModel JSON
            JsonNode simpleModelJson = JsonUtils.parseTree(processDefinitionInfo.getSimpleModel());
            if (simpleModelJson == null) {
                return null;
            }

            // 遍历节点查找匹配的节点配置
            JsonNode rootNode = simpleModelJson.get("childNode");
            if (rootNode == null) {
                return null;
            }

            Integer passScore = findPassScoreInNode(rootNode, taskDefinitionKey);
            if (passScore != null) {
                log.info("[ReviewTaskStatusListener] 找到通过分数配置: taskKey={}, passScore={}",
                        taskDefinitionKey, passScore);
            }
            return passScore;

        } catch (Exception e) {
            log.error("[ReviewTaskStatusListener] 获取节点配置失败: taskKey={}, processInstanceId={}",
                    taskDefinitionKey, processInstanceId, e);
            return null;
        }
    }

    /**
     * 递归查找节点配置中的通过分数
     */
    private Integer findPassScoreInNode(JsonNode node, String targetTaskDefinitionKey) {
        if (node == null) {
            return null;
        }

        // 检查当前节点是否匹配
        String nodeId = node.has("id") ? node.get("id").asText() : null;
        if (targetTaskDefinitionKey.equals(nodeId)) {
            // 查找 passScore
            if (node.has("passScore") && !node.get("passScore").isNull()) {
                return node.get("passScore").asInt();
            }
            // 查找 approveMethod 相关的配置
            JsonNode approveMethod = node.get("approveMethod");
            if (approveMethod != null && approveMethod.asInt() == 4) { // 4 = 评审（按总分）
                JsonNode passScoreNode = node.get("passScore");
                if (passScoreNode != null && !passScoreNode.isNull()) {
                    return passScoreNode.asInt();
                }
            }
        }

        // 递归查找子节点
        JsonNode childNode = node.get("childNode");
        if (childNode != null) {
            Integer passScore = findPassScoreInNode(childNode, targetTaskDefinitionKey);
            if (passScore != null) {
                return passScore;
            }
        }

        // 递归查找条件分支节点
        JsonNode conditionNodes = node.get("conditionNodes");
        if (conditionNodes != null && conditionNodes.isArray()) {
            for (JsonNode conditionNode : conditionNodes) {
                Integer passScore = findPassScoreInNode(conditionNode, targetTaskDefinitionKey);
                if (passScore != null) {
                    return passScore;
                }
            }
        }

        return null;
    }

    /**
     * 触发流程驳回
     * 根据流程节点配置的"审批人拒绝时"设置进行驳回
     *
     * @param event        任务事件
     * @param averageScore 平均分数
     * @param passScore    通过分数
     */
    private void triggerReject(BpmTaskStatusEvent event, BigDecimal averageScore, Integer passScore) {
        try {
            // 构建驳回请求
            BpmTaskRejectReqVO rejectReqVO = new BpmTaskRejectReqVO();
            rejectReqVO.setId(event.getTaskId());
            rejectReqVO.setReason(String.format(REJECT_REASON_TEMPLATE, averageScore.toString(), passScore.toString()));

            // 调用 BPM 驳回接口
            // 注意：这里使用 system 用户执行驳回操作
            bpmTaskService.rejectTask(null, rejectReqVO);

            log.info("[ReviewTaskStatusListener] 分数未达标，自动驳回成功: taskId={}, processInstanceId={}, reason={}",
                    event.getTaskId(), event.getProcessInstanceId(), rejectReqVO.getReason());

        } catch (Exception e) {
            log.error("[ReviewTaskStatusListener] 分数未达标，驳回失败: taskId={}, processInstanceId={}",
                    event.getTaskId(), event.getProcessInstanceId(), e);
        }
    }
}
