package cn.gemrun.base.module.declare.dal.mysql.training;

import org.apache.ibatis.annotations.Mapper;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.training.TrainingRegistrationDO;

/**
 * 活动报名 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface TrainingRegistrationMapper extends BaseMapperX<TrainingRegistrationDO> {

    default TrainingRegistrationDO selectByTrainingIdAndUserId(Long trainingId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<TrainingRegistrationDO>()
                .eq(TrainingRegistrationDO::getTrainingId, trainingId)
                .eq(TrainingRegistrationDO::getUserId, userId)
                .eq(TrainingRegistrationDO::getDeleted, false));
    }

    default Long selectCountByTrainingId(Long trainingId) {
        return selectCount(new LambdaQueryWrapperX<TrainingRegistrationDO>()
                .eq(TrainingRegistrationDO::getTrainingId, trainingId)
                .eq(TrainingRegistrationDO::getStatus, 1) // 已报名
                .eq(TrainingRegistrationDO::getDeleted, false));
    }

}
