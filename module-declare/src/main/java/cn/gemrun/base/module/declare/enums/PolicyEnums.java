package cn.gemrun.base.module.declare.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 政策通知相关枚举
 *
 * @author Gemini
 */
public class PolicyEnums {

    /**
     * 发布单位：1=国家局，2=省局
     */
    @Getter
    @AllArgsConstructor
    public enum ReleaseDept {
        NATIONAL(1, "国家局"),
        PROVINCIAL(2, "省局");

        private final Integer value;
        private final String name;

        public static ReleaseDept valueOf(Integer value) {
            if (value == null) {
                return null;
            }
            return Arrays.stream(values())
                    .filter(e -> e.getValue().equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * 目标范围：1=全国，2=全省
     */
    @Getter
    @AllArgsConstructor
    public enum TargetScope {
        NATIONAL(1, "全国"),
        PROVINCIAL(2, "全省");

        private final Integer value;
        private final String name;

        public static TargetScope valueOf(Integer value) {
            if (value == null) {
                return null;
            }
            return Arrays.stream(values())
                    .filter(e -> e.getValue().equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * 政策状态：1=已发布，2=已下架
     */
    @Getter
    @AllArgsConstructor
    public enum Status {
        PUBLISHED(1, "已发布"),
        UNPUBLISHED(2, "已下架");

        private final Integer value;
        private final String name;

        public static Status valueOf(Integer value) {
            if (value == null) {
                return null;
            }
            return Arrays.stream(values())
                    .filter(e -> e.getValue().equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * 阅读状态：1=已阅读，2=已过期
     */
    @Getter
    @AllArgsConstructor
    public enum ReadStatus {
        READ(1, "已阅读"),
        EXPIRED(2, "已过期");

        private final Integer value;
        private final String name;

        public static ReadStatus valueOf(Integer value) {
            if (value == null) {
                return null;
            }
            return Arrays.stream(values())
                    .filter(e -> e.getValue().equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * 回执状态：1=已阅读，2=已反馈，3=已取消
     */
    @Getter
    @AllArgsConstructor
    public enum ReceiptStatus {
        READ(1, "已阅读"),
        FEEDBACK(2, "已反馈"),
        CANCELLED(3, "已取消");

        private final Integer value;
        private final String name;

        public static ReceiptStatus valueOf(Integer value) {
            if (value == null) {
                return null;
            }
            return Arrays.stream(values())
                    .filter(e -> e.getValue().equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * 政策类型：1=政策文件，2=工作通知，3=经验分享，4=典型案例，5=工具模板
     */
    @Getter
    @AllArgsConstructor
    public enum PolicyType {
        POLICY_FILE(1, "政策文件"),
        WORK_NOTICE(2, "工作通知"),
        EXPERIENCE(3, "经验分享"),
        TYPICAL_CASE(4, "典型案例"),
        TOOL_TEMPLATE(5, "工具模板");

        private final Integer value;
        private final String name;

        public static PolicyType valueOf(Integer value) {
            if (value == null) {
                return null;
            }
            return Arrays.stream(values())
                    .filter(e -> e.getValue().equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }

}
