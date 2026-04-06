package cn.gemrun.base.module.declare.service.progress;

import cn.gemrun.base.module.declare.vo.progress.DeclareNationalExportReqVO;
import cn.gemrun.base.module.declare.vo.progress.ExportSheetData;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 国家局导出 Service 接口
 *
 * @author Gemini
 */
public interface DeclareNationalExportService {

    /**
     * 导出国家局填报数据 (简单条件)
     *
     * @param reqVO 导出请求参数
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    void exportNationalReport(DeclareNationalExportReqVO reqVO, HttpServletResponse response) throws IOException;

    /**
     * 导出国家局填报数据 (高级条件 - 含指标条件)
     *
     * @param reqVO 导出请求参数 (含 indicatorGroups)
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    void exportNationalReportAdvanced(DeclareNationalExportReqVO reqVO, HttpServletResponse response) throws IOException;

    /**
     * 构建导出数据 (供测试或预览用)
     *
     * @param reqVO 导出请求参数
     * @return 每个项目类型的Sheet数据列表
     */
    List<ExportSheetData> buildExportData(DeclareNationalExportReqVO reqVO);
}
