package cn.gemrun.base.module.declare.controller.admin.indicator;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorValueRespVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorValueSaveReqVO;
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
    @PreAuthorize("@ss.hasPermission('declare:filing:query')")
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

}
