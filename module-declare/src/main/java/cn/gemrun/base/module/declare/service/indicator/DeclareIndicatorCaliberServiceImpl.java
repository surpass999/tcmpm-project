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
import java.util.Collections;
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
        return caliberMapper.selectList(new LambdaQueryWrapperX<DeclareIndicatorCaliberDO>()
                .eq(DeclareIndicatorCaliberDO::getStatus, 1));
    }

    @Override
    public PageResult<DeclareIndicatorCaliberDO> getCaliberPage(DeclareIndicatorCaliberPageReqVO pageReqVO) {
        LambdaQueryWrapperX<DeclareIndicatorCaliberDO> wrapper = new LambdaQueryWrapperX<DeclareIndicatorCaliberDO>()
                .likeIfPresent(DeclareIndicatorCaliberDO::getDefinition, pageReqVO.getDefinition())
                .eqIfPresent(DeclareIndicatorCaliberDO::getIndicatorId, pageReqVO.getIndicatorId())
                .eqIfPresent(DeclareIndicatorCaliberDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(DeclareIndicatorCaliberDO::getId);

        // 如果传了 projectType，先获取该项目类型下的指标ID列表
        if (pageReqVO.getProjectType() != null) {
            List<DeclareIndicatorDO> indicators = indicatorMapper.selectList(
                    new LambdaQueryWrapperX<DeclareIndicatorDO>()
                            .eq(DeclareIndicatorDO::getProjectType, pageReqVO.getProjectType()));
            if (indicators == null || indicators.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
            List<Long> indicatorIds = indicators.stream()
                    .map(DeclareIndicatorDO::getId)
                    .collect(Collectors.toList());
            wrapper.in(DeclareIndicatorCaliberDO::getIndicatorId, indicatorIds);
        }

        PageResult<DeclareIndicatorCaliberDO> pageResult = caliberMapper.selectPage(pageReqVO, wrapper);

        // 填充指标名称
        if (pageResult.getList() != null && !pageResult.getList().isEmpty()) {
            List<Long> resultIndicatorIds = pageResult.getList().stream()
                    .map(DeclareIndicatorCaliberDO::getIndicatorId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, String> indicatorNameMap = indicatorMapper.selectBatchIds(resultIndicatorIds).stream()
                    .collect(Collectors.toMap(DeclareIndicatorDO::getId, DeclareIndicatorDO::getIndicatorName));

            pageResult.getList().forEach(caliber -> {
                caliber.setIndicatorName(indicatorNameMap.get(caliber.getIndicatorId()));
            });
        }

        return pageResult;
    }

    @Override
    public DeclareIndicatorCaliberDO getCaliberByIndicatorId(Long indicatorId) {
        List<DeclareIndicatorCaliberDO> list = caliberMapper.selectByIndicatorId(indicatorId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        // 优先返回启用状态的，否则取第一条（兜底脏数据情况）
        return list.stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .findFirst()
                .orElse(list.get(0));
    }

    @Override
    public void updateCaliberStatus(Long id, Integer status) {
        ValidateCaliberExists(id);
        DeclareIndicatorCaliberDO updateObj = new DeclareIndicatorCaliberDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        caliberMapper.updateById(updateObj);
    }

    private void ValidateCaliberExists(Long id) {
        if (caliberMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.CALIBER_NOT_EXISTS);
        }
    }

}
