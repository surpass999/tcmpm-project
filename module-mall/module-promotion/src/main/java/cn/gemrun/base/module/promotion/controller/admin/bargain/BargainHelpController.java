package cn.gemrun.base.module.promotion.controller.admin.bargain;

import cn.hutool.core.collection.CollUtil;
import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.promotion.controller.admin.bargain.vo.help.BargainHelpPageReqVO;
import cn.gemrun.base.module.promotion.controller.admin.bargain.vo.help.BargainHelpRespVO;
import cn.gemrun.base.module.promotion.convert.bargain.BargainHelpConvert;
import cn.gemrun.base.module.promotion.dal.dataobject.bargain.BargainHelpDO;
import cn.gemrun.base.module.promotion.service.bargain.BargainHelpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 砍价助力")
@RestController
@RequestMapping("/promotion/bargain-help")
@Validated
public class BargainHelpController {

    @Resource
    private BargainHelpService bargainHelpService;

    @GetMapping("/page")
    @Operation(summary = "获得砍价助力分页")
    @PreAuthorize("@ss.hasPermission('promotion:bargain-help:query')")
    public CommonResult<PageResult<BargainHelpRespVO>> getBargainHelpPage(@Valid BargainHelpPageReqVO pageVO) {
        PageResult<BargainHelpDO> pageResult = bargainHelpService.getBargainHelpPage(pageVO);
        // userMap removed - cannot depend on member module
        return success(BargainHelpConvert.INSTANCE.convertPage(pageResult, Collections.emptyMap()));
    }

}
