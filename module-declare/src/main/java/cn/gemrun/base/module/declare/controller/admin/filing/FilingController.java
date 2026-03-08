package cn.gemrun.base.module.declare.controller.admin.filing;

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

import cn.gemrun.base.module.declare.controller.admin.filing.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.module.declare.service.filing.FilingService;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorValueService;
import cn.gemrun.base.module.bpm.framework.process.annotation.BpmProcess;
import cn.gemrun.base.module.bpm.framework.process.annotation.BpmProcessQuery;

/**
 * 项目备案核心信息 Controller（流程配置化示例）
 *
 * 使用说明：
 * 1. 在需要触发流程的方法上添加 @BpmProcess 注解
 * 2. businessType 自动解析为：{模块名}:{业务名}:{动作}
 *    例如：submitFiling -> declare:filing:submit
 * 3. 流程由 AOP 自动启动，无需手动调用
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 项目备案核心信息")
@RestController
@RequestMapping("/declare/filing")
@Validated
public class FilingController {

    @Resource
    private FilingService filingService;

    @Resource
    private DeclareIndicatorValueService indicatorValueService;

    // 业务类型：1=备案
    private static final Integer BUSINESS_TYPE_FILING = 1;

    // ========== 基础操作（启动流程）==========

    @PostMapping("/create")
    @Operation(summary = "创建项目备案核心信息")
    @PreAuthorize("@ss.hasPermission('declare:filing:create')")
    @BpmProcess
    public CommonResult<Long> createFiling(@Valid @RequestBody FilingSaveReqVO createReqVO) {
        return success(filingService.createFiling(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新项目备案核心信息")
    @PreAuthorize("@ss.hasPermission('declare:filing:update')")
    public CommonResult<Boolean> updateFiling(@Valid @RequestBody FilingSaveReqVO updateReqVO) {
        filingService.updateFiling(updateReqVO);
        return success(true);
    }

    // ========== 触发流程的操作 ==========

    // 注意：流程相关操作（submit/withdraw/resubmit）不在此 Controller 中实现
    // 而是统一由 BpmProcessAspect AOP 拦截 @BpmProcess 注解的方法自动触发流程
    // 业务模块只需要在 Service 层添加 @BpmProcess 注解的方法即可

    // ========== 查询操作 ==========

    @GetMapping("/get")
    @Operation(summary = "获得项目备案核心信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:filing:query')")
    @BpmProcessQuery
    public CommonResult<FilingRespVO> getFiling(@RequestParam("id") Long id) {
        FilingDO filing = filingService.getFiling(id);
        return success(BeanUtils.toBean(filing, FilingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得项目备案核心信息分页")
    @PreAuthorize("@ss.hasPermission('declare:filing:query')")
    @BpmProcessQuery
    public CommonResult<PageResult<FilingRespVO>> getFilingPage(@Valid FilingPageReqVO pageReqVO) {
        PageResult<FilingDO> pageResult = filingService.getFilingPage(pageReqVO);

        // 转换并填充指标值
        List<FilingRespVO> voList = new ArrayList<>();
        for (FilingDO filing : pageResult.getList()) {
            FilingRespVO respVO = BeanUtils.toBean(filing, FilingRespVO.class);

            // 查询指标值
            Map<String, DeclareIndicatorValueDO> indicatorValueMap =
                indicatorValueService.getIndicatorValueMap(BUSINESS_TYPE_FILING, filing.getId());
            // 转换为显示值 Map（key: indicatorCode, value: 显示值）
            Map<String, Object> displayValues = new HashMap<>();
            for (Map.Entry<String, DeclareIndicatorValueDO> entry : indicatorValueMap.entrySet()) {
                displayValues.put(entry.getKey(), getDisplayValue(entry.getValue()));
            }
            respVO.setIndicatorValues(displayValues);

            voList.add(respVO);
        }

        return success(new PageResult<>(voList, pageResult.getTotal()));
    }

    /**
     * 获取指标值的显示值
     */
    private Object getDisplayValue(DeclareIndicatorValueDO indicatorValue) {
        if (indicatorValue == null) {
            return null;
        }
        Integer valueType = indicatorValue.getValueType();
        // 值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本，6=单选，7=多选，8=日期区间，9=文件上传 10=下拉单选 11=下拉多选   
        switch (valueType) {
            case 1: // 数字
                return indicatorValue.getValueNum();
            case 2: // 字符串
            case 6: // 单选
            case 7: // 多选
            case 10: // 下拉单选
            case 11: // 下拉多选
            case 9: // 文件上传
                return indicatorValue.getValueStr();
            case 3: // 布尔
                return indicatorValue.getValueBool();
            case 4: // 日期
                return indicatorValue.getValueDate();
            case 5: // 长文本
                return indicatorValue.getValueText();
            case 8: // 日期区间
                Map<String, Object> dateRange = new HashMap<>();
                if (indicatorValue.getValueDateStart() != null) {
                    dateRange.put("start", indicatorValue.getValueDateStart().toString());
                }
                if (indicatorValue.getValueDateEnd() != null) {
                    dateRange.put("end", indicatorValue.getValueDateEnd().toString());
                }
                return dateRange.isEmpty() ? null : dateRange.toString();
            default:
                return null;
        }
    }

    // ========== 删除操作 ==========

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目备案核心信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('declare:filing:delete')")
    public CommonResult<Boolean> deleteFiling(@RequestParam("id") Long id) {
        filingService.deleteFiling(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除项目备案核心信息")
    @PreAuthorize("@ss.hasPermission('declare:filing:delete')")
    public CommonResult<Boolean> deleteFilingList(@RequestParam("ids") List<Long> ids) {
        filingService.deleteFilingListByIds(ids);
        return success(true);
    }

}
