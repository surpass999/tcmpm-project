package cn.gemrun.base.module.declare.service.filing;

import java.util.*;
import javax.validation.*;
import cn.gemrun.base.module.declare.controller.admin.filing.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.pojo.PageParam;

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

}