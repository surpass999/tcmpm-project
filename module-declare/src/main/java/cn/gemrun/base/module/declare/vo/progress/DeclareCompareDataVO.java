package cn.gemrun.base.module.declare.vo.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 进度填报数据对比响应 VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclareCompareDataVO {

    /**
     * 申报记录A基础信息
     */
    private DeclareProgressReportVO reportA;

    /**
     * 申报记录B基础信息
     */
    private DeclareProgressReportVO reportB;

    /**
     * 指标对比数据列表（已按一级分组+二级分组+指标代号排序）
     */
    private List<DeclareCompareIndicatorRowVO> indicators;
}
