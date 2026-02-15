package cn.gemrun.base.module.bpm.service.definition;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.bpm.controller.admin.dsl.vo.BpmProcessNodeDslPageReqVO;
import cn.gemrun.base.module.bpm.controller.admin.dsl.vo.BpmProcessNodeDslSaveReqVO;
import cn.gemrun.base.module.bpm.dal.dataobject.dsl.BpmProcessNodeDslDO;

import javax.validation.Valid;
import java.util.List;

/**
 * BPM 流程节点 DSL 配置 Service 接口
 *
 * @author Gemini
 */
public interface BpmProcessNodeDslService {

    /**
     * 创建 DSL 配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDsl(@Valid BpmProcessNodeDslSaveReqVO createReqVO);

    /**
     * 更新 DSL 配置
     *
     * @param updateReqVO 更新信息
     */
    void updateDsl(@Valid BpmProcessNodeDslSaveReqVO updateReqVO);

    /**
     * 删除 DSL 配置
     *
     * @param id 编号
     */
    void deleteDsl(Long id);

    /**
     * 获得 DSL 配置
     *
     * @param id 编号
     * @return DSL 配置
     */
    BpmProcessNodeDslDO getDsl(Long id);

    /**
     * 根据流程定义Key和节点Key获取 DSL 配置
     *
     * @param processDefinitionKey 流程定义Key
     * @param nodeKey 节点Key
     * @return DSL 配置
     */
    BpmProcessNodeDslDO getDslByNodeKey(String processDefinitionKey, String nodeKey);

    /**
     * 根据流程定义Key获取所有 DSL 配置
     *
     * @param processDefinitionKey 流程定义Key
     * @return DSL 配置列表
     */
    List<BpmProcessNodeDslDO> getDslListByProcessKey(String processDefinitionKey);

    /**
     * 获取所有已启用的 DSL 配置
     *
     * @return DSL 配置列表
     */
    List<BpmProcessNodeDslDO> getAllEnabledDslList();

    /**
     * 获取 DSL 配置分页
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<BpmProcessNodeDslDO> getDslPage(BpmProcessNodeDslPageReqVO pageReqVO);

    /**
     * 验证 DSL 配置 JSON
     *
     * @param dslConfigJson DSL 配置 JSON 字符串
     * @throws IllegalArgumentException 如果配置无效
     */
    void validateDslConfig(String dslConfigJson);

}
