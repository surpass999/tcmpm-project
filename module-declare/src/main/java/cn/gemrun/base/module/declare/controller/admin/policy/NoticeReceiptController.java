package cn.gemrun.base.module.declare.controller.admin.policy;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.NoticeReceiptPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.NoticeReceiptRespVO;
import cn.gemrun.base.module.declare.service.policy.NoticeReceiptService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知回执 Controller
 *
 * @author Gemini
 */
@RestController
@RequestMapping("/declare/notice-receipt")
@RequiredArgsConstructor
@Validated
public class NoticeReceiptController {

    private final NoticeReceiptService noticeReceiptService;

    /**
     * 标记已读
     */
    @PostMapping("/read")
    public CommonResult<Long> markAsRead(@RequestParam("policyId") Long policyId,
                          @RequestParam("userId") Long userId,
                          @RequestParam("userName") String userName,
                          @RequestParam(value = "orgName", required = false) String orgName) {
        return CommonResult.success(noticeReceiptService.markAsRead(policyId, userId, userName, orgName));
    }

    /**
     * 提交反馈
     */
    @PostMapping("/feedback")
    public CommonResult<Boolean> submitFeedback(@RequestParam("receiptId") Long receiptId,
                              @RequestParam("feedbackContent") String feedbackContent) {
        noticeReceiptService.submitFeedback(receiptId, feedbackContent);
        return CommonResult.success(true);
    }

    /**
     * 取消回执
     */
    @PostMapping("/cancel")
    public CommonResult<Boolean> cancelReceipt(@RequestParam("receiptId") Long receiptId) {
        noticeReceiptService.cancelReceipt(receiptId);
        return CommonResult.success(true);
    }

    /**
     * 获取回执详情
     */
    @GetMapping("/get")
    public CommonResult<NoticeReceiptRespVO> getReceipt(@RequestParam("id") Long id) {
        return CommonResult.success(noticeReceiptService.getReceipt(id));
    }

    /**
     * 获取回执分页列表
     */
    @GetMapping("/page")
    public CommonResult<PageResult<NoticeReceiptRespVO>> getReceiptPage(@Valid NoticeReceiptPageReqVO pageReqVO) {
        return CommonResult.success(noticeReceiptService.getReceiptPage(pageReqVO));
    }

    /**
     * 获取某政策的回执列表
     */
    @GetMapping("/list-by-policy")
    public CommonResult<List<NoticeReceiptRespVO>> getReceiptListByPolicyId(@RequestParam("policyId") Long policyId) {
        return CommonResult.success(noticeReceiptService.getReceiptListByPolicyId(policyId));
    }

    /**
     * 获取当前用户未阅读的通知列表
     */
    @GetMapping("/my-unread")
    public CommonResult<List<NoticeReceiptRespVO>> getMyUnreadNotices(@RequestParam("userId") Long userId) {
        return CommonResult.success(noticeReceiptService.getUnreadNotices(userId));
    }

    /**
     * 检查用户是否已阅读某政策
     */
    @GetMapping("/has-read")
    public CommonResult<Boolean> hasRead(@RequestParam("policyId") Long policyId, @RequestParam("userId") Long userId) {
        return CommonResult.success(noticeReceiptService.hasRead(policyId, userId));
    }

}
