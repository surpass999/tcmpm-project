package cn.gemrun.base.module.declare.api;

import cn.gemrun.base.module.declare.api.dto.DeclareIndicatorValueGetReqDTO;
import cn.gemrun.base.module.declare.api.dto.DeclareIndicatorValueRespDTO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorValueService;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 指标值 API 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class DeclareIndicatorValueApiImpl implements DeclareIndicatorValueApi {

    @Resource
    private DeclareIndicatorValueService indicatorValueService;

    @Override
    /**
     * 获取业务的所有指标值
     */
    public List<DeclareIndicatorValueRespDTO> getIndicatorValues(DeclareIndicatorValueGetReqDTO reqDTO) {
        List<DeclareIndicatorValueDO> values = indicatorValueService.getIndicatorValues(
                reqDTO.getBusinessType(), reqDTO.getBusinessId());
        return BeanUtils.toBean(values, DeclareIndicatorValueRespDTO.class);
    }

    @Override
    /**
     * 获取业务的所有指标值（Map形式）
     */
    public Map<String, DeclareIndicatorValueRespDTO> getIndicatorValueMap(DeclareIndicatorValueGetReqDTO reqDTO) {
        Map<String, DeclareIndicatorValueDO> values = indicatorValueService.getIndicatorValueMap(
                reqDTO.getBusinessType(), reqDTO.getBusinessId());
        return values.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> BeanUtils.toBean(entry.getValue(), DeclareIndicatorValueRespDTO.class)
                ));
    }

}
