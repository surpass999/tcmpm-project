package cn.gemrun.base.module.declare.enums;

/**
 * 申报模块操作日志常量
 * 目的：统一管理操作日志的类型和模板，也减少 Service 里各种"复杂"字符串
 *
 * @author Gemini
 */
public interface DeclareLogRecordConstants {

    // ======================= 备案管理 FILING =======================

    String FILING_TYPE = "申报 备案";
    String FILING_CREATE_SUB_TYPE = "创建备案";
    String FILING_CREATE_SUCCESS = "创建了备案【{{#createReqVO.orgName}}】";
    String FILING_UPDATE_SUB_TYPE = "更新备案";
    String FILING_UPDATE_SUCCESS = "更新了备案: {_DIFF{#updateReqVO}}";
    String FILING_DELETE_SUB_TYPE = "删除备案";
    String FILING_DELETE_SUCCESS = "删除了备案";
    String FILING_BATCH_DELETE_SUB_TYPE = "批量删除备案";
    String FILING_BATCH_DELETE_SUCCESS = "批量删除了{{#ids.size()}}条备案";
    String FILING_START_PROCESS_SUB_TYPE = "发起备案流程";
    String FILING_START_PROCESS_SUCCESS = "发起了备案【{{#id}}】的审批流程";
    String FILING_AUTO_CREATE_PROJECT_SUB_TYPE = "自动立项";
    String FILING_AUTO_CREATE_PROJECT_SUCCESS = "备案审核通过，自动创建了项目";
    String FILING_STATUS_CHANGE_SUB_TYPE = "备案状态变更";
    String FILING_STATUS_CHANGE_SUCCESS = "备案【{{#bizStatus}}】";

    // ======================= 项目管理 PROJECT =======================

    String PROJECT_TYPE = "申报 项目";
    String PROJECT_CREATE_SUB_TYPE = "创建项目";
    String PROJECT_CREATE_SUCCESS = "创建了项目";
    String PROJECT_UPDATE_SUB_TYPE = "更新项目";
    String PROJECT_UPDATE_SUCCESS = "更新了项目: {_DIFF{#updateReqVO}}";
    String PROJECT_DELETE_SUB_TYPE = "删除项目";
    String PROJECT_DELETE_SUCCESS = "删除了项目";
    String PROJECT_BATCH_DELETE_SUB_TYPE = "批量删除项目";
    String PROJECT_BATCH_DELETE_SUCCESS = "批量删除了{{#ids.size()}}个项目";
    String PROJECT_STATUS_CHANGE_SUB_TYPE = "更新项目状态";
    String PROJECT_STATUS_CHANGE_SUCCESS = "更新了项目状态为【{{#status}}】";
    String PROJECT_START_PROCESS_SUB_TYPE = "发起项目流程";
    String PROJECT_START_PROCESS_SUCCESS = "发起了项目的流程";

    // ======================= 过程记录 PROJECT_PROCESS =======================

    String PROJECT_PROCESS_TYPE = "申报 过程记录";
    String PROJECT_PROCESS_CREATE_SUB_TYPE = "创建过程记录";
    String PROJECT_PROCESS_CREATE_SUCCESS = "创建了过程记录【{{#createReqVO.processTitle}}】";
    String PROJECT_PROCESS_UPDATE_SUB_TYPE = "更新过程记录";
    String PROJECT_PROCESS_UPDATE_SUCCESS = "更新了过程记录: {_DIFF{#updateReqVO}}";
    String PROJECT_PROCESS_DELETE_SUB_TYPE = "删除过程记录";
    String PROJECT_PROCESS_DELETE_SUCCESS = "删除了过程记录";
    String PROJECT_PROCESS_BATCH_DELETE_SUB_TYPE = "批量删除过程记录";
    String PROJECT_PROCESS_BATCH_DELETE_SUCCESS = "批量删除了{{#ids.size()}}条过程记录";
    String PROJECT_PROCESS_SUBMIT_SUB_TYPE = "提交过程记录";
    String PROJECT_PROCESS_SUBMIT_SUCCESS = "提交了过程记录";
    String PROJECT_PROCESS_WITHDRAW_SUB_TYPE = "撤回过程记录";
    String PROJECT_PROCESS_WITHDRAW_SUCCESS = "撤回了过程记录";
    String PROJECT_PROCESS_STATUS_CHANGE_SUB_TYPE = "过程记录状态变更";
    String PROJECT_PROCESS_STATUS_CHANGE_SUCCESS = "过程记录【{{#bizStatus}}】";

