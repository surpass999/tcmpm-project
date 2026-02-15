package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.module.declare.controller.admin.indicator.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.framework.common.pojo.PageResult;

import java.util.List;

/**
 * 指标体系 Service 接口
 *
 * @author Gemini
 */
public interface DeclareIndicatorService {

    /**
     * 创建指标
     */
    Long createIndicator(DeclareIndicatorSaveReqVO createReqVO);

    /**
     * 更新指标
     */
    void updateIndicator(DeclareIndicatorSaveReqVO updateReqVO);

    /**
     * 删除指标
     */
    void deleteIndicator(Long id);

    /**
     * 获取指标
     */
    DeclareIndicatorDO getIndicator(Long id);

    /**
     * 获取指标分页
     */
    PageResult<DeclareIndicatorDO> getIndicatorPage(DeclareIndicatorPageReqVO pageReqVO);

    /**
     * 根据项目类型和业务类型获取指标列表
     */
    List<DeclareIndicatorDO> getIndicatorsByProjectType(Integer projectType, String businessType);

    /**
     * 根据业务类型获取指标列表
     */
    List<DeclareIndicatorDO> getIndicatorsByBusinessType(String businessType);

    /**
     * 根据指标代号获取指标
     */
    DeclareIndicatorDO getIndicatorByCode(String indicatorCode);

}
