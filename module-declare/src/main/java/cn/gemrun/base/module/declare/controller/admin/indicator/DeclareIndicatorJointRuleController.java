package cn.gemrun.base.module.declare.controller.admin.indicator;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorJointRuleDO;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorJointRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 指标联合规则管理 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 指标联合规则管理")
@RestController
@RequestMapping("/declare/indicator-joint-rule")
@Validated
@Slf4j
public class DeclareIndicatorJointRuleController {

    @Resource
    private DeclareIndicatorJointRuleService jointRuleService;

    @PostMapping("/create")
    @Operation(summary = "创建联合规则")
    public CommonResult<Long> createJointRule(@Valid @RequestBody DeclareIndicatorJointRuleSaveReqVO reqVO) {
        return CommonResult.success(jointRuleService.createJointRule(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新联合规则")
    public CommonResult<Boolean> updateJointRule(@Valid @RequestBody DeclareIndicatorJointRuleSaveReqVO reqVO) {
        jointRuleService.updateJointRule(reqVO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除联合规则")
    public CommonResult<Boolean> deleteJointRule(@RequestParam("id") Long id) {
        jointRuleService.deleteJointRule(id);
        return CommonResult.success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取联合规则")
    public CommonResult<DeclareIndicatorJointRuleRespVO> getJointRule(@RequestParam("id") Long id) {
        DeclareIndicatorJointRuleDO jointRule = jointRuleService.getJointRule(id);
        return CommonResult.success(BeanUtils.toBean(jointRule, DeclareIndicatorJointRuleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取联合规则分页")
    public CommonResult<PageResult<DeclareIndicatorJointRuleRespVO>> getJointRulePage(@Valid DeclareIndicatorJointRulePageReqVO pageReqVO) {
        PageResult<DeclareIndicatorJointRuleDO> pageResult = jointRuleService.getJointRulePage(pageReqVO);
        return CommonResult.success(BeanUtils.toBean(pageResult, DeclareIndicatorJointRuleRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获取联合规则列表")
    public CommonResult<List<DeclareIndicatorJointRuleRespVO>> getJointRuleList() {
        List<DeclareIndicatorJointRuleDO> list = jointRuleService.getJointRuleList();
        return CommonResult.success(BeanUtils.toBean(list, DeclareIndicatorJointRuleRespVO.class));
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获取启用的联合规则列表")
    public CommonResult<List<DeclareIndicatorJointRuleRespVO>> getEnabledJointRules(
            @RequestParam(value = "projectType", required = false) Integer projectType,
            @RequestParam(value = "processNode", required = false) String processNode,
            @RequestParam(value = "triggerTiming", required = false) String triggerTiming) {
        List<DeclareIndicatorJointRuleDO> list = jointRuleService.getEnabledJointRules(projectType, processNode, triggerTiming);
        return CommonResult.success(BeanUtils.toBean(list, DeclareIndicatorJointRuleRespVO.class));
    }

}
