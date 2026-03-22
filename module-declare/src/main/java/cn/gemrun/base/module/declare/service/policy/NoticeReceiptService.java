package cn.gemrun.base.module.declare.service.policy;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.NoticeReceiptPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.NoticeReceiptRespVO;

import java.util.List;

/**
 * 通知回执 Service 接口
 *
 * @author Gemini
 */
public interface NoticeReceiptService {

    /**
     * 标记已读（创建回执）
     */
    Long markAsRead(Long policyId, Long userId, String userName, String orgName);

    /**
     * 提交反馈
     */
    void submitFeedback(Long receiptId, String feedbackContent);

    /**
     * 取消回执
     */
    void cancelReceipt(Long receiptId);

    /**
     * 获取回执详情
     */
    NoticeReceiptRespVO getReceipt(Long id);

    /**
     * 获取回执分页列表
     */
    PageResult<NoticeReceiptRespVO> getReceiptPage(NoticeReceiptPageReqVO pageReqVO);

    /**
     * 获取某政策的回执列表
     */
    List<NoticeReceiptRespVO> getReceiptListByPolicyId(Long policyId);

    /**
     * 获取当前用户未阅读的通知列表
     */
    List<NoticeReceiptRespVO> getUnreadNotices(Long userId);

    /**
     * 检查用户是否已阅读某政策
     */
    boolean hasRead(Long policyId, Long userId);

}