    // ======================= 成果管理 ACHIEVEMENT =======================

    String ACHIEVEMENT_TYPE = "申报 成果";
    String ACHIEVEMENT_CREATE_SUB_TYPE = "创建成果";
    String ACHIEVEMENT_CREATE_SUCCESS = "创建了成果【{{#createReqVO.achievementName}}】";
    String ACHIEVEMENT_UPDATE_SUB_TYPE = "更新成果";
    String ACHIEVEMENT_UPDATE_SUCCESS = "更新了成果: {_DIFF{#updateReqVO}}";
    String ACHIEVEMENT_DELETE_SUB_TYPE = "删除成果";
    String ACHIEVEMENT_DELETE_SUCCESS = "删除了成果";
    String ACHIEVEMENT_SUBMIT_SUB_TYPE = "提交成果审核";
    String ACHIEVEMENT_SUBMIT_SUCCESS = "提交了成果进行审核";
    String ACHIEVEMENT_RECOMMEND_SUB_TYPE = "推荐至国家局";
    String ACHIEVEMENT_RECOMMEND_SUCCESS = "推荐了成果至国家局";
    String ACHIEVEMENT_RECOMMEND_LIBRARY_SUB_TYPE = "推荐至推广库";
    String ACHIEVEMENT_RECOMMEND_LIBRARY_SUCCESS = "推荐成果【{{#id}}】至推广库";
    String ACHIEVEMENT_STATUS_CHANGE_SUB_TYPE = "成果状态变更";
    String ACHIEVEMENT_STATUS_CHANGE_SUCCESS = "成果【{{#bizStatus}}】";

    // ======================= 评审管理 REVIEW =======================

    String REVIEW_TYPE = "申报 评审";
    String REVIEW_TASK_CREATE_SUB_TYPE = "创建评审任务";
    String REVIEW_TASK_CREATE_SUCCESS = "创建了评审任务【{{#createReqVO.taskName}}】";
    String REVIEW_TASK_UPDATE_SUB_TYPE = "更新评审任务";
    String REVIEW_TASK_UPDATE_SUCCESS = "更新了评审任务: {_DIFF{#updateReqVO}}";
    String REVIEW_TASK_DELETE_SUB_TYPE = "删除评审任务";
    String REVIEW_TASK_DELETE_SUCCESS = "删除了评审任务";
    String REVIEW_TASK_ASSIGN_SUB_TYPE = "分配评审专家";
    String REVIEW_TASK_ASSIGN_SUCCESS = "为评审任务分配了专家";
    String REVIEW_RESULT_RECEIVE_SUB_TYPE = "接收评审任务";
    String REVIEW_RESULT_RECEIVE_SUCCESS = "专家接收了评审任务";
    String REVIEW_RESULT_START_SUB_TYPE = "开始评审";
    String REVIEW_RESULT_START_SUCCESS = "专家开始评审";
    String REVIEW_RESULT_SUBMIT_SUB_TYPE = "提交评审结果";
    String REVIEW_RESULT_SUBMIT_SUCCESS = "专家提交了评审";
    String REVIEW_RESULT_APPLY_AVOID_SUB_TYPE = "申请回避";
    String REVIEW_RESULT_APPLY_AVOID_SUCCESS = "专家申请回避评审任务";
    String REVIEW_RESULT_REJECT_SUB_TYPE = "拒绝评审任务";
    String REVIEW_RESULT_REJECT_SUCCESS = "专家拒绝了评审任务";

    // ======================= 专家管理 EXPERT =======================

    String EXPERT_TYPE = "申报 专家";
    String EXPERT_CREATE_SUB_TYPE = "创建专家";
    String EXPERT_CREATE_SUCCESS = "创建了专家【{{#createReqVO.expertName}}】";
    String EXPERT_UPDATE_SUB_TYPE = "更新专家";
    String EXPERT_UPDATE_SUCCESS = "更新了专家: {_DIFF{#updateReqVO}}";
    String EXPERT_DELETE_SUB_TYPE = "删除专家";
    String EXPERT_DELETE_SUCCESS = "删除了专家";
    String EXPERT_STATUS_CHANGE_SUB_TYPE = "修改专家状态";
    String EXPERT_STATUS_CHANGE_SUCCESS = "将专家的状态变更为";

