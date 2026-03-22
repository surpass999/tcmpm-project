package cn.gemrun.base.module.declare.service.achievement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
import cn.gemrun.base.module.declare.controller.admin.achievement.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.dal.mysql.achievement.AchievementMapper;
import cn.gemrun.base.module.declare.enums.AchievementStatus;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import cn.gemrun.base.module.bpm.api.task.BpmProcessInstanceApi;
import cn.gemrun.base.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import java.time.LocalDateTime;
import com.mzt.logapi.starter.annotation.LogRecord;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;
import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 成果与流通 Service 实现类
 *
 * @author
 */
@Service
@Slf4j
public class AchievementServiceImpl implements AchievementService {

    @Resource
    private AchievementMapper achievementMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ProjectService projectService;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Override
    @LogRecord(type = ACHIEVEMENT_TYPE, subType = ACHIEVEMENT_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}", success = ACHIEVEMENT_CREATE_SUCCESS)
    public Long createAchievement(AchievementSaveReqVO createReqVO) {
        Long deptId = null;
        Long userId = null;
        try {
            userId = WebFrameworkUtils.getLoginUserId();
            if (userId != null) {
                AdminUserRespDTO user = adminUserApi.getUser(userId);
                if (user != null) {
                    deptId = user.getDeptId();
                }
            }
        } catch (Exception e) {
            log.warn("[createAchievement] 获取用户部门ID失败: {}", e.getMessage());
        }

        AchievementDO achievement = BeanUtils.toBean(createReqVO, AchievementDO.class);
        achievement.setDeptId(deptId);
        achievement.setStatus(AchievementStatus.DRAFT.getStatus()); // DRAFT 草稿
        achievement.setRecommendStatus(0); // 未推荐

        achievementMapper.insert(achievement);
        log.info("[createAchievement] 创建成果与流通成功, id: {}", achievement.getId());

        return achievement.getId();
    }

    @Override
    @LogRecord(type = ACHIEVEMENT_TYPE, subType = ACHIEVEMENT_UPDATE_SUB_TYPE,
            bizNo = "{{#updateReqVO.id}}", success = ACHIEVEMENT_UPDATE_SUCCESS)
    public void updateAchievement(AchievementSaveReqVO updateReqVO) {
        validateAchievementExists(updateReqVO.getId());

        AchievementDO updateObj = BeanUtils.toBean(updateReqVO, AchievementDO.class);
        achievementMapper.updateById(updateObj);

        log.info("[updateAchievement] 更新成果与流通, id: {}", updateReqVO.getId());
    }

    @Override
    @LogRecord(type = ACHIEVEMENT_TYPE, subType = ACHIEVEMENT_DELETE_SUB_TYPE,
            bizNo = "{{#id}}", success = ACHIEVEMENT_DELETE_SUCCESS)
    public void deleteAchievement(Long id) {
        AchievementDO achievement = validateAchievementExists(id);

        if (!AchievementStatus.isDraft(achievement.getStatus())) {
            throw new RuntimeException("只有草稿状态的成果才能删除，当前状态: " + achievement.getStatus());
        }

        achievementMapper.deleteById(id);
        log.info("[deleteAchievement] 删除成果与流通, id: {}", id);
    }

    @Override
    public AchievementDO getAchievement(Long id) {
        AchievementDO achievement = achievementMapper.selectById(id);
        if (achievement != null && achievement.getProjectId() != null) {
            fillProjectName(achievement);
        }
        return achievement;
    }

    @Override
    public PageResult<AchievementDO> getAchievementPage(AchievementPageReqVO pageReqVO) {
        PageResult<AchievementDO> pageResult = achievementMapper.selectPage(pageReqVO);
        if (pageResult.getList() != null && !pageResult.getList().isEmpty()) {
            for (AchievementDO achievement : pageResult.getList()) {
                if (achievement.getProjectId() != null) {
                    fillProjectName(achievement);
                }
            }
        }
        return pageResult;
    }

    @Override
    public List<AchievementDO> getAchievementListByProjectId(Long projectId) {
        List<AchievementDO> list = achievementMapper.selectByProjectId(projectId);
        if (list != null && !list.isEmpty()) {
            for (AchievementDO achievement : list) {
                fillProjectName(achievement);
            }
        }
        return list;
    }

