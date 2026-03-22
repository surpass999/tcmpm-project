package cn.gemrun.base.module.declare.dal.mysql.training;

import org.apache.ibatis.annotations.Mapper;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.training.TrainingDO;

/**
 * 培训交流活动 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface TrainingMapper extends BaseMapperX<TrainingDO> {

    default TrainingDO selectByIdAndDeleted(Long id) {
        return selectOne(new LambdaQueryWrapperX<TrainingDO>()
                .eq(TrainingDO::getId, id)
                .eq(TrainingDO::getDeleted, false));
    }

}
