package cn.gemrun.base.module.declare.controller.admin.progress;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.declare.service.progress.DeclareNationalExportService;
import cn.gemrun.base.module.declare.service.progress.DeclareProgressReportService;
import cn.gemrun.base.module.declare.service.progress.DeclareReportWindowService;
import cn.gemrun.base.module.declare.vo.progress.DeclareCompareDataVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareNationalExportReqVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareNationalSearchReqVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareProgressReportAuditReqVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareProgressReportCreateReqVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareProgressReportSaveReqVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareProgressReportVO;
import cn.gemrun.base.module.declare.vo.progress.ReportWindowVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/declare/progress-report")
@Validated
public class DeclareProgressReportController {

    @Resource
    private DeclareProgressReportService progressReportService;

    @Resource
    private DeclareReportWindowService reportWindowService;

    @Resource
    private DeclareNationalExportService exportService;

    /**
     * 创建填报记录
     */
    @PostMapping("/create")
    public CommonResult<Long> createReport(@Valid @RequestBody DeclareProgressReportCreateReqVO reqVO) {
        return success(progressReportService.createReport(reqVO));
    }

    /**
     * 保存填报记录及指标值（一体化接口）
     */
    @PostMapping("/save")
    public CommonResult<Long> saveReport(@Valid @RequestBody DeclareProgressReportSaveReqVO reqVO) {
        return success(progressReportService.saveReport(reqVO));
    }

    /**
     * 获取填报详情
     */
    @GetMapping("/get")
    public CommonResult<DeclareProgressReportVO> getReport(@RequestParam("id") Long id) {
        return success(progressReportService.getReport(id));
    }

    /**
     * 获取填报列表（医院端）
     */
    @GetMapping("/hospital-list")
    public CommonResult<List<DeclareProgressReportVO>> getHospitalReportList(
            @RequestParam("hospitalId") Long hospitalId,
            @RequestParam(value = "reportYear", required = false) Integer reportYear) {
        return success(progressReportService.getReportListByHospital(hospitalId, reportYear));
    }

    /**
     * 获取填报列表（省级端）
     */
    @GetMapping("/province-list")
    public CommonResult<List<DeclareProgressReportVO>> getProvinceReportList(
            @RequestParam("provinceCode") String provinceCode,
            @RequestParam(value = "reportYear", required = false) Integer reportYear) {
        return success(progressReportService.getReportListByProvince(provinceCode, reportYear));
    }

    /**
     * 获取填报列表（省级端）- 由框架数据权限自动按 dept 过滤，省局用户只看到本省数据
     */
    @GetMapping("/province-list-by-dept")
    public CommonResult<List<DeclareProgressReportVO>> getProvinceReportListByDept(
            @RequestParam(value = "reportYear", required = false) Integer reportYear) {
        return success(progressReportService.getReportListByProvince(null, reportYear));
    }

    /**
     * 获取省级待审核列表
     */
    @GetMapping("/province-pending")
    public CommonResult<List<DeclareProgressReportVO>> getProvincePendingList(@RequestParam("provinceCode") String provinceCode) {
        return success(progressReportService.getProvincePendingList(provinceCode));
    }

    /**
     * 获取省级审核通过待上报列表
     */
    @GetMapping("/province-approved")
    public CommonResult<List<DeclareProgressReportVO>> getProvinceApprovedList(@RequestParam("provinceCode") String provinceCode) {
        return success(progressReportService.getProvinceApprovedList(provinceCode));
    }

    /**
     * 提交审核
     */
    @PostMapping("/submit")
    public CommonResult<Boolean> submitReport(
            @RequestParam("id") Long id,
            @RequestParam(value = "auditUserName", required = false) String auditUserName) {
        progressReportService.submitReport(id, auditUserName);
        return success(true);
    }

    /**
     * 医院审核员审核
     */
    @PostMapping("/hospital-audit")
    public CommonResult<Boolean> hospitalAudit(@Valid @RequestBody DeclareProgressReportAuditReqVO reqVO) {
        progressReportService.hospitalAudit(reqVO);
        return success(true);
    }

    /**
     * 省级审核
     */
    @PostMapping("/province-audit")
    public CommonResult<Boolean> provinceAudit(@Valid @RequestBody DeclareProgressReportAuditReqVO reqVO) {
        progressReportService.provinceAudit(reqVO);
        return success(true);
    }

