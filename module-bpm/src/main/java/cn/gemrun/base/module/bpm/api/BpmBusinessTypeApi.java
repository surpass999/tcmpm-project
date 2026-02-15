package cn.gemrun.base.module.bpm.api;

import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeGetReqDTO;
import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeRespDTO;

/**
 * BPM 业务类型 Api 接口
 *
 * 用于其他模块查询业务类型与流程定义的映射关系
 *
 * @author Gemini
 */
public interface BpmBusinessTypeApi {

    /**
     * 根据业务类型标识获取流程定义Key
     *
     * @param businessType 业务类型标识
     * @return 流程定义Key，如果不存在或未启用返回null
     */
    String getProcessDefinitionKey(String businessType);

    /**
     * 检查业务类型是否配置了流程
     *
     * @param businessType 业务类型标识
     * @return true=已配置，false=未配置
     */
    Boolean hasProcessConfig(String businessType);

    /**
     * 获取流程配置信息
     *
     * @param reqDTO 请求参数
     * @return 流程配置信息
     */
    BpmBusinessTypeRespDTO getProcessConfig(BpmBusinessTypeGetReqDTO reqDTO);

}
