package cn.gemrun.base.module.declare.dal.mysql.process;

import cn.gemrun.base.module.declare.dal.dataobject.process.DeclareBusinessProcessDO;
import org.apache.ibatis.annotations.*;
import java.util.*;

/**
 * 业务流程关联 Mapper
 *
 * @author Gemini
 */
public interface DeclareBusinessProcessMapper {

    /**
     * 根据业务类型和业务ID查询
     */
    @Select("SELECT * FROM declare_business_process WHERE business_type = #{businessType} AND business_id = #{businessId} AND deleted = 0")
    DeclareBusinessProcessDO selectByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 根据流程实例ID查询
     */
    @Select("SELECT * FROM declare_business_process WHERE process_instance_id = #{processInstanceId} AND deleted = 0")
    DeclareBusinessProcessDO selectByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    /**
     * 查询业务的所有流程记录
     */
    @Select("SELECT * FROM declare_business_process WHERE business_type = #{businessType} AND business_id = #{businessId} AND deleted = 0 ORDER BY create_time DESC")
    List<DeclareBusinessProcessDO> selectByBusinessId(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 根据发起人ID查询
     */
    @Select("SELECT * FROM declare_business_process WHERE initiator_id = #{initiatorId} AND deleted = 0 ORDER BY create_time DESC")
    List<DeclareBusinessProcessDO> selectByInitiatorId(@Param("initiatorId") Long initiatorId);

}