    private void fillProjectName(AchievementDO achievement) {
        try {
            cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectRespVO project = projectService.getProject(achievement.getProjectId());
            if (project != null) {
                achievement.setProjectName(project.getProjectName());
            }
        } catch (Exception e) {
            log.warn("[fillProjectName] 获取项目名称失败: projectId={}, error={}",
                achievement.getProjectId(), e.getMessage());
        }
    }

    @Override
    @LogRecord(type = ACHIEVEMENT_TYPE, subType = ACHIEVEMENT_SUBMIT_SUB_TYPE,
            bizNo = "{{#id}}", success = ACHIEVEMENT_SUBMIT_SUCCESS)
    public String submitAchievement(Long id, String processDefinitionKey) {
        AchievementDO achievement = validateAchievementExists(id);

        if (!AchievementStatus.isDraft(achievement.getStatus())) {
            throw new RuntimeException("只有草稿状态的成果才能提交审核，当前状态: " + achievement.getStatus());
        }

        if (achievement.getProcessInstanceId() != null && !achievement.getProcessInstanceId().isEmpty()) {
            log.warn("[submitAchievement] 成果已存在流程实例: achievementId={}, processInstanceId={}",
                id, achievement.getProcessInstanceId());
            return achievement.getProcessInstanceId();
        }

        // 构建 businessKey（对齐备案格式：{processDefinitionKey}_{businessId}）
        String businessKey = processDefinitionKey + "_" + id;
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessKey", businessKey);
        variables.put("businessType", "achievement");
        variables.put("title", "成果与流通审核 - " + achievement.getAchievementName());

        // 构建创建请求（走 BpmProcessInstanceApi，对齐 BpmStartProcessController）
        BpmProcessInstanceCreateReqDTO createReqDTO = new BpmProcessInstanceCreateReqDTO();
        createReqDTO.setProcessDefinitionKey(processDefinitionKey);
        createReqDTO.setBusinessKey(businessKey);
        createReqDTO.setVariables(variables);

        // 发起流程（状态更新由 DeclareProcessStartedListener 通过事件驱动）
        Long userId = getCurrentUserId();
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId, createReqDTO);

        log.info("[submitAchievement] 提交审核成功: achievementId={}, processInstanceId={}",
            id, processInstanceId);

