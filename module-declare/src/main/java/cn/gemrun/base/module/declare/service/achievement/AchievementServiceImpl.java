package cn.gemrun.base.module.declare.service.achievement;

import cn.gemrun.base.module.declare.controller.admin.achievement.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import cn.gemrun.base.framework.common.pojo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AchievementServiceImpl implements AchievementService {

    @Override
    public Long createAchievement(AchievementSaveReqVO createReqVO) {
        log.info("[createAchievement] 创建成果");
        return 0L;
    }

    @Override
    public void updateAchievement(AchievementSaveReqVO updateReqVO) {
        log.info("[updateAchievement] 更新成果");
    }

    @Override
    public void deleteAchievement(Long id) {
        log.info("[deleteAchievement] 删除成果: id={}", id);
    }

    @Override
    public AchievementDO getAchievement(Long id) {
        return null;
    }

    @Override
    public PageResult<AchievementDO> getAchievementPage(AchievementPageReqVO pageReqVO) {
        return new PageResult<>(new java.util.ArrayList<>(), 0L);
    }

    @Override
    public List<AchievementDO> getAchievementListByProjectId(Long projectId) {
        return new java.util.ArrayList<>();
    }

    @Override
    public String submitAchievement(Long id, String processDefinitionKey) {
        log.info("[submitAchievement] 提交成果审核: id={}", id);
        return "";
    }

    @Override
    public void recommendToNation(Long id, String opinion) {
        log.info("[recommendToNation] 推荐成果至国家局: id={}", id);
    }

    @Override
    public void recommendToLibrary(Long id) {
        log.info("[recommendToLibrary] 推荐成果至推广库: id={}", id);
    }

    @Override
    public void updateAchievementStatus(Long id, String bizStatus) {
        log.info("[updateAchievementStatus] achievementId={}, bizStatus={}", id, bizStatus);
    }

    @Override
    public void updateAchievementProcessInstance(Long id, String processInstanceId) {
        log.info("[updateAchievementProcessInstance] id={}, processInstanceId={}", id, processInstanceId);
    }

    @Override
    public void syncIndicatorsOnApproved(Long achievementId) {
        log.info("[syncIndicatorsOnApproved] 同步指标值: achievementId={}", achievementId);
    }

    @Override
    public List<AchievementRespVO> getPublishedAchievementList() {
        return new java.util.ArrayList<>();
    }

    @Override
    public AchievementRespVO getPublishedAchievement(Long id) {
        return new AchievementRespVO();
    }
}
