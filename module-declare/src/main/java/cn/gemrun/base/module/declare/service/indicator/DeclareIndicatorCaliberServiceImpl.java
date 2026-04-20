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
        DeclareIndicatorCaliberDO caliber = caliberMapper.selectById(id);
        if (caliber == null) {
            return null;
        }
        // 填充指标代号和名称
        DeclareIndicatorDO indicator = indicatorMapper.selectById(caliber.getIndicatorId());
        if (indicator != null) {
            caliber.setIndicatorCode(indicator.getIndicatorCode());
            caliber.setIndicatorName(indicator.getIndicatorName());
            caliber.setProjectType(indicator.getProjectType());
        }
        return caliber;
    }

    @Override
    public List<DeclareIndicatorCaliberDO> getCaliberList() {
        List<DeclareIndicatorCaliberDO> list = caliberMapper.selectList(new LambdaQueryWrapperX<DeclareIndicatorCaliberDO>()
                .eq(DeclareIndicatorCaliberDO::getStatus, 1));
        if (list == null || list.isEmpty()) {
            return list;
        }
        // 批量填充指标信息
        List<Long> indicatorIds = list.stream()
                .map(DeclareIndicatorCaliberDO::getIndicatorId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, DeclareIndicatorDO> indicatorMap = indicatorMapper.selectBatchIds(indicatorIds).stream()
                .collect(Collectors.toMap(DeclareIndicatorDO::getId, d -> d));
        list.forEach(caliber -> {
            DeclareIndicatorDO ind = indicatorMap.get(caliber.getIndicatorId());
            if (ind != null) {
                caliber.setIndicatorCode(ind.getIndicatorCode());
                caliber.setIndicatorName(ind.getIndicatorName());
                caliber.setProjectType(ind.getProjectType());
            }
        });
        return list;
    }

    @Override
    public PageResult<DeclareIndicatorCaliberDO> getCaliberPage(DeclareIndicatorCaliberPageReqVO pageReqVO) {
        LambdaQueryWrapperX<DeclareIndicatorCaliberDO> wrapper = new LambdaQueryWrapperX<DeclareIndicatorCaliberDO>()
                .likeIfPresent(DeclareIndicatorCaliberDO::getDefinition, pageReqVO.getDefinition())
                .eqIfPresent(DeclareIndicatorCaliberDO::getIndicatorId, pageReqVO.getIndicatorId())
                .eqIfPresent(DeclareIndicatorCaliberDO::getStatus, pageReqVO.getStatus());

        // 如果传了 projectType，先获取该项目类型下的指标ID列表（按指标 sort 排序）
        List<Long> sortedIndicatorIds = null;
        if (pageReqVO.getProjectType() != null) {
            List<DeclareIndicatorDO> indicators = indicatorMapper.selectList(
                    new LambdaQueryWrapperX<DeclareIndicatorDO>()
                            .eq(DeclareIndicatorDO::getProjectType, pageReqVO.getProjectType())
                            .orderByAsc(DeclareIndicatorDO::getSort)
                            .orderByAsc(DeclareIndicatorDO::getId));
            if (indicators == null || indicators.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
            sortedIndicatorIds = indicators.stream()
                    .map(DeclareIndicatorDO::getId)
                    .collect(Collectors.toList());
            wrapper.in(DeclareIndicatorCaliberDO::getIndicatorId, sortedIndicatorIds);
        }

        PageResult<DeclareIndicatorCaliberDO> pageResult = caliberMapper.selectPage(pageReqVO, wrapper);

        // 填充指标信息并按指标 sort 排序
        if (pageResult.getList() != null && !pageResult.getList().isEmpty()) {
            List<Long> resultIndicatorIds = pageResult.getList().stream()
                    .map(DeclareIndicatorCaliberDO::getIndicatorId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, DeclareIndicatorDO> indicatorMap = indicatorMapper.selectBatchIds(resultIndicatorIds).stream()
                    .collect(Collectors.toMap(DeclareIndicatorDO::getId, d -> d));

            pageResult.getList().forEach(caliber -> {
                DeclareIndicatorDO ind = indicatorMap.get(caliber.getIndicatorId());
                if (ind != null) {
                    caliber.setIndicatorName(ind.getIndicatorCode() + " - " + ind.getIndicatorName());
                    caliber.setIndicatorCode(ind.getIndicatorCode());
                    caliber.setProjectType(ind.getProjectType());
                }
            });

            // 按指标的 sort 字段排序（从小到大）
            pageResult.getList().sort((a, b) -> {
                DeclareIndicatorDO indA = indicatorMap.get(a.getIndicatorId());
                DeclareIndicatorDO indB = indicatorMap.get(b.getIndicatorId());
                int sortA = indA != null && indA.getSort() != null ? indA.getSort() : Integer.MAX_VALUE;
                int sortB = indB != null && indB.getSort() != null ? indB.getSort() : Integer.MAX_VALUE;
                if (sortA != sortB) return Integer.compare(sortA, sortB);
                // sort 相同时按 id 排
                return Long.compare(
                        indA != null ? indA.getId() : a.getIndicatorId() != null ? a.getIndicatorId() : 0L,
                        indB != null ? indB.getId() : b.getIndicatorId() != null ? b.getIndicatorId() : 0L
                );
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
        DeclareIndicatorCaliberDO caliber = list.stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .findFirst()
                .orElse(list.get(0));
        // 填充指标信息
        DeclareIndicatorDO indicator = indicatorMapper.selectById(caliber.getIndicatorId());
        if (indicator != null) {
            caliber.setIndicatorCode(indicator.getIndicatorCode());
            caliber.setIndicatorName(indicator.getIndicatorName());
            caliber.setProjectType(indicator.getProjectType());
        }
        return caliber;
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
