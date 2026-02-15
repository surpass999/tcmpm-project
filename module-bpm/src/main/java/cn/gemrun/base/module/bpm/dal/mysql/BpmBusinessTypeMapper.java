package cn.gemrun.base.module.bpm.dal.mysql;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.QueryWrapperX;
import cn.gemrun.base.module.bpm.controller.admin.businessType.vo.BpmBusinessTypePageReqVO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * BPM 业务类型 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface BpmBusinessTypeMapper extends BaseMapperX<BpmBusinessTypeDO> {

    default BpmBusinessTypeDO selectByBusinessType(String businessType) {
        return selectOne(new QueryWrapperX<BpmBusinessTypeDO>()
                .eq("business_type", businessType)
                .eq("deleted", 0));
    }

    default List<BpmBusinessTypeDO> selectByProcessDefinitionKey(String processDefinitionKey) {
        return selectList(new QueryWrapperX<BpmBusinessTypeDO>()
                .eq("process_definition_key", processDefinitionKey)
                .eq("deleted", 0)
                .orderByAsc("sort"));
    }

    default List<BpmBusinessTypeDO> selectByCategory(String category) {
        return selectList(new QueryWrapperX<BpmBusinessTypeDO>()
                .eq("process_category", category)
                .eq("enabled", 1)
                .eq("deleted", 0)
                .orderByAsc("sort"));
    }

    default List<BpmBusinessTypeDO> selectAllEnabled() {
        return selectList(new QueryWrapperX<BpmBusinessTypeDO>()
                .eq("enabled", 1)
                .eq("deleted", 0)
                .orderByAsc("sort"));
    }

    default cn.gemrun.base.framework.common.pojo.PageResult<BpmBusinessTypeDO> selectPage(BpmBusinessTypePageReqVO reqVO) {
        return selectPage(reqVO, new QueryWrapperX<BpmBusinessTypeDO>()
                .likeIfPresent("business_type", reqVO.getBusinessType())
                .likeIfPresent("business_name", reqVO.getBusinessName())
                .likeIfPresent("process_definition_key", reqVO.getProcessDefinitionKey())
                .likeIfPresent("process_category", reqVO.getProcessCategory())
                .eqIfPresent("enabled", reqVO.getEnabled())
                .orderByDesc("id"));
    }

}
