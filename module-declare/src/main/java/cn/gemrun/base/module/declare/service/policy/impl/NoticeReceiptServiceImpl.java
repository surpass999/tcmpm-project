package cn.gemrun.base.module.declare.service.policy.impl;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.NoticeReceiptPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.NoticeReceiptRespVO;
import cn.gemrun.base.module.declare.dal.dataobject.policy.NoticeReceiptDO;
import cn.gemrun.base.module.declare.dal.dataobject.policy.PolicyDO;
import cn.gemrun.base.module.declare.dal.mysql.policy.NoticeReceiptMapper;
import cn.gemrun.base.module.declare.dal.mysql.policy.PolicyMapper;
import cn.gemrun.base.module.declare.enums.PolicyEnums;
import cn.gemrun.base.module.declare.service.policy.NoticeReceiptService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知回执 Service 实现类
 *
 * @author Gemini
 */
@Service
@RequiredArgsConstructor
public class NoticeReceiptServiceImpl implements NoticeReceiptService {

    private final NoticeReceiptMapper noticeReceiptMapper;
    private final PolicyMapper policyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long markAsRead(Long policyId, Long userId, String userName, String orgName) {
        // 检查是否已存在回执
        NoticeReceiptDO existing = noticeReceiptMapper.selectByPolicyIdAndReaderId(policyId, userId);
        if (existing != null) {
            // 已存在，更新为已读状态
            existing.setReadStatus(PolicyEnums.ReadStatus.READ.getValue());
            existing.setReceiptStatus(PolicyEnums.ReceiptStatus.READ.getValue());
            existing.setReadTime(LocalDateTime.now());
            noticeReceiptMapper.updateById(existing);
            return existing.getId();
        }

        // 创建新的回执
        NoticeReceiptDO receipt = NoticeReceiptDO.builder()
                .policyId(policyId)
                .readerId(userId)
                .readerName(userName)
                .orgName(orgName)
                .readTime(LocalDateTime.now())
                .readStatus(PolicyEnums.ReadStatus.READ.getValue())
                .receiptStatus(PolicyEnums.ReceiptStatus.READ.getValue())
                .build();

        noticeReceiptMapper.insert(receipt);
        return receipt.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFeedback(Long receiptId, String feedbackContent) {
        NoticeReceiptDO receipt = noticeReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new RuntimeException("回执不存在");
        }

        receipt.setFeedbackContent(feedbackContent);
        receipt.setFeedbackTime(LocalDateTime.now());
        receipt.setReceiptStatus(PolicyEnums.ReceiptStatus.FEEDBACK.getValue());

        noticeReceiptMapper.updateById(receipt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReceipt(Long receiptId) {
        NoticeReceiptDO receipt = noticeReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new RuntimeException("回执不存在");
        }

        receipt.setReceiptStatus(PolicyEnums.ReceiptStatus.CANCELLED.getValue());
        noticeReceiptMapper.updateById(receipt);
    }

    @Override
    public NoticeReceiptRespVO getReceipt(Long id) {
        NoticeReceiptDO receipt = noticeReceiptMapper.selectById(id);
        if (receipt == null) {
            return null;
        }
        return convertToRespVO(receipt);
    }

    @Override
    public PageResult<NoticeReceiptRespVO> getReceiptPage(NoticeReceiptPageReqVO pageReqVO) {
        PageResult<NoticeReceiptDO> page = noticeReceiptMapper.selectPage(pageReqVO);
        List<NoticeReceiptRespVO> list = page.getList().stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public List<NoticeReceiptRespVO> getReceiptListByPolicyId(Long policyId) {
        List<NoticeReceiptDO> list = noticeReceiptMapper.selectByPolicyId(policyId);
        return list.stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NoticeReceiptRespVO> getUnreadNotices(Long userId) {
        List<NoticeReceiptDO> receipts = noticeReceiptMapper.selectByReaderId(userId);
        
        // 获取已读的通知ID
        List<Long> readPolicyIds = receipts.stream()
                .filter(r -> PolicyEnums.ReceiptStatus.READ.getValue().equals(r.getReceiptStatus()) 
                        || PolicyEnums.ReceiptStatus.FEEDBACK.getValue().equals(r.getReceiptStatus()))
                .map(NoticeReceiptDO::getPolicyId)
                .collect(Collectors.toList());

        // 查询所有已发布且用户未读的政策
        List<PolicyDO> allPublished = policyMapper.selectPublishedList();
        List<PolicyDO> unreadPolicies = allPublished.stream()
                .filter(p -> !readPolicyIds.contains(p.getId()))
                .collect(Collectors.toList());

        return unreadPolicies.stream()
                .map(p -> {
                    NoticeReceiptRespVO respVO = new NoticeReceiptRespVO();
                    respVO.setPolicyId(p.getId());
                    respVO.setPolicyTitle(p.getPolicyTitle());
                    respVO.setCreateTime(p.getCreateTime());
                    return respVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasRead(Long policyId, Long userId) {
        NoticeReceiptDO receipt = noticeReceiptMapper.selectByPolicyIdAndReaderId(policyId, userId);
        return receipt != null && PolicyEnums.ReadStatus.READ.getValue().equals(receipt.getReadStatus());
    }

    /**
     * 转换为响应VO
     */
    private NoticeReceiptRespVO convertToRespVO(NoticeReceiptDO receipt) {
        NoticeReceiptRespVO respVO = BeanUtils.toBean(receipt, NoticeReceiptRespVO.class);
        
        // 填充政策标题
        if (receipt.getPolicyId() != null) {
            PolicyDO policy = policyMapper.selectById(receipt.getPolicyId());
            if (policy != null) {
                respVO.setPolicyTitle(policy.getPolicyTitle());
            }
        }
        
        return respVO;
    }

}
