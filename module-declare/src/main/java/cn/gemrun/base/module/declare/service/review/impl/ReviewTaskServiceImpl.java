package cn.gemrun.base.module.declare.service.review.impl;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.review.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.expert.ExpertDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewTaskDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewResultDO;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewTaskMapper;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewResultMapper;
import cn.gemrun.base.module.declare.service.expert.ExpertService;
import cn.gemrun.base.module.declare.service.review.ReviewTaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.mzt.logapi.starter.annotation.LogRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 评审任务 Service 实现
 *
 * @author Gemini
 */
@Service
@RequiredArgsConstructor
public class ReviewTaskServiceImpl implements ReviewTaskService {

    private final ReviewTaskMapper reviewTaskMapper;
    private final ReviewResultMapper reviewResultMapper;
    private final ExpertService expertService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = REVIEW_TYPE, subType = REVIEW_TASK_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}", success = REVIEW_TASK_CREATE_SUCCESS)
    public Long createReviewTask(ReviewTaskSaveReqVO createReqVO) {
        // 1. 创建评审任务
        ReviewTaskDO reviewTask = BeanUtils.toBean(createReqVO, ReviewTaskDO.class);
        reviewTask.setStatus(0); // 待分配
        reviewTaskMapper.insert(reviewTask);

        // 2. 如果已指定专家，创建评审结果记录
        if (StringUtils.hasText(createReqVO.getExpertIds())) {
            createReviewResults(reviewTask.getId(), createReqVO);
        }

        return reviewTask.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = REVIEW_TYPE, subType = REVIEW_TASK_UPDATE_SUB_TYPE,
            bizNo = "{{#updateReqVO.id}}", success = REVIEW_TASK_UPDATE_SUCCESS)
    public void updateReviewTask(ReviewTaskSaveReqVO updateReqVO) {
        ReviewTaskDO existingTask = reviewTaskMapper.selectById(updateReqVO.getId());
        if (existingTask == null) {
            throw new RuntimeException("评审任务不存在");
        }

        // 更新评审任务
        ReviewTaskDO reviewTask = BeanUtils.toBean(updateReqVO, ReviewTaskDO.class);
        reviewTaskMapper.updateById(reviewTask);

        // 如果专家有变化，更新评审结果
        if (StringUtils.hasText(updateReqVO.getExpertIds())
                && !updateReqVO.getExpertIds().equals(existingTask.getExpertIds())) {
            // 删除旧的评审结果
            LambdaQueryWrapper<ReviewResultDO> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(ReviewResultDO::getTaskId, updateReqVO.getId());
            reviewResultMapper.delete(deleteWrapper);

            // 创建新的评审结果
            createReviewResults(updateReqVO.getId(), updateReqVO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = REVIEW_TYPE, subType = REVIEW_TASK_DELETE_SUB_TYPE,
            bizNo = "{{#id}}", success = REVIEW_TASK_DELETE_SUCCESS)
    public void deleteReviewTask(Long id) {
        // 删除评审任务
        reviewTaskMapper.deleteById(id);

        // 删除关联的评审结果
        LambdaQueryWrapper<ReviewResultDO> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ReviewResultDO::getTaskId, id);
        reviewResultMapper.delete(deleteWrapper);
    }

    @Override
    public ReviewTaskDO getReviewTask(Long id) {
        return reviewTaskMapper.selectById(id);
    }

    @Override
    public ReviewTaskRespVO getReviewTaskDetail(Long id) {
        ReviewTaskDO reviewTask = reviewTaskMapper.selectById(id);
        if (reviewTask == null) {
            return null;
        }

        ReviewTaskRespVO respVO = BeanUtils.toBean(reviewTask, ReviewTaskRespVO.class);

        // 填充专家信息
        if (StringUtils.hasText(reviewTask.getExpertIds())) {
            List<Long> expertIds = Arrays.stream(reviewTask.getExpertIds().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            List<ExpertDO> experts = expertService.getExpertListByIds(expertIds);
            respVO.setExperts(experts.stream().map(e -> {
                ReviewTaskRespVO.ExpertSimpleVO expertVO = new ReviewTaskRespVO.ExpertSimpleVO();
                expertVO.setId(e.getId());
                expertVO.setExpertName(e.getExpertName());
                expertVO.setWorkUnit(e.getWorkUnit());
                return expertVO;
            }).collect(Collectors.toList()));
        }

        // 填充评审结果
        LambdaQueryWrapper<ReviewResultDO> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.eq(ReviewResultDO::getTaskId, id);
        List<ReviewResultDO> results = reviewResultMapper.selectList(resultWrapper);
        respVO.setResults(results.stream().map(r -> {
            ReviewResultRespVO resultVO = BeanUtils.toBean(r, ReviewResultRespVO.class);
            ExpertDO expert = expertService.getExpert(r.getExpertId());
            if (expert != null) {
                resultVO.setExpertName(expert.getExpertName());
                resultVO.setExpertWorkUnit(expert.getWorkUnit());
                resultVO.setExpertPhone(expert.getPhone());
            }
            return resultVO;
        }).collect(Collectors.toList()));

        return respVO;
    }

    @Override
    public PageResult<ReviewTaskRespVO> getReviewTaskPage(ReviewTaskPageReqVO pageReqVO) {
        // 先查询主表分页
        LambdaQueryWrapper<ReviewTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(pageReqVO.getTaskType() != null, ReviewTaskDO::getTaskType, pageReqVO.getTaskType())
                .eq(pageReqVO.getBusinessType() != null, ReviewTaskDO::getBusinessType, pageReqVO.getBusinessType())
                .eq(pageReqVO.getBusinessId() != null, ReviewTaskDO::getBusinessId, pageReqVO.getBusinessId())
                .like(StringUtils.hasText(pageReqVO.getTaskName()), ReviewTaskDO::getTaskName, pageReqVO.getTaskName())
                .eq(pageReqVO.getStatus() != null, ReviewTaskDO::getStatus, pageReqVO.getStatus())
                .ge(pageReqVO.getCreateTime() != null && pageReqVO.getCreateTime().length > 0,
                        ReviewTaskDO::getCreateTime, pageReqVO.getCreateTime()[0])
                .le(pageReqVO.getCreateTime() != null && pageReqVO.getCreateTime().length > 1,
                        ReviewTaskDO::getCreateTime, pageReqVO.getCreateTime()[1])
                .orderByDesc(ReviewTaskDO::getId);

        PageResult<ReviewTaskDO> pageResult = reviewTaskMapper.selectPage(pageReqVO, wrapper);

        // 转换为响应VO
        List<ReviewTaskRespVO> voList = new ArrayList<>();
        for (ReviewTaskDO task : pageResult.getList()) {
            ReviewTaskRespVO respVO = BeanUtils.toBean(task, ReviewTaskRespVO.class);

            // 填充专家信息
            if (StringUtils.hasText(task.getExpertIds())) {
                List<Long> expertIds = Arrays.stream(task.getExpertIds().split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                List<ExpertDO> experts = expertService.getExpertListByIds(expertIds);
                respVO.setExperts(experts.stream().map(e -> {
                    ReviewTaskRespVO.ExpertSimpleVO expertVO = new ReviewTaskRespVO.ExpertSimpleVO();
                    expertVO.setId(e.getId());
                    expertVO.setExpertName(e.getExpertName());
                    expertVO.setWorkUnit(e.getWorkUnit());
                    return expertVO;
                }).collect(Collectors.toList()));
            }

            voList.add(respVO);
        }

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public List<ReviewTaskDO> getReviewTaskByBusiness(Integer businessType, Long businessId) {
        LambdaQueryWrapper<ReviewTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewTaskDO::getBusinessType, businessType)
                .eq(ReviewTaskDO::getBusinessId, businessId)
                .orderByDesc(ReviewTaskDO::getId);
        return reviewTaskMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = REVIEW_TYPE, subType = REVIEW_TASK_ASSIGN_SUB_TYPE,
            bizNo = "{{#taskId}}", success = REVIEW_TASK_ASSIGN_SUCCESS)
    public void assignExperts(Long taskId, String expertIds) {
        ReviewTaskDO task = reviewTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("评审任务不存在");
        }

        // 删除旧的评审结果
        LambdaQueryWrapper<ReviewResultDO> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ReviewResultDO::getTaskId, taskId);
        reviewResultMapper.delete(deleteWrapper);

        // 更新任务的专家ID
        task.setExpertIds(expertIds);
        task.setStatus(1); // 评审中
        reviewTaskMapper.updateById(task);

        // 创建新的评审结果记录
        ReviewTaskSaveReqVO reqVO = new ReviewTaskSaveReqVO();
        reqVO.setExpertIds(expertIds);
        createReviewResults(taskId, reqVO);
    }

    @Override
    public void updateReviewTaskStatus(Long id, Integer status) {
        ReviewTaskDO task = reviewTaskMapper.selectById(id);
        if (task != null) {
            task.setStatus(status);
            reviewTaskMapper.updateById(task);
        }
    }

    @Override
    public void updateReviewTaskResult(Long id, BigDecimal totalScore, String reviewConclusion) {
        ReviewTaskDO task = reviewTaskMapper.selectById(id);
        if (task != null) {
            task.setTotalScore(totalScore);
            task.setReviewConclusion(reviewConclusion);
            task.setStatus(2); // 已完成
            reviewTaskMapper.updateById(task);
        }
    }

    /**
     * 创建评审结果记录
     */
    private void createReviewResults(Long taskId, ReviewTaskSaveReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getExpertIds())) {
            return;
        }

        List<Long> expertIdList = Arrays.stream(reqVO.getExpertIds().split(","))
                .map(String::trim)
                .filter(s -> StringUtils.hasText(s))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        for (Long expertId : expertIdList) {
            ReviewResultDO result = new ReviewResultDO();
            result.setTaskId(taskId);
            result.setExpertId(expertId);
            result.setProcessInstanceId(reqVO.getProcessInstanceId());
            result.setBusinessType(reqVO.getBusinessType());
            result.setBusinessId(reqVO.getBusinessId());
            result.setStatus(0); // 待评审
            result.setIsConflict(false);
            result.setIsAvoid(false);
            reviewResultMapper.insert(result);
        }
    }

}
