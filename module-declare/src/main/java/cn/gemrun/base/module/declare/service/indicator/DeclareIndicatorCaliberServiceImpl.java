package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorCaliberPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorCaliberSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorCaliberDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorCaliberMapper;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorMapper;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 指标口径 Service 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class DeclareIndicatorCaliberServiceImpl implements DeclareIndicatorCaliberService {

    @Resource
    private DeclareIndicatorCaliberMapper caliberMapper;

    @Resource
    private DeclareIndicatorMapper indicatorMapper;

    @Override
    public Long createCaliber(DeclareIndicatorCaliberSaveReqVO reqVO) {
        // 检查指标是否存在
        // TODO: 校验指标ID

        DeclareIndicatorCaliberDO caliber = BeanUtils.toBean(reqVO, DeclareIndicatorCaliberDO.class);
        caliberMapper.insert(caliber);
        return caliber.getId();
    }

    @Override
    public void updateCaliber(DeclareIndicatorCaliberSaveReqVO reqVO) {
        // 校验存在
        ValidateCaliberExists(reqVO.getId());

        // 更新
        DeclareIndicatorCaliberDO updateObj = BeanUtils.toBean(reqVO, DeclareIndicatorCaliberDO.class);
        caliberMapper.updateById(updateObj);
    }

    @Override
    public void deleteCaliber(Long id) {
        // 校验存在
        ValidateCaliberExists(id);
        // 删除
        caliberMapper.deleteById(id);
    }

    @Override
    public DeclareIndicatorCaliberDO getCaliber(Long id) {
        return caliberMapper.selectById(id);
    }

    @Override
    public List<DeclareIndicatorCaliberDO> getCaliberList() {
        return caliberMapper.selectList();
    }

    @Override
    public PageResult<DeclareIndicatorCaliberDO> getCaliberPage(DeclareIndicatorCaliberPageReqVO pageReqVO) {
        PageResult<DeclareIndicatorCaliberDO> pageResult = caliberMapper.selectPage(pageReqVO,
                new LambdaQueryWrapperX<DeclareIndicatorCaliberDO>()
                        .likeIfPresent(DeclareIndicatorCaliberDO::getDefinition, pageReqVO.getDefinition())
                        .eqIfPresent(DeclareIndicatorCaliberDO::getIndicatorId, pageReqVO.getIndicatorId())
                        .orderByDesc(DeclareIndicatorCaliberDO::getId));

        // 填充指标名称
        if (pageResult.getList() != null && !pageResult.getList().isEmpty()) {
            List<Long> indicatorIds = pageResult.getList().stream()
                    .map(DeclareIndicatorCaliberDO::getIndicatorId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, String> indicatorNameMap = indicatorMapper.selectBatchIds(indicatorIds).stream()
                    .collect(Collectors.toMap(DeclareIndicatorDO::getId, DeclareIndicatorDO::getIndicatorName));

            pageResult.getList().forEach(caliber -> {
                caliber.setIndicatorName(indicatorNameMap.get(caliber.getIndicatorId()));
            });
        }

        return pageResult;
    }

    @Override
    public DeclareIndicatorCaliberDO getCaliberByIndicatorId(Long indicatorId) {
        return caliberMapper.selectByIndicatorIdSingle(indicatorId);
    }

    private void ValidateCaliberExists(Long id) {
        if (caliberMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.CALIBER_NOT_EXISTS);
        }
    }

}
