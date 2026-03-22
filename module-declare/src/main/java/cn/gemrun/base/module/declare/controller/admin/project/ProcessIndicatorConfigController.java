package cn.gemrun.base.module.declare.controller.admin.project;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorRespVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessIndicatorConfigPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessIndicatorConfigRespVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessIndicatorConfigSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProcessIndicatorConfigDO;
import cn.gemrun.base.module.declare.enums.ProcessType;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorService;
import cn.gemrun.base.module.declare.service.project.ProcessIndicatorConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.*;
import java.util.*;
import java.util.stream.Collectors;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 过程指标配置 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 过程指标配置")
@RestController
@RequestMapping("/declare/process-indicator-config")
@Validated
@Slf4j
public class ProcessIndicatorConfigController {

    @Resource
    private ProcessIndicatorConfigService configService;

    @Resource
    private DeclareIndicatorService indicatorService;

    // ========== 基础操作 ==========

    @PostMapping("/create")
    @Operation(summary = "创建过程指标配置")
    @PreAuthorize("@ss.hasPermission('declare:process-indicator-config:create')")
    public CommonResult<Long> createConfig(@Valid @RequestBody ProcessIndicatorConfigSaveReqVO createReqVO) {
        return success(configService.createConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新过程指标配置")
    @PreAuthorize("@ss.hasPermission('declare:process-indicator-config:update')")
    public CommonResult<Boolean> updateConfig(@Valid @RequestBody ProcessIndicatorConfigSaveReqVO updateReqVO) {
        configService.updateConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除过程指标配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('declare:process-indicator-config:delete')")
    public CommonResult<Boolean> deleteConfig(@RequestParam("id") Long id) {
        configService.deleteConfig(id);
        return success(true);
    }

    // ========== 查询操作 ==========

    @GetMapping("/get")
    @Operation(summary = "获得过程指标配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ProcessIndicatorConfigRespVO> getConfig(@RequestParam("id") Long id) {
        ProcessIndicatorConfigDO config = configService.getConfig(id);
        if (config == null) {
            return success(null);
        }
        return success(buildRespVO(config));
    }

    @GetMapping("/page")
    @Operation(summary = "获得过程指标配置分页")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<PageResult<ProcessIndicatorConfigRespVO>> getConfigPage(@Valid ProcessIndicatorConfigPageReqVO pageReqVO) {
        PageResult<ProcessIndicatorConfigDO> page = configService.getConfigPage(pageReqVO);
        if (page.getList().isEmpty()) {
            return success(PageResult.empty());
        }

        // 批量查询指标信息
        Set<Long> indicatorIds = page.getList().stream()
                .map(ProcessIndicatorConfigDO::getIndicatorId)
                .collect(Collectors.toSet());
        Map<Long, DeclareIndicatorDO> indicatorMap = indicatorService.getIndicatorMap(indicatorIds);

        // 转换结果
        List<ProcessIndicatorConfigRespVO> list = page.getList().stream()
                .map(config -> {
                    ProcessIndicatorConfigRespVO respVO = buildRespVO(config);
                    DeclareIndicatorDO indicator = indicatorMap.get(config.getIndicatorId());
                    if (indicator != null) {
                        respVO.setIndicatorCode(indicator.getIndicatorCode());
                        respVO.setIndicatorName(indicator.getIndicatorName());
                        respVO.setUnit(indicator.getUnit());
                        respVO.setCategory(indicator.getCategory());
                        respVO.setValueType(indicator.getValueType());
                    }
                    return respVO;
                })
                .collect(Collectors.toList());

        return success(new PageResult<>(list, page.getTotal()));
    }

    @GetMapping("/list-by-process-type")
    @Operation(summary = "根据过程类型获取已配置的指标列表")
    @Parameter(name = "processType", description = "过程类型")
    @Parameter(name = "projectType", description = "项目类型")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<ProcessIndicatorConfigRespVO>> getConfigListByProcessTypeAndProjectType(
            @RequestParam("processType") Integer processType,
            @RequestParam(value = "projectType", required = false) Integer projectType) {
        List<ProcessIndicatorConfigDO> configs = configService.getConfigListByProcessTypeAndProjectType(processType, projectType);
        if (configs.isEmpty()) {
            return success(new ArrayList<>());
        }

        // 批量查询指标信息
        Set<Long> indicatorIds = configs.stream()
                .map(ProcessIndicatorConfigDO::getIndicatorId)
                .collect(Collectors.toSet());
        Map<Long, DeclareIndicatorDO> indicatorMap = indicatorService.getIndicatorMap(indicatorIds);

        // 转换结果
        List<ProcessIndicatorConfigRespVO> list = configs.stream()
                .map(config -> {
                    ProcessIndicatorConfigRespVO respVO = buildRespVO(config);
                    DeclareIndicatorDO indicator = indicatorMap.get(config.getIndicatorId());
                    if (indicator != null) {
                        respVO.setIndicatorCode(indicator.getIndicatorCode());
                        respVO.setIndicatorName(indicator.getIndicatorName());
                        respVO.setUnit(indicator.getUnit());
                        respVO.setCategory(indicator.getCategory());
                        respVO.setValueType(indicator.getValueType());
                    }
                    return respVO;
                })
                .collect(Collectors.toList());

        return success(list);
    }

    @GetMapping("/indicator-ids-by-process-type")
    @Operation(summary = "根据过程类型和项目类型获取已配置的指标ID列表")
    @Parameter(name = "processType", description = "过程类型")
    @Parameter(name = "projectType", description = "项目类型")
    public CommonResult<List<Long>> getIndicatorIdsByProcessType(
            @RequestParam("processType") Integer processType,
            @RequestParam(value = "projectType", required = false) Integer projectType) {
        return success(configService.getIndicatorIdsByProcessTypeAndProjectType(processType, projectType));
    }

    // ========== 批量操作 ==========

    @PutMapping("/save-batch")
    @Operation(summary = "批量保存过程指标配置")
    @Parameter(name = "processType", description = "过程类型")
    @Parameter(name = "projectType", description = "项目类型")
    @PreAuthorize("@ss.hasPermission('declare:process-indicator-config:save')")
    public CommonResult<Boolean> saveConfigs(
            @RequestParam("processType") Integer processType,
            @RequestParam("projectType") Integer projectType,
            @RequestBody List<ProcessIndicatorConfigSaveReqVO> configs) {
        configService.saveConfigs(processType, projectType, configs);
        return success(true);
    }

    // ========== 辅助方法 ==========

    private ProcessIndicatorConfigRespVO buildRespVO(ProcessIndicatorConfigDO config) {
        ProcessIndicatorConfigRespVO respVO = BeanUtils.toBean(config, ProcessIndicatorConfigRespVO.class);

        // 设置过程类型名称
        ProcessType processTypeEnum = ProcessType.valueOf(config.getProcessType());
        respVO.setProcessTypeName(processTypeEnum != null ? processTypeEnum.getName() : "未知");

        // 设置项目类型名称（从字典表获取）
        respVO.setProjectTypeName(configService.getProjectTypeName(config.getProjectType()));

        return respVO;
    }

}
