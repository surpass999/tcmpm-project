package cn.gemrun.base.module.declare.dal.mysql.process;

import cn.gemrun.base.module.declare.dal.dataobject.process.DeclareBusinessTypeDO;
import org.apache.ibatis.annotations.*;
import java.util.*;

/**
 * 业务类型配置 Mapper
 *
 * @author Gemini
 */
public interface DeclareBusinessTypeMapper {

    /**
     * 根据 businessType 查询配置
     */
    @Select("SELECT * FROM declare_business_type WHERE business_type = #{businessType} AND deleted = 0")
    DeclareBusinessTypeDO selectByBusinessType(@Param("businessType") String businessType);

    /**
     * 根据 category 查询配置列表
     */
    @Select("SELECT * FROM declare_business_type WHERE process_category = #{category} AND deleted = 0 ORDER BY sort")
    List<DeclareBusinessTypeDO> selectByCategory(@Param("category") String category);

    /**
     * 查询所有启用的配置
     */
    @Select("SELECT * FROM declare_business_type WHERE enabled = 1 AND deleted = 0 ORDER BY sort")
    List<DeclareBusinessTypeDO> selectAllEnabled();

    /**
     * 根据 processDefinitionKey 查询配置
     */
    @Select("SELECT * FROM declare_business_type WHERE process_definition_key = #{processKey} AND deleted = 0")
    DeclareBusinessTypeDO selectByProcessKey(@Param("processKey") String processKey);

}
