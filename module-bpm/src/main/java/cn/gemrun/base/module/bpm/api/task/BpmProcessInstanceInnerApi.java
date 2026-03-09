package cn.gemrun.base.module.bpm.api.task;

import cn.gemrun.base.module.bpm.api.task.dto.BpmApprovalDetailReqDTO;
import cn.gemrun.base.module.bpm.api.task.dto.BpmApprovalDetailRespDTO;

import javax.validation.Valid;

/**
 * 流程实例内部 API 接口
 *
 * 用于其他模块获取审批详情等内部功能，无需权限校验
 *
 * @author Gemini
 */
public interface BpmProcessInstanceInnerApi {

    /**
     * 获取审批详情（内部接口，无权限校验）
     *
     * @param userId 用户ID
     * @param reqDTO 请求参数
     * @return 审批详情
     */
    BpmApprovalDetailRespDTO getApprovalDetail(Long userId, @Valid BpmApprovalDetailReqDTO reqDTO);

}
