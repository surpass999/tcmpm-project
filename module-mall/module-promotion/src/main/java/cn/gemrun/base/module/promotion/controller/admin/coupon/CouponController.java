package cn.gemrun.base.module.promotion.controller.admin.coupon;

import cn.hutool.core.collection.CollUtil;
import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.collection.MapUtils;
import cn.gemrun.base.module.promotion.controller.admin.coupon.vo.coupon.CouponPageItemRespVO;
import cn.gemrun.base.module.promotion.controller.admin.coupon.vo.coupon.CouponPageReqVO;
import cn.gemrun.base.module.promotion.controller.admin.coupon.vo.coupon.CouponSendReqVO;
import cn.gemrun.base.module.promotion.convert.coupon.CouponConvert;
import cn.gemrun.base.module.promotion.dal.dataobject.coupon.CouponDO;
import cn.gemrun.base.module.promotion.service.coupon.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 优惠劵")
@RestController
@RequestMapping("/promotion/coupon")
@Validated
public class CouponController {

    @Resource
    private CouponService couponService;

    @DeleteMapping("/delete")
    @Operation(summary = "回收优惠劵")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('promotion:coupon:delete')")
    public CommonResult<Boolean> deleteCoupon(@RequestParam("id") Long id) {
        couponService.deleteCoupon(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得优惠劵分页")
    @PreAuthorize("@ss.hasPermission('promotion:coupon:query')")
    public CommonResult<PageResult<CouponPageItemRespVO>> getCouponPage(@Valid CouponPageReqVO pageVO) {
        PageResult<CouponDO> pageResult = couponService.getCouponPage(pageVO);
        PageResult<CouponPageItemRespVO> pageResulVO = CouponConvert.INSTANCE.convertPage(pageResult);
        // userMap removed - cannot depend on member module
        return success(pageResulVO);
    }

    @PostMapping("/send")
    @Operation(summary = "发送优惠劵")
    @PreAuthorize("@ss.hasPermission('promotion:coupon:send')")
    public CommonResult<Boolean> sendCoupon(@Valid @RequestBody CouponSendReqVO reqVO) {
        couponService.takeCouponByAdmin(reqVO.getTemplateId(), reqVO.getUserIds());
        return success(true);
    }

}
