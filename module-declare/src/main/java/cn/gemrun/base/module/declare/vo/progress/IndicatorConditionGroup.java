package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 指标条件组（组内 OR 连接）
 *
 * @author Gemini
 */
@Data
public class IndicatorConditionGroup implements Serializable {

    /**
     * 条件组内连接逻辑：OR（组内各条件 OR 连接）
     */
    private String innerLogic = "OR";

    /**
     * 该组的条件列表
     */
    private List<IndicatorCondition> conditions = new ArrayList<>();
}
