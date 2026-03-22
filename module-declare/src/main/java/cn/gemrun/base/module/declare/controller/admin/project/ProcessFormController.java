package cn.gemrun.base.module.declare.controller.admin.project;

import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessFormConfigRespVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessFormDataRespVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessFormDataSaveReqVO;
import cn.gemrun.base.module.declare.service.project.ProcessFormService;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 过程填报 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 过程填报")
@RestController
@RequestMapping("/project/process-form")
@Validated
public class ProcessFormController {

    @Resource
    private ProcessFormService processFormService;

    @GetMapping("/config")
    @Operation(summary = "获取表单配置")
    @Parameter(name = "processId", description = "过程记录ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project-process:query')")
    public CommonResult<ProcessFormConfigRespVO> getFormConfig(@RequestParam("processId") Long processId) {
        return success(processFormService.getFormConfig(processId));
    }

    @GetMapping("/data")
    @Operation(summary = "获取填报数据")
    @Parameter(name = "processId", description = "过程记录ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project-process:query')")
    public CommonResult<ProcessFormDataRespVO> getFormData(@RequestParam("processId") Long processId) {
        return success(processFormService.getFormData(processId));
    }

    @PostMapping("/save")
    @Operation(summary = "保存填报数据")
    @PreAuthorize("@ss.hasPermission('declare:project-process:update')")
    public CommonResult<Long> saveFormData(@Valid @RequestBody ProcessFormDataSaveReqVO reqVO) {
        return success(processFormService.saveFormData(reqVO));
    }

    @PostMapping("/validate")
    @Operation(summary = "验证填报数据")
    @PreAuthorize("@ss.hasPermission('declare:project-process:update')")
    public CommonResult<Map<String, Object>> validateFormData(@Valid @RequestBody ProcessFormDataSaveReqVO reqVO) {
        return success(processFormService.validateFormData(reqVO));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回填报数据")
    @Parameter(name = "processId", description = "过程记录ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project-process:update')")
    public CommonResult<Boolean> withdrawFormData(@RequestParam("processId") Long processId) {
        processFormService.withdrawFormData(processId);
        return success(true);
    }

    @PostMapping("/sync-indicator")
    @Operation(summary = "手动同步指标")
    @Parameter(name = "processId", description = "过程记录ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project-process:update')")
    public CommonResult<Boolean> syncIndicator(@RequestParam("processId") Long processId) {
        processFormService.syncIndicatorToProject(processId);
        return success(true);
    }

}
