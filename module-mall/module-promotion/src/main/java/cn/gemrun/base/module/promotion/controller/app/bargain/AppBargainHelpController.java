package cn.gemrun.base.module.promotion.controller.app.bargain;

import cn.hutool.core.collection.CollUtil;
import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.promotion.controller.app.bargain.vo.help.AppBargainHelpCreateReqVO;
import cn.gemrun.base.module.promotion.controller.app.bargain.vo.help.AppBargainHelpRespVO;
import cn.gemrun.base.module.promotion.convert.bargain.BargainHelpConvert;
import cn.gemrun.base.module.promotion.dal.dataobject.bargain.BargainHelpDO;
import cn.gemrun.base.module.promotion.service.bargain.BargainHelpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;
import static cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 砍价助力")
@RestController
@RequestMapping("/promotion/bargain-help")
@Validated
public class AppBargainHelpController {

    @Resource
    private BargainHelpService bargainHelpService;

    @PostMapping("/create")
    @Operation(summary = "创建砍价助力", description = "给拼团记录砍一刀") // 返回结果为砍价金额，单位：分
    public CommonResult<Integer> createBargainHelp(@RequestBody AppBargainHelpCreateReqVO reqVO) {
        BargainHelpDO help = bargainHelpService.createBargainHelp(getLoginUserId(), reqVO);
        return success(help.getReducePrice());
    }

    @GetMapping("/list")
    @Operation(summary = "获得砍价助力列表")
    @Parameter(name = "recordId", description = "砍价记录编号", required = true, example = "111")
    public CommonResult<List<AppBargainHelpRespVO>> getBargainHelpList(@RequestParam("recordId") Long recordId) {
        List<BargainHelpDO> helps = bargainHelpService.getBargainHelpListByRecordId(recordId);
        if (CollUtil.isEmpty(helps)) {
            return success(Collections.emptyList());
        }
        helps.sort((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime())); // 倒序展示

        // 拼接数据 - userMap removed, cannot depend on member module
        return success(BargainHelpConvert.INSTANCE.convertList(helps, Collections.emptyMap()));
    }

}
