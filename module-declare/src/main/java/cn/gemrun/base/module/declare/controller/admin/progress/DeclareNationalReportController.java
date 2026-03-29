package cn.gemrun.base.module.declare.controller.admin.progress;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.declare.service.progress.DeclareNationalReportService;
import cn.gemrun.base.module.declare.vo.progress.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/declare/national-report")
@Validated
public class DeclareNationalReportController {

    @Resource
    private DeclareNationalReportService nationalReportService;

    /**
     * 批量上报国家局
     */
    @PostMapping("/batch-report")
    public CommonResult<Long> batchReport(@Valid @RequestBody BatchNationalReportReqVO reqVO) {
        return success(nationalReportService.batchReport(reqVO));
    }

    /**
     * 获取上报记录列表
     */
    @GetMapping("/record-list")
    public CommonResult<List<DeclareNationalReportRecordVO>> getRecordList(@RequestParam(value = "provinceCode", required = false) String provinceCode) {
        return success(nationalReportService.getRecordList(provinceCode));
    }

    /**
     * 获取上报记录详情
     */
    @GetMapping("/record-get")
    public CommonResult<DeclareNationalReportRecordVO> getRecord(@RequestParam("id") Long id) {
        return success(nationalReportService.getRecord(id));
    }
}