    /**
     * 获取填报历史
     */
    @GetMapping("/history")
    public CommonResult<List<DeclareProgressReportVO>> getReportHistory(
            @RequestParam("hospitalId") Long hospitalId,
            @RequestParam(value = "reportYear", required = false) Integer reportYear) {
        return success(progressReportService.getReportHistory(hospitalId, reportYear));
    }

    /**
     * 检查填报时间窗口
     */
    @GetMapping("/check-window")
    public CommonResult<Boolean> checkReportWindow(@RequestParam("hospitalId") Long hospitalId) {
        return success(progressReportService.isInReportWindow(hospitalId));
    }

    /**
     * 检查填报次数限制
     */
    @GetMapping("/check-limit")
    public CommonResult<Boolean> checkReportLimit(
            @RequestParam("hospitalId") Long hospitalId,
            @RequestParam("reportYear") Integer reportYear) {
        return success(!progressReportService.isOverLimit(hospitalId, reportYear));
    }

    /**
     * 获取医院当前可用的填报窗口
     */
    @GetMapping("/available-windows")
    public CommonResult<List<ReportWindowVO>> getAvailableReportWindows(
            @RequestParam("hospitalId") Long hospitalId) {
        return success(reportWindowService.getAvailableWindowsForHospital(hospitalId));
    }

    /**
     * 获取最新开放填报窗口
     */
    @GetMapping("/latest-window")
    public CommonResult<ReportWindowVO> getLatestOpenWindow() {
        return success(reportWindowService.getLatestOpenWindow());
    }

    /**
     * 删除填报记录（仅 DRAFT/SAVED 状态可删除）
     */
    @DeleteMapping("/delete")
    public CommonResult<Boolean> deleteReport(@RequestParam("id") Long id) {
        progressReportService.deleteReport(id);
        return success(true);
    }

    /**
     * 获取两条申报记录的数据对比视图
     */
    @GetMapping("/compare-data")
    public CommonResult<DeclareCompareDataVO> getCompareData(
            @RequestParam("reportIdA") Long reportIdA,
            @RequestParam("reportIdB") Long reportIdB) {
        return success(progressReportService.getCompareData(reportIdA, reportIdB));
    }

    /**
     * 国家局高级搜索（基本信息 + 指标值条件组合）
     */
    @PostMapping("/national-search")
    public CommonResult<List<DeclareProgressReportVO>> nationalSearch(
            @RequestBody DeclareNationalSearchReqVO reqVO) {
        return success(progressReportService.nationalSearch(reqVO));
    }

    /**
     * 获取填报列表（国家局端）- 只显示医院已提交审核的填报
     */
    @GetMapping("/national-list")
    public CommonResult<List<DeclareProgressReportVO>> getNationalReportList() {
        return success(progressReportService.getReportListByNational());
    }

    /**
     * 导出国家局填报数据 (简单条件)
     */
    @GetMapping("/export-national")
    public void exportNationalReport(
            @RequestParam(value = "hospitalName", required = false) String hospitalName,
            @RequestParam(value = "reportYear", required = false) Integer reportYear,
            @RequestParam(value = "reportBatch", required = false) Integer reportBatch,
            @RequestParam(value = "reportStatus", required = false) String reportStatus,
            @RequestParam(value = "provinceStatus", required = false) Integer provinceStatus,
            @RequestParam(value = "nationalReportStatus", required = false) Integer nationalReportStatus,
            @RequestParam(value = "projectType", required = false) Integer projectType,
            HttpServletResponse response) throws IOException {
        DeclareNationalExportReqVO reqVO = new DeclareNationalExportReqVO();
        reqVO.setHospitalName(hospitalName);
        reqVO.setReportYear(reportYear);
        reqVO.setReportBatch(reportBatch);
        reqVO.setReportStatus(reportStatus);
        reqVO.setProvinceStatus(provinceStatus);
        reqVO.setNationalReportStatus(nationalReportStatus);
        reqVO.setProjectType(projectType);
        exportService.exportNationalReport(reqVO, response);
    }

    /**
     * 导出国家局填报数据 (高级条件 - 含指标条件)
     */
    @PostMapping("/export-national-advanced")
    public void exportNationalReportAdvanced(
            @RequestBody DeclareNationalExportReqVO reqVO,
            HttpServletResponse response) throws IOException {
        exportService.exportNationalReportAdvanced(reqVO, response);
    }
}