    // ======================= 培训管理 TRAINING =======================

    String TRAINING_TYPE = "申报 培训";
    String TRAINING_CREATE_SUB_TYPE = "创建培训活动";
    String TRAINING_CREATE_SUCCESS = "创建了培训活动【{{#createReqVO.name}}】";
    String TRAINING_UPDATE_SUB_TYPE = "更新培训活动";
    String TRAINING_UPDATE_SUCCESS = "更新了培训活动";
    String TRAINING_DELETE_SUB_TYPE = "删除培训活动";
    String TRAINING_DELETE_SUCCESS = "删除了培训活动";
    String TRAINING_PUBLISH_SUB_TYPE = "发布培训活动";
    String TRAINING_PUBLISH_SUCCESS = "发布了培训活动";
    String TRAINING_UNPUBLISH_SUB_TYPE = "取消发布培训";
    String TRAINING_UNPUBLISH_SUCCESS = "取消了培训活动的发布";
    String TRAINING_REGISTER_SUB_TYPE = "报名参加培训";
    String TRAINING_REGISTER_SUCCESS = "报名参加了培训活动";
    String TRAINING_CANCEL_REGISTER_SUB_TYPE = "取消培训报名";
    String TRAINING_CANCEL_REGISTER_SUCCESS = "取消了培训活动的报名";
    String TRAINING_SIGN_IN_SUB_TYPE = "培训签到";
    String TRAINING_SIGN_IN_SUCCESS = "在培训活动签到";
    String TRAINING_FEEDBACK_SUB_TYPE = "提交培训反馈";
    String TRAINING_FEEDBACK_SUCCESS = "为培训活动提交了反馈评分";

    // ======================= 政策管理 POLICY =======================

    String POLICY_TYPE = "申报 政策";
    String POLICY_CREATE_SUB_TYPE = "创建政策通知";
    String POLICY_CREATE_SUCCESS = "创建了政策通知";
    String POLICY_UPDATE_SUB_TYPE = "更新政策通知";
    String POLICY_UPDATE_SUCCESS = "更新了政策通知";
    String POLICY_DELETE_SUB_TYPE = "删除政策通知";
    String POLICY_DELETE_SUCCESS = "删除了政策通知";
    String POLICY_PUBLISH_SUB_TYPE = "发布政策通知";
    String POLICY_PUBLISH_SUCCESS = "发布了政策通知";
    String POLICY_UNPUBLISH_SUB_TYPE = "下架政策通知";
    String POLICY_UNPUBLISH_SUCCESS = "下架了政策通知";
    String POLICY_MARK_READ_SUB_TYPE = "标记通知已读";
    String POLICY_MARK_READ_SUCCESS = "查看了政策通知";
    String POLICY_FEEDBACK_SUB_TYPE = "提交通知反馈";
    String POLICY_FEEDBACK_SUCCESS = "为政策通知提交了反馈";

    // ======================= 整改管理 RECTIFICATION =======================

    String RECTIFICATION_TYPE = "申报 整改";
    String RECTIFICATION_CREATE_SUB_TYPE = "创建整改记录";
    String RECTIFICATION_CREATE_SUCCESS = "创建了整改记录【{{#createReqVO.rectifyContent}}】";
    String RECTIFICATION_UPDATE_SUB_TYPE = "更新整改记录";
    String RECTIFICATION_UPDATE_SUCCESS = "更新了整改记录: {_DIFF{#updateReqVO}}";
    String RECTIFICATION_DELETE_SUB_TYPE = "删除整改记录";
    String RECTIFICATION_DELETE_SUCCESS = "删除了整改记录";
    String RECTIFICATION_BATCH_DELETE_SUB_TYPE = "批量删除整改记录";
    String RECTIFICATION_BATCH_DELETE_SUCCESS = "批量删除了{{#ids.size()}}条整改记录";

    // ======================= 指标管理 INDICATOR =======================

