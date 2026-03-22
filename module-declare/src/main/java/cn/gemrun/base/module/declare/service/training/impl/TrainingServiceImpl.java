package cn.gemrun.base.module.declare.service.training.impl;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.controller.admin.training.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.training.TrainingDO;
import cn.gemrun.base.module.declare.dal.dataobject.training.TrainingRegistrationDO;
import cn.gemrun.base.module.declare.dal.mysql.training.TrainingMapper;
import cn.gemrun.base.module.declare.dal.mysql.training.TrainingRegistrationMapper;
import cn.gemrun.base.module.declare.enums.training.RegistrationStatusEnum;
import cn.gemrun.base.module.declare.enums.training.TrainingStatusEnum;
import cn.gemrun.base.module.declare.enums.training.TrainingTypeEnum;
import cn.gemrun.base.module.declare.enums.training.TargetScopeEnum;
import cn.gemrun.base.module.declare.service.training.TrainingService;
import cn.gemrun.base.module.declare.service.training.TrainingStatisticsRespVO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mzt.logapi.starter.annotation.LogRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 培训活动 Service 实现类
 *
 * @author Gemini
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {

    private final TrainingMapper trainingMapper;
    private final TrainingRegistrationMapper registrationMapper;

    @Override
    @LogRecord(type = TRAINING_TYPE, subType = TRAINING_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}", success = TRAINING_CREATE_SUCCESS)
    public Long createTraining(TrainingSaveReqVO createReqVO) {
        TrainingDO training = BeanUtils.toBean(createReqVO, TrainingDO.class);
        training.setAttachments(createReqVO.getAttachmentUrls());
        training.setPosterUrls(createReqVO.getPosterUrls());
        training.setStatus(TrainingStatusEnum.DRAFT.getCode());
        training.setCurrentParticipants(0);
        trainingMapper.insert(training);
        return training.getId();
    }

    @Override
    @LogRecord(type = TRAINING_TYPE, subType = TRAINING_UPDATE_SUB_TYPE,
            bizNo = "{{#updateReqVO.id}}", success = TRAINING_UPDATE_SUCCESS)
    public void updateTraining(TrainingSaveReqVO updateReqVO) {
        TrainingDO training = trainingMapper.selectById(updateReqVO.getId());
        if (training == null) {
            throw new RuntimeException("活动不存在");
        }
        if (TrainingStatusEnum.REGISTRATION.getCode().equals(training.getStatus())
                || TrainingStatusEnum.IN_PROGRESS.getCode().equals(training.getStatus())) {
            throw new RuntimeException("报名中或进行中的活动不允许修改");
        }
        // 复制所有可编辑字段
        training.setName(updateReqVO.getName());
        training.setType(updateReqVO.getType());
        training.setContent(updateReqVO.getContent());
        training.setOrganizer(updateReqVO.getOrganizer());
        training.setSpeaker(updateReqVO.getSpeaker());
        training.setStartTime(updateReqVO.getStartTime());
        training.setEndTime(updateReqVO.getEndTime());
        training.setLocation(updateReqVO.getLocation());
        training.setOnlineLink(updateReqVO.getOnlineLink());
        training.setTargetScope(updateReqVO.getTargetScope());
        training.setTargetProvinces(updateReqVO.getTargetProvinces());
        training.setRegistrationDeadline(updateReqVO.getRegistrationDeadline());
        training.setMaxParticipants(updateReqVO.getMaxParticipants());
        training.setAttachments(updateReqVO.getAttachmentUrls());
        training.setMeetingMaterials(updateReqVO.getMeetingMaterials());
        training.setPosterUrls(updateReqVO.getPosterUrls());
        training.setRemark(updateReqVO.getRemark());
        trainingMapper.updateById(training);
    }

    @Override
    @LogRecord(type = TRAINING_TYPE, subType = TRAINING_DELETE_SUB_TYPE,
            bizNo = "{{#id}}", success = TRAINING_DELETE_SUCCESS)
    public void deleteTraining(Long id) {
        TrainingDO training = trainingMapper.selectById(id);
        if (training == null) {
            throw new RuntimeException("活动不存在");
        }
        if (TrainingStatusEnum.REGISTRATION.getCode().equals(training.getStatus())
                || TrainingStatusEnum.IN_PROGRESS.getCode().equals(training.getStatus())) {
            throw new RuntimeException("报名中或进行中的活动不允许删除");
        }
        trainingMapper.deleteById(id);
    }

    @Override
    public TrainingRespVO getTraining(Long id) {
        TrainingDO training = trainingMapper.selectById(id);
        return convertToRespVO(training);
    }

    @Override
    public PageResult<TrainingRespVO> getTrainingPage(TrainingPageReqVO pageReqVO) {
        LambdaQueryWrapperX<TrainingDO> wrapper = new LambdaQueryWrapperX<>();
        if (StrUtil.isNotEmpty(pageReqVO.getKeyword())) {
            wrapper.and(w -> w.like(TrainingDO::getName, pageReqVO.getKeyword())
                    .or()
                    .like(TrainingDO::getOrganizer, pageReqVO.getKeyword())
                    .or()
                    .like(TrainingDO::getSpeaker, pageReqVO.getKeyword()));
        }
        if (pageReqVO.getType() != null) {
            wrapper.eq(TrainingDO::getType, pageReqVO.getType());
        }
        if (pageReqVO.getStatus() != null) {
            wrapper.eq(TrainingDO::getStatus, pageReqVO.getStatus());
        }
        if (pageReqVO.getTargetScope() != null) {
            wrapper.eq(TrainingDO::getTargetScope, pageReqVO.getTargetScope());
        }
        wrapper.orderByDesc(TrainingDO::getCreateTime);
        PageResult<TrainingDO> page = trainingMapper.selectPage(pageReqVO, wrapper);
        List<TrainingRespVO> list = page.getList().stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = TRAINING_TYPE, subType = TRAINING_PUBLISH_SUB_TYPE,
            bizNo = "{{#id}}", success = TRAINING_PUBLISH_SUCCESS)
    public void publishTraining(Long id) {
        TrainingDO training = trainingMapper.selectById(id);
        if (training == null) {
            throw new RuntimeException("活动不存在");
        }
        if (!TrainingStatusEnum.DRAFT.getCode().equals(training.getStatus())) {
            throw new RuntimeException("只有草稿状态的活动可以发布");
        }
        training.setStatus(TrainingStatusEnum.REGISTRATION.getCode());
        training.setPublishTime(LocalDateTime.now());
        trainingMapper.updateById(training);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = TRAINING_TYPE, subType = TRAINING_UNPUBLISH_SUB_TYPE,
            bizNo = "{{#id}}", success = TRAINING_UNPUBLISH_SUCCESS)
    public void unpublishTraining(Long id) {
        TrainingDO training = trainingMapper.selectById(id);
        if (training == null) {
            throw new RuntimeException("活动不存在");
        }
        if (!TrainingStatusEnum.REGISTRATION.getCode().equals(training.getStatus())) {
            throw new RuntimeException("只有报名中的活动可以取消发布");
        }
        training.setStatus(TrainingStatusEnum.CANCELLED.getCode());
        trainingMapper.updateById(training);
    }

    @Override
    public List<TrainingRespVO> getPublishedTrainingList() {
        List<TrainingDO> list = trainingMapper.selectList(
                new LambdaQueryWrapperX<TrainingDO>()
                        .eq(TrainingDO::getStatus, TrainingStatusEnum.REGISTRATION.getCode())
                        .orderByDesc(TrainingDO::getStartTime)
        );
        return list.stream().map(this::convertToRespVO).collect(Collectors.toList());
    }

    @Override
    public TrainingRespVO getPublishedTraining(Long id) {
        TrainingDO training = trainingMapper.selectById(id);
        if (training == null || !TrainingStatusEnum.REGISTRATION.getCode().equals(training.getStatus())) {
            throw new RuntimeException("活动不存在或未发布");
        }
        return convertToRespVO(training);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = TRAINING_TYPE, subType = TRAINING_REGISTER_SUB_TYPE,
            bizNo = "{{#registerReqVO.trainingId}}", success = TRAINING_REGISTER_SUCCESS)
    public void registerTraining(RegisterReqVO registerReqVO, Long userId, String userName) {
        TrainingDO training = trainingMapper.selectById(registerReqVO.getTrainingId());
        if (training == null) {
            throw new RuntimeException("活动不存在");
        }
        if (!TrainingStatusEnum.REGISTRATION.getCode().equals(training.getStatus())) {
            throw new RuntimeException("活动不在报名中");
        }
        if (training.getRegistrationDeadline() != null
                && LocalDateTime.now().isAfter(training.getRegistrationDeadline())) {
            throw new RuntimeException("报名已截止");
        }
        if (training.getMaxParticipants() != null) {
            Long currentCount = registrationMapper.selectCountByTrainingId(registerReqVO.getTrainingId());
            if (currentCount >= training.getMaxParticipants()) {
                throw new RuntimeException("报名人数已满");
            }
        }
        TrainingRegistrationDO existReg = registrationMapper.selectByTrainingIdAndUserId(
                registerReqVO.getTrainingId(), userId);
        if (existReg != null && !RegistrationStatusEnum.CANCELLED.getCode().equals(existReg.getStatus())) {
            throw new RuntimeException("您已报名过该活动");
        }
        TrainingRegistrationDO registration = BeanUtils.toBean(registerReqVO, TrainingRegistrationDO.class);
        registration.setUserId(userId);
        registration.setUserName(userName);
        registration.setStatus(RegistrationStatusEnum.REGISTERED.getCode());
        registration.setRegisterTime(LocalDateTime.now());
        if (existReg != null) {
            registration.setId(existReg.getId());
            registration.setUserName(existReg.getUserName());
            registration.setOrganization(existReg.getOrganization());
            registrationMapper.updateById(registration);
        } else {
            registrationMapper.insert(registration);
        }
        training.setCurrentParticipants(
                registrationMapper.selectCountByTrainingId(registerReqVO.getTrainingId()).intValue());
        trainingMapper.updateById(training);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = TRAINING_TYPE, subType = TRAINING_CANCEL_REGISTER_SUB_TYPE,
            bizNo = "{{#trainingId}}", success = TRAINING_CANCEL_REGISTER_SUCCESS)
    public void cancelRegistration(Long trainingId, Long userId) {
        TrainingRegistrationDO registration = registrationMapper.selectByTrainingIdAndUserId(trainingId, userId);
        if (registration == null) {
            throw new RuntimeException("报名记录不存在");
        }
        if (!RegistrationStatusEnum.REGISTERED.getCode().equals(registration.getStatus())) {
            throw new RuntimeException("当前状态不允许取消");
        }
        registration.setStatus(RegistrationStatusEnum.CANCELLED.getCode());
        registration.setCancelTime(LocalDateTime.now());
        registrationMapper.updateById(registration);
        TrainingDO training = trainingMapper.selectById(trainingId);
        if (training != null) {
            training.setCurrentParticipants(
                    registrationMapper.selectCountByTrainingId(trainingId).intValue());
            trainingMapper.updateById(training);
        }
    }

    @Override
    public PageResult<RegistrationRespVO> getMyRegistrations(Long userId, TrainingPageReqVO pageReqVO) {
        LambdaQueryWrapperX<TrainingRegistrationDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(TrainingRegistrationDO::getUserId, userId);
        wrapper.ne(TrainingRegistrationDO::getStatus, RegistrationStatusEnum.CANCELLED.getCode());
        wrapper.orderByDesc(TrainingRegistrationDO::getRegisterTime);
        PageResult<TrainingRegistrationDO> page = registrationMapper.selectPage(pageReqVO, wrapper);
        List<Long> trainingIds = page.getList().stream()
                .map(TrainingRegistrationDO::getTrainingId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        Map<Long, TrainingDO> trainingMap = CollUtil.emptyIfNull(
                trainingMapper.selectList(new LambdaQueryWrapperX<TrainingDO>()
                        .in(CollUtil.isNotEmpty(trainingIds), TrainingDO::getId, trainingIds)))
                .stream().collect(Collectors.toMap(TrainingDO::getId, t -> t));
        List<RegistrationRespVO> result = page.getList().stream()
                .map(reg -> convertToRegistrationRespVO(reg, trainingMap.get(reg.getTrainingId())))
                .collect(Collectors.toList());
        return new PageResult<>(result, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = TRAINING_TYPE, subType = TRAINING_FEEDBACK_SUB_TYPE,
            bizNo = "{{#feedbackReqVO.registrationId}}", success = TRAINING_FEEDBACK_SUCCESS)
    public void submitFeedback(FeedbackReqVO feedbackReqVO, Long userId) {
        TrainingRegistrationDO registration = registrationMapper.selectById(feedbackReqVO.getRegistrationId());
        if (registration == null) {
            throw new RuntimeException("报名记录不存在");
        }
        if (!registration.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此报名记录");
        }
        if (!RegistrationStatusEnum.SIGNED_IN.getCode().equals(registration.getStatus())) {
            throw new RuntimeException("只有已签到才能提交反馈");
        }
        registration.setFeedback(feedbackReqVO.getFeedback());
        registration.setRating(feedbackReqVO.getRating());
        registrationMapper.updateById(registration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = TRAINING_TYPE, subType = TRAINING_SIGN_IN_SUB_TYPE,
            bizNo = "{{#registrationId}}", success = TRAINING_SIGN_IN_SUCCESS)
    public void signIn(Long registrationId) {
        TrainingRegistrationDO registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new RuntimeException("报名记录不存在");
        }
        if (!RegistrationStatusEnum.REGISTERED.getCode().equals(registration.getStatus())) {
            throw new RuntimeException("当前状态不允许签到");
        }
        registration.setStatus(RegistrationStatusEnum.SIGNED_IN.getCode());
        registration.setSignInTime(LocalDateTime.now());
        registrationMapper.updateById(registration);
    }

    @Override
    public PageResult<RegistrationRespVO> getRegistrationPage(RegistrationPageReqVO pageReqVO) {
        LambdaQueryWrapperX<TrainingRegistrationDO> wrapper = new LambdaQueryWrapperX<>();
        if (pageReqVO.getTrainingId() != null) {
            wrapper.eq(TrainingRegistrationDO::getTrainingId, pageReqVO.getTrainingId());
        }
        if (StrUtil.isNotEmpty(pageReqVO.getKeyword())) {
            wrapper.and(w -> w.like(TrainingRegistrationDO::getUserName, pageReqVO.getKeyword())
                    .or()
                    .like(TrainingRegistrationDO::getOrganization, pageReqVO.getKeyword()));
        }
        if (pageReqVO.getStatus() != null) {
            wrapper.eq(TrainingRegistrationDO::getStatus, pageReqVO.getStatus());
        }
        wrapper.orderByDesc(TrainingRegistrationDO::getRegisterTime);
        PageResult<TrainingRegistrationDO> page = registrationMapper.selectPage(pageReqVO, wrapper);
        List<Long> trainingIds = page.getList().stream()
                .map(TrainingRegistrationDO::getTrainingId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        Map<Long, TrainingDO> trainingMap = CollUtil.emptyIfNull(
                trainingMapper.selectList(new LambdaQueryWrapperX<TrainingDO>()
                        .in(CollUtil.isNotEmpty(trainingIds), TrainingDO::getId, trainingIds)))
                .stream().collect(Collectors.toMap(TrainingDO::getId, t -> t));
        List<RegistrationRespVO> list = page.getList().stream()
                .map(reg -> convertToRegistrationRespVO(reg, trainingMap.get(reg.getTrainingId())))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public TrainingStatisticsRespVO getStatistics(Long id) {
        TrainingStatisticsRespVO vo = new TrainingStatisticsRespVO();
        LambdaQueryWrapperX<TrainingRegistrationDO> baseWrapper = new LambdaQueryWrapperX<>();
        baseWrapper.eq(TrainingRegistrationDO::getTrainingId, id);
        vo.setTotalRegistrations(registrationMapper.selectCount(baseWrapper.clone()));
        LambdaQueryWrapperX<TrainingRegistrationDO> signedWrapper = new LambdaQueryWrapperX<>();
        signedWrapper.eq(TrainingRegistrationDO::getTrainingId, id);
        signedWrapper.eq(TrainingRegistrationDO::getStatus, RegistrationStatusEnum.SIGNED_IN.getCode());
        vo.setSignedInCount(registrationMapper.selectCount(signedWrapper));
        LambdaQueryWrapperX<TrainingRegistrationDO> cancelledWrapper = new LambdaQueryWrapperX<>();
        cancelledWrapper.eq(TrainingRegistrationDO::getTrainingId, id);
        cancelledWrapper.eq(TrainingRegistrationDO::getStatus, RegistrationStatusEnum.CANCELLED.getCode());
        vo.setCancelledCount(registrationMapper.selectCount(cancelledWrapper));
        LambdaQueryWrapperX<TrainingRegistrationDO> absentWrapper = new LambdaQueryWrapperX<>();
        absentWrapper.eq(TrainingRegistrationDO::getTrainingId, id);
        absentWrapper.eq(TrainingRegistrationDO::getStatus, RegistrationStatusEnum.ABSENT.getCode());
        vo.setAbsentCount(registrationMapper.selectCount(absentWrapper));
        return vo;
    }

    private TrainingRespVO convertToRespVO(TrainingDO training) {
        if (training == null) {
            return null;
        }
        TrainingRespVO vo = BeanUtils.toBean(training, TrainingRespVO.class);
        vo.setTypeLabel(TrainingTypeEnum.getByCode(training.getType()) != null
                ? TrainingTypeEnum.getByCode(training.getType()).getLabel() : "");
        vo.setStatusLabel(TrainingStatusEnum.getByCode(training.getStatus()) != null
                ? TrainingStatusEnum.getByCode(training.getStatus()).getLabel() : "");
        vo.setTargetScopeLabel(TargetScopeEnum.getByCode(training.getTargetScope()) != null
                ? TargetScopeEnum.getByCode(training.getTargetScope()).getLabel() : "");
        vo.setAttachmentUrls(training.getAttachments());
        vo.setPosterUrls(training.getPosterUrls());
        return vo;
    }

    private RegistrationRespVO convertToRegistrationRespVO(TrainingRegistrationDO reg, TrainingDO training) {
        if (reg == null) {
            return null;
        }
        RegistrationRespVO vo = BeanUtils.toBean(reg, RegistrationRespVO.class);
        vo.setStatusLabel(RegistrationStatusEnum.getByCode(reg.getStatus()) != null
                ? RegistrationStatusEnum.getByCode(reg.getStatus()).getLabel() : "");
        if (training != null) {
            vo.setTrainingName(training.getName());
        }
        return vo;
    }

}
