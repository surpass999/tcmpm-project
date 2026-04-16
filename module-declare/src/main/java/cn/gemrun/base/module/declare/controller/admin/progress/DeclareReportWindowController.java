package cn.gemrun.base.module.declare.controller.admin.progress;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.declare.service.progress.DeclareReportWindowService;
import cn.gemrun.base.module.declare.vo.progress.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/declare/report-window")
@Validated
public class DeclareReportWindowController {

    @Resource
    private DeclareReportWindowService reportWindowService;

    /**
     * 创建时间窗口
     */
    @PostMapping("/create")
    public CommonResult<Long> createWindow(@Valid @RequestBody ReportWindowCreateReqVO reqVO) {
        return success(reportWindowService.createWindow(reqVO));
    }

    /**
     * 更新时间窗口
     */
    @PutMapping("/update")
    public CommonResult<Boolean> updateWindow(@RequestParam("id") Long id, @Valid @RequestBody ReportWindowCreateReqVO reqVO) {
        reportWindowService.updateWindow(id, reqVO);
        return success(true);
    }

    /**
     * 删除时间窗口
     */
    @DeleteMapping("/delete")
    public CommonResult<Boolean> deleteWindow(@RequestParam("id") Long id) {
        reportWindowService.deleteWindow(id);
        return success(true);
    }

    /**
     * 获取时间窗口列表
     */
    @GetMapping("/list")
    public CommonResult<List<ReportWindowVO>> getWindowList(
            @RequestParam(value = "reportYear", required = false) Integer reportYear,
            @RequestParam(value = "status", required = false) Integer status) {
        return success(reportWindowService.getWindowList(reportYear, status));
    }

    /**
     * 检查是否有开放的时间窗口
     */
    @GetMapping("/check-open")
    public CommonResult<Boolean> checkWindowOpen() {
        return success(reportWindowService.isAnyWindowOpen());
    }
}
