package cn.gemrun.base.module.declare.service.review;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.review.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewResultDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 评审结果 Service 接口
 *
 * @author Gemini
 */
public interface ReviewResultService {

    /**
     * 创建评审结果（自动创建）
     *
     * @param createReqVO 创建信息
     * @return 结果编号
     */
    Long createReviewResult(@Valid ReviewResultSaveReqVO createReqVO);

    /**
     * 更新评审结果
     *
     * @param updateReqVO 更新信息
     */
    void updateReviewResult(@Valid ReviewResultSaveReqVO updateReqVO);

    /**
     * 删除评审结果
     *
     * @param id 结果编号
     */
    void deleteReviewResult(Long id);

    /**
     * 获取评审结果
     *
     * @param id 结果编号
     * @return 评审结果
     */
    ReviewResultDO getReviewResult(Long id);

    /**
     * 获取评审结果详情
     *
     * @param id 结果编号
     * @return 评审结果详情
     */
    ReviewResultRespVO getReviewResultDetail(Long id);

    /**
     * 获取评审结果分页
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ReviewResultRespVO> getReviewResultPage(ReviewResultPageReqVO pageReqVO);

    /**
     * 根据任务ID获取评审结果列表
     *
     * @param taskId 任务ID
     * @return 评审结果列表
     */
    List<ReviewResultDO> getReviewResultByTaskId(Long taskId);

    /**
     * 根据专家ID获取评审结果列表
     *
     * @param expertId 专家ID
     * @return 评审结果列表
     */
    List<ReviewResultDO> getReviewResultByExpertId(Long expertId);

    /**
     * 专家接收任务
     *
     * @param id 结果ID
     */
    void receiveTask(Long id);

    /**
     * 专家开始评审
     *
     * @param id 结果ID
     */
    void startReview(Long id);

    /**
     * 专家提交评审
     *
     * @param id 结果ID
     * @param saveReqVO 评审数据
     */
    void submitReview(Long id, ReviewResultSaveReqVO saveReqVO);

    /**
     * 专家申请回避
     *
     * @param id 结果ID
     * @param reason 回避原因
     */
    void applyAvoid(Long id, String reason);

    /**
     * 根据Flowable任务ID获取评审结果
     *
     * @param flowableTaskId Flowable任务ID
     * @return 评审结果
     */
    ReviewResultDO getReviewResultByFlowableTaskId(String flowableTaskId);

    /**
     * 计算任务汇总评分
     *
     * @param taskId 任务ID
     * @return 汇总评分
     */
    java.math.BigDecimal calculateTaskTotalScore(Long taskId);

    /**
     * 获取专家待办评审任务数量
     *
     * @param expertId 专家ID
     * @return 待评审数量
     */
    Long getExpertPendingCount(Long expertId);

    /**
     * 根据业务类型和业务ID获取评审汇总（按流程阶段分组）
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 评审汇总列表
     */
    List<ReviewSummaryRespVO> getReviewSummaryByBusiness(Integer businessType, Long businessId);

    /**
     * 专家拒绝评审任务
     *
     * @param id 评审结果ID
     * @param reason 拒绝原因
     */
    void rejectTask(Long id, String reason);

}
