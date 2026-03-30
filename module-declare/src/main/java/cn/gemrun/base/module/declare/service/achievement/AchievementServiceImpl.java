package cn.gemrun.base.module.declare.service.achievement;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import cn.gemrun.base.module.declare.controller.admin.achievement.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import cn.gemrun.base.module.declare.dal.dataobject.hospital.DeclareHospitalDO;
import cn.gemrun.base.module.declare.dal.mysql.DeclareHospitalMapper;
import cn.gemrun.base.module.declare.dal.mysql.achievement.AchievementMapper;
import cn.gemrun.base.module.declare.enums.AchievementStatus;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 成果与流通 Service 实现类
 *
 * @author
 */
@Slf4j
@Service
public class AchievementServiceImpl implements AchievementService {

    @Resource
    private AchievementMapper achievementMapper;
    @Resource
    private DeclareHospitalMapper hospitalMapper;
    @Resource
    @Lazy
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAchievement(AchievementSaveReqVO createReqVO) {
        log.info("[createAchievement] 创建成果");

        // 自动设置 deptId：从当前登录用户获取 deptId
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw new IllegalStateException("无法获取当前登录用户");
        }

        AdminUserRespDTO user = adminUserApi.getUser(userId);
        Long deptId = user != null ? user.getDeptId() : null;
        if (deptId == null) {
            throw new IllegalStateException("当前用户未关联医院部门");
        }

        // 通过 deptId 查询医院信息，自动设置 projectId
        DeclareHospitalDO hospital = hospitalMapper.selectByDeptId(deptId);
        if (hospital == null) {
            throw new IllegalStateException("未找到对应的医院信息");
        }

        // 构建成果实体
        AchievementDO achievement = new AchievementDO();
        achievement.setDeptId(deptId);
        achievement.setProjectId(hospital.getId()); // projectId = hospital.id
        achievement.setAchievementName(createReqVO.getAchievementName());
        achievement.setAchievementType(createReqVO.getAchievementType());
        achievement.setApplicationField(createReqVO.getApplicationField());
        achievement.setDescription(createReqVO.getDescription());
        achievement.setEffectDescription(createReqVO.getEffectDescription());
        achievement.setReplicationValue(createReqVO.getReplicationValue());
        achievement.setPromotionScope(createReqVO.getPromotionScope());
        achievement.setPromotionCount(createReqVO.getPromotionCount());
        achievement.setTransformType(createReqVO.getTransformType());
        achievement.setDataName(createReqVO.getDataName());
        achievement.setDataDescription(createReqVO.getDataDescription());
        achievement.setDataType(createReqVO.getDataType());
        achievement.setDataSource(createReqVO.getDataSource());
        achievement.setDataVolume(createReqVO.getDataVolume());
        achievement.setDataQuality(createReqVO.getDataQuality());
        achievement.setShareScope(createReqVO.getShareScope());
        achievement.setFlowType(createReqVO.getFlowType());
        achievement.setFlowObject(createReqVO.getFlowObject());
        achievement.setFlowPurpose(createReqVO.getFlowPurpose());
        achievement.setSecurityFilingNo(createReqVO.getSecurityFilingNo());
        achievement.setSecurityFilingTime(createReqVO.getSecurityFilingTime());
        achievement.setStartTime(createReqVO.getStartTime());
        achievement.setEndTime(createReqVO.getEndTime());
        achievement.setCertificateCount(createReqVO.getCertificateCount());
        achievement.setPropertyCount(createReqVO.getPropertyCount());
        achievement.setTransactionCount(createReqVO.getTransactionCount());
        achievement.setTransactionAmount(createReqVO.getTransactionAmount());
        achievement.setTransactionObject(createReqVO.getTransactionObject());
        achievement.setTransactionTime(createReqVO.getTransactionTime());
        achievement.setTransactionContract(createReqVO.getTransactionContract());
        achievement.setAttachmentIds(createReqVO.getAttachmentIds());
        achievement.setStatus(AchievementStatus.DRAFT.getStatus());
        achievement.setRecommendStatus(0);

