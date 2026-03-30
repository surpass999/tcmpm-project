package cn.gemrun.base.module.declare.service.achievement;

import java.util.*;
import javax.validation.*;
import cn.gemrun.base.module.declare.controller.admin.achievement.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import cn.gemrun.base.framework.common.pojo.PageResult;

/**
 * 成果与流通 Service 接口
 *
 * @author
 */
public interface AchievementService {

    /**
     * 创建成果与流通
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAchievement(@Valid AchievementSaveReqVO createReqVO);

    /**
     * 更新成果与流通
     *
     * @param updateReqVO 更新信息
     */
    void updateAchievement(@Valid AchievementSaveReqVO updateReqVO);

    /**
     * 删除成果与流通
     *
     * @param id 编号
     */
    void deleteAchievement(Long id);

    /**
     * 获得成果与流通
     *
     * @param id 编号
     * @return 成果与流通
     */
    AchievementDO getAchievement(Long id);

    /**
     * 获得成果与流通分页
     *
     * @param pageReqVO 分页查询
     * @return 成果与流通分页
     */
    PageResult<AchievementDO> getAchievementPage(AchievementPageReqVO pageReqVO);

    /**
     * 根据项目ID查询成果列表
     *
     * @param projectId 项目ID
     * @return 成果列表
     */
    List<AchievementDO> getAchievementListByProjectId(Long projectId);

    /**
     * 提交成果审核
     *
     * @param id 成果ID
     * @param processDefinitionKey 流程定义Key
     * @return 流程实例ID
     */
    String submitAchievement(Long id, String processDefinitionKey);

    /**
     * 推荐成果至国家局
     *
     * @param id 成果ID
     * @param opinion 推荐意见
     */
    void recommendToNation(Long id, String opinion);

    /**
     * 推荐成果至推广库（遴选完成）
     * 条件：status=NATIONAL_APPROVED 且 recommendStatus=1
     *
     * @param id 成果ID
     */
    void recommendToLibrary(Long id);

    /**
     * 更新成果状态（流程事件触发）
     *
     * @param id 成果ID
     * @param bizStatus 流程状态
     */
    void updateAchievementStatus(Long id, String bizStatus);

    /**
     * 更新成果的流程实例ID（由流程发起事件监听器调用）
     *
     * @param id 成果ID
     * @param processInstanceId 流程实例ID
     */
    void updateAchievementProcessInstance(Long id, String processInstanceId);

    /**
     * 获取已纳入推广库的成果列表（公开接口）
     * 筛选条件：recommendStatus=2（已纳入推广库，遴选完成）
     *
     * @return 成果列表
     */
    List<AchievementRespVO> getPublishedAchievementList();

    /**
     * 获取已纳入推广库的成果详情（公开接口）
     * 筛选条件：recommendStatus=2（已纳入推广库，遴选完成）
     *
     * @param id 成果ID
     * @return 成果详情
     */
    AchievementRespVO getPublishedAchievement(Long id);

}
