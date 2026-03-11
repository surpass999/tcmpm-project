package cn.gemrun.base.module.bpm.dal.mysql.process;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.QueryWrapperX;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BPM 业务-流程关联 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface BpmBusinessProcessMapper extends BaseMapperX<BpmBusinessProcessDO> {

    default BpmBusinessProcessDO selectByBusiness(String businessType, Long businessId) {
        return selectOne(new QueryWrapperX<BpmBusinessProcessDO>()
                .eq("business_type", businessType)
                .eq("business_id", businessId)
                .eq("deleted", 0));
    }

    default BpmBusinessProcessDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(new QueryWrapperX<BpmBusinessProcessDO>()
                .eq("process_instance_id", processInstanceId)
                .eq("deleted", 0));
    }

    default List<BpmBusinessProcessDO> selectByBusinessType(String businessType) {
        return selectList(new QueryWrapperX<BpmBusinessProcessDO>()
                .eq("business_type", businessType)
                .eq("deleted", 0));
    }

    default List<BpmBusinessProcessDO> selectByBusinessIds(String businessType, List<Long> businessIds) {
        return selectList(new QueryWrapperX<BpmBusinessProcessDO>()
                .eq("business_type", businessType)
                .in("business_id", businessIds)
                .eq("deleted", 0));
    }

    default List<BpmBusinessProcessDO> selectByProcessInstanceIds(List<String> processInstanceIds) {
        return selectList(new QueryWrapperX<BpmBusinessProcessDO>()
                .in("process_instance_id", processInstanceIds)
                .eq("deleted", 0));
    }

    /**
     * 根据业务类型和用户ID，查询该用户参与的业务ID列表
     * 用于数据权限过滤：发起人 OR 参与人
     *
     * @param businessType 业务类型（如 declare:filing:submit）
     * @param userId      用户ID
     * @return 参与的业务ID列表
     */
    default List<Long> selectBusinessIdsByUserId(String businessType, Long userId) {
        // initiator_ids 格式：,1,2,3,  即逗号分隔，首尾有逗号
        String likePattern = "%," + userId + ",%";
        List<BpmBusinessProcessDO> processes = selectList(new QueryWrapperX<BpmBusinessProcessDO>()
                .eq("business_type", businessType)
                .like("initiator_ids", likePattern)
                .eq("deleted", 0));
        return processes.stream()
                .map(BpmBusinessProcessDO::getBusinessId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 根据多个业务类型和用户ID列表，查询用户参与的业务ID列表
     * 用于数据权限过滤：根据 @BpmProcessQuery 注解查询
     *
     * @param businessTypes 业务类型列表
     * @param userId       用户ID
     * @return 参与的业务ID列表
     */
    default List<Long> selectBusinessIdsByUserIdAndBusinessTypes(List<String> businessTypes, Long userId) {
        if (businessTypes == null || businessTypes.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 使用 FIND_IN_SET 精确匹配逗号分隔的 ID 列表，避免 like 匹配到 1433 的问题
        // initiator_ids 格式：144,143 或 144,143,145
        // 组合条件：initiator_id = userId OR FIND_IN_SET(userId, initiator_ids) > 0
        List<BpmBusinessProcessDO> processes = selectList(new QueryWrapperX<BpmBusinessProcessDO>()
                .in("business_type", businessTypes)
                .and(w -> w.eq("initiator_id", userId)
                        .or()
                        .apply("FIND_IN_SET(" + userId + ", initiator_ids) > 0"))
                .eq("deleted", 0));
        return processes.stream()
                .map(BpmBusinessProcessDO::getBusinessId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 根据有权限的业务ID列表，查询流程数据
     * 用于数据权限过滤：结合 @BpmProcessQuery 注解的查询结果
     *
     * @param businessType   业务类型
     * @param businessIds   有权限的业务ID列表（从 @BpmProcessQuery 注解获取）
     * @return 流程数据列表
     */
    default List<BpmBusinessProcessDO> selectByAuthorizedBusinessIds(String businessType, List<Long> businessIds) {
        if (businessIds == null || businessIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new QueryWrapperX<BpmBusinessProcessDO>()
                .eq("business_type", businessType)
                .in("business_id", businessIds)
                .eq("deleted", 0));
    }

}
