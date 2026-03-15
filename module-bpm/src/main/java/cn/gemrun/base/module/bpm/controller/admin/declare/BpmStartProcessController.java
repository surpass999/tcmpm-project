package cn.gemrun.base.module.bpm.controller.admin.declare;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.bpm.api.task.BpmProcessInstanceApi;
import cn.gemrun.base.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmStartProcessReqVO;
import cn.gemrun.base.module.bpm.service.business.BpmBusinessTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

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

    @PostMapping("/start")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发起流程", description = "通用流程启动接口，支持流程变量和自选审批人")
    public CommonResult<String> startProcess(@Valid @RequestBody BpmStartProcessReqVO reqVO) {
        // 获取流程定义Key
        String processDefinitionKey = reqVO.getProcessDefinitionKey();
        if (processDefinitionKey == null || processDefinitionKey.isEmpty()) {
            // 如果未指定 processDefinitionKey，则通过 businessType 自动查找
            String businessType = reqVO.getBusinessType();
            if (businessType == null || businessType.isEmpty()) {
                throw new IllegalArgumentException("processDefinitionKey 和 businessType 不能同时为空");
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

        // 设置流程变量
        createReqDTO.setVariables(reqVO.getVariables());

        // 设置自选审批人
        createReqDTO.setStartUserSelectAssignees(reqVO.getStartUserSelectAssignees());

        // 调用流程接口创建实例
        String processInstanceId = processInstanceApi.createProcessInstance(getLoginUserId(), createReqDTO);
        return success(processInstanceId);
    }

}
