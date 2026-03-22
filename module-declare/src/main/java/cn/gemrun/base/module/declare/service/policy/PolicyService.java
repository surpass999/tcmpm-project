package cn.gemrun.base.module.declare.service.policy;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicyPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicyRespVO;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicySaveReqVO;

import java.util.List;

/**
 * 政策通知 Service 接口
 *
 * @author Gemini
 */
public interface PolicyService {

    /**
     * 创建政策通知
     */
    Long createPolicy(PolicySaveReqVO createReqVO);

    /**
     * 更新政策通知
     */
    void updatePolicy(PolicySaveReqVO updateReqVO);

    /**
     * 删除政策通知
     */
    void deletePolicy(Long id);

    /**
     * 获取政策通知详情
     */
    PolicyRespVO getPolicy(Long id);

    /**
     * 获取政策通知分页列表
     */
    PageResult<PolicyRespVO> getPolicyPage(PolicyPageReqVO pageReqVO);

    /**
     * 发布政策通知
     */
    void publishPolicy(Long id);

    /**
     * 下架政策通知
     */
    void unpublishPolicy(Long id);

    /**
     * 获取已发布的政策列表（用于通知推送）
     */
    List<PolicyRespVO> getPublishedPolicyList();

}
