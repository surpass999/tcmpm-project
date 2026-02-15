package cn.gemrun.base.module.bpm.controller.admin.dsl;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.bpm.controller.admin.dsl.vo.BpmProcessNodeDslPageReqVO;
import cn.gemrun.base.module.bpm.controller.admin.dsl.vo.BpmProcessNodeDslRespVO;
import cn.gemrun.base.module.bpm.controller.admin.dsl.vo.BpmProcessNodeDslSaveReqVO;
import cn.gemrun.base.module.bpm.dal.dataobject.dsl.BpmProcessNodeDslDO;
import cn.gemrun.base.module.bpm.service.definition.BpmProcessNodeDslService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 流程节点DSL配置")
@RestController
@RequestMapping("/bpm/process-node-dsl")
@Validated
public class BpmProcessNodeDslController {

    @Resource
    private BpmProcessNodeDslService dslService;

    @PostMapping("/create")
    @Operation(summary = "创建DSL配置")
    @PreAuthorize("@ss.hasPermission('bpm:process-node-dsl:create')")
    public CommonResult<Long> createDsl(@Valid @RequestBody BpmProcessNodeDslSaveReqVO createReqVO) {
        return success(dslService.createDsl(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新DSL配置")
    @PreAuthorize("@ss.hasPermission('bpm:process-node-dsl:update')")
    public CommonResult<Boolean> updateDsl(@Valid @RequestBody BpmProcessNodeDslSaveReqVO updateReqVO) {
        dslService.updateDsl(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除DSL配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:process-node-dsl:delete')")
    public CommonResult<Boolean> deleteDsl(@RequestParam("id") Long id) {
        dslService.deleteDsl(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得DSL配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('bpm:process-node-dsl:query')")
    public CommonResult<BpmProcessNodeDslRespVO> getDsl(@RequestParam("id") Long id) {
        BpmProcessNodeDslDO dsl = dslService.getDsl(id);
        return success(BeanUtils.toBean(dsl, BpmProcessNodeDslRespVO.class));
    }

    @GetMapping("/get-by-node")
    @Operation(summary = "根据流程定义Key和节点Key获取DSL配置")
    @Parameter(name = "processDefinitionKey", description = "流程定义Key", required = true)
    @Parameter(name = "nodeKey", description = "节点Key", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:process-node-dsl:query')")
    public CommonResult<BpmProcessNodeDslRespVO> getDslByNodeKey(
            @RequestParam("processDefinitionKey") String processDefinitionKey,
            @RequestParam("nodeKey") String nodeKey) {
        BpmProcessNodeDslDO dsl = dslService.getDslByNodeKey(processDefinitionKey, nodeKey);
        return success(BeanUtils.toBean(dsl, BpmProcessNodeDslRespVO.class));
    }

    @GetMapping("/list-by-process")
    @Operation(summary = "根据流程定义Key获取所有DSL配置")
    @Parameter(name = "processDefinitionKey", description = "流程定义Key", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:process-node-dsl:query')")
    public CommonResult<List<BpmProcessNodeDslRespVO>> getDslListByProcessKey(
            @RequestParam("processDefinitionKey") String processDefinitionKey) {
        List<BpmProcessNodeDslDO> list = dslService.getDslListByProcessKey(processDefinitionKey);
        return success(BeanUtils.toBean(list, BpmProcessNodeDslRespVO.class));
    }

    @GetMapping("/list-all-enabled")
    @Operation(summary = "获取所有已启用的DSL配置")
    public CommonResult<List<BpmProcessNodeDslRespVO>> getAllEnabledDslList() {
        List<BpmProcessNodeDslDO> list = dslService.getAllEnabledDslList();
        return success(BeanUtils.toBean(list, BpmProcessNodeDslRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得DSL配置分页")
    @PreAuthorize("@ss.hasPermission('bpm:process-node-dsl:query')")
    public CommonResult<PageResult<BpmProcessNodeDslRespVO>> getDslPage(@Valid BpmProcessNodeDslPageReqVO pageVO) {
        PageResult<BpmProcessNodeDslDO> pageResult = dslService.getDslPage(pageVO);
        return success(BeanUtils.toBean(pageResult, BpmProcessNodeDslRespVO.class));
    }

    @PostMapping("/validate")
    @Operation(summary = "验证DSL配置JSON")
    public CommonResult<Boolean> validateDslConfig(@RequestParam("dslConfig") String dslConfig) {
        dslService.validateDslConfig(dslConfig);
        return success(true);
    }

}
