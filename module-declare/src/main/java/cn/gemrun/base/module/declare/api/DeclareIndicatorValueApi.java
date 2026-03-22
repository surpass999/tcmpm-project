package cn.gemrun.base.module.declare.api;

import cn.gemrun.base.module.declare.api.dto.DeclareIndicatorValueGetReqDTO;
import cn.gemrun.base.module.declare.api.dto.DeclareIndicatorValueRespDTO;

import java.util.List;
import java.util.Map;

/**
 * 指标值 API 接口
 *
 * 供其他模块查询业务指标值
 *
 * @author Gemini
 */
public interface DeclareIndicatorValueApi {

    /**
     * 获取业务的所有指标值
     *
     * @param reqDTO 请求参数
     * @return 指标值列表
     */
    List<DeclareIndicatorValueRespDTO> getIndicatorValues(DeclareIndicatorValueGetReqDTO reqDTO);

    /**
     * 获取业务的所有指标值（Map形式）
     *
     * @param reqDTO 请求参数
     * @return 指标值 Map（key 为 indicatorCode）
     */
    Map<String, DeclareIndicatorValueRespDTO> getIndicatorValueMap(DeclareIndicatorValueGetReqDTO reqDTO);

}
