package cn.gemrun.base.module.declare.service.progress.impl;

import cn.gemrun.base.framework.common.util.http.HttpUtils;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorGroupDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorGroupService;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorService;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorValueService;
import cn.gemrun.base.module.declare.service.progress.DeclareNationalExportService;
import cn.gemrun.base.module.declare.service.progress.DeclareProgressReportService;
import cn.gemrun.base.module.declare.vo.progress.DeclareNationalExportReqVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareProgressReportVO;
import cn.gemrun.base.module.declare.vo.progress.ExportSheetData;
import cn.gemrun.base.module.declare.vo.progress.SheetHeaderConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 国家局导出 Service 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class DeclareNationalExportServiceImpl implements DeclareNationalExportService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int DEFAULT_COLUMN_WIDTH = 15;
    private static final int MAX_CONTAINER_ENTRIES = 50;

    @Resource
    @Lazy
    private DeclareProgressReportService progressReportService;

    @Resource
    private DeclareIndicatorService indicatorService;

    @Resource
    private DeclareIndicatorGroupService indicatorGroupService;

    @Resource
    private DeclareIndicatorValueService indicatorValueService;

    // ==================== 公共常量: 固定列定义 ====================

    private static final List<SheetHeaderConfig.ColumnDefinition> FIXED_COLUMNS;

    static {
        FIXED_COLUMNS = Arrays.asList(
            SheetHeaderConfig.ColumnDefinition.builder().code("seq").title("序号").width(6).rowSpan(3).build(),
            SheetHeaderConfig.ColumnDefinition.builder().code("hospitalName").title("医院名称").width(25).rowSpan(3).build(),
            SheetHeaderConfig.ColumnDefinition.builder().code("unifiedSocialCreditCode").title("统一社会信用代码").width(22).rowSpan(3).build(),
            SheetHeaderConfig.ColumnDefinition.builder().code("medicalLicenseNo").title("医疗机构执业许可证").width(25).rowSpan(3).build(),
            SheetHeaderConfig.ColumnDefinition.builder().code("provinceName").title("省份").width(10).rowSpan(3).build(),
            SheetHeaderConfig.ColumnDefinition.builder().code("reportYear").title("填报年度").width(10).rowSpan(3).build(),
            SheetHeaderConfig.ColumnDefinition.builder().code("reportBatch").title("填报批次").width(10).rowSpan(3).build(),
            SheetHeaderConfig.ColumnDefinition.builder().code("reportStatus").title("填报状态").width(12).rowSpan(3).build(),
            SheetHeaderConfig.ColumnDefinition.builder().code("nationalReportStatus").title("上报状态").width(12).rowSpan(3).build(),
            SheetHeaderConfig.ColumnDefinition.builder().code("createTime").title("创建时间").width(18).rowSpan(3).build()
        );
    }

    // ==================== 公共常量: 项目类型名称 ====================

    private static final Map<Integer, String> PROJECT_TYPE_NAMES = new LinkedHashMap<>();

    static {
        PROJECT_TYPE_NAMES.put(1, "综合型");
        PROJECT_TYPE_NAMES.put(2, "中医电子病历型");
        PROJECT_TYPE_NAMES.put(3, "智慧中药房型");
        PROJECT_TYPE_NAMES.put(4, "名老中医传承型");
        PROJECT_TYPE_NAMES.put(5, "中医临床科研型");
        PROJECT_TYPE_NAMES.put(6, "中医智慧医共体型");
    }

    // ==================== 公共方法: 导出接口 ====================

    @Override
    public void exportNationalReport(DeclareNationalExportReqVO reqVO, HttpServletResponse response) throws IOException {
        // 1. 查询填报记录 (DeclareNationalExportReqVO extends DeclareNationalSearchReqVO)
        List<DeclareProgressReportVO> reportList = progressReportService.nationalSearch(reqVO);

        // 2. 按项目类型分组
        Map<Integer, List<DeclareProgressReportVO>> groupedReports = reportList.stream()
                .collect(Collectors.groupingBy(r -> r.getProjectType() != null ? r.getProjectType() : 0));

        // 3. 构建导出数据
        List<ExportSheetData> sheetDataList = buildSheetDataList(groupedReports, reqVO);

        // 4. 生成Excel并写出
        writeExcelToResponse(sheetDataList, response);
    }

    @Override
    public void exportNationalReportAdvanced(DeclareNationalExportReqVO reqVO, HttpServletResponse response) throws IOException {
        // 1. 查询填报记录 (含指标条件)
        List<DeclareProgressReportVO> reportList = progressReportService.nationalSearch(reqVO);

        // 2. 按项目类型分组
        Map<Integer, List<DeclareProgressReportVO>> groupedReports = reportList.stream()
                .collect(Collectors.groupingBy(r -> r.getProjectType() != null ? r.getProjectType() : 0));

        // 3. 构建导出数据
        List<ExportSheetData> sheetDataList = buildSheetDataList(groupedReports, reqVO);

        // 4. 生成Excel并写出
        writeExcelToResponse(sheetDataList, response);
    }

    @Override
    public List<ExportSheetData> buildExportData(DeclareNationalExportReqVO reqVO) {
        List<DeclareProgressReportVO> reportList = progressReportService.nationalSearch(reqVO);
        Map<Integer, List<DeclareProgressReportVO>> groupedReports = reportList.stream()
                .collect(Collectors.groupingBy(r -> r.getProjectType() != null ? r.getProjectType() : 0));
        return buildSheetDataList(groupedReports, reqVO);
    }

    // ==================== 私有方法: 构建导出数据 ====================

    /**
     * 构建每个项目类型的Sheet数据
     */
    private List<ExportSheetData> buildSheetDataList(
            Map<Integer, List<DeclareProgressReportVO>> groupedReports,
            DeclareNationalExportReqVO reqVO) {

        List<ExportSheetData> result = new ArrayList<>();

        // 确定要导出的项目类型
        List<Integer> projectTypesToExport;
        if (reqVO.getProjectType() != null) {
            // 指定了项目类型，只导该类型
            projectTypesToExport = Collections.singletonList(reqVO.getProjectType());
        } else {
            // 未指定，导出所有有数据的类型
            projectTypesToExport = groupedReports.keySet().stream()
                    .filter(pt -> pt > 0 && PROJECT_TYPE_NAMES.containsKey(pt))
                    .sorted()
                    .collect(Collectors.toList());
        }

        for (Integer projectType : projectTypesToExport) {
            List<DeclareProgressReportVO> reports = groupedReports.getOrDefault(projectType, Collections.emptyList());
            if (reports.isEmpty()) {
                continue;
            }

            // 构建该类型的Sheet数据
            ExportSheetData sheetData = buildSheetData(projectType, reports, reqVO);
            result.add(sheetData);
        }

        return result;
    }

    /**
     * 构建单个项目类型的Sheet数据
     */
    private ExportSheetData buildSheetData(
            Integer projectType,
            List<DeclareProgressReportVO> reports,
            DeclareNationalExportReqVO reqVO) {

        // 1. 查询该类型的指标定义
        List<DeclareIndicatorDO> indicators = indicatorService.getIndicators(projectType, "process");

        // 2. 查询该类型的分组定义
        List<DeclareIndicatorGroupDO> allGroups = indicatorGroupService.getAllEnabledList();

        // 3. 查询指标值
        List<Long> reportIds = reports.stream().map(DeclareProgressReportVO::getId).collect(Collectors.toList());
        Map<Long, Map<String, DeclareIndicatorValueDO>> valueMap = indicatorValueService.getValueMapByReports(
                reportIds, 3);

        // 4. 构建表头配置
        SheetHeaderConfig headerConfig = buildHeaderConfig(indicators, allGroups);

        // 5. 构建数据行
        List<ExportSheetData.ExportRowData> rowDataList = new ArrayList<>();
        int seqNo = 1;
        for (DeclareProgressReportVO report : reports) {
            Map<String, DeclareIndicatorValueDO> reportValues = valueMap.getOrDefault(report.getId(), Collections.emptyMap());
            ExportSheetData.ExportRowData rowData = buildRowData(report, reportValues, headerConfig, seqNo++);
            rowDataList.add(rowData);
        }

        return ExportSheetData.builder()
                .sheetName(PROJECT_TYPE_NAMES.getOrDefault(projectType, "未知类型"))
                .projectType(projectType)
                .headerConfig(headerConfig)
                .rows(rowDataList)
                .rowCount(rowDataList.size())
                .build();
    }

    /**
     * 构建三行表头配置
     */
    private SheetHeaderConfig buildHeaderConfig(
            List<DeclareIndicatorDO> indicators,
            List<DeclareIndicatorGroupDO> allGroups) {

        // 构建分组树
        Map<Long, List<DeclareIndicatorGroupDO>> childrenMap = allGroups.stream()
                .filter(g -> g.getParentId() != null && g.getParentId() > 0)
                .collect(Collectors.groupingBy(DeclareIndicatorGroupDO::getParentId));

        // 按 parentId=0 找一级分组
        List<DeclareIndicatorGroupDO> level1Groups = allGroups.stream()
                .filter(g -> g.getParentId() == null || g.getParentId() == 0)
                .sorted(Comparator.comparing(g -> g.getSort() != null ? g.getSort() : 0))
                .collect(Collectors.toList());

        // 构建指标分组 Map: groupId -> indicators
        Map<Long, List<DeclareIndicatorDO>> indicatorByGroup = indicators.stream()
                .collect(Collectors.groupingBy(ind -> ind.getGroupId() != null ? ind.getGroupId() : 0L));

        // 分配列索引
        int columnIndex = FIXED_COLUMNS.size();

        List<SheetHeaderConfig.GroupHeaderConfig> groupHeaders = new ArrayList<>();

        for (DeclareIndicatorGroupDO level1 : level1Groups) {
            // 查找该一级分组下的二级分组
            List<DeclareIndicatorGroupDO> level2Groups = childrenMap.getOrDefault(level1.getId(), Collections.emptyList())
                    .stream()
                    .sorted(Comparator.comparing(g -> g.getSort() != null ? g.getSort() : 0))
                    .collect(Collectors.toList());

            if (level2Groups.isEmpty()) {
                // 无二级分组: 一级分组直接包含指标
                List<DeclareIndicatorDO> groupIndicators = indicatorByGroup.getOrDefault(level1.getId(), Collections.emptyList())
                        .stream()
                        .sorted(Comparator.comparing(ind -> ind.getSort() != null ? ind.getSort() : 0))
                        .collect(Collectors.toList());

                if (!groupIndicators.isEmpty()) {
                    SheetHeaderConfig.GroupHeaderConfig ghConfig = buildGroupHeaderConfig(
                            level1, null, groupIndicators, columnIndex);
                    groupHeaders.add(ghConfig);
                    columnIndex += ghConfig.getColumnSpan();
                }
            } else {
                // 有二级分组: 每个二级分组独立
                for (DeclareIndicatorGroupDO level2 : level2Groups) {
                    List<DeclareIndicatorDO> groupIndicators = indicatorByGroup.getOrDefault(level2.getId(), Collections.emptyList())
                            .stream()
                            .sorted(Comparator.comparing(ind -> ind.getSort() != null ? ind.getSort() : 0))
                            .collect(Collectors.toList());

                    if (!groupIndicators.isEmpty()) {
                        SheetHeaderConfig.GroupHeaderConfig ghConfig = buildGroupHeaderConfig(
                                level1, level2, groupIndicators, columnIndex);
                        groupHeaders.add(ghConfig);
                        columnIndex += ghConfig.getColumnSpan();
                    }
                }
            }
        }

        return SheetHeaderConfig.builder()
                .fixedColumns(new ArrayList<>(FIXED_COLUMNS))
                .groupHeaders(groupHeaders)
                .totalColumnCount(columnIndex)
                .build();
    }

    /**
     * 构建分组表头配置 (单个一级分组+可选二级分组)
     */
    private SheetHeaderConfig.GroupHeaderConfig buildGroupHeaderConfig(
            DeclareIndicatorGroupDO level1,
            DeclareIndicatorGroupDO level2,
            List<DeclareIndicatorDO> indicators,
            int startColumnIndex) {

        List<SheetHeaderConfig.IndicatorColumnConfig> indicatorColumns = new ArrayList<>();
        int columnSpan = 0;

        for (DeclareIndicatorDO indicator : indicators) {
            SheetHeaderConfig.IndicatorColumnConfig.IndicatorColumnConfigBuilder icBuilder = SheetHeaderConfig.IndicatorColumnConfig.builder()
                    .indicatorId(indicator.getId())
                    .indicatorCode(indicator.getIndicatorCode())
                    .indicatorName(indicator.getIndicatorName())
                    .unit(indicator.getUnit())
                    .valueType(indicator.getValueType())
                    .valueOptions(indicator.getValueOptions())
                    .groupId(indicator.getGroupId())
                    .sort(indicator.getSort());

            // 处理动态容器指标
            if (indicator.getValueType() != null && indicator.getValueType() == 12) {
                List<SheetHeaderConfig.DynamicFieldConfig> fields = parseDynamicFields(indicator.getValueOptions());
                icBuilder.dynamicFields(fields);
                columnSpan += fields.size();
            } else {
                columnSpan += 1;
            }

            indicatorColumns.add(icBuilder.build());
        }

        return SheetHeaderConfig.GroupHeaderConfig.builder()
                .level1Id(level1.getId())
                .level1Code(level1.getGroupCode())
                .level1Name(level1.getGroupName())
                .level2Id(level2 != null ? level2.getId() : null)
                .level2Code(level2 != null ? level2.getGroupCode() : null)
                .level2Name(level2 != null ? level2.getGroupName() : null)
                .indicators(indicatorColumns)
                .startColumnIndex(startColumnIndex)
                .endColumnIndex(startColumnIndex + columnSpan - 1)
                .columnSpan(columnSpan)
                .build();
    }

    /**
     * 解析动态容器的子字段定义
     *
     * valueOptions JSON 结构：
     * {
     *   "mode": "conditional" | "autoEntry" | "manual",
     *   "fields": [ {fieldCode, fieldLabel, fieldType, options[], suffix, ...}, ... ],
     *   "link": "501"
     * }
     */
    private List<SheetHeaderConfig.DynamicFieldConfig> parseDynamicFields(String valueOptions) {
        if (valueOptions == null || valueOptions.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // 第一步：解析外层容器对象
            JSONObject container = JSON.parseObject(valueOptions);
            JSONArray fieldArray = container.getJSONArray("fields");
            if (fieldArray == null || fieldArray.isEmpty()) {
                return Collections.emptyList();
            }

            // 第二步：解析每个字段定义
            List<SheetHeaderConfig.DynamicFieldConfig> result = new ArrayList<>();
            for (int i = 0; i < fieldArray.size(); i++) {
                JSONObject fieldObj = fieldArray.getJSONObject(i);
                String fieldCode = fieldObj.getString("fieldCode");
                String fieldLabel = fieldObj.getString("fieldLabel");
                String fieldType = fieldObj.getString("fieldType");
                String suffix = fieldObj.getString("suffix"); // number 类型可能有后缀单位

                // 字段的选项列表 (radio/checkbox/select/multiSelect)
                JSONArray optsArray = fieldObj.getJSONArray("options");
                String fieldOptionsJson = null;
                if (optsArray != null && !optsArray.isEmpty()) {
                    fieldOptionsJson = optsArray.toJSONString();
                }

                if (fieldCode == null) {
                    continue;
                }

                result.add(SheetHeaderConfig.DynamicFieldConfig.builder()
                        .fieldCode(fieldCode)
                        .fieldLabel(fieldLabel != null ? fieldLabel : fieldCode)
                        .fieldType(fieldType)
                        .fieldOptions(fieldOptionsJson)
                        .unit(suffix)
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("解析动态容器字段定义失败: {}", valueOptions, e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建单行数据
     */
    private ExportSheetData.ExportRowData buildRowData(
            DeclareProgressReportVO report,
            Map<String, DeclareIndicatorValueDO> reportValues,
            SheetHeaderConfig headerConfig,
            int seqNo) {

        ExportSheetData.ExportRowData.ExportRowDataBuilder builder = ExportSheetData.ExportRowData.builder()
                .sequenceNo(seqNo)
                .reportId(report.getId())
                .hospitalName(report.getHospitalName())
                .unifiedSocialCreditCode(report.getUnifiedSocialCreditCode())
                .medicalLicenseNo(report.getMedicalLicenseNo())
                .provinceName(report.getProvinceName())
                .reportYear(report.getReportYear())
                .reportBatch(report.getReportBatch())
                .reportStatus(report.getReportStatusName())
                .nationalReportStatus(report.getNationalReportStatusName())
                .createTime(report.getCreateTime() != null ? report.getCreateTime().format(DATETIME_FORMATTER) : "");

        // 构建指标值 Map
        Map<String, String> indicatorValues = new LinkedHashMap<>();
        for (SheetHeaderConfig.GroupHeaderConfig group : headerConfig.getGroupHeaders()) {
            for (SheetHeaderConfig.IndicatorColumnConfig col : group.getIndicators()) {
                DeclareIndicatorValueDO valueDO = reportValues.get(col.getIndicatorCode());

                if (col.isDynamicContainer()) {
                    // 动态容器: 解析JSON数组，每个子字段提取一个值（含选项映射）
                    String jsonStr = valueDO != null ? valueDO.getValueStr() : null;
                    for (SheetHeaderConfig.DynamicFieldConfig field : col.getDynamicFields()) {
                        String fieldValue = extractDynamicFieldValue(jsonStr, field.getFieldCode(), field.getFieldType(), field.getFieldOptions());
                        String columnKey = col.getIndicatorCode() + "." + field.getFieldCode();
                        indicatorValues.put(columnKey, fieldValue);
                    }
                } else {
                    // 普通指标: 直接格式化值（含选项映射）
                    String displayValue = formatIndicatorValue(valueDO, col.getValueType(), col.getValueOptions());
                    indicatorValues.put(col.getIndicatorCode(), displayValue);
                }
            }
        }

        builder.indicatorValues(indicatorValues);
        return builder.build();
    }

    /**
     * 提取动态容器中指定字段的值 (多条记录用换行连接)
     */
    private String extractDynamicFieldValue(String jsonArray, String fieldCode, String fieldType, String fieldOptions) {
        if (jsonArray == null || jsonArray.isEmpty()) {
            return "";
        }
        try {
            JSONArray entries = JSON.parseArray(jsonArray);
            if (entries == null || entries.isEmpty()) {
                return "";
            }
            int maxEntries = MAX_CONTAINER_ENTRIES;
            List<String> values = new ArrayList<>();
            int count = 0;
            for (Object entryObj : entries) {
                if (count >= maxEntries) {
                    values.add("...");
                    break;
                }
                JSONObject entry = (JSONObject) entryObj;
                Object val = entry.get(fieldCode);
                if (val != null) {
                    // 对选项类字段进行值→标签映射
                    String formatted = formatContainerFieldValue(val, fieldType, fieldOptions);
                    values.add(formatted);
                }
                count++;
            }
            return String.join("\n", values);
        } catch (Exception e) {
            log.warn("解析动态容器JSON字段失败: fieldCode={}, json={}", fieldCode, jsonArray, e);
            return "";
        }
    }

    /**
     * 格式化动态容器字段值（含选项映射）
     */
    private String formatContainerFieldValue(Object val, String fieldType, String fieldOptions) {
        if (val == null) {
            return "";
        }
        // 选项类字段需要做值→标签映射
        if ("radio".equals(fieldType) || "select".equals(fieldType)) {
            return mapOptionValueToLabel(val.toString(), fieldOptions);
        }
        if ("checkbox".equals(fieldType) || "multiSelect".equals(fieldType)) {
            return mapMultiOptionValuesToLabels(val.toString(), fieldOptions);
        }
        // 其他类型直接返回字符串
        if (val instanceof Number) {
            return ((Number) val).toString();
        }
        return val.toString();
    }

    /**
     * 格式化普通指标值
     * @param valueOptions 指标选项定义JSON，用于单选/多选/单选下拉/多选下拉的 值→标签 映射
     */
    private String formatIndicatorValue(DeclareIndicatorValueDO valueDO, Integer valueType, String valueOptions) {
        if (valueDO == null) {
            return "";
        }
        switch (valueType != null ? valueType : 0) {
            case 1: // 数字
                return valueDO.getValueNum() != null ? valueDO.getValueNum().toPlainString() : "";
            case 2: // 字符串
                return valueDO.getValueStr() != null ? valueDO.getValueStr() : "";
            case 3: // 布尔
                Boolean bool = valueDO.getValueBool();
                return bool != null ? (bool ? "是" : "否") : "";
            case 4: // 日期
                return valueDO.getValueDate() != null ? valueDO.getValueDate().toString() : "";
            case 5: // 长文本
                return valueDO.getValueText() != null ? valueDO.getValueText() : "";
            case 6: // 单选
            case 10: // 单选下拉
                return mapOptionValueToLabel(valueDO.getValueStr(), valueOptions);
            case 7: // 多选
            case 11: // 多选下拉
                return mapMultiOptionValuesToLabels(valueDO.getValueStr(), valueOptions);
            case 8: // 日期区间
                String start = valueDO.getValueDateStart() != null ? valueDO.getValueDateStart().toString() : "";
                String end = valueDO.getValueDateEnd() != null ? valueDO.getValueDateEnd().toString() : "";
                if (!start.isEmpty() || !end.isEmpty()) {
                    return start + " 至 " + end;
                }
                return "";
            case 12: // 动态容器 (不应走到这里，已在上面处理)
                return valueDO.getValueStr() != null ? valueDO.getValueStr() : "";
            default:
                return "";
        }
    }

    /**
     * 单选/单选下拉: 将选项值映射为标签
     * @param value 选项值（如 "1"）
     * @param valueOptions JSON数组 [{value, label}, ...]
     */
    private String mapOptionValueToLabel(String value, String valueOptions) {
        if (value == null || value.isEmpty() || valueOptions == null || valueOptions.isEmpty()) {
            return value != null ? value : "";
        }
        Map<String, String> optionMap = parseOptionMap(valueOptions);
        return optionMap.getOrDefault(value, value);
    }

    /**
     * 多选/多选下拉: 将逗号分隔的多个选项值映射为标签，用 "、" 连接
     * @param values 逗号分隔的选项值（如 "1,3"）
     * @param valueOptions JSON数组 [{value, label}, ...]
     */
    private String mapMultiOptionValuesToLabels(String values, String valueOptions) {
        if (values == null || values.isEmpty() || valueOptions == null || valueOptions.isEmpty()) {
            return values != null ? values : "";
        }
        Map<String, String> optionMap = parseOptionMap(valueOptions);
        String[] parts = values.split(",");
        List<String> labels = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                labels.add(optionMap.getOrDefault(trimmed, trimmed));
            }
        }
        return String.join("、", labels);
    }

    /**
     * 解析 valueOptions JSON 为 Map<value, label>
     * 兼容 value 为字符串或数字的情况
     */
    private Map<String, String> parseOptionMap(String valueOptions) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            JSONArray options = JSON.parseArray(valueOptions);
            if (options == null) {
                return map;
            }
            for (Object item : options) {
                JSONObject obj = (JSONObject) item;
                String val = obj.getString("value");
                String label = obj.getString("label");
                if (val != null && label != null) {
                    map.put(val, label);
                }
            }
        } catch (Exception e) {
            log.warn("解析指标选项定义失败: {}", valueOptions, e);
        }
        return map;
    }

    // ==================== 私有方法: 生成Excel ====================

    /**
     * 将Sheet数据列表写入HTTP响应
     */
    private void writeExcelToResponse(List<ExportSheetData> sheetDataList, HttpServletResponse response) throws IOException {
        if (sheetDataList == null || sheetDataList.isEmpty()) {
            throw new IOException("没有可导出的数据");
        }

        // 生成文件名
        String filename = "国家局填报数据_" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";

        // 设置响应头
        response.setHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // 生成Excel
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) { // 内存保留1000行
            workbook.setCompressTempFiles(true); // 压缩临时文件

            for (ExportSheetData sheetData : sheetDataList) {
                createSheet(workbook, sheetData);
            }

            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    /**
     * 创建单个Sheet
     */
    private void createSheet(SXSSFWorkbook workbook, ExportSheetData sheetData) {
        SXSSFSheet sheet = workbook.createSheet(sheetData.getSheetName());

        // 1. 绘制三行表头
        buildSheetHeader(sheet, sheetData.getHeaderConfig());

        // 2. 填充数据行
        buildSheetDataRows(sheet, sheetData);

        // 3. 设置列宽
        setColumnWidths(sheet, sheetData.getHeaderConfig());

        // 4. 冻结前三行 (表头)
        sheet.createFreezePane(0, 3);
    }

    /**
     * 绘制三行表头
     */
    private void buildSheetHeader(SXSSFSheet sheet, SheetHeaderConfig config) {
        // 创建前三行
        Row row1 = sheet.createRow(0); // 一级分组
        Row row2 = sheet.createRow(1); // 二级分组
        Row row3 = sheet.createRow(2); // 指标编码+标题

        // 设置行高
        row1.setHeight((short) 500);
        row2.setHeight((short) 400);
        row3.setHeight((short) 600);

        // 单元格样式
        CellStyle headerStyle = createHeaderCellStyle(sheet.getWorkbook());

        // 1. 绘制固定列 (Row3)
        for (int i = 0; i < config.getFixedColumns().size(); i++) {
            SheetHeaderConfig.ColumnDefinition col = config.getFixedColumns().get(i);
            Cell cell = row3.createCell(i);
            cell.setCellValue(col.getTitle());
            cell.setCellStyle(headerStyle);
            // 固定列跨3行
            if (col.getRowSpan() > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 2, i, i));
            }
        }

        // 2. 绘制指标列
        int columnIndex = config.getFixedColumns().size();
        for (SheetHeaderConfig.GroupHeaderConfig group : config.getGroupHeaders()) {
            int groupStartCol = columnIndex;

            // Row1: 一级分组名 (合并)
            Cell cell1 = row1.createCell(groupStartCol);
            cell1.setCellValue(group.getLevel1Name());
            cell1.setCellStyle(headerStyle);

            // Row3: 指标列标题，同时收集该分组占据的列
            List<Integer> colIndices = new ArrayList<>();
            for (SheetHeaderConfig.IndicatorColumnConfig col : group.getIndicators()) {
                if (col.isDynamicContainer()) {
                    // 动态容器: 每个子字段一列
                    for (SheetHeaderConfig.DynamicFieldConfig field : col.getDynamicFields()) {
                        Cell cell3 = row3.createCell(columnIndex);
                        cell3.setCellValue(field.getColumnTitle(col.getIndicatorCode()));
                        cell3.setCellStyle(headerStyle);
                        colIndices.add(columnIndex);
                        columnIndex++;
                    }
                } else {
                    // 普通指标: 一列
                    Cell cell3 = row3.createCell(columnIndex);
                    String title = col.getIndicatorCode() + " " + col.getIndicatorName();
                    if (col.getUnit() != null && !"-".equals(col.getUnit()) && !col.getUnit().isEmpty()) {
                        title += " [单位:" + col.getUnit() + "]";
                    }
                    cell3.setCellValue(title);
                    cell3.setCellStyle(headerStyle);
                    colIndices.add(columnIndex);
                    columnIndex++;
                }
            }

            int groupEndCol = columnIndex - 1;

            // 合并Row1一级分组名
            if (groupStartCol < groupEndCol) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, groupStartCol, groupEndCol));
            }

            // Row2: 二级分组名 (如果有)
            if (group.hasLevel2()) {
                if (groupStartCol < groupEndCol) {
                    Cell cell2 = row2.createCell(groupStartCol);
                    cell2.setCellValue(group.getLevel2Name());
                    cell2.setCellStyle(headerStyle);
                    sheet.addMergedRegion(new CellRangeAddress(1, 1, groupStartCol, groupEndCol));
                } else {
                    Cell cell2 = row2.createCell(groupStartCol);
                    cell2.setCellValue(group.getLevel2Name());
                    cell2.setCellStyle(headerStyle);
                }
            }
        }
    }

    /**
     * 创建表头单元格样式
     */
    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 填充数据行
     */
    private void buildSheetDataRows(SXSSFSheet sheet, ExportSheetData sheetData) {
        SheetHeaderConfig config = sheetData.getHeaderConfig();
        CellStyle dataStyle = createDataCellStyle(sheet.getWorkbook());
        CellStyle wrapStyle = createWrapCellStyle(sheet.getWorkbook());

        int rowIndex = 3; // 从第4行开始 (前3行是表头)
        for (ExportSheetData.ExportRowData row : sheetData.getRows()) {
            Row dataRow = sheet.createRow(rowIndex++);
            int colIndex = 0;

            // 固定列
            writeCell(dataRow, colIndex++, String.valueOf(row.getSequenceNo()), dataStyle);
            writeCell(dataRow, colIndex++, row.getHospitalName(), dataStyle);
            writeCell(dataRow, colIndex++, row.getUnifiedSocialCreditCode(), dataStyle);
            writeCell(dataRow, colIndex++, row.getMedicalLicenseNo(), dataStyle);
            writeCell(dataRow, colIndex++, row.getProvinceName(), dataStyle);
            writeCell(dataRow, colIndex++, row.getReportYear() != null ? String.valueOf(row.getReportYear()) : "", dataStyle);
            writeCell(dataRow, colIndex++, row.getReportBatch() != null ? "第" + row.getReportBatch() + "期" : "", dataStyle);
            writeCell(dataRow, colIndex++, row.getReportStatus(), dataStyle);
            writeCell(dataRow, colIndex++, row.getNationalReportStatus(), dataStyle);
            writeCell(dataRow, colIndex++, row.getCreateTime(), dataStyle);

            // 指标列
            for (SheetHeaderConfig.GroupHeaderConfig group : config.getGroupHeaders()) {
                for (SheetHeaderConfig.IndicatorColumnConfig col : group.getIndicators()) {
                    if (col.isDynamicContainer()) {
                        // 动态容器: 每个子字段一列，使用自动换行样式
                        for (SheetHeaderConfig.DynamicFieldConfig field : col.getDynamicFields()) {
                            String columnKey = col.getIndicatorCode() + "." + field.getFieldCode();
                            String value = row.getIndicatorValues().getOrDefault(columnKey, "");
                            writeCell(dataRow, colIndex++, value, wrapStyle);
                        }
                    } else {
                        // 普通指标: 一列
                        String value = row.getIndicatorValues().getOrDefault(col.getIndicatorCode(), "");
                        writeCell(dataRow, colIndex++, value, dataStyle);
                    }
                }
            }
        }
    }

    private void writeCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private CellStyle createDataCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createWrapCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 设置列宽
     */
    private void setColumnWidths(SXSSFSheet sheet, SheetHeaderConfig config) {
        // 固定列宽
        for (int i = 0; i < config.getFixedColumns().size(); i++) {
            sheet.setColumnWidth(i, config.getFixedColumns().get(i).getWidth() * 256);
        }
        // 指标列宽 (默认15)
        for (int i = config.getFixedColumns().size(); i < config.getTotalColumnCount(); i++) {
            sheet.setColumnWidth(i, DEFAULT_COLUMN_WIDTH * 256);
        }
    }

    // ==================== 私有方法: 辅助方法 ====================

}
