package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorCaliberPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorCaliberSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorCaliberDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 指标口径 Service 接口
 *
 * @author Gemini
 */
public interface DeclareIndicatorCaliberService {

    /**
     * 创建指标口径
     */
    Long createCaliber(@Valid DeclareIndicatorCaliberSaveReqVO reqVO);

    /**
     * 更新指标口径
     */
    void updateCaliber(@Valid DeclareIndicatorCaliberSaveReqVO reqVO);

    /**
     * 删除指标口径
     */
    void deleteCaliber(Long id);

    /**
     * 获取指标口径
     */
    DeclareIndicatorCaliberDO getCaliber(Long id);

    /**
     * 获取指标口径列表
     */
    List<DeclareIndicatorCaliberDO> getCaliberList();

    /**
     * 获取指标口径分页
     */
    PageResult<DeclareIndicatorCaliberDO> getCaliberPage(DeclareIndicatorCaliberPageReqVO pageReqVO);

    /**
     * 根据指标ID获取口径
     */
    DeclareIndicatorCaliberDO getCaliberByIndicatorId(Long indicatorId);

}
