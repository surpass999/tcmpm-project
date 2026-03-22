package cn.gemrun.base.module.bpm.enums;

/**
 * BPM 模块操作日志常量
 * 目的：统一管理操作日志的类型和模板，也减少 Service 里各种"复杂"字符串
 *
 * @author Gemini
 */
public interface BpmOperateLogConstants {

    // ======================= 流程任务 BPM_TASK =======================

    String BPM_TASK_TYPE = "BPM 流程任务";
    String BPM_TASK_APPROVE_SUB_TYPE = "审批通过";
    String BPM_TASK_APPROVE_SUCCESS = "审批通过了任务【{{#taskName}}】，意见：{{#reason}}";
    String BPM_TASK_REJECT_SUB_TYPE = "审批拒绝";
    String BPM_TASK_REJECT_SUCCESS = "审批拒绝了任务【{{#taskName}}】，原因：{{#reason}}";
    String BPM_TASK_RETURN_SUB_TYPE = "退回任务";
    String BPM_TASK_RETURN_SUCCESS = "将任务【{{#taskName}}】从节点【{{#fromNode}}】退回到【{{#toNode}}】，原因：{{#reason}}";
    String BPM_TASK_WITHDRAW_SUB_TYPE = "撤回任务";
    String BPM_TASK_WITHDRAW_SUCCESS = "撤回了任务【{{#taskName}}】";
    String BPM_TASK_DELEGATE_SUB_TYPE = "委派任务";
    String BPM_TASK_DELEGATE_SUCCESS = "将任务【{{#taskName}}】委派给【{getAdminUserById{#delegateUserId}}】";
    String BPM_TASK_TRANSFER_SUB_TYPE = "转派任务";
    String BPM_TASK_TRANSFER_SUCCESS = "将任务【{{#taskName}}】转派给【{getAdminUserById{#assigneeUserId}}】";
    String BPM_TASK_CREATE_SIGN_SUB_TYPE = "加签";
    String BPM_TASK_CREATE_SIGN_SUCCESS = "在任务【{{#taskName}}】添加加签人【{{#signUserNames}}】";
    String BPM_TASK_DELETE_SIGN_SUB_TYPE = "减签";
    String BPM_TASK_DELETE_SIGN_SUCCESS = "在任务【{{#taskName}}】移除减签人【{getAdminUserById{#cancelUserId}}】";
    String BPM_TASK_COPY_SUB_TYPE = "任务抄送";
    String BPM_TASK_COPY_SUCCESS = "将任务【{{#taskName}}】抄送给【{{#copyUserNames}}】";
    String BPM_TASK_SET_NEXT_ASSIGNEES_SUB_TYPE = "设置下一审批人";
    String BPM_TASK_SET_NEXT_ASSIGNEES_SUCCESS = "为任务【{{#taskName}}】设置了下一节点审批人";

    // ======================= 流程实例 BPM_PROCESS_INSTANCE =======================

    String BPM_PROCESS_INSTANCE_TYPE = "BPM 流程实例";
    String BPM_PROCESS_INSTANCE_CREATE_SUB_TYPE = "发起流程";
    String BPM_PROCESS_INSTANCE_CREATE_SUCCESS = "发起了【{{#processName}}】流程";
    String BPM_PROCESS_INSTANCE_CANCEL_SUB_TYPE = "取消流程";
    String BPM_PROCESS_INSTANCE_CANCEL_SUCCESS = "取消了【{{#processName}}】流程，原因：{{#reason}}";
    String BPM_PROCESS_INSTANCE_CANCEL_ADMIN_SUB_TYPE = "管理员取消流程";
    String BPM_PROCESS_INSTANCE_CANCEL_ADMIN_SUCCESS = "【管理员】取消了【{{#processName}}】流程，原因：{{#reason}}";

    // ======================= 流程定义 BPM_MODEL =======================

    String BPM_MODEL_TYPE = "BPM 流程模型";
    String BPM_MODEL_CREATE_SUB_TYPE = "创建流程模型";
    String BPM_MODEL_CREATE_SUCCESS = "创建了流程模型【{{#reqVO.name}}】";
    String BPM_MODEL_UPDATE_SUB_TYPE = "更新流程模型";
    String BPM_MODEL_UPDATE_SUCCESS = "更新了流程模型【{{#modelName}}】";
    String BPM_MODEL_DELETE_SUB_TYPE = "删除流程模型";
    String BPM_MODEL_DELETE_SUCCESS = "删除了流程模型【{{#modelName}}】";
    String BPM_MODEL_DEPLOY_SUB_TYPE = "部署流程";
    String BPM_MODEL_DEPLOY_SUCCESS = "部署了流程模型【{{#modelName}}】为流程【{{#processDefinitionName}}】";

