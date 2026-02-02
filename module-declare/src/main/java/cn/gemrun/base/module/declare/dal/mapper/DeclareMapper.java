package cn.gemrun.base.module.declare.dal.mapper;

import cn.gemrun.base.module.declare.dal.DeclareDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for DeclareDO.
 */
@Mapper
public interface DeclareMapper {

    DeclareDO selectById(@Param("id") Long id);

    int insert(DeclareDO declareDO);
}