        return processInstanceId;
    }

    @Override
    @LogRecord(type = ACHIEVEMENT_TYPE, subType = ACHIEVEMENT_RECOMMEND_SUB_TYPE,
            bizNo = "{{#id}}", success = ACHIEVEMENT_RECOMMEND_SUCCESS)
    public void recommendToNation(Long id, String opinion) {
        AchievementDO achievement = validateAchievementExists(id);

        if (!AchievementStatus.isApproved(achievement.getStatus())) {
            throw new RuntimeException("只有已通过的成果才能推荐至国家局");
        }

        achievement.setRecommendStatus(1); // 已推荐至国家局
        achievement.setRecommendOpinion(opinion);
        achievement.setRecommenderId(getCurrentUserId());
        achievement.setRecommendTime(LocalDateTime.now());
        achievementMapper.updateById(achievement);

        log.info("[recommendToNation] 推荐成果至国家局: achievementId={}, opinion={}", id, opinion);
    }

    @Override
    @LogRecord(type = ACHIEVEMENT_TYPE, subType = ACHIEVEMENT_STATUS_CHANGE_SUB_TYPE,
            bizNo = "{{#id}}", success = ACHIEVEMENT_STATUS_CHANGE_SUCCESS)
    public void updateAchievementStatus(Long id, String bizStatus) {
        AchievementDO achievement = achievementMapper.selectById(id);
        if (achievement == null) {
            log.warn("[updateAchievementStatus] 成果不存在: id={}", id);
            return;
        }

        // 解析 bizStatus，格式可能是 "NATIONAL_APPROVED|RECOMMEND" 或 "APPROVED|SUCCESS"
        String status = bizStatus;
        Integer recommendStatus = 0;
        if (bizStatus != null && bizStatus.contains("|")) {
            String[] parts = bizStatus.split("\\|");
            status = parts[0];
            if (parts.length > 1 && ("RECOMMEND".equals(parts[1]) || "SUCCESS".equals(parts[1]))) {
                recommendStatus = 1;
            }
        }

        achievement.setStatus(status);
        achievement.setRecommendStatus(recommendStatus);
        if (recommendStatus == 1) {
            achievement.setRecommendTime(LocalDateTime.now());
        }
        achievementMapper.updateById(achievement);

        if ("APPROVED".equals(status)) {
            syncIndicatorsOnApproved(id);
        }

        log.info("[updateAchievementStatus] 更新成果状态: id={}, bizStatus={}, status={}, recommendStatus={}",
            id, bizStatus, achievement.getStatus(), achievement.getRecommendStatus());
    }

    @Override
    public void updateAchievementProcessInstance(Long id, String processInstanceId) {
        AchievementDO achievement = achievementMapper.selectById(id);
        if (achievement == null) {
            log.warn("[updateAchievementProcessInstance] 成果不存在: id={}", id);
            return;
        }
        achievement.setProcessInstanceId(processInstanceId);
        achievement.setStatus(AchievementStatus.SUBMITTED.getStatus());
        achievementMapper.updateById(achievement);
        log.info("[updateAchievementProcessInstance] 更新成果流程实例: id={}, processInstanceId={}", id, processInstanceId);
    }

    @Override
    @LogRecord(type = ACHIEVEMENT_TYPE, subType = ACHIEVEMENT_RECOMMEND_LIBRARY_SUB_TYPE,
            bizNo = "{{#id}}", success = ACHIEVEMENT_RECOMMEND_LIBRARY_SUCCESS)
    public void recommendToLibrary(Long id) {
        AchievementDO achievement = validateAchievementExists(id);
        // 校验：必须 status=NATION_APPROVED 且 recommendStatus=1 才能推荐至推广库 错误的status:NATIONAL_APPROVED
        if (!"NATION_APPROVED".equals(achievement.getStatus()) || !Integer.valueOf(1).equals(achievement.getRecommendStatus())) {
            log.warn("[recommendToLibrary] 成果不能推荐至推广库: achievementId={}, status={}, recommendStatus={}", id, achievement.getStatus(), achievement.getRecommendStatus());
            throw exception(ACHIEVEMENT_NOT_RECOMMENDED_TO_NATION);
        }
        achievement.setRecommendStatus(2);
        achievement.setRecommendTime(LocalDateTime.now());
        achievementMapper.updateById(achievement);
        log.info("[recommendToLibrary] 推荐成果至推广库: achievementId={}", id);
    }

    @Override
    public void syncIndicatorsOnApproved(Long achievementId) {
        AchievementDO achievement = achievementMapper.selectById(achievementId);
        if (achievement == null) {
            log.warn("[syncIndicatorsOnApproved] 成果不存在: id={}", achievementId);
            return;
        }

        log.info("[syncIndicatorsOnApproved] 审核通过，同步指标: achievementId={}, certificateCount={}, propertyCount={}, transactionCount={}, transactionAmount={}",
            achievementId, achievement.getCertificateCount(), achievement.getPropertyCount(),
            achievement.getTransactionCount(), achievement.getTransactionAmount());
    }

    @Override
    public List<AchievementRespVO> getPublishedAchievementList() {
        List<AchievementDO> list = achievementMapper.selectPublishedList();
        if (list != null && !list.isEmpty()) {
            for (AchievementDO achievement : list) {
                if (achievement.getProjectId() != null) {
                    fillProjectName(achievement);
                }
            }
        }
        List<AchievementRespVO> voList = BeanUtils.toBean(list, AchievementRespVO.class);
        for (AchievementRespVO vo : voList) {
            fillCreatorName(vo);
        }
        return voList;
    }

    @Override
    public AchievementRespVO getPublishedAchievement(Long id) {
        AchievementDO achievement = achievementMapper.selectPublishedById(id);
        if (achievement != null && achievement.getProjectId() != null) {
            fillProjectName(achievement);
        }
        AchievementRespVO vo = BeanUtils.toBean(achievement, AchievementRespVO.class);
        fillCreatorName(vo);
        return vo;
    }

    private void fillCreatorName(AchievementRespVO vo) {
        if (vo == null || vo.getCreator() == null) {
            return;
        }
        try {
            AdminUserRespDTO user = adminUserApi.getUser(vo.getCreator());
            if (user != null) {
                vo.setCreatorName(user.getNickname());
            }
        } catch (Exception e) {
            log.warn("[fillCreatorName] 获取创建者昵称失败: creatorId={}, error={}",
                vo.getCreator(), e.getMessage());
        }
    }

    private AchievementDO validateAchievementExists(Long id) {
        AchievementDO achievement = achievementMapper.selectById(id);
        if (achievement == null) {
            throw exception(ACHIEVEMENT_NOT_EXISTS);
        }
        return achievement;
    }

    private Long getCurrentUserId() {
        try {
            return SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception e) {
            log.warn("[getCurrentUserId] 获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

}
