package cn.gemrun.base.module.declare.service.training;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.training.vo.*;

/**
 * 培训活动 Service 接口
 *
 * @author Gemini
 */
public interface TrainingService {

    /**
     * 创建培训活动
     */
    Long createTraining(TrainingSaveReqVO createReqVO);

    /**
     * 更新培训活动
     */
    void updateTraining(TrainingSaveReqVO updateReqVO);

    /**
     * 删除培训活动
     */
    void deleteTraining(Long id);

    /**
     * 获取培训活动详情
     */
    TrainingRespVO getTraining(Long id);

    /**
     * 获取培训活动分页列表
     */
    PageResult<TrainingRespVO> getTrainingPage(TrainingPageReqVO pageReqVO);

    /**
     * 发布培训活动
     */
    void publishTraining(Long id);

    /**
     * 取消发布培训活动
     */
    void unpublishTraining(Long id);

    /**
     * 获取已发布的活动列表（用户端）
     */
    java.util.List<TrainingRespVO> getPublishedTrainingList();

    /**
     * 获取活动详情（用户端）
     */
    TrainingRespVO getPublishedTraining(Long id);

    /**
     * 报名参加活动（用户端）
     */
    void registerTraining(RegisterReqVO registerReqVO, Long userId, String userName);

    /**
     * 取消报名（用户端）
     */
    void cancelRegistration(Long trainingId, Long userId);

    /**
     * 获取我的报名列表（用户端）
     */
    PageResult<RegistrationRespVO> getMyRegistrations(Long userId, TrainingPageReqVO pageReqVO);

    /**
     * 提交反馈评分（用户端）
     */
    void submitFeedback(FeedbackReqVO feedbackReqVO, Long userId);

    /**
     * 签到（管理端）
     */
    void signIn(Long registrationId);

    /**
     * 获取活动报名分页列表（管理端）
     */
    PageResult<RegistrationRespVO> getRegistrationPage(RegistrationPageReqVO pageReqVO);

    /**
     * 获取活动统计数据
     */
    TrainingStatisticsRespVO getStatistics(Long id);

}
