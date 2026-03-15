package cn.gemrun.base.module.declare.service.dataflow;

import java.util.*;
import javax.validation.*;
import cn.gemrun.base.module.declare.controller.admin.dataflow.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.dataflow.DataFlowDO;
import cn.gemrun.base.framework.common.pojo.PageResult;

/**
 * 数据流通记录 Service 接口
 *
 * @author
 */
public interface DataFlowService {

    /**
     * 创建数据流通记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDataFlow(@Valid DataFlowSaveReqVO createReqVO);

    /**
     * 更新数据流通记录
     *
     * @param updateReqVO 更新信息
     */
    void updateDataFlow(@Valid DataFlowSaveReqVO updateReqVO);

    /**
     * 删除数据流通记录
     *
     * @param id 编号
     */
    void deleteDataFlow(Long id);

    /**
     * 获得数据流通记录
     *
     * @param id 编号
     * @return 数据流通记录
     */
    DataFlowDO getDataFlow(Long id);

    /**
     * 获得数据流通记录分页
     *
     * @param pageReqVO 分页查询
     * @return 数据流通记录分页
     */
    PageResult<DataFlowDO> getDataFlowPage(DataFlowPageReqVO pageReqVO);

    /**
     * 根据项目ID查询数据流通记录列表
     *
     * @param projectId 项目ID
     * @return 数据流通记录列表
     */
    List<DataFlowDO> getDataFlowListByProjectId(Long projectId);

    /**
     * 提交数据流通记录审核
     *
     * @param id 数据流通ID
     * @param processDefinitionKey 流程定义Key
     * @return 流程实例ID
     */
    String submitDataFlow(Long id, String processDefinitionKey);

    /**
     * 更新数据流通状态（流程事件触发）
     *
     * @param id 数据流通ID
     * @param bizStatus 流程状态
     */
    void updateDataFlowStatus(Long id, String bizStatus);

    /**
     * 更新数据流通的流程实例ID和状态
     *
     * @param id 数据流通ID
     * @param processInstanceId 流程实例ID
     * @param status 状态
     */
    void updateDataFlowProcessInstance(Long id, String processInstanceId, Integer status);

}
