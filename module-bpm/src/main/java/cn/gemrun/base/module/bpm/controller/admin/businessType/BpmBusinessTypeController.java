package cn.gemrun.base.module.bpm.controller.admin.businessType;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.bpm.controller.admin.businessType.vo.BpmBusinessTypePageReqVO;
import cn.gemrun.base.module.bpm.controller.admin.businessType.vo.BpmBusinessTypeRespVO;
import cn.gemrun.base.module.bpm.controller.admin.businessType.vo.BpmBusinessTypeSaveReqVO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessTypeDO;
import cn.gemrun.base.module.bpm.service.definition.BpmBusinessTypeService;
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

@Tag(name = "管理后台 - 业务类型管理")
@RestController
@RequestMapping("/bpm/business-type")
@Validated
public class BpmBusinessTypeController {

    @Resource
    private BpmBusinessTypeService businessTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建业务类型")
    @PreAuthorize("@ss.hasPermission('bpm:business-type:create')")
    public CommonResult<Long> createBusinessType(@Valid @RequestBody BpmBusinessTypeSaveReqVO createReqVO) {
        return success(businessTypeService.createBusinessType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新业务类型")
    @PreAuthorize("@ss.hasPermission('bpm:business-type:update')")
    public CommonResult<Boolean> updateBusinessType(@Valid @RequestBody BpmBusinessTypeSaveReqVO updateReqVO) {
        businessTypeService.updateBusinessType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除业务类型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:business-type:delete')")
    public CommonResult<Boolean> deleteBusinessType(@RequestParam("id") Long id) {
        businessTypeService.deleteBusinessType(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得业务类型")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('bpm:business-type:query')")
    public CommonResult<BpmBusinessTypeRespVO> getBusinessType(@RequestParam("id") Long id) {
        BpmBusinessTypeDO businessType = businessTypeService.getBusinessType(id);
        return success(BeanUtils.toBean(businessType, BpmBusinessTypeRespVO.class));
    }

    @GetMapping("/get-by-type")
    @Operation(summary = "根据业务类型标识获取业务类型")
    @Parameter(name = "businessType", description = "业务类型标识", required = true, example = "declare:filing:create")
    public CommonResult<BpmBusinessTypeRespVO> getBusinessTypeByType(@RequestParam("businessType") String businessType) {
        BpmBusinessTypeDO businessTypeDO = businessTypeService.getBusinessTypeByType(businessType);
        return success(BeanUtils.toBean(businessTypeDO, BpmBusinessTypeRespVO.class));
    }

    @GetMapping("/list-by-process")
    @Operation(summary = "根据流程定义Key获取业务类型列表")
    @Parameter(name = "processDefinitionKey", description = "流程定义Key", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:business-type:query')")
    public CommonResult<List<BpmBusinessTypeRespVO>> getBusinessTypeListByProcessKey(
            @RequestParam("processDefinitionKey") String processDefinitionKey) {
        List<BpmBusinessTypeDO> list = businessTypeService.getBusinessTypeListByProcessKey(processDefinitionKey);
        return success(BeanUtils.toBean(list, BpmBusinessTypeRespVO.class));
    }

    @GetMapping("/list-by-category")
    @Operation(summary = "根据分类获取业务类型列表")
    @Parameter(name = "category", description = "流程分类", required = true)
    public CommonResult<List<BpmBusinessTypeRespVO>> getBusinessTypeListByCategory(
            @RequestParam("category") String category) {
        List<BpmBusinessTypeDO> list = businessTypeService.getBusinessTypeListByCategory(category);
        return success(BeanUtils.toBean(list, BpmBusinessTypeRespVO.class));
    }

    @GetMapping("/list-all-enabled")
    @Operation(summary = "获取所有已启用的业务类型列表")
    public CommonResult<List<BpmBusinessTypeRespVO>> getAllEnabledBusinessTypeList() {
        List<BpmBusinessTypeDO> list = businessTypeService.getAllEnabledBusinessTypeList();
        return success(BeanUtils.toBean(list, BpmBusinessTypeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取业务类型分页")
    @PreAuthorize("@ss.hasPermission('bpm:business-type:query')")
    public CommonResult<PageResult<BpmBusinessTypeRespVO>> getBusinessTypePage(
            @Valid BpmBusinessTypePageReqVO pageReqVO) {
        PageResult<BpmBusinessTypeDO> pageResult = businessTypeService.getBusinessTypePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, BpmBusinessTypeRespVO.class));
    }

    @GetMapping("/get-process-key")
    @Operation(summary = "根据业务类型标识获取流程定义Key")
    @Parameter(name = "businessType", description = "业务类型标识", required = true, example = "declare:filing:create")
    public CommonResult<String> getProcessDefinitionKey(@RequestParam("businessType") String businessType) {
        return success(businessTypeService.getProcessDefinitionKey(businessType));
    }

}
