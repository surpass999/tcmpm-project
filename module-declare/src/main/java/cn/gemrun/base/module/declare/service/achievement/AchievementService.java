package cn.gemrun.base.module.declare.service.achievement;

import java.util.*;
import javax.validation.*;
import cn.gemrun.base.module.declare.controller.admin.achievement.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import cn.gemrun.base.framework.common.pojo.PageResult;

/**
 * 成果信息 Service 接口
 *
 * @author
 */
public interface AchievementService {

    /**
     * 创建成果信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAchievement(@Valid AchievementSaveReqVO createReqVO);

    /**
     * 更新成果信息
     *
     * @param updateReqVO 更新信息
     */
    void updateAchievement(@Valid AchievementSaveReqVO updateReqVO);

    /**
     * 删除成果信息
     *
     * @param id 编号
     */
    void deleteAchievement(Long id);

    /**
     * 获得成果信息
     *
     * @param id 编号
     * @return 成果信息
     */
    AchievementDO getAchievement(Long id);

    /**
     * 获得成果信息分页
     *
     * @param pageReqVO 分页查询
     * @return 成果信息分页
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
     * 更新成果状态（流程事件触发）
     *
     * @param id 成果ID
     * @param bizStatus 流程状态
     */
    void updateAchievementStatus(Long id, String bizStatus);

    /**
     * 更新成果的流程实例ID和状态
     *
     * @param id 成果ID
     * @param processInstanceId 流程实例ID
     * @param auditStatus 审核状态
     */
    void updateAchievementProcessInstance(Long id, String processInstanceId, Integer auditStatus);

}
