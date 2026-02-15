package cn.gemrun.base.module.bpm.dal.mysql.dsl;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.QueryWrapperX;
import cn.gemrun.base.module.bpm.controller.admin.dsl.vo.BpmProcessNodeDslPageReqVO;
import cn.gemrun.base.module.bpm.dal.dataobject.dsl.BpmProcessNodeDslDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * BPM 流程节点 DSL 配置 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface BpmProcessNodeDslMapper extends BaseMapperX<BpmProcessNodeDslDO> {

    default List<BpmProcessNodeDslDO> selectByProcessDefinitionKey(String processDefinitionKey) {
        return selectList(new QueryWrapperX<BpmProcessNodeDslDO>()
                .eq("process_definition_key", processDefinitionKey)
                .eq("deleted", 0)
                .orderByAsc("id"));
    }

    default BpmProcessNodeDslDO selectByProcessDefinitionKeyAndNodeKey(String processDefinitionKey, String nodeKey) {
        return selectOne(new QueryWrapperX<BpmProcessNodeDslDO>()
                .eq("process_definition_key", processDefinitionKey)
                .eq("node_key", nodeKey)
                .eq("deleted", 0));
    }

    default List<BpmProcessNodeDslDO> selectAllEnabled() {
        return selectList(new QueryWrapperX<BpmProcessNodeDslDO>()
                .eq("enabled", 1)
                .eq("deleted", 0)
                .orderByDesc("id"));
    }

    default cn.gemrun.base.framework.common.pojo.PageResult<BpmProcessNodeDslDO> selectPage(BpmProcessNodeDslPageReqVO reqVO) {
        return selectPage(reqVO, new QueryWrapperX<BpmProcessNodeDslDO>()
                .likeIfPresent("process_definition_key", reqVO.getProcessDefinitionKey())
                .likeIfPresent("node_key", reqVO.getNodeKey())
                .likeIfPresent("node_name", reqVO.getNodeName())
                .eqIfPresent("enabled", reqVO.getEnabled())
                .orderByDesc("id"));
    }

}
