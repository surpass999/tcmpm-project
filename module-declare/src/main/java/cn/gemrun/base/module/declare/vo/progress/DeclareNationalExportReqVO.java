package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;
import java.util.List;

/**
 * 国家局导出请求参数 VO
 *
 * @author Gemini
 */
@Data
public class DeclareNationalExportReqVO extends DeclareNationalSearchReqVO {

    private static final long serialVersionUID = 1L;

    // ========== 导出选项 ==========

    /**
     * 是否包含动态容器明细（默认 true）
     * false时，动态容器指标只导出总条目数
     */
    private Boolean includeContainerDetail = true;

    /**
     * 动态容器每字段最大条目数（默认50）
     * 超过此数量截断并显示 "..."
     */
    private Integer containerMaxEntries = 50;

    /**
     * 填报记录ID列表（用于导出选中记录）
     * 优先级最高，传此参数时忽略其他筛选条件
     */
    private List<Long> reportIds;
}
