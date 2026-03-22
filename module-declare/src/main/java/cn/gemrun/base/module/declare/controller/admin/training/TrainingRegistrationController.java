package cn.gemrun.base.module.declare.controller.admin.training;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.training.vo.RegistrationPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.training.vo.RegistrationRespVO;
import cn.gemrun.base.module.declare.controller.admin.training.vo.SignInReqVO;
import cn.gemrun.base.module.declare.service.training.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 活动报名管理 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 活动报名管理")
@RestController
@RequestMapping("/declare/registration")
@Validated
@RequiredArgsConstructor
public class TrainingRegistrationController {

    private final TrainingService trainingService;

    @GetMapping("/page")
    @Operation(summary = "获取报名列表")
    public CommonResult<PageResult<RegistrationRespVO>> getRegistrationPage(@Validated RegistrationPageReqVO pageReqVO) {
        return success(trainingService.getRegistrationPage(pageReqVO));
    }

    @PutMapping("/sign-in")
    @Operation(summary = "签到")
    @PreAuthorize("@ss.hasPermission('declare:training:signIn')")
    public CommonResult<Boolean> signIn(@Validated @RequestBody SignInReqVO reqVO) {
        trainingService.signIn(reqVO.getRegistrationId());
        return success(true);
    }

}
