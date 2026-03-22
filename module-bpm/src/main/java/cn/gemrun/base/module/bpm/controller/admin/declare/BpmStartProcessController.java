package cn.gemrun.base.module.bpm.controller.admin.declare;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.bpm.api.event.ProcessStartedEvent;
import cn.gemrun.base.module.bpm.api.task.BpmProcessInstanceApi;
import cn.gemrun.base.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmStartProcessReqVO;
import cn.gemrun.base.module.bpm.service.business.BpmBusinessTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;
import static cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 流程启动 Controller
 * 提供通用的流程发起接口，支持完整参数（流程变量、自选审批人等）
 *
 * @author jason
 */
@Tag(name = "管理后台 - 流程启动")
@RestController
@RequestMapping("/bpm/process")
@Validated
public class BpmStartProcessController {

    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private BpmBusinessTypeService bpmBusinessTypeService;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @PostMapping("/start")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发起流程", description = "通用流程启动接口，支持流程变量和自选审批人")
    public CommonResult<String> startProcess(@Valid @RequestBody BpmStartProcessReqVO reqVO) {
        // 获取流程定义Key
        String processDefinitionKey = reqVO.getProcessDefinitionKey();
        String businessType = reqVO.getBusinessType();
        String processCategory = reqVO.getProcessCategory();
        Integer type = reqVO.getType();

        // 优先级：processDefinitionKey > businessType > (processCategory + type)
        if (processDefinitionKey == null || processDefinitionKey.isEmpty()) {
            // 如果未指定 processDefinitionKey，则尝试通过 businessType 自动查找
            if (businessType == null || businessType.isEmpty()) {
                // 如果也未指定 businessType，则通过 processCategory + type 自动拼接
                if (processCategory == null || processCategory.isEmpty() || type == null) {
                    throw new IllegalArgumentException("processDefinitionKey、businessType 和 processCategory+type 不能同时为空");
                }
                // 拼接 businessType：表名_process:type:值，如 project_process:type:1
                businessType = processCategory + "_process:type:" + type;
            }
            processDefinitionKey = bpmBusinessTypeService.getProcessDefinitionKey(businessType);
            if (processDefinitionKey == null) {
                throw new IllegalArgumentException("未找到业务类型对应的流程定义: " + businessType);
            }
        }

        // 构建流程创建请求
        BpmProcessInstanceCreateReqDTO createReqDTO = new BpmProcessInstanceCreateReqDTO();
        createReqDTO.setProcessDefinitionKey(processDefinitionKey);

        // 业务Key：如果未指定，则使用 processDefinitionKey + "_" + businessId
        String businessKey = reqVO.getBusinessKey();
        if (businessKey == null || businessKey.isEmpty()) {
            businessKey = processDefinitionKey + "_" + reqVO.getBusinessId();
        }
        createReqDTO.setBusinessKey(businessKey);

        // 设置流程变量（将 businessType、businessCreatorId 写入流程变量，供后续节点完成任务时使用）
        Map<String, Object> variables = reqVO.getVariables() != null
                ? new HashMap<>(reqVO.getVariables()) : new HashMap<>();
        variables.put("businessType", businessType);
        variables.put("businessCreatorId", getLoginUserId()); // 业务创建人ID，用于业务发起人策略
        createReqDTO.setVariables(variables);

        // 设置自选审批人
        createReqDTO.setStartUserSelectAssignees(reqVO.getStartUserSelectAssignees());

        // 调用流程接口创建实例
        String processInstanceId = processInstanceApi.createProcessInstance(getLoginUserId(), createReqDTO);

        // 发布流程发起事件（由 BpmProcessStartedListener 统一更新业务状态）
        applicationEventPublisher.publishEvent(new ProcessStartedEvent(
                processInstanceId,
                businessType,
                businessKey,
                reqVO.getBusinessId(),
                processDefinitionKey,
                getLoginUserId()
        ));

        return success(processInstanceId);
    }

}