    String INDICATOR_TYPE = "申报 指标";
    String INDICATOR_CREATE_SUB_TYPE = "创建指标";
    String INDICATOR_CREATE_SUCCESS = "创建了指标【{{#createReqVO.indicatorName}}】";
    String INDICATOR_UPDATE_SUB_TYPE = "更新指标";
    String INDICATOR_UPDATE_SUCCESS = "更新了指标【{{#indicatorName}}】: {_DIFF{#reqVO}}";
    String INDICATOR_DELETE_SUB_TYPE = "删除指标";
    String INDICATOR_DELETE_SUCCESS = "删除了指标【{{#indicatorName}}】";

    // ======================= 指标配置 INDICATOR_CONFIG =======================

    String INDICATOR_CONFIG_TYPE = "申报 指标配置";
    String INDICATOR_CONFIG_CREATE_SUB_TYPE = "创建指标配置";
    String INDICATOR_CONFIG_CREATE_SUCCESS = "创建了指标配置";
    String INDICATOR_CONFIG_UPDATE_SUB_TYPE = "更新指标配置";
    String INDICATOR_CONFIG_UPDATE_SUCCESS = "更新了指标配置";
    String INDICATOR_CONFIG_DELETE_SUB_TYPE = "删除指标配置";
    String INDICATOR_CONFIG_DELETE_SUCCESS = "删除了指标配置";
    String INDICATOR_CONFIG_BATCH_SAVE_SUB_TYPE = "批量保存指标配置";
    String INDICATOR_CONFIG_BATCH_SAVE_SUCCESS = "批量保存了{{#configs.size()}}条指标配置";

    // ======================= 指标值 INDICATOR_VALUE =======================

    String INDICATOR_VALUE_TYPE = "申报 指标值";
    String INDICATOR_VALUE_SAVE_SUB_TYPE = "保存指标值";
    String INDICATOR_VALUE_SAVE_SUCCESS = "保存了业务ID【{{#businessId}}】的指标值";
    String INDICATOR_VALUE_DELETE_SUB_TYPE = "删除指标值";
    String INDICATOR_VALUE_DELETE_SUCCESS = "删除了业务ID【{{#businessId}}】的指标值";

    // ======================= 指标口径 INDICATOR_CALIBER =======================

    String INDICATOR_CALIBER_TYPE = "申报 指标口径";
    String INDICATOR_CALIBER_CREATE_SUB_TYPE = "创建指标口径";
    String INDICATOR_CALIBER_CREATE_SUCCESS = "创建了指标口径";
    String INDICATOR_CALIBER_UPDATE_SUB_TYPE = "更新指标口径";
    String INDICATOR_CALIBER_UPDATE_SUCCESS = "更新了指标口径";
    String INDICATOR_CALIBER_DELETE_SUB_TYPE = "删除指标口径";
    String INDICATOR_CALIBER_DELETE_SUCCESS = "删除了指标口径";

    // ======================= 指标联合规则 INDICATOR_JOINT_RULE =======================

    String INDICATOR_JOINT_RULE_TYPE = "申报 指标规则";
    String INDICATOR_JOINT_RULE_CREATE_SUB_TYPE = "创建指标规则";
    String INDICATOR_JOINT_RULE_CREATE_SUCCESS = "创建了指标规则";
    String INDICATOR_JOINT_RULE_UPDATE_SUB_TYPE = "更新指标规则";
    String INDICATOR_JOINT_RULE_UPDATE_SUCCESS = "更新了指标规则";
    String INDICATOR_JOINT_RULE_DELETE_SUB_TYPE = "删除指标规则";
    String INDICATOR_JOINT_RULE_DELETE_SUCCESS = "删除了指标规则";

    // ======================= 定时任务 JOB =======================

    String JOB_ANNUAL_SUMMARY_SUB_TYPE = "创建年度总结";
    String JOB_ANNUAL_SUMMARY_SUCCESS = "【定时任务】为项目【{{#projectName}}】创建了{{#isAnnual ? '年度总结' : '半年报'}}过程记录并启动流程";
    String JOB_ACCEPTANCE_CHECK_SUB_TYPE = "创建验收申请";
    String JOB_ACCEPTANCE_CHECK_SUCCESS = "【定时任务】为项目【{{#projectName}}】创建了验收申请并启动流程";
}
