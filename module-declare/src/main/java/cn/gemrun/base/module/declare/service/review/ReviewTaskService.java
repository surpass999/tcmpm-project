package cn.gemrun.base.module.declare.service.review;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.review.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewTaskDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 评审任务 Service 接口
 *
 * @author Gemini
 */
public interface ReviewTaskService {

    /**
     * 创建评审任务
     *
     * @param createReqVO 创建信息
     * @return 任务编号
     */
    Long createReviewTask(@Valid ReviewTaskSaveReqVO createReqVO);

    /**
     * 更新评审任务
     *
     * @param updateReqVO 更新信息
     */
    void updateReviewTask(@Valid ReviewTaskSaveReqVO updateReqVO);

    /**
     * 删除评审任务
     *
     * @param id 任务编号
     */
    void deleteReviewTask(Long id);

    /**
     * 获取评审任务
     *
     * @param id 任务编号
     * @return 评审任务
     */
    ReviewTaskDO getReviewTask(Long id);

    /**
     * 获取评审任务详情（含扩展信息）
     *
     * @param id 任务编号
     * @return 评审任务详情
     */
    ReviewTaskRespVO getReviewTaskDetail(Long id);

    /**
     * 获取评审任务分页
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ReviewTaskRespVO> getReviewTaskPage(ReviewTaskPageReqVO pageReqVO);

    /**
     * 根据业务ID获取评审任务
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 评审任务列表
     */
    List<ReviewTaskDO> getReviewTaskByBusiness(Integer businessType, Long businessId);

    /**
     * 分配评审专家
     *
     * @param taskId     任务ID
     * @param expertIds  专家ID列表（逗号分隔）
     */
    void assignExperts(Long taskId, String expertIds);

    /**
     * 更新评审任务状态
     *
     * @param id     任务ID
     * @param status 状态
     */
    void updateReviewTaskStatus(Long id, Integer status);

    /**
     * 更新评审任务汇总结果
     *
     * @param id              任务ID
     * @param totalScore      总分
     * @param reviewConclusion 评审结论
     */
    void updateReviewTaskResult(Long id, java.math.BigDecimal totalScore, String reviewConclusion);

}
