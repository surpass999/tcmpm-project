package cn.gemrun.base.module.declare.controller.admin.filing;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.constraints.*;
import javax.validation.*;
import java.util.*;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

import cn.gemrun.base.module.declare.controller.admin.filing.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import cn.gemrun.base.module.declare.service.filing.FilingService;
import cn.gemrun.base.module.declare.framework.process.annotation.DeclareProcess;

/**
 * 项目备案核心信息 Controller（流程配置化示例）
 *
 * 使用说明：
 * 1. 在需要触发流程的方法上添加 @DeclareProcess 注解
 * 2. businessType 自动解析为：{模块名}:{业务名}:{动作}
 *    例如：submitFiling -> declare:filing:submit
 * 3. 流程由 AOP 自动启动，无需手动调用
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 项目备案核心信息")
@RestController
@RequestMapping("/declare/filing")
@Validated
public class FilingController {

    @Resource
    private FilingService filingService;

    // ========== 基础操作（不触发流程）==========

    @PostMapping("/create")
    @Operation(summary = "创建项目备案核心信息")
    @PreAuthorize("@ss.hasPermission('declare:filing:create')")
    public CommonResult<Long> createFiling(@Valid @RequestBody FilingSaveReqVO createReqVO) {
        return success(filingService.createFiling(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新项目备案核心信息")
    @PreAuthorize("@ss.hasPermission('declare:filing:update')")
    public CommonResult<Boolean> updateFiling(@Valid @RequestBody FilingSaveReqVO updateReqVO) {
        filingService.updateFiling(updateReqVO);
        return success(true);
    }

    // ========== 触发流程的操作 ==========

    /**
     * 提交备案
     * 自动解析 businessType = "declare:filing:submit"
     * 如果 declare_business_type 表中有配置，则自动启动流程
     *
     * 配置示例：
     * business_type: declare:filing:submit
     * process_definition_key: proc_filing
     */
    @PostMapping("/submit")
    @Operation(summary = "提交备案申请")
    @PreAuthorize("@ss.hasPermission('declare:filing:submit')")
    @DeclareProcess  // 自动解析 businessType
    public CommonResult<Boolean> submitFiling(@RequestParam("id") Long id) {
        filingService.submitFiling(id);
        return success(true);
    }

    /**
     * 撤回备案
     * 自动解析 businessType = "declare:filing:withdraw"
     */
    @PostMapping("/withdraw")
    @Operation(summary = "撤回备案申请")
    @PreAuthorize("@ss.hasPermission('declare:filing:withdraw')")
    @DeclareProcess(required = false)  // 可选流程
    public CommonResult<Boolean> withdrawFiling(@RequestParam("id") Long id) {
        filingService.withdrawFiling(id);
        return success(true);
    }

    /**
     * 重新提交备案
     * 手动指定 businessType
     */
    @PostMapping("/resubmit")
    @Operation(summary = "重新提交备案")
    @PreAuthorize("@ss.hasPermission('declare:filing:resubmit')")
    @DeclareProcess(businessType = "declare:filing:resubmit")
    public CommonResult<Boolean> resubmitFiling(@RequestParam("id") Long id) {
        filingService.resubmitFiling(id);
        return success(true);
    }

    // ========== 查询操作 ==========

    @GetMapping("/get")
    @Operation(summary = "获得项目备案核心信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:filing:query')")
    public CommonResult<FilingRespVO> getFiling(@RequestParam("id") Long id) {
        FilingDO filing = filingService.getFiling(id);
        return success(BeanUtils.toBean(filing, FilingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得项目备案核心信息分页")
    @PreAuthorize("@ss.hasPermission('declare:filing:query')")
    public CommonResult<PageResult<FilingRespVO>> getFilingPage(@Valid FilingPageReqVO pageReqVO) {
        PageResult<FilingDO> pageResult = filingService.getFilingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, FilingRespVO.class));
    }

    // ========== 删除操作 ==========

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目备案核心信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('declare:filing:delete')")
    public CommonResult<Boolean> deleteFiling(@RequestParam("id") Long id) {
        filingService.deleteFiling(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除项目备案核心信息")
    @PreAuthorize("@ss.hasPermission('declare:filing:delete')")
    public CommonResult<Boolean> deleteFilingList(@RequestParam("ids") List<Long> ids) {
        filingService.deleteFilingListByIds(ids);
        return success(true);
    }

}
