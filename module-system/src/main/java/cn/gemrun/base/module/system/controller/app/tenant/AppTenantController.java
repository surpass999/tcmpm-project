package cn.gemrun.base.module.system.controller.app.tenant;

import cn.gemrun.base.framework.common.enums.CommonStatusEnum;
import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.system.controller.app.tenant.vo.AppTenantRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 租户")
@RestController
@RequestMapping("/system/tenant")
public class AppTenantController {

    @GetMapping("/get-by-website")
    @PermitAll
    @Operation(summary = "使用域名，获得租户信息", description = "根据用户的域名，获得租户信息")
    @Parameter(name = "website", description = "域名", required = true, example = "www.iocoder.cn")
    public CommonResult<AppTenantRespVO> getTenantByWebsite(@RequestParam("website") String website) {
        return success(null);
    }

}
