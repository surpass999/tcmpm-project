package cn.gemrun.base.module.system.convert.user;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.gemrun.base.framework.common.enums.CommonStatusEnum;
import cn.gemrun.base.framework.excel.core.function.ExcelColumnSelectFunction;
import cn.gemrun.base.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.gemrun.base.module.system.dal.dataobject.dept.DeptDO;
import cn.gemrun.base.module.system.service.dept.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Excel 部门列转换器
 * <p>
 * 实现两个接口：
 * 1. Converter&lt;Long&gt;：单元格的值转换（Long → "部门名 (ID)"）
 * 2. ExcelColumnSelectFunction：下拉框选项（树形缩进展示）
 * <p>
 * 注意：FastExcel 使用 new 实例化 Converter，不走 Spring DI，
 * 所以内部所有依赖都通过 SpringUtil.getBean() 获取。
 */
@Slf4j
@Component
public class DeptConvert implements Converter<Long>, ExcelColumnSelectFunction {

    private DeptService getDeptService() {
        return SpringUtil.getBean(DeptService.class);
    }

    // ========== Converter<Long> 实现：处理单元格读写 ==========

    @Override
    public Class<?> supportJavaTypeKey() {
        return Long.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public Long convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                  GlobalConfiguration globalConfiguration) {
        String label = cellData.getStringValue();
        if (StrUtil.isBlank(label)) {
            return null;
        }
        // 从 "部门名 (ID)" 中解析出 ID
        return parseIdFromLabel(label);
    }

    @Override
    public WriteCellData<?> convertToExcelData(Long value, ExcelContentProperty contentProperty,
                                               GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        // 查找部门，生成 "部门名 (ID)" 标签
        DeptDO dept = getDeptService().getDept(value);
        if (dept == null) {
            return new WriteCellData<>(value.toString());
        }
        String label = dept.getName() + " (" + dept.getId() + ")";
        return new WriteCellData<>(label);
    }

    // ========== ExcelColumnSelectFunction 实现：下拉框选项 ==========

    @Override
    public String getName() {
        return "deptListForExcel";
    }

    /**
     * 返回树形缩进格式的部门列表，用于 Excel 下拉框
     */
    @Override
    public List<String> getOptions() {
        List<DeptDO> allDepts = getDeptService().getDeptList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        if (allDepts.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建父子关系 map
        Map<Long, List<DeptDO>> childrenMap = new HashMap<>();
        for (DeptDO dept : allDepts) {
            childrenMap.computeIfAbsent(dept.getParentId(), k -> new ArrayList<>()).add(dept);
        }
        childrenMap.values().forEach(list -> list.sort(Comparator.comparing(DeptDO::getSort)));

        // 从根节点开始递归，构建所有部门的层级标签
        List<String> results = new ArrayList<>();
        List<DeptDO> rootDepts = childrenMap.getOrDefault(0L, Collections.emptyList());
        for (int i = 0; i < rootDepts.size(); i++) {
            buildLabelsRecursive(rootDepts.get(i), i != rootDepts.size() - 1, new ArrayList<>(), childrenMap, results);
        }
        return results;
    }

    // ========== 私有方法 ==========

    /**
     * 从 "部门名 (ID)" 格式中解析出 ID
     */
    private Long parseIdFromLabel(String label) {
        int left = label.lastIndexOf('(');
        int right = label.lastIndexOf(')');
        if (left > 0 && right > left) {
            try {
                return Long.parseLong(label.substring(left + 1, right));
            } catch (NumberFormatException e) {
                log.error("[parseIdFromLabel] 无法从 label 解析部门 ID: {}", label);
            }
        }
        return null;
    }

    /**
     * 递归构建树形层级标签
     *
     * @param ancestorsHasNextSibling 当前节点的祖先链中，是否有后续兄弟节点（影响竖线显示）
     * @param ancestorHasNextList    每层祖先是否有后续兄弟节点（影响缩进竖线）
     */
    private void buildLabelsRecursive(DeptDO dept, boolean ancestorsHasNextSibling, List<Boolean> ancestorHasNextList,
                                     Map<Long, List<DeptDO>> childrenMap, List<String> results) {
        // 构建当前节点的缩进
        StringBuilder indent = new StringBuilder();
        for (Boolean hasNext : ancestorHasNextList) {
            indent.append(hasNext ? "│  " : "    ");
        }
        // 当前节点的分叉符号
        indent.append(ancestorsHasNextSibling ? "├── " : "└── ");
        results.add(indent + dept.getName() + " (" + dept.getId() + ")");

        // 递归子节点
        List<DeptDO> children = childrenMap.getOrDefault(dept.getId(), Collections.emptyList());
        children.sort(Comparator.comparing(DeptDO::getSort));
        for (int i = 0; i < children.size(); i++) {
            boolean isLast = (i == children.size() - 1);
            List<Boolean> newList = new ArrayList<>(ancestorHasNextList);
            newList.add(!isLast);
            buildLabelsRecursive(children.get(i), isLast, newList, childrenMap, results);
        }
    }

}
