package cn.gemrun.base.module.declare.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 指标搜索辅助工具类
 *
 * @author Gemini
 */
@Component
public class IndicatorSearchHelper {

    /**
     * 将逗号分隔的搜索值拆分为 List
     * 用于 has_any / has_all 操作符的多值处理
     */
    public static List<String> splitValues(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
