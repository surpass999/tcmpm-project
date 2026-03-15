package cn.gemrun.base.module.declare.controller.admin.dataflow;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.*;
import java.util.*;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

import cn.gemrun.base.module.declare.controller.admin.dataflow.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.dataflow.DataFlowDO;
import cn.gemrun.base.module.declare.service.dataflow.DataFlowService;

/**
 * 数据流通记录 Controller
 *
 * @author
 */
@Tag(name = "管理后台 - 数据流通记录")
@RestController
@RequestMapping("/declare/data-flow")
@Validated
public class DataFlowController {

    @Resource
    private DataFlowService dataFlowService;

    // ========== 基础操作 ==========

    @PostMapping("/create")
    @Operation(summary = "创建数据流通记录")
    @PreAuthorize("@ss.hasPermission('declare:data-flow:create')")
    public CommonResult<Long> createDataFlow(@Valid @RequestBody DataFlowSaveReqVO createReqVO) {
        return success(dataFlowService.createDataFlow(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新数据流通记录")
    @PreAuthorize("@ss.hasPermission('declare:data-flow:update')")
    public CommonResult<Boolean> updateDataFlow(@Valid @RequestBody DataFlowSaveReqVO updateReqVO) {
        dataFlowService.updateDataFlow(updateReqVO);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交数据流通审核")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<String> submitDataFlow(
            @RequestParam("id") Long id,
            @RequestParam(value = "processDefinitionKey", required = false, defaultValue = "declare_data_flow") String processDefinitionKey) {
        String processInstanceId = dataFlowService.submitDataFlow(id, processDefinitionKey);
        return success(processInstanceId);
    }

    // ========== 查询操作 ==========

    @GetMapping("/get")
    @Operation(summary = "获得数据流通记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:data-flow:query')")
    public CommonResult<DataFlowRespVO> getDataFlow(@RequestParam("id") Long id) {
        DataFlowDO dataFlow = dataFlowService.getDataFlow(id);
        DataFlowRespVO respVO = BeanUtils.toBean(dataFlow, DataFlowRespVO.class);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得数据流通记录分页")
    @PreAuthorize("@ss.hasPermission('declare:data-flow:query')")
    public CommonResult<PageResult<DataFlowRespVO>> getDataFlowPage(@Valid DataFlowPageReqVO pageReqVO) {
        PageResult<DataFlowDO> pageResult = dataFlowService.getDataFlowPage(pageReqVO);
        List<DataFlowRespVO> voList = BeanUtils.toBean(pageResult.getList(), DataFlowRespVO.class);
        return success(new PageResult<>(voList, pageResult.getTotal()));
    }

    // ========== 删除操作 ==========

    @DeleteMapping("/delete")
    @Operation(summary = "删除数据流通记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('declare:data-flow:delete')")
    public CommonResult<Boolean> deleteDataFlow(@RequestParam("id") Long id) {
        dataFlowService.deleteDataFlow(id);
        return success(true);
    }

}
