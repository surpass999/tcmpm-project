package cn.gemrun.base.module.declare.service.filing;

import java.util.*;
import javax.validation.*;
import cn.gemrun.base.module.declare.controller.admin.filing.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import cn.gemrun.base.framework.common.pojo.PageResult;

/**
 * 项目备案核心信息 Service 接口
 *
 * @author 芋道源码
 */
public interface FilingService {

    /**
     * 创建项目备案核心信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFiling(@Valid FilingSaveReqVO createReqVO);

    /**
     * 更新项目备案核心信息
     *
     * @param updateReqVO 更新信息
     */
    void updateFiling(@Valid FilingSaveReqVO updateReqVO);

    /**
     * 删除项目备案核心信息
     *
     * @param id 编号
     */
    void deleteFiling(Long id);

    /**
    * 批量删除项目备案核心信息
    *
    * @param ids 编号
    */
    void deleteFilingListByIds(List<Long> ids);

    /**
     * 获得项目备案核心信息
     *
     * @param id 编号
     * @return 项目备案核心信息
     */
    FilingDO getFiling(Long id);

    /**
     * 获得项目备案核心信息分页
     *
     * @param pageReqVO 分页查询
     * @return 项目备案核心信息分页
     */
    PageResult<FilingDO> getFilingPage(FilingPageReqVO pageReqVO);

    /**
     * 更新备案状态（流程事件触发）
     *
     * @param id        备案ID
     * @param bizStatus 流程 DSL 中定义的 bizStatus
     */
    void updateFilingStatus(Long id, String bizStatus);

    /**
     * 发起 BPM 流程
     *
     * @param id 备案ID
     * @param processDefinitionKey 流程定义Key
     * @return 流程实例ID
     */
    String startProcess(Long id, String processDefinitionKey);

    /**
     * 更新备案的流程实例ID和状态（由BPM模块调用）
     *
     * @param id 备案ID
     * @param processInstanceId 流程实例ID
     * @param filingStatus 备案状态
     */
    void updateFilingProcessInstance(Long id, String processInstanceId, String filingStatus);

    /**
     * 自动从备案创建项目
     * 当备案国家局审核通过时，自动创建一个项目记录
     *
     * @param filingId 备案ID
     * @return 创建的项目ID
     */
    Long autoCreateProjectFromFiling(Long filingId);

}