        achievementMapper.insert(achievement);
        log.info("[createAchievement] 成果创建成功，id={}, deptId={}, projectId={}", achievement.getId(), deptId, hospital.getId());
        return achievement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAchievement(AchievementSaveReqVO updateReqVO) {
        log.info("[updateAchievement] 更新成果");
        if (updateReqVO.getId() == null) {
            throw new IllegalArgumentException("成果ID不能为空");
        }

        AchievementDO achievement = achievementMapper.selectById(updateReqVO.getId());
        if (achievement == null) {
            throw new IllegalArgumentException("成果不存在");
        }

        // 草稿状态才允许更新基本信息
        if (AchievementStatus.DRAFT.getStatus().equals(achievement.getStatus())) {
            if (updateReqVO.getAchievementName() != null) {
                achievement.setAchievementName(updateReqVO.getAchievementName());
            }
            if (updateReqVO.getAchievementType() != null) {
                achievement.setAchievementType(updateReqVO.getAchievementType());
            }
            if (updateReqVO.getApplicationField() != null) {
                achievement.setApplicationField(updateReqVO.getApplicationField());
            }
            if (updateReqVO.getDescription() != null) {
                achievement.setDescription(updateReqVO.getDescription());
            }
            if (updateReqVO.getEffectDescription() != null) {
                achievement.setEffectDescription(updateReqVO.getEffectDescription());
            }
            if (updateReqVO.getReplicationValue() != null) {
                achievement.setReplicationValue(updateReqVO.getReplicationValue());
            }
            if (updateReqVO.getPromotionScope() != null) {
                achievement.setPromotionScope(updateReqVO.getPromotionScope());
            }
            if (updateReqVO.getPromotionCount() != null) {
                achievement.setPromotionCount(updateReqVO.getPromotionCount());
            }
            if (updateReqVO.getTransformType() != null) {
                achievement.setTransformType(updateReqVO.getTransformType());
            }
            if (updateReqVO.getDataName() != null) {
                achievement.setDataName(updateReqVO.getDataName());
            }
            if (updateReqVO.getDataDescription() != null) {
                achievement.setDataDescription(updateReqVO.getDataDescription());
            }
            if (updateReqVO.getDataType() != null) {
                achievement.setDataType(updateReqVO.getDataType());
            }
            if (updateReqVO.getDataSource() != null) {
                achievement.setDataSource(updateReqVO.getDataSource());
            }
            if (updateReqVO.getDataVolume() != null) {
                achievement.setDataVolume(updateReqVO.getDataVolume());
            }
            if (updateReqVO.getDataQuality() != null) {
                achievement.setDataQuality(updateReqVO.getDataQuality());
            }
            if (updateReqVO.getShareScope() != null) {
                achievement.setShareScope(updateReqVO.getShareScope());
            }
            if (updateReqVO.getFlowType() != null) {
                achievement.setFlowType(updateReqVO.getFlowType());
            }
            if (updateReqVO.getFlowObject() != null) {
                achievement.setFlowObject(updateReqVO.getFlowObject());
            }
            if (updateReqVO.getFlowPurpose() != null) {
                achievement.setFlowPurpose(updateReqVO.getFlowPurpose());
            }
            if (updateReqVO.getSecurityFilingNo() != null) {
                achievement.setSecurityFilingNo(updateReqVO.getSecurityFilingNo());
            }
            if (updateReqVO.getSecurityFilingTime() != null) {
                achievement.setSecurityFilingTime(updateReqVO.getSecurityFilingTime());
            }
            if (updateReqVO.getStartTime() != null) {
                achievement.setStartTime(updateReqVO.getStartTime());
            }
            if (updateReqVO.getEndTime() != null) {
                achievement.setEndTime(updateReqVO.getEndTime());
            }
            if (updateReqVO.getCertificateCount() != null) {
                achievement.setCertificateCount(updateReqVO.getCertificateCount());
            }
            if (updateReqVO.getPropertyCount() != null) {
                achievement.setPropertyCount(updateReqVO.getPropertyCount());
            }
            if (updateReqVO.getTransactionCount() != null) {
                achievement.setTransactionCount(updateReqVO.getTransactionCount());
            }
            if (updateReqVO.getTransactionAmount() != null) {
                achievement.setTransactionAmount(updateReqVO.getTransactionAmount());
            }
            if (updateReqVO.getTransactionObject() != null) {
                achievement.setTransactionObject(updateReqVO.getTransactionObject());
            }
            if (updateReqVO.getTransactionTime() != null) {
                achievement.setTransactionTime(updateReqVO.getTransactionTime());
            }
            if (updateReqVO.getTransactionContract() != null) {
                achievement.setTransactionContract(updateReqVO.getTransactionContract());
            }
            if (updateReqVO.getAttachmentIds() != null) {
                achievement.setAttachmentIds(updateReqVO.getAttachmentIds());
            }
        } else {
            // 非草稿状态只允许更新证书、交易、附件等流通信息
            achievement.setCertificateCount(updateReqVO.getCertificateCount());
            achievement.setPropertyCount(updateReqVO.getPropertyCount());
            achievement.setTransactionCount(updateReqVO.getTransactionCount());
            achievement.setTransactionAmount(updateReqVO.getTransactionAmount());
            achievement.setTransactionObject(updateReqVO.getTransactionObject());
            achievement.setTransactionTime(updateReqVO.getTransactionTime());
            achievement.setTransactionContract(updateReqVO.getTransactionContract());
            if (updateReqVO.getAttachmentIds() != null) {
                achievement.setAttachmentIds(updateReqVO.getAttachmentIds());
            }
        }

        achievementMapper.updateById(achievement);
        log.info("[updateAchievement] 成果更新成功，id={}", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAchievement(Long id) {
        log.info("[deleteAchievement] 删除成果: id={}", id);
        achievementMapper.deleteById(id);
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
        log.info("[submitAchievement] 提交成果审核: id={}", id);
        AchievementDO achievement = achievementMapper.selectById(id);
        if (achievement == null) {
            throw new IllegalArgumentException("成果不存在");
        }
        achievement.setStatus(AchievementStatus.SUBMITTED.getStatus());
        achievementMapper.updateById(achievement);
        // TODO: 发起 BPM 流程，返回 processInstanceId
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
    public List<AchievementRespVO> getPublishedAchievementList() {
        List<AchievementDO> list = achievementMapper.selectPublishedList();
        return BeanUtils.toBean(list, AchievementRespVO.class);
    }

    @Override
    public AchievementRespVO getPublishedAchievement(Long id) {
        AchievementDO achievement = achievementMapper.selectPublishedById(id);
        if (achievement == null) {
            return new AchievementRespVO();
        }
        return BeanUtils.toBean(achievement, AchievementRespVO.class);
    }
}
