package cn.gemrun.base.module.declare.controller.admin.indicator;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorValueRespVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorValueSaveReqVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.LastPeriodValuesRespVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorValueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 指标值 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 指标值")
@RestController
@RequestMapping("/declare/indicator-value")
@Validated
public class DeclareIndicatorValueController {

    @Resource
    private DeclareIndicatorValueService indicatorValueService;

    @PostMapping("/save")
    @Operation(summary = "保存指标值")
    @PreAuthorize("@ss.hasPermission('declare:filing:save')")
    public CommonResult<Boolean> saveIndicatorValues(@Valid @RequestBody DeclareIndicatorValueSaveReqVO reqVO) {
        List<DeclareIndicatorValueDO> values = BeanUtils.toBean(reqVO.getValues(), DeclareIndicatorValueDO.class);
        indicatorValueService.batchSaveIndicatorValues(
                reqVO.getBusinessType(),
                reqVO.getBusinessId(),
                values,
                SecurityFrameworkUtils.getLoginUserId()
        );
        return CommonResult.success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取指标值列表")
    @Parameter(name = "businessType", description = "业务类型", required = true)
    @Parameter(name = "businessId", description = "业务ID", required = true)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<DeclareIndicatorValueRespVO>> getIndicatorValueList(
            @RequestParam("businessType") Integer businessType,
            @RequestParam("businessId") Long businessId) {
        List<DeclareIndicatorValueDO> list = indicatorValueService.getIndicatorValues(businessType, businessId);
        return CommonResult.success(BeanUtils.toBean(list, DeclareIndicatorValueRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除指标值")
    @Parameter(name = "businessType", description = "业务类型", required = true)
    @Parameter(name = "businessId", description = "业务ID", required = true)
    @PreAuthorize("@ss.hasPermission('declare:filing:delete')")
    public CommonResult<Boolean> deleteIndicatorValues(
            @RequestParam("businessType") Integer businessType,
            @RequestParam("businessId") Long businessId) {
        indicatorValueService.deleteIndicatorValues(businessType, businessId);
        return CommonResult.success(true);
    }

    @GetMapping("/last-period-values")
    @Operation(summary = "获取上期指标值（用于填报参考）")
    @Parameter(name = "hospitalId", description = "医院ID", required = true)
    @Parameter(name = "reportYear", description = "填报年度", required = true)
    @Parameter(name = "reportBatch", description = "填报批次", required = true)
    @Parameter(name = "businessType", description = "业务类型", required = false)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Map<String, String>> getLastPeriodIndicatorValues(
            @RequestParam("hospitalId") Long hospitalId,
            @RequestParam("reportYear") Integer reportYear,
            @RequestParam("reportBatch") Integer reportBatch,
            @RequestParam(value = "businessType", required = false, defaultValue = "3") Integer businessType) {
        return CommonResult.success(
                indicatorValueService.getLastPeriodIndicatorValues(hospitalId, reportYear, reportBatch, businessType));
    }

    @GetMapping("/last-period-values-with-raw")
    @Operation(summary = "获取上期指标值（包含显示值和原始值，用于填报参考和 inputType 解析）")
    @Parameter(name = "hospitalId", description = "医院ID", required = true)
    @Parameter(name = "reportYear", description = "填报年度", required = true)
    @Parameter(name = "reportBatch", description = "填报批次", required = true)
    @Parameter(name = "businessType", description = "业务类型", required = false)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<LastPeriodValuesRespVO> getLastPeriodIndicatorValuesWithRaw(
            @RequestParam("hospitalId") Long hospitalId,
            @RequestParam("reportYear") Integer reportYear,
            @RequestParam("reportBatch") Integer reportBatch,
            @RequestParam(value = "businessType", required = false, defaultValue = "3") Integer businessType) {
        Map<String, Map<String, String>> result = indicatorValueService.getLastPeriodIndicatorValuesWithRaw(
                hospitalId, reportYear, reportBatch, businessType);
        return CommonResult.success(LastPeriodValuesRespVO.builder()
                .display(result.get("display"))
                .raw(result.get("raw"))
                .build());
    }

    @GetMapping("/progress-report-values")
    @Operation(summary = "获取进度填报的指标值列表")
    @Parameter(name = "reportId", description = "填报记录ID", required = true)
    @Parameter(name = "businessType", description = "业务类型：3=进度填报", required = false)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<DeclareIndicatorValueRespVO>> getProgressReportIndicatorValues(
            @RequestParam("reportId") Long reportId,
            @RequestParam(value = "businessType", required = false, defaultValue = "3") Integer businessType) {
        List<DeclareIndicatorValueDO> list = indicatorValueService.getIndicatorValues(businessType, reportId);
        return CommonResult.success(BeanUtils.toBean(list, DeclareIndicatorValueRespVO.class));
    }

}
