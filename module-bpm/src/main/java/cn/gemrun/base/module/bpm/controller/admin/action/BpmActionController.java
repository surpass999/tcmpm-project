package cn.gemrun.base.module.bpm.controller.admin.action;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import cn.gemrun.base.module.bpm.api.BpmActionApi;
import cn.gemrun.base.module.bpm.api.dto.BpmActionRespDTO;
import cn.gemrun.base.module.bpm.controller.admin.action.vo.BpmActionSubmitReqVO;
import cn.gemrun.base.module.bpm.controller.admin.action.vo.BpmActionWithdrawReqVO;
import cn.gemrun.base.module.bpm.framework.process.service.BpmProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * BPM 按钮定义 Controller
 *
 * 提供给前端获取按钮配置定义
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - BPM按钮定义")
@RestController
@RequestMapping("/bpm/action")
@Validated
public class BpmActionController {

    @Resource
    private BpmActionApi bpmActionApi;

    @Resource
    private BpmProcessService bpmProcessService;

    /**
     * 获取所有按钮定义列表
     *
     * @return 按钮定义列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取按钮定义列表")
    public CommonResult<List<BpmActionRespDTO>> getActionList() {
        return success(bpmActionApi.getAllActions());
    }

    /**
     * 根据 key 获取按钮定义
     *
     * @param key 按钮标识
     * @return 按钮定义
     */
    @GetMapping("/{key}")
    @Operation(summary = "获取按钮定义")
    public CommonResult<BpmActionRespDTO> getAction(@PathVariable("key") String key) {
        return success(bpmActionApi.getActionByKey(key));
    }

    /**
     * 获取当前用户对指定业务可执行的操作列表
     *
     * @param businessType 业务类型，如 declare:filing:create
     * @param businessId   业务ID
     * @return 当前节点 DSL 定义的可执行操作列表（仅当前用户有待处理任务时返回）
     */
    @GetMapping("/available")
    @Operation(summary = "获取当前用户可执行操作列表")
    @Parameter(name = "businessType", description = "业务类型", required = true)
    @Parameter(name = "businessId", description = "业务ID", required = true)
    public CommonResult<List<BpmActionRespDTO>> getAvailableActions(
            @RequestParam("businessType") String businessType,
            @RequestParam("businessId") Long businessId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return success(bpmProcessService.getAvailableActions(businessType, businessId, userId));
    }

    /**
     * 批量获取当前用户对多个业务可执行的操作列表
     *
     * @param businessType  业务类型，如 declare:filing:create
     * @param businessIds   业务ID列表，逗号分隔
     * @return Map<businessId, 操作列表>
     */
    @GetMapping("/available-batch")
    @Operation(summary = "批量获取当前用户可执行操作列表")
    @Parameter(name = "businessType", description = "业务类型", required = true)
    @Parameter(name = "businessIds", description = "业务ID列表，逗号分隔", required = true)
    public CommonResult<Map<Long, List<BpmActionRespDTO>>> getAvailableActionsBatch(
            @RequestParam("businessType") String businessType,
            @RequestParam("businessIds") String businessIds) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        List<Long> ids = cn.hutool.core.util.StrUtil.split(businessIds, ',')
                .stream()
                .map(s -> {
                    try {
                        return Long.parseLong(s.trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toList());
        return success(bpmProcessService.getAvailableActionsBatch(businessType, ids, userId));
    }

    /**
     * 提交操作（完成任务，推进流程）
     *
     * @param reqVO 提交请求
     */
    @PostMapping("/submit")
    @Operation(summary = "提交操作")
    public CommonResult<Boolean> submitAction(@Validated @RequestBody BpmActionSubmitReqVO reqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        bpmProcessService.submitAction(reqVO.getBusinessType(), reqVO.getBusinessId(),
                reqVO.getActionKey(), userId, reqVO.getReason());
        return success(true);
    }

    /**
     * 撤回流程（撤销到草稿状态）
     * 删除 Flowable 运行时流程实例，业务状态需要业务方自行处理
     *
     * @param reqVO 撤回请求
     */
    @PostMapping("/withdraw")
    @Operation(summary = "撤回流程")
    public CommonResult<Boolean> withdrawProcess(@Validated @RequestBody BpmActionWithdrawReqVO reqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        bpmProcessService.withdrawProcess(reqVO.getBusinessType(), reqVO.getBusinessId(), userId);
        return success(true);
    }

}
