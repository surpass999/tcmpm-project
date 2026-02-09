package cn.gemrun.base.module.declare.controller.admin;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import javax.annotation.security.PermitAll;

/**
 * 申报模块管理后台接口
 */
@Tag(name = "申报模块-管理后台")
@RestController
@RequestMapping("/declare")
public class DeclareController {

    @GetMapping("/test")
    @PermitAll
    @Operation(summary = "测试接口-无需登录")
    public CommonResult<String> test() {
        
        return CommonResult.success("54321接口请求成功112");
    }
}
