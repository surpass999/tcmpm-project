package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorMapper;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 指标体系 Service 实现
 *
 */
@Slf4j
@Service
public class DeclareIndicatorServiceImpl implements DeclareIndicatorService {

    /**
     * 值类型：单选
     */
    private static final Integer VALUE_TYPE_RADIO = 6;
    /**
     * 值类型：多选
     */
    private static final Integer VALUE_TYPE_CHECKBOX = 7;

    @Resource
    private DeclareIndicatorMapper indicatorMapper;

    @Override
    public Long createIndicator(DeclareIndicatorSaveReqVO createReqVO) {
        // 校验指标配置
        validateIndicator(createReqVO);

        // 校验指标代号唯一
        DeclareIndicatorDO existing = indicatorMapper.selectByIndicatorCode(createReqVO.getIndicatorCode());
        if (existing != null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_CODE_EXISTS, createReqVO.getIndicatorCode());
        }

        // 插入
        DeclareIndicatorDO indicator = BeanUtils.toBean(createReqVO, DeclareIndicatorDO.class);
        indicatorMapper.insert(indicator);
        return indicator.getId();
    }

    @Override
    public void updateIndicator(DeclareIndicatorSaveReqVO updateReqVO) {
        // 校验存在
        DeclareIndicatorDO existing = indicatorMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_NOT_EXISTS);
        }

        // 校验指标配置
        validateIndicator(updateReqVO);

        // 校验指标代号唯一
        if (!existing.getIndicatorCode().equals(updateReqVO.getIndicatorCode())) {
            DeclareIndicatorDO sameCode = indicatorMapper.selectByIndicatorCode(updateReqVO.getIndicatorCode());
            if (sameCode != null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_CODE_EXISTS, updateReqVO.getIndicatorCode());
            }
        }

        // 更新
        DeclareIndicatorDO indicator = BeanUtils.toBean(updateReqVO, DeclareIndicatorDO.class);
        indicatorMapper.updateById(indicator);
    }

    /**
     * 校验指标配置
     */
    private void validateIndicator(DeclareIndicatorSaveReqVO reqVO) {
        Integer valueType = reqVO.getValueType();

        // 单选或多选时，选项定义不能为空
        if (VALUE_TYPE_RADIO.equals(valueType) || VALUE_TYPE_CHECKBOX.equals(valueType)) {
            if (!StringUtils.hasText(reqVO.getValueOptions())) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_VALUE_OPTIONS_REQUIRED);
            }
        }
    }

    @Override
    public void deleteIndicator(Long id) {
        // 校验存在
        DeclareIndicatorDO existing = indicatorMapper.selectById(id);
        if (existing == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_NOT_EXISTS);
        }
        // 删除
        indicatorMapper.deleteById(id);
    }

    @Override
    public DeclareIndicatorDO getIndicator(Long id) {
        return indicatorMapper.selectById(id);
    }

    @Override
    public PageResult<DeclareIndicatorDO> getIndicatorPage(DeclareIndicatorPageReqVO pageReqVO) {
        return indicatorMapper.selectPage(pageReqVO);
    }

    @Override
    public List<DeclareIndicatorDO> getIndicatorsByProjectType(Integer projectType, String businessType) {
        return indicatorMapper.selectByProjectTypeAndBusinessType(projectType, businessType);
    }

    @Override
    public List<DeclareIndicatorDO> getIndicatorsByBusinessType(String businessType) {
        return indicatorMapper.selectByBusinessType(businessType);
    }

    @Override
    public DeclareIndicatorDO getIndicatorByCode(String indicatorCode) {
        return indicatorMapper.selectByIndicatorCode(indicatorCode);
    }

}
