package cn.gemrun.base.module.bpm.controller.admin.declare;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.bpm.api.task.BpmProcessInstanceApi;
import cn.gemrun.base.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmCommonProcessStartReqVO;
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
 * 通用流程 Controller
 * 提供通用的流程发起接口，只需要传入业务ID、业务类型、流程key即可发起流程
 *
 * @author jason
 */
@Tag(name = "管理后台 - 通用流程")
@RestController
@RequestMapping("/bpm/common/process")
@Validated
public class BpmCommonProcessController {

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @PostMapping("/start")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发起通用流程")
    public CommonResult<String> startProcess(@Valid @RequestBody BpmCommonProcessStartReqVO reqVO) {
        // 构建流程创建请求
        BpmProcessInstanceCreateReqDTO createReqDTO = new BpmProcessInstanceCreateReqDTO();
        createReqDTO.setProcessDefinitionKey(reqVO.getProcessDefinitionKey());
        // 业务Key格式：流程定义Key_业务ID，例如：project_midterm_123
        // 这样同一张业务表可以关联多个不同类型的流程，通过 processDefinitionKey 区分
        createReqDTO.setBusinessKey(reqVO.getProcessDefinitionKey() + "_" + reqVO.getBusinessId());

        // 调用流程接口创建实例
        String processInstanceId = processInstanceApi.createProcessInstance(getLoginUserId(), createReqDTO);
        return success(processInstanceId);
    }

}
