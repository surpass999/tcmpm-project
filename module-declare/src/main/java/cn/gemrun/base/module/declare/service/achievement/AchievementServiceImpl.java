package cn.gemrun.base.module.declare.service.achievement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
import cn.gemrun.base.module.declare.controller.admin.achievement.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import cn.gemrun.base.module.declare.dal.dataobject.dataflow.DataFlowDO;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.dal.mysql.achievement.AchievementMapper;
import cn.gemrun.base.module.declare.service.dataflow.DataFlowService;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCreateReqVO;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import org.flowable.engine.repository.ProcessDefinition;
import java.time.LocalDateTime;
import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;

/**
 * 成果信息 Service 实现类
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
    private DataFlowService dataFlowService;
    @Resource
    private BpmProcessInstanceService bpmProcessInstanceService;
    @Resource
    private BpmProcessDefinitionService bpmProcessDefinitionService;

    @Override
    public Long createAchievement(AchievementSaveReqVO createReqVO) {
        // 校验有已通过审核的数据流通记录
        validateHasDataFlow(createReqVO.getProjectId());

        // 获取当前登录用户的部门ID
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

        // 转换并插入
        AchievementDO achievement = BeanUtils.toBean(createReqVO, AchievementDO.class);
        achievement.setDeptId(deptId);
        achievement.setAuditStatus(0); // 待审核
        achievement.setRecommendStatus(0); // 未推荐

        achievementMapper.insert(achievement);
        log.info("[createAchievement] 创建成果信息成功, id: {}", achievement.getId());

        return achievement.getId();
    }

    @Override
    public void updateAchievement(AchievementSaveReqVO updateReqVO) {
        // 校验存在
        validateAchievementExists(updateReqVO.getId());

        // 更新
        AchievementDO updateObj = BeanUtils.toBean(updateReqVO, AchievementDO.class);
        achievementMapper.updateById(updateObj);

        log.info("[updateAchievement] 更新成果信息, id: {}", updateReqVO.getId());
    }

    @Override
    public void deleteAchievement(Long id) {
        // 校验存在
        validateAchievementExists(id);
        // 删除
        achievementMapper.deleteById(id);
        log.info("[deleteAchievement] 删除成果信息, id: {}", id);
    }

    @Override
    public AchievementDO getAchievement(Long id) {
        return achievementMapper.selectById(id);
    }

    @Override
    public PageResult<AchievementDO> getAchievementPage(AchievementPageReqVO pageReqVO) {
        return achievementMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AchievementDO> getAchievementListByProjectId(Long projectId) {
        return achievementMapper.selectByProjectId(projectId);
    }

    @Override
    public String submitAchievement(Long id, String processDefinitionKey) {
        // 1. 校验成果是否存在
        AchievementDO achievement = validateAchievementExists(id);

        // 2. 校验状态必须是草稿才能提交
        if (achievement.getAuditStatus() != 0) {
            throw new RuntimeException("只有草稿状态的成果才能提交审核，当前状态: " + achievement.getAuditStatus());
        }

        // 3. 校验有已通过审核的数据流通记录
        validateHasDataFlow(achievement.getProjectId());

        // 4. 如果已有流程实例ID，说明已发起流程
        if (achievement.getProcessInstanceId() != null && !achievement.getProcessInstanceId().isEmpty()) {
            log.warn("[submitAchievement] 成果已存在流程实例: achievementId={}, processInstanceId={}",
                id, achievement.getProcessInstanceId());
            return achievement.getProcessInstanceId();
        }

        // 5. 获取流程定义
        ProcessDefinition processDefinition = bpmProcessDefinitionService.getActiveProcessDefinition(processDefinitionKey);
        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在: " + processDefinitionKey);
        }

        // 6. 构建流程变量
        String businessKey = String.format("declare:achievement:submit:%d", id);
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessKey", businessKey);
        variables.put("businessType", "achievement");
        variables.put("title", "成果转化审核 - " + achievement.getAchievementName());

        // 7. 发起流程
        BpmProcessInstanceCreateReqVO createReqVO = new BpmProcessInstanceCreateReqVO();
        createReqVO.setProcessDefinitionId(processDefinition.getId());
        createReqVO.setVariables(variables);

        Long userId = getCurrentUserId();
        String processInstanceId = bpmProcessInstanceService.createProcessInstance(userId, createReqVO);

        // 8. 更新成果的流程实例ID和状态
        achievement.setProcessInstanceId(processInstanceId);
        achievement.setAuditStatus(1); // 省级通过/待国家局审核
        achievementMapper.updateById(achievement);

        log.info("[submitAchievement] 提交审核成功: achievementId={}, processInstanceId={}",
            id, processInstanceId);

        return processInstanceId;
    }

    @Override
    public void recommendToNation(Long id, String opinion) {
        AchievementDO achievement = validateAchievementExists(id);

        // 只有已通过的成果才能推荐
        if (achievement.getAuditStatus() != 3) {
            throw new RuntimeException("只有已通过的成果才能推荐至国家局");
        }

        // 更新推荐状态
        achievement.setRecommendStatus(1); // 已推荐至国家局
        achievement.setRecommendOpinion(opinion);
        achievement.setRecommenderId(getCurrentUserId());
        achievement.setRecommendTime(LocalDateTime.now());
        achievementMapper.updateById(achievement);

        log.info("[recommendToNation] 推荐成果至国家局: achievementId={}, opinion={}", id, opinion);
    }

    @Override
    public void updateAchievementStatus(Long id, String bizStatus) {
        AchievementDO achievement = achievementMapper.selectById(id);
        if (achievement == null) {
            log.warn("[updateAchievementStatus] 成果不存在: id={}", id);
            return;
        }

        // 解析状态码
        Integer status = parseAuditStatus(bizStatus);
        achievement.setAuditStatus(status);
        achievementMapper.updateById(achievement);

        log.info("[updateAchievementStatus] 更新成果状态: id={}, bizStatus={}, auditStatus={}", id, bizStatus, status);
    }

    @Override
    public void updateAchievementProcessInstance(Long id, String processInstanceId, Integer auditStatus) {
        AchievementDO achievement = validateAchievementExists(id);
        achievement.setProcessInstanceId(processInstanceId);
        achievement.setAuditStatus(auditStatus);
        achievementMapper.updateById(achievement);

        log.info("[updateAchievementProcessInstance] 更新成果流程实例: id={}, processInstanceId={}, auditStatus={}",
            id, processInstanceId, auditStatus);
    }

    private void validateHasDataFlow(Long projectId) {
        List<DataFlowDO> dataFlows = dataFlowService.getDataFlowListByProjectId(projectId);
        if (dataFlows == null || dataFlows.isEmpty()) {
            throw exception(ACHIEVEMENT_NO_DATA_FLOW);
        }

        // 校验至少有1条流通记录审核通过
        boolean hasApprovedFlow = dataFlows.stream()
            .anyMatch(df -> df.getStatus() != null && df.getStatus() == 3);
        if (!hasApprovedFlow) {
            throw exception(ACHIEVEMENT_NO_DATA_FLOW);
        }
    }

    private AchievementDO validateAchievementExists(Long id) {
        AchievementDO achievement = achievementMapper.selectById(id);
        if (achievement == null) {
            throw exception(ACHIEVEMENT_NOT_EXISTS);
        }
        return achievement;
    }

    private Integer parseAuditStatus(String bizStatus) {
        if (bizStatus == null) {
            return 2; // 国家局审核中
        }
        if (bizStatus.contains("APPROVED") || bizStatus.contains("通过")) {
            return 3; // 已认定推广
        } else if (bizStatus.contains("REJECTED") || bizStatus.contains("退回")) {
            return 4; // 退回
        } else if (bizStatus.contains("SUBMITTED") || bizStatus.contains("提交")) {
            return 1; // 省级通过/待国家局审核
        }
        return 2; // 国家局审核中
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
