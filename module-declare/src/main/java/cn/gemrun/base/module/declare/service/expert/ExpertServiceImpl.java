package cn.gemrun.base.module.declare.service.expert;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.controller.admin.expert.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.expert.ExpertDO;
import cn.gemrun.base.module.declare.dal.mysql.expert.ExpertMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mzt.logapi.starter.annotation.LogRecord;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;
import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 专家管理 Service 实现类
 *
 * @author Gemini
 */
@Service
@Slf4j
public class ExpertServiceImpl implements ExpertService {

    @Resource
    private ExpertMapper expertMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = EXPERT_TYPE, subType = EXPERT_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}", success = EXPERT_CREATE_SUCCESS)
    public Long createExpert(ExpertSaveReqVO createReqVO) {
        // 校验用户ID是否已关联专家
        if (createReqVO.getUserId() != null) {
            ExpertDO existExpert = expertMapper.selectOne(new LambdaQueryWrapperX<ExpertDO>()
                    .eq(ExpertDO::getUserId, createReqVO.getUserId())
                    .eq(ExpertDO::getDeleted, false));
            if (existExpert != null) {
                throw exception(EXPERT_USER_ALREADY_EXISTS);
            }
        }

        // 插入专家信息
        ExpertDO expert = BeanUtils.toBean(createReqVO, ExpertDO.class);
        expert.setReviewCount(0); // 初始评审次数为0
        expert.setStatus(1); // 默认在册状态
        expertMapper.insert(expert);

        return expert.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = EXPERT_TYPE, subType = EXPERT_UPDATE_SUB_TYPE,
            bizNo = "{{#updateReqVO.id}}", success = EXPERT_UPDATE_SUCCESS)
    public void updateExpert(ExpertSaveReqVO updateReqVO) {
        // 校验存在
        validateExpertExists(updateReqVO.getId());

        // 校验用户ID是否与其他专家关联（排除自己）
        if (updateReqVO.getUserId() != null) {
            ExpertDO existExpert = expertMapper.selectOne(new LambdaQueryWrapperX<ExpertDO>()
                    .eq(ExpertDO::getUserId, updateReqVO.getUserId())
                    .ne(ExpertDO::getId, updateReqVO.getId())
                    .eq(ExpertDO::getDeleted, false));
            if (existExpert != null) {
                throw exception(EXPERT_USER_ALREADY_EXISTS);
            }
        }

        // 更新专家信息
        ExpertDO updateObj = BeanUtils.toBean(updateReqVO, ExpertDO.class);
        expertMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = EXPERT_TYPE, subType = EXPERT_STATUS_CHANGE_SUB_TYPE,
            bizNo = "{{#id}}", success = EXPERT_STATUS_CHANGE_SUCCESS)
    public void updateExpertStatus(Long id, Integer status) {
        // 校验存在
        validateExpertExists(id);

        // 校验状态值
        if (status < 1 || status > 3) {
            throw exception(EXPERT_STATUS_ERROR);
        }

        // 更新状态
        ExpertDO updateObj = new ExpertDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        expertMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = EXPERT_TYPE, subType = EXPERT_DELETE_SUB_TYPE,
            bizNo = "{{#id}}", success = EXPERT_DELETE_SUCCESS)
    public void deleteExpert(Long id) {
        // 校验存在
        validateExpertExists(id);

        // 逻辑删除
        expertMapper.deleteById(id);
    }

    @Override
    public ExpertDO getExpert(Long id) {
        return expertMapper.selectById(id);
    }

    @Override
    public ExpertDO getExpertByUserId(Long userId) {
        return expertMapper.selectOne(new LambdaQueryWrapperX<ExpertDO>()
                .eq(ExpertDO::getUserId, userId)
                .eq(ExpertDO::getDeleted, false));
    }

    @Override
    public List<ExpertDO> getExpertListByIds(List<Long> ids) {
        return expertMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<ExpertDO> getExpertPage(ExpertPageReqVO pageReqVO) {
        return expertMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<ExpertDO>()
                .likeIfPresent(ExpertDO::getExpertName, pageReqVO.getExpertName())
                .eqIfPresent(ExpertDO::getExpertType, pageReqVO.getExpertType())
                .eqIfPresent(ExpertDO::getStatus, pageReqVO.getStatus())
                .likeIfPresent(ExpertDO::getWorkUnit, pageReqVO.getWorkUnit())
                .likeIfPresent(ExpertDO::getPhone, pageReqVO.getPhone())
                .eqIfPresent(ExpertDO::getUserId, pageReqVO.getUserId())
                .orderByDesc(ExpertDO::getId));
    }

    @Override
    public List<ExpertDO> getExpertList(ExpertListReqVO reqVO) {
        return expertMapper.selectList(new LambdaQueryWrapperX<ExpertDO>()
                .eqIfPresent(ExpertDO::getExpertType, reqVO.getExpertType())
                .eqIfPresent(ExpertDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ExpertDO::getExpertName, reqVO.getExpertName())
                .likeIfPresent(ExpertDO::getWorkUnit, reqVO.getWorkUnit())
                .eq(ExpertDO::getDeleted, false)
                .orderByDesc(ExpertDO::getId));
    }

    @Override
    public List<Long> getExpertUserIds() {
        return expertMapper.selectList(new LambdaQueryWrapperX<ExpertDO>()
                .isNotNull(ExpertDO::getUserId)
                .eq(ExpertDO::getDeleted, false))
                .stream()
                .map(ExpertDO::getUserId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public PageResult<ExpertDO> getExpertSelectList(ExpertPageReqVO pageReqVO) {
        // 直接调用 getExpertPage，回避逻辑在 Controller 层处理
        return getExpertPage(pageReqVO);
    }

    /**
     * 校验专家是否存在
     */
    private void validateExpertExists(Long id) {
        if (expertMapper.selectById(id) == null) {
            throw exception(EXPERT_NOT_EXISTS);
        }
    }

    @Override
    public void updateExpertReviewStats(Long expertId, BigDecimal newScore) {
        if (expertId == null) {
            return;
        }
        ExpertDO expert = expertMapper.selectById(expertId);
        if (expert == null) {
            return;
        }

        int currentCount = expert.getReviewCount() != null ? expert.getReviewCount() : 0;
        BigDecimal currentAvg = expert.getReviewScore() != null ? expert.getReviewScore() : BigDecimal.ZERO;

        // 计算新的平均分：newAvg = (currentAvg * currentCount + newScore) / (currentCount + 1)
        BigDecimal totalScore = currentAvg.multiply(BigDecimal.valueOf(currentCount))
                .add(newScore != null ? newScore : BigDecimal.ZERO);
        int newCount = currentCount + 1;
        BigDecimal newAvg = totalScore.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        ExpertDO updateObj = new ExpertDO();
        updateObj.setId(expertId);
        updateObj.setReviewCount(newCount);
        updateObj.setReviewScore(newAvg);
        updateObj.setLastReviewTime(LocalDateTime.now());
        expertMapper.updateById(updateObj);
    }

}
