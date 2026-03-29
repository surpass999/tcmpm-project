package cn.gemrun.base.module.declare.dal.mysql;

import cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagDO;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医院标签Mapper
 *
 * @author Gemini
 */
@Mapper
public interface HospitalTagMapper extends BaseMapperX<HospitalTagDO> {
}