    // ======================= 流程分类 BPM_CATEGORY =======================

    String BPM_CATEGORY_TYPE = "BPM 流程分类";
    String BPM_CATEGORY_CREATE_SUB_TYPE = "创建流程分类";
    String BPM_CATEGORY_CREATE_SUCCESS = "创建了流程分类【{{#reqVO.name}}】";
    String BPM_CATEGORY_UPDATE_SUB_TYPE = "更新流程分类";
    String BPM_CATEGORY_UPDATE_SUCCESS = "更新了流程分类【{{#categoryName}}】";
    String BPM_CATEGORY_DELETE_SUB_TYPE = "删除流程分类";
    String BPM_CATEGORY_DELETE_SUCCESS = "删除了流程分类【{{#categoryName}}】";

    // ======================= 流程表单 BPM_FORM =======================

    String BPM_FORM_TYPE = "BPM 流程表单";
    String BPM_FORM_CREATE_SUB_TYPE = "创建流程表单";
    String BPM_FORM_CREATE_SUCCESS = "创建了流程表单【{{#reqVO.name}}】";
    String BPM_FORM_UPDATE_SUB_TYPE = "更新流程表单";
    String BPM_FORM_UPDATE_SUCCESS = "更新了流程表单【{{#formName}}】";
    String BPM_FORM_DELETE_SUB_TYPE = "删除流程表单";
    String BPM_FORM_DELETE_SUCCESS = "删除了流程表单【{{#formName}}】";

    // ======================= 流程监听器 BPM_PROCESS_LISTENER =======================

    String BPM_PROCESS_LISTENER_TYPE = "BPM 流程监听器";
    String BPM_PROCESS_LISTENER_CREATE_SUB_TYPE = "创建流程监听器";
    String BPM_PROCESS_LISTENER_CREATE_SUCCESS = "创建了流程监听器【{{#reqVO.name}}】";
    String BPM_PROCESS_LISTENER_UPDATE_SUB_TYPE = "更新流程监听器";
    String BPM_PROCESS_LISTENER_UPDATE_SUCCESS = "更新了流程监听器【{{#listenerName}}】";
    String BPM_PROCESS_LISTENER_DELETE_SUB_TYPE = "删除流程监听器";
    String BPM_PROCESS_LISTENER_DELETE_SUCCESS = "删除了流程监听器【{{#listenerName}}】";

    // ======================= 流程表达式 BPM_PROCESS_EXPRESSION =======================

    String BPM_PROCESS_EXPRESSION_TYPE = "BPM 流程表达式";
    String BPM_PROCESS_EXPRESSION_CREATE_SUB_TYPE = "创建流程表达式";
    String BPM_PROCESS_EXPRESSION_CREATE_SUCCESS = "创建了流程表达式【{{#reqVO.name}}】";
    String BPM_PROCESS_EXPRESSION_UPDATE_SUB_TYPE = "更新流程表达式";
    String BPM_PROCESS_EXPRESSION_UPDATE_SUCCESS = "更新了流程表达式【{{#expressionName}}】";
    String BPM_PROCESS_EXPRESSION_DELETE_SUB_TYPE = "删除流程表达式";
    String BPM_PROCESS_EXPRESSION_DELETE_SUCCESS = "删除了流程表达式【{{#expressionName}}】";

    // ======================= 用户组 BPM_USER_GROUP =======================

    String BPM_USER_GROUP_TYPE = "BPM 用户组";
    String BPM_USER_GROUP_CREATE_SUB_TYPE = "创建用户组";
    String BPM_USER_GROUP_CREATE_SUCCESS = "创建了用户组【{{#reqVO.name}}】";
    String BPM_USER_GROUP_UPDATE_SUB_TYPE = "更新用户组";
    String BPM_USER_GROUP_UPDATE_SUCCESS = "更新了用户组【{{#groupName}}】";
    String BPM_USER_GROUP_DELETE_SUB_TYPE = "删除用户组";
    String BPM_USER_GROUP_DELETE_SUCCESS = "删除了用户组【{{#groupName}}】";
}
