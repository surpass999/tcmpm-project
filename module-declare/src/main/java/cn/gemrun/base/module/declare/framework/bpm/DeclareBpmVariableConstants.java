package cn.gemrun.base.module.declare.framework.bpm;

/**
 * 申报模块 BPM 流程变量常量
 * <p>
 * 统一管理申报相关流程中使用的流程变量名称
 *
 * @author Gemini
 */
public class DeclareBpmVariableConstants {

    private DeclareBpmVariableConstants() {
    }

    // ========== 业务变量 ==========

    /**
     * 业务标识（用于显示）
     */
    public static final String BUSINESS_KEY = "businessKey";

    /**
     * 业务类型
     */
    public static final String BUSINESS_TYPE = "businessType";

    // ========== 项目相关变量 ==========

    /**
     * 项目ID
     */
    public static final String PROJECT_ID = "projectId";

    /**
     * 项目名称
     */
    public static final String PROJECT_NAME = "projectName";

    /**
     * 医院/机构ID
     */
    public static final String HOSPITAL_ID = "hospitalId";

    /**
     * 医院/机构名称
     */
    public static final String HOSPITAL_NAME = "hospitalName";

    // ========== 过程相关变量 ==========

    /**
     * 过程记录ID
     */
    public static final String PROCESS_ID = "processId";

    /**
     * 过程类型
     * <p>
     * 1=建设过程, 2=半年报, 3=年度总结, 4=中期评估, 5=整改记录, 6=验收申请
     */
    public static final String PROCESS_TYPE = "processType";

    /**
     * 过程标题
     */
    public static final String PROCESS_TITLE = "processTitle";

    /**
     * 报告周期开始时间
     */
    public static final String REPORT_PERIOD_START = "reportPeriodStart";

    /**
     * 报告周期结束时间
     */
    public static final String REPORT_PERIOD_END = "reportPeriodEnd";

    // ========== 评审相关变量 ==========

    /**
     * 评审是否通过
     */
    public static final String REVIEW_PASS = "reviewPass";

    /**
     * 评审总分
     */
    public static final String TOTAL_SCORE = "totalScore";

    /**
     * 通过分数
     */
    public static final String PASS_SCORE = "passScore";

    /**
     * 预期评审专家数量
     */
    public static final String EXPECTED_REVIEW_COUNT = "expectedReviewCount";

}
