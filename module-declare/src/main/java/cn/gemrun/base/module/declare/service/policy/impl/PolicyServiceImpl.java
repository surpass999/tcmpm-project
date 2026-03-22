package cn.gemrun.base.module.declare.service.policy.impl;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicyPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicyRespVO;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicySaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.policy.PolicyDO;
import cn.gemrun.base.module.declare.dal.mysql.policy.PolicyMapper;
import cn.gemrun.base.module.declare.enums.PolicyEnums;
import cn.gemrun.base.module.declare.service.policy.PolicyService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mzt.logapi.starter.annotation.LogRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 政策通知 Service 实现类
 *
 * @author Gemini
 */
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyMapper policyMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = POLICY_TYPE, subType = POLICY_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}", success = POLICY_CREATE_SUCCESS)
    public Long createPolicy(PolicySaveReqVO createReqVO) {
        // 转换VO为DO
        PolicyDO policyDO = BeanUtils.toBean(createReqVO, PolicyDO.class);
        
        // 处理附件URL
        if (createReqVO.getAttachmentUrls() != null && !createReqVO.getAttachmentUrls().isEmpty()) {
            policyDO.setAttachments(String.join(",", createReqVO.getAttachmentUrls()));
        } else {
            policyDO.setAttachments("");
        }
        
        // 设置发布时间（如果未指定，则使用当前时间）
        if (createReqVO.getReleaseTime() != null && !createReqVO.getReleaseTime().isEmpty()) {
            policyDO.setReleaseTime(LocalDateTime.parse(createReqVO.getReleaseTime(), DATE_TIME_FORMATTER));
        } else {
            policyDO.setReleaseTime(LocalDateTime.now());
        }
        
        // 默认状态为下架（待发布）
        policyDO.setStatus(PolicyEnums.Status.UNPUBLISHED.getValue());
        
        // 插入数据
        policyMapper.insert(policyDO);
        return policyDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = POLICY_TYPE, subType = POLICY_UPDATE_SUB_TYPE,
            bizNo = "{{#updateReqVO.id}}", success = POLICY_UPDATE_SUCCESS)
    public void updatePolicy(PolicySaveReqVO updateReqVO) {
        PolicyDO existingPolicy = policyMapper.selectById(updateReqVO.getId());
        if (existingPolicy == null) {
            throw new RuntimeException("政策不存在");
        }
        
        // 更新DO
        PolicyDO policyDO = BeanUtils.toBean(updateReqVO, PolicyDO.class);
        
        // 处理附件URL
        if (updateReqVO.getAttachmentUrls() != null && !updateReqVO.getAttachmentUrls().isEmpty()) {
            policyDO.setAttachments(String.join(",", updateReqVO.getAttachmentUrls()));
        } else {
            policyDO.setAttachments("");
        }
        
        // 处理发布时间
        if (updateReqVO.getReleaseTime() != null && !updateReqVO.getReleaseTime().isEmpty()) {
            policyDO.setReleaseTime(LocalDateTime.parse(updateReqVO.getReleaseTime(), DATE_TIME_FORMATTER));
        }
        
        policyMapper.updateById(policyDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = POLICY_TYPE, subType = POLICY_DELETE_SUB_TYPE,
            bizNo = "{{#id}}", success = POLICY_DELETE_SUCCESS)
    public void deletePolicy(Long id) {
        policyMapper.deleteById(id);
    }

    @Override
    public PolicyRespVO getPolicy(Long id) {
        PolicyDO policyDO = policyMapper.selectById(id);
        if (policyDO == null) {
            return null;
        }
        return convertToRespVO(policyDO);
    }

    @Override
    public PageResult<PolicyRespVO> getPolicyPage(PolicyPageReqVO pageReqVO) {
        PageResult<PolicyDO> page = policyMapper.selectPage(pageReqVO);
        List<PolicyRespVO> list = page.getList().stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = POLICY_TYPE, subType = POLICY_PUBLISH_SUB_TYPE,
            bizNo = "{{#id}}", success = POLICY_PUBLISH_SUCCESS)
    public void publishPolicy(Long id) {
        PolicyDO policyDO = policyMapper.selectById(id);
        if (policyDO == null) {
            throw new RuntimeException("政策不存在");
        }
        
        policyDO.setStatus(PolicyEnums.Status.PUBLISHED.getValue());
        policyDO.setReleaseTime(LocalDateTime.now());
        policyMapper.updateById(policyDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = POLICY_TYPE, subType = POLICY_UNPUBLISH_SUB_TYPE,
            bizNo = "{{#id}}", success = POLICY_UNPUBLISH_SUCCESS)
    public void unpublishPolicy(Long id) {
        PolicyDO policyDO = policyMapper.selectById(id);
        if (policyDO == null) {
            throw new RuntimeException("政策不存在");
        }
        
        policyDO.setStatus(PolicyEnums.Status.UNPUBLISHED.getValue());
        policyMapper.updateById(policyDO);
    }

    @Override
    public List<PolicyRespVO> getPublishedPolicyList() {
        List<PolicyDO> list = policyMapper.selectPublishedList();
        return list.stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
    }

    /**
     * 将DO转换为RespVO
     */
    private PolicyRespVO convertToRespVO(PolicyDO policyDO) {
        PolicyRespVO respVO = BeanUtils.toBean(policyDO, PolicyRespVO.class);
        
        // 处理附件URL
        if (policyDO.getAttachments() != null && !policyDO.getAttachments().isEmpty()) {
            List<String> attachmentUrls = Arrays.asList(policyDO.getAttachments().split(","));
            respVO.setAttachmentUrls(attachmentUrls);
        } else {
            respVO.setAttachmentUrls(Collections.emptyList());
        }
        
        return respVO;
    }

}
