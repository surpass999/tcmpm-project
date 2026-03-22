package cn.gemrun.base.module.declare.service.review.impl;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.review.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.expert.ExpertDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewIndicatorScoreDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewResultDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewTaskDO;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewResultMapper;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewTaskMapper;
import cn.gemrun.base.module.declare.service.expert.ExpertService;
import cn.gemrun.base.module.declare.service.review.ReviewIndicatorScoreService;
import cn.gemrun.base.module.declare.service.review.ReviewResultService;
import cn.gemrun.base.module.bpm.service.task.BpmTaskService;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.gemrun.base.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.gemrun.base.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.mzt.logapi.starter.annotation.LogRecord;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 评审结果 Service 实现
 *
 * @author Gemini
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewResultServiceImpl implements ReviewResultService {

    private final ReviewResultMapper reviewResultMapper;
    private final ReviewTaskMapper reviewTaskMapper;
    private final ExpertService expertService;
    private final ReviewIndicatorScoreService reviewIndicatorScoreService;

    @Resource
    private BpmTaskService bpmTaskService;

    @Resource
    private TaskService taskService;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private BpmProcessInstanceService bpmProcessInstanceService;

    @Resource
    private BpmProcessDefinitionService bpmProcessDefinitionService;

    @Override
    public Long createReviewResult(ReviewResultSaveReqVO createReqVO) {
        ReviewResultDO reviewResult = BeanUtils.toBean(createReqVO, ReviewResultDO.class);
        reviewResultMapper.insert(reviewResult);
        return reviewResult.getId();
    }

    @Override
    public void updateReviewResult(ReviewResultSaveReqVO updateReqVO) {
        ReviewResultDO existing = reviewResultMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw new RuntimeException("评审结果不存在");
        }

        ReviewResultDO reviewResult = BeanUtils.toBean(updateReqVO, ReviewResultDO.class);
        reviewResultMapper.updateById(reviewResult);

        // 如果已提交，更新任务汇总
        if (reviewResult.getStatus() == 3) {
            updateTaskSummary(updateReqVO.getTaskId());
        }
    }

    @Override
    public void deleteReviewResult(Long id) {
        reviewResultMapper.deleteById(id);
    }

    @Override
    public ReviewResultDO getReviewResult(Long id) {
        return reviewResultMapper.selectById(id);
    }

    @Override
    public ReviewResultRespVO getReviewResultDetail(Long id) {
        ReviewResultDO result = reviewResultMapper.selectById(id);
        if (result == null) {
            return null;
        }

        ReviewResultRespVO respVO = BeanUtils.toBean(result, ReviewResultRespVO.class);

        // 填充专家信息（expertId 存的是系统用户ID，用 getExpertByUserId 查询）
        if (result.getExpertId() != null) {
            ExpertDO expert = expertService.getExpertByUserId(result.getExpertId());
            if (expert != null) {
                respVO.setExpertName(expert.getExpertName());
                respVO.setExpertWorkUnit(expert.getWorkUnit());
                respVO.setExpertPhone(expert.getPhone());
            }
        }

        return respVO;
    }

    @Override
    public PageResult<ReviewResultRespVO> getReviewResultPage(ReviewResultPageReqVO pageReqVO) {
        LambdaQueryWrapper<ReviewResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(pageReqVO.getTaskId() != null, ReviewResultDO::getTaskId, pageReqVO.getTaskId())
                .eq(pageReqVO.getExpertId() != null, ReviewResultDO::getExpertId, pageReqVO.getExpertId())
                .eq(pageReqVO.getBusinessType() != null, ReviewResultDO::getBusinessType, pageReqVO.getBusinessType())
                .eq(pageReqVO.getBusinessId() != null, ReviewResultDO::getBusinessId, pageReqVO.getBusinessId())
                .eq(pageReqVO.getStatus() != null, ReviewResultDO::getStatus, pageReqVO.getStatus())
                .eq(pageReqVO.getIsAvoid() != null, ReviewResultDO::getIsAvoid, pageReqVO.getIsAvoid())
                .eq(StringUtils.hasLength(pageReqVO.getProcessInstanceId()),
                        ReviewResultDO::getProcessInstanceId, pageReqVO.getProcessInstanceId());

        // 日期范围查询 - 添加防御性检查避免 NPE
        LocalDateTime[] createTimeRange = pageReqVO.getCreateTime();
        if (createTimeRange != null && createTimeRange.length >= 1 && createTimeRange[0] != null) {
            wrapper.ge(ReviewResultDO::getCreateTime, createTimeRange[0]);
        }
        if (createTimeRange != null && createTimeRange.length >= 2 && createTimeRange[1] != null) {
            wrapper.le(ReviewResultDO::getCreateTime, createTimeRange[1]);
        }

        wrapper.orderByDesc(ReviewResultDO::getId);

        PageResult<ReviewResultDO> pageResult = reviewResultMapper.selectPage(pageReqVO, wrapper);

        List<ReviewResultRespVO> voList = new ArrayList<>();
        for (ReviewResultDO result : pageResult.getList()) {
            ReviewResultRespVO respVO = BeanUtils.toBean(result, ReviewResultRespVO.class);

            if (result.getExpertId() != null) {
                ExpertDO expert = expertService.getExpertByUserId(result.getExpertId());
                if (expert != null) {
                    respVO.setExpertName(expert.getExpertName());
                    respVO.setExpertWorkUnit(expert.getWorkUnit());
                    respVO.setExpertPhone(expert.getPhone());
                }
            }

            // 查询任务信息，填充任务类型和业务名称
            if (result.getTaskId() != null) {
                ReviewTaskDO task = reviewTaskMapper.selectById(result.getTaskId());
                if (task != null) {
                    respVO.setTaskType(task.getTaskType());
                    // 使用任务名称作为业务名称
                    respVO.setBusinessName(task.getTaskName());
                }
            }

            voList.add(respVO);
        }

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public List<ReviewResultDO> getReviewResultByTaskId(Long taskId) {
        LambdaQueryWrapper<ReviewResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewResultDO::getTaskId, taskId)
                .orderByDesc(ReviewResultDO::getId);
        return reviewResultMapper.selectList(wrapper);
    }

    @Override
    public List<ReviewResultDO> getReviewResultByExpertId(Long expertId) {
        LambdaQueryWrapper<ReviewResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewResultDO::getExpertId, expertId)
                .orderByDesc(ReviewResultDO::getId);
        return reviewResultMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = REVIEW_TYPE, subType = REVIEW_RESULT_RECEIVE_SUB_TYPE,
            bizNo = "{{#id}}", success = REVIEW_RESULT_RECEIVE_SUCCESS)
    public void receiveTask(Long id) {
        ReviewResultDO result = reviewResultMapper.selectById(id);
        if (result == null) {
            throw new RuntimeException("评审结果不存在");
        }

        if (result.getStatus() != 0) {
            throw new RuntimeException("任务状态不正确，无法接收");
        }

        result.setStatus(1); // 已接收
        result.setReceiveTime(LocalDateTime.now());
        reviewResultMapper.updateById(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = REVIEW_TYPE, subType = REVIEW_RESULT_START_SUB_TYPE,
            bizNo = "{{#id}}", success = REVIEW_RESULT_START_SUCCESS)
    public void startReview(Long id) {
        ReviewResultDO result = reviewResultMapper.selectById(id);
        if (result == null) {
            throw new RuntimeException("评审结果不存在");
        }

        if (result.getStatus() != 1) {
            throw new RuntimeException("任务状态不正确，无法开始评审");
        }

        result.setStatus(2); // 评审中
        reviewResultMapper.updateById(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = REVIEW_TYPE, subType = REVIEW_RESULT_SUBMIT_SUB_TYPE,
            bizNo = "{{#id}}", success = REVIEW_RESULT_SUBMIT_SUCCESS)
    public void submitReview(Long id, ReviewResultSaveReqVO saveReqVO) {
        ReviewResultDO result = reviewResultMapper.selectById(id);
        if (result == null) {
            throw new RuntimeException("评审结果不存在");
        }

        if (result.getStatus() != 2) {
            throw new RuntimeException("任务状态不正确，无法提交");
        }

        // 更新评审数据
        result.setScoreData(saveReqVO.getScoreData());
        result.setTotalScore(saveReqVO.getTotalScore());
        result.setConclusion(saveReqVO.getConclusion());
        result.setOpinion(saveReqVO.getOpinion());
        result.setStatus(3); // 已提交
        result.setSubmitTime(LocalDateTime.now());
        reviewResultMapper.updateById(result);

        // 更新专家评审统计：评审次数、平均分、上次评审时间
        if (result.getExpertId() != null && saveReqVO.getTotalScore() != null) {
            expertService.updateExpertReviewStats(result.getExpertId(), saveReqVO.getTotalScore());
        }

        // 保存结构化的指标评分数据
        if (!CollectionUtils.isEmpty(saveReqVO.getIndicatorScores())) {
            List<ReviewIndicatorScoreDO> scores = new ArrayList<>();
            for (ReviewResultSaveReqVO.IndicatorScore indicatorScore : saveReqVO.getIndicatorScores()) {
                ReviewIndicatorScoreDO scoreDO = new ReviewIndicatorScoreDO();
                scoreDO.setReviewResultId(id);
                scoreDO.setIndicatorId(indicatorScore.getIndicatorId());
                scoreDO.setIndicatorCode(indicatorScore.getIndicatorCode());
                scoreDO.setExpertId(result.getExpertId());
                scoreDO.setMaxScore(indicatorScore.getMaxScore());
                scoreDO.setScore(indicatorScore.getScore());
                scoreDO.setScoreLevel(indicatorScore.getScoreLevel());
                scoreDO.setScoreRatio(indicatorScore.getScoreRatio());
                scoreDO.setComment(indicatorScore.getComment());
                scoreDO.setOpinion(indicatorScore.getOpinion());
                scores.add(scoreDO);
            }
            reviewIndicatorScoreService.saveScores(id, scores);
        }

        // 存入 Flowable 变量
        saveReviewScoreToVariable(result);

        // 检查评分是否达标（在 BPM 任务完成之前执行，因为完成后流程实例可能已结束）
        boolean shouldReject = checkAndHandleScoreCompletion(result);

        // 如果评分未达标且所有专家都已提交，则驳回流程，不继续执行 approveBpmTask
        if (shouldReject) {
            log.info("[submitReview] 评分未达标，流程已驳回，跳过 BPM 任务完成");
            // 更新任务汇总
            updateTaskSummary(result.getTaskId());
            return;
        }

        // 调用 BPM 完成流程任务
        approveBpmTask(result, saveReqVO);

        // 更新任务汇总
        updateTaskSummary(result.getTaskId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = REVIEW_TYPE, subType = REVIEW_RESULT_APPLY_AVOID_SUB_TYPE,
            bizNo = "{{#id}}", success = REVIEW_RESULT_APPLY_AVOID_SUCCESS)
    public void applyAvoid(Long id, String reason) {
        ReviewResultDO result = reviewResultMapper.selectById(id);
        if (result == null) {
            throw new RuntimeException("评审结果不存在");
        }

        result.setIsAvoid(true);
        result.setAvoidReason(reason);
        reviewResultMapper.updateById(result);

        // TODO: 通知管理员有专家申请回避，需要重新分配专家
    }

    @Override
    public ReviewResultDO getReviewResultByFlowableTaskId(String flowableTaskId) {
        LambdaQueryWrapper<ReviewResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewResultDO::getFlowableTaskId, flowableTaskId)
                .last("LIMIT 1");
        return reviewResultMapper.selectOne(wrapper);
    }

    @Override
    public BigDecimal calculateTaskTotalScore(Long taskId) {
        List<ReviewResultDO> results = getReviewResultByTaskId(taskId);

        // 只计算已提交的评分
        List<BigDecimal> scores = results.stream()
                .filter(r -> r.getStatus() == 3 && r.getTotalScore() != null)
                .map(ReviewResultDO::getTotalScore)
                .collect(Collectors.toList());

        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算平均分
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public Long getExpertPendingCount(Long expertId) {
        LambdaQueryWrapper<ReviewResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewResultDO::getExpertId, expertId)
                .in(ReviewResultDO::getStatus, 0, 1, 2); // 待评审、已接收、评审中
        return reviewResultMapper.selectCount(wrapper);
    }

    /**
     * 更新任务汇总评分
     */
    private void updateTaskSummary(Long taskId) {
        List<ReviewResultDO> results = getReviewResultByTaskId(taskId);

        // 检查是否全部提交
        boolean allSubmitted = results.stream().allMatch(r -> r.getStatus() == 3);

        if (allSubmitted && !results.isEmpty()) {
            // 计算平均分
            BigDecimal avgScore = calculateTaskTotalScore(taskId);

            // 确定评审结论
            String conclusion = calculateConclusion(avgScore);

            // 更新任务
            ReviewTaskDO task = reviewTaskMapper.selectById(taskId);
            if (task != null) {
                task.setTotalScore(avgScore);
                task.setReviewConclusion(conclusion);
                task.setStatus(2); // 已完成
                reviewTaskMapper.updateById(task);
            }
        }
    }

    /**
     * 根据评分计算评审结论
     */
    private String calculateConclusion(BigDecimal score) {
        if (score == null) {
            return "未通过";
        }
        if (score.compareTo(new BigDecimal("90")) >= 0) {
            return "通过";
        } else if (score.compareTo(new BigDecimal("60")) >= 0) {
            return "需整改";
        } else {
            return "未通过";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = REVIEW_TYPE, subType = REVIEW_RESULT_REJECT_SUB_TYPE,
            bizNo = "{{#id}}", success = REVIEW_RESULT_REJECT_SUCCESS)
    public void rejectTask(Long id, String reason) {
        ReviewResultDO result = reviewResultMapper.selectById(id);
        if (result == null) {
            throw new RuntimeException("评审任务不存在");
        }

        // 校验状态（只能拒绝待接收/已接收的任务）
        if (result.getStatus() != 0 && result.getStatus() != 1) {
            throw new RuntimeException("当前状态不允许拒绝");
        }

        // 1. 从 BPM 流程中移除该专家任务
        if (StringUtils.hasText(result.getProcessInstanceId())) {
            removeExpertFromBpmTask(result.getProcessInstanceId(), result.getExpertId());
        }

        // 2. 更新评审结果状态为已拒绝
        result.setStatus(5); // 5=已拒绝
        result.setOpinion(reason);
        result.setReceiveTime(LocalDateTime.now());
        reviewResultMapper.updateById(result);

        log.info("[rejectTask] 专家拒绝任务: id={}, expertId={}, reason={}",
                id, result.getExpertId(), reason);
    }

    /**
     * 从 BPM 流程中移除专家任务
     * 将任务设置为 unclaim（释放），从候选人/待办中移除
     */
    private void removeExpertFromBpmTask(String processInstanceId, Long expertId) {
        try {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskAssignee(expertId.toString())
                    .list();

            for (Task task : tasks) {
                // 释放任务（设置为null，即从待办中移除）
                taskService.unclaim(task.getId());
                log.info("[removeExpertFromBpmTask] 释放任务: taskId={}, expertId={}, taskName={}",
                        task.getId(), expertId, task.getName());
            }

            // 如果使用了候选人模式，也需要从候选人列表中移除
            List<Task> candidateTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskCandidateUser(expertId.toString())
                    .list();

            for (Task task : candidateTasks) {
                taskService.deleteCandidateUser(task.getId(), expertId.toString());
                log.info("[removeExpertFromBpmTask] 移除候选人: taskId={}, expertId={}, taskName={}",
                        task.getId(), expertId, task.getName());
            }
        } catch (Exception e) {
            log.error("[removeExpertFromBpmTask] 移除专家任务失败: processInstanceId={}, expertId={}",
                    processInstanceId, expertId, e);
            // 不抛出异常，避免影响本地状态更新
        }
    }

    @Override
    public List<ReviewSummaryRespVO> getReviewSummaryByBusiness(Integer businessType, Long businessId) {
        // 获取该业务下的所有评审结果
        LambdaQueryWrapper<ReviewResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewResultDO::getBusinessType, businessType)
                .eq(ReviewResultDO::getBusinessId, businessId)
                .orderByDesc(ReviewResultDO::getCreateTime);

        List<ReviewResultDO> results = reviewResultMapper.selectList(wrapper);

        // 按流程阶段分组汇总
        // 这里简化处理，实际应该根据 taskDefinitionKey 或 taskType 分组
        List<ReviewSummaryRespVO> summaryList = new ArrayList<>();

        if (!results.isEmpty()) {
            ReviewSummaryRespVO summary = new ReviewSummaryRespVO();
            summary.setProcessType(1); // 简化：默认一个流程阶段
            summary.setProcessTypeName("专家评审");

            // 统计专家数量
            summary.setExpertCount(results.size());

            // 计算平均分（只计算已提交的）
            List<BigDecimal> submittedScores = results.stream()
                    .filter(r -> r.getStatus() == 3 && r.getTotalScore() != null)
                    .map(ReviewResultDO::getTotalScore)
                    .collect(Collectors.toList());

            if (!submittedScores.isEmpty()) {
                BigDecimal avgScore = submittedScores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(submittedScores.size()), 2, BigDecimal.ROUND_HALF_UP);
                summary.setAverageScore(avgScore);

                // 判断通过状态
                if (avgScore.compareTo(new BigDecimal("60")) >= 0) {
                    summary.setPassStatus("通过");
                } else {
                    summary.setPassStatus("未通过");
                }
            }

            // 获取最近提交时间
            results.stream()
                    .filter(r -> r.getSubmitTime() != null)
                    .max(Comparator.comparing(ReviewResultDO::getSubmitTime))
                    .ifPresent(r -> summary.setReviewTime(r.getSubmitTime().toString()));

            // 填充评审结果列表（results 字段）
            List<ReviewResultRespVO> resultVOList = new ArrayList<>();
            for (ReviewResultDO resultDO : results) {
                ReviewResultRespVO resultVO = BeanUtils.toBean(resultDO, ReviewResultRespVO.class);

                // 填充专家信息
                if (resultDO.getExpertId() != null) {
                    ExpertDO expert = expertService.getExpertByUserId(resultDO.getExpertId());
                    if (expert != null) {
                        resultVO.setExpertName(expert.getExpertName());
                        resultVO.setExpertWorkUnit(expert.getWorkUnit());
                        resultVO.setExpertPhone(expert.getPhone());
                    }
                }

                // 填充结构化指标评分
                List<ReviewIndicatorScoreDO> indicatorScores = reviewIndicatorScoreService.getScoresByReviewResultId(resultDO.getId());
                if (!indicatorScores.isEmpty()) {
                    List<ReviewResultRespVO.IndicatorScore> scoreVOList = indicatorScores.stream()
                            .map(score -> {
                                ReviewResultRespVO.IndicatorScore scoreVO = new ReviewResultRespVO.IndicatorScore();
                                scoreVO.setIndicatorId(score.getIndicatorId());
                                scoreVO.setIndicatorCode(score.getIndicatorCode());
                                scoreVO.setMaxScore(score.getMaxScore());
                                scoreVO.setScore(score.getScore());
                                scoreVO.setScoreLevel(score.getScoreLevel());
                                scoreVO.setScoreRatio(score.getScoreRatio());
                                scoreVO.setComment(score.getComment());
                                scoreVO.setOpinion(score.getOpinion());
                                return scoreVO;
                            })
                            .collect(Collectors.toList());
                    resultVO.setIndicatorScores(scoreVOList);
                }

                resultVOList.add(resultVO);
            }
            summary.setResults(resultVOList);

            summaryList.add(summary);
        }

        return summaryList;
    }

    /**
     * 存入 Flowable 变量
     */
    @SuppressWarnings("unchecked")
    private void saveReviewScoreToVariable(ReviewResultDO result) {
        String processInstanceId = result.getProcessInstanceId();
        if (processInstanceId == null) {
            log.warn("[saveReviewScoreToVariable] processInstanceId 为空，跳过存入变量");
            return;
        }

        // 获取已有评分列表
        List<Map<String, Object>> scores = (List<Map<String, Object>>)
            runtimeService.getVariable(processInstanceId, "reviewScores");

        if (scores == null) {
            scores = new ArrayList<>();
        }

        // 添加当前专家的评分
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("expertId", result.getExpertId());
        scoreData.put("expertName", getExpertName(result.getExpertId()));
        scoreData.put("score", result.getTotalScore());
        scoreData.put("opinion", result.getOpinion());
        scoreData.put("submitTime", LocalDateTime.now());
        scores.add(scoreData);

        // 存入变量
        runtimeService.setVariable(processInstanceId, "reviewScores", scores);

        log.info("[saveReviewScoreToVariable] 存入评分: processInstanceId={}, expertId={}, score={}",
            processInstanceId, result.getExpertId(), result.getTotalScore());
    }

    /**
     * 获取专家名称
     */
    private String getExpertName(Long expertId) {
        if (expertId == null) {
            return null;
        }
        ExpertDO expert = expertService.getExpert(expertId);
        return expert != null ? expert.getExpertName() : null;
    }

    /**
     * 调用 BPM 完成流程任务
     */
    private void approveBpmTask(ReviewResultDO result, ReviewResultSaveReqVO saveReqVO) {
        try {
            BpmTaskApproveReqVO approveReqVO = new BpmTaskApproveReqVO();
            approveReqVO.setId(result.getFlowableTaskId());
            approveReqVO.setReason("专家提交评审，分数：" + saveReqVO.getTotalScore());

            // 传递按钮ID（用于获取bizStatus）
            approveReqVO.setButtonId(saveReqVO.getButtonId());

            // 传递评分变量（可选，用于流程表达式）
            Map<String, Object> variables = new HashMap<>();
            variables.put("reviewScore", saveReqVO.getTotalScore());
            approveReqVO.setVariables(variables);

            bpmTaskService.approveTask(result.getExpertId(), approveReqVO);

            log.info("[approveBpmTask] BPM任务完成: flowableTaskId={}, buttonId={}",
                result.getFlowableTaskId(), saveReqVO.getButtonId());
        } catch (Exception e) {
            log.error("[approveBpmTask] BPM任务完成失败: flowableTaskId={}", result.getFlowableTaskId(), e);
            throw new RuntimeException("BPM流程提交失败", e);
        }
    }

    /**
     * 检查评分是否达标，如果所有专家都已提交且评分未达标则驳回
     * <p>
     * 此方法在 BPM 任务完成之前调用，用于在流程结束前判断是否需要驳回
     *
     * @return true 如果需要驳回流程，false 正常继续
     */
    @SuppressWarnings("unchecked")
    private boolean checkAndHandleScoreCompletion(ReviewResultDO result) {
        String processInstanceId = result.getProcessInstanceId();
        if (processInstanceId == null) {
            return false;
        }

        // 获取评分列表
        List<Map<String, Object>> reviewScores = (List<Map<String, Object>>)
            runtimeService.getVariable(processInstanceId, "reviewScores");

        if (reviewScores == null || reviewScores.isEmpty()) {
            log.warn("[checkAndHandleScoreCompletion] 评分列表为空，跳过检查: processInstanceId={}", processInstanceId);
            return false;
        }

        // 获取通过分数（从流程定义扩展属性中获取）
        Integer passScore = getPassScoreFromProcess(processInstanceId);
        if (passScore == null) {
            log.warn("[checkAndHandleScoreCompletion] 通过分数未配置，跳过检查: processInstanceId={}", processInstanceId);
            return false;
        }

        // 计算平均分
        BigDecimal averageScore = calculateAverageScore(reviewScores);
        boolean isPass = averageScore.compareTo(BigDecimal.valueOf(passScore)) >= 0;

        log.info("[checkAndHandleScoreCompletion] 评分计算完成: processInstanceId={}, 平均分={}, 通过分数={}, 是否通过={}",
                processInstanceId, averageScore, passScore, isPass);

        // 如果评分未达标，需要判断是否所有专家都已提交
        if (!isPass) {
            // 使用 Flowable 的多实例变量判断：已完成数量 >= 总数量
            Integer nrOfInstances = (Integer) runtimeService.getVariable(processInstanceId, "nrOfInstances");
            Integer nrOfCompletedInstances = (Integer) runtimeService.getVariable(processInstanceId, "nrOfCompletedInstances");

            log.info("[checkAndHandleScoreCompletion] 多实例状态: nrOfInstances={}, nrOfCompletedInstances={}",
                    nrOfInstances, nrOfCompletedInstances);

            // 如果已完成数量 >= 总数量，说明所有专家都已提交
            if (nrOfInstances != null && nrOfCompletedInstances != null
                    && nrOfCompletedInstances >= nrOfInstances) {
                log.info("[checkAndHandleScoreCompletion] 所有专家已提交，评分未达标，执行驳回: processInstanceId={}", processInstanceId);
                rejectBpmTask(processInstanceId, "评审评分未达到通过分数，当前平均分：" + averageScore);
                return true;
            }
        }
        return false;
    }

    /**
     * 从流程定义中获取通过分数
     * <p>
     * 通过解析流程定义的 simpleModel JSON 获取节点配置的 passScore
     */
    private Integer getPassScoreFromProcess(String processInstanceId) {
        try {
            // 获取流程实例信息
            org.flowable.engine.runtime.ProcessInstance processInstance = bpmProcessInstanceService.getProcessInstance(processInstanceId);
            if (processInstance == null) {
                log.warn("[getPassScoreFromProcess] 未找到流程实例: processInstanceId={}", processInstanceId);
                return null;
            }

            // 获取流程定义信息
            String processDefinitionId = processInstance.getProcessDefinitionId();
            BpmProcessDefinitionInfoDO processDefinitionInfo = bpmProcessDefinitionService.getProcessDefinitionInfo(processDefinitionId);
            if (processDefinitionInfo == null || !StringUtils.hasText(processDefinitionInfo.getSimpleModel())) {
                log.warn("[getPassScoreFromProcess] 未找到流程定义信息: processDefinitionId={}", processDefinitionId);
                return null;
            }

            // 获取当前节点的任务定义Key
            List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
            if (tasks == null || tasks.isEmpty()) {
                // 没有待办任务时，使用第一个评审任务的节点Key
                log.info("[getPassScoreFromProcess] 无待办任务，跳过passScore获取");
                return null;
            }
            String taskDefinitionKey = tasks.get(0).getTaskDefinitionKey();

            // 解析 simpleModel JSON，查找 passScore
            JsonNode simpleModelJson = JsonUtils.parseTree(processDefinitionInfo.getSimpleModel());
            if (simpleModelJson == null) {
                return null;
            }

            JsonNode rootNode = simpleModelJson.get("childNode");
            if (rootNode == null) {
                return null;
            }

            Integer passScore = findPassScoreInNode(rootNode, taskDefinitionKey);
            if (passScore != null) {
                log.info("[getPassScoreFromProcess] 找到通过分数: processInstanceId={}, taskKey={}, passScore={}",
                        processInstanceId, taskDefinitionKey, passScore);
            }
            return passScore;

        } catch (Exception e) {
            log.warn("[getPassScoreFromProcess] 获取通过分数失败: processInstanceId={}", processInstanceId, e);
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
     * 计算平均分
     */
    private BigDecimal calculateAverageScore(List<Map<String, Object>> scores) {
        if (scores == null || scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = scores.stream()
            .filter(s -> s.get("score") != null)
            .map(s -> new BigDecimal(s.get("score").toString()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(scores.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 驳回 BPM 任务
     */
    private void rejectBpmTask(String processInstanceId, String reason) {
        try {
            // 获取当前流程的任务
            List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

            if (tasks != null && !tasks.isEmpty()) {
                Task task = tasks.get(0);
                BpmTaskRejectReqVO rejectReqVO = new BpmTaskRejectReqVO();
                rejectReqVO.setId(task.getId());
                rejectReqVO.setReason(reason);
                bpmTaskService.rejectTask(1L, rejectReqVO);
                log.info("[rejectBpmTask] 驳回成功: taskId={}, reason={}", task.getId(), reason);
            }
        } catch (Exception e) {
            log.error("[rejectBpmTask] 驳回失败: processInstanceId={}", processInstanceId, e);
            // 注意：这里不抛出异常，避免影响主流程
        }
    }

}
