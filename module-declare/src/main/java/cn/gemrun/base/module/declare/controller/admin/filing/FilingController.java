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
import javax.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.gemrun.base.framework.common.pojo.PageParam;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

import cn.gemrun.base.framework.excel.core.util.ExcelUtils;

import cn.gemrun.base.framework.apilog.core.annotation.ApiAccessLog;
import static cn.gemrun.base.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.gemrun.base.module.declare.controller.admin.filing.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import cn.gemrun.base.module.declare.service.filing.FilingService;

@Tag(name = "管理后台 - 项目备案核心信息")
@RestController
@RequestMapping("/declare/filing")
@Validated
public class FilingController {

    @Resource
    private FilingService filingService;

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

    @GetMapping("/export-excel")
    @Operation(summary = "导出项目备案核心信息 Excel")
    @PreAuthorize("@ss.hasPermission('declare:filing:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFilingExcel(@Valid FilingPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<FilingDO> list = filingService.getFilingPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "项目备案核心信息.xls", "数据", FilingRespVO.class,
                        BeanUtils.toBean(list, FilingRespVO.class));
    }

}