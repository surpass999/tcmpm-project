package cn.gemrun.base.module.declare.controller.admin.policy;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicyPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicyRespVO;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicySaveReqVO;
import cn.gemrun.base.module.declare.service.policy.PolicyService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 政策通知 Controller
 *
 * @author Gemini
 */
@RestController
@RequestMapping("/declare/policy")
@RequiredArgsConstructor
@Validated
public class PolicyController {

    private final PolicyService policyService;

    /**
     * 创建政策通知
     */
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('declare:policy:create')")
    public CommonResult<Long> createPolicy(@Valid @RequestBody PolicySaveReqVO createReqVO) {
        return CommonResult.success(policyService.createPolicy(createReqVO));
    }

    /**
     * 更新政策通知
     */
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('declare:policy:update')")
    public CommonResult<Boolean> updatePolicy(@Valid @RequestBody PolicySaveReqVO updateReqVO) {
        policyService.updatePolicy(updateReqVO);
        return CommonResult.success(true);
    }

    /**
     * 删除政策通知
     */
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('declare:policy:delete')")
    public CommonResult<Boolean> deletePolicy(@RequestParam("id") Long id) {
        policyService.deletePolicy(id);
        return CommonResult.success(true);
    }

    /**
     * 获取政策通知详情
     */
    @GetMapping("/get")
    public CommonResult<PolicyRespVO> getPolicy(@RequestParam("id") Long id) {
        PolicyRespVO policyRespVO = policyService.getPolicy(id);
        return CommonResult.success(policyRespVO);
    }

    /**
     * 获取政策通知分页列表
     */
    @GetMapping("/page")
    public CommonResult<PageResult<PolicyRespVO>> getPolicyPage(@Valid PolicyPageReqVO pageReqVO) {
        return CommonResult.success(policyService.getPolicyPage(pageReqVO));
    }

    /**
     * 发布政策通知
     */
    @PutMapping("/publish")
    @PreAuthorize("@ss.hasPermission('declare:policy:publish')")
    public CommonResult<Boolean> publishPolicy(@RequestParam("id") Long id) {
        policyService.publishPolicy(id);
        return CommonResult.success(true);
    }

    /**
     * 下架政策通知
     */
    @PutMapping("/unpublish")
    @PreAuthorize("@ss.hasPermission('declare:policy:publish')")
    public CommonResult<Boolean> unpublishPolicy(@RequestParam("id") Long id) {
        policyService.unpublishPolicy(id);
        return CommonResult.success(true);
    }

    /**
     * 获取已发布的政策列表
     */
    @GetMapping("/list-published")
    public CommonResult<List<PolicyRespVO>> getPublishedPolicyList() {
        return CommonResult.success(policyService.getPublishedPolicyList());
    }

}
