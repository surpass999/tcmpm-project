package cn.gemrun.base.module.system.controller.admin.captcha;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.hutool.core.util.StrUtil;
import cn.gemrun.base.framework.common.util.servlet.ServletUtils;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;

@Tag(name = "管理后台 - 验证码")
@RestController("adminCaptchaController")
@RequestMapping("/system/captcha")
public class CaptchaController {

    @Resource
    private CaptchaService captchaService;

    @PostMapping({"/get"})
    @Operation(summary = "获得验证码")
    @PermitAll
    public CommonResult<ResponseModel> get(@RequestBody CaptchaVO data, HttpServletRequest request) {
        assert request.getRemoteHost() != null;
        data.setBrowserInfo(getRemoteId(request));
        ResponseModel response = captchaService.get(data);
        return CommonResult.success(response);
    }

    @PostMapping("/check")
    @Operation(summary = "校验验证码")
    @PermitAll
    public CommonResult<ResponseModel> check(@RequestBody CaptchaVO data, HttpServletRequest request) {
        data.setBrowserInfo(getRemoteId(request));
        ResponseModel response = captchaService.check(data);
        return CommonResult.success(response);
    }

    public static String getRemoteId(HttpServletRequest request) {
        String ip = ServletUtils.getClientIP(request);
        String ua = request.getHeader("user-agent");
        if (StrUtil.isNotBlank(ip)) {
            return ip + ua;
        }
        return request.getRemoteAddr() + ua;
    }

}
