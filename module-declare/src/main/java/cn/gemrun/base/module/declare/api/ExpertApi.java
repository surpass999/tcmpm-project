package cn.gemrun.base.module.declare.api;

import cn.gemrun.base.module.declare.api.dto.ExpertRespDTO;

import java.util.List;

/**
 * 专家信息 Api 接口
 *
 * 用于其他模块获取专家信息
 *
 * @author Gemini
 */
public interface ExpertApi {

    /**
     * 根据用户ID获取专家信息
     *
     * @param userId 系统用户ID
     * @return 专家信息，如果不存在返回 null
     */
    ExpertRespDTO getExpertByUserId(Long userId);

    /**
     * 根据用户ID判断是否是专家
     *
     * @param userId 系统用户ID
     * @return true 是专家，false 不是专家
     */
    boolean isExpert(Long userId);

    /**
     * 获取所有专家的userId列表
     *
     * @return 专家用户ID列表
     */
    List<Long> getExpertUserIds();

}
