package cn.gemrun.base.module.bpm.api;

import cn.gemrun.base.module.bpm.api.dto.BpmActionRespDTO;

import java.util.List;

/**
 * BPM 按钮定义 Api 接口
 *
 * 用于其他模块或前端获取按钮配置定义
 *
 * @author Gemini
 */
public interface BpmActionApi {

    /**
     * 获取所有按钮定义列表
     *
     * @return 按钮定义列表
     */
    List<BpmActionRespDTO> getAllActions();

    /**
     * 根据 key 获取按钮定义
     *
     * @param key 按钮标识
     * @return 按钮定义，如果不存在返回 null
     */
    BpmActionRespDTO getActionByKey(String key);

}
