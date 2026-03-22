package cn.gemrun.base.module.bpm.framework.flowable.core.enums;

import cn.hutool.core.util.ArrayUtil;
import cn.gemrun.base.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * BPM 任务的候选人策略枚举
 *
 * 例如说：分配给指定人审批
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum BpmTaskCandidateStrategyEnum implements ArrayValuable<Integer> {

    ROLE(10, "角色"),
    DEPT_MEMBER(20, "部门的成员"), // 包括负责人
    DEPT_LEADER(21, "部门的负责人"),
    MULTI_DEPT_LEADER_MULTI(23, "连续多级部门的负责人"),
    POST(22, "岗位"),
    USER(30, "用户"),
    APPROVE_USER_SELECT(34, "审批人自身"), // 当前审批人，可在审批时，选择下一个节点的审批人
    START_USER_SELECT(35, "发起人自选"), // 申请人自己，可在提交申请时，选择此节点的审批人
    START_USER(36, "发起人自己"), // 申请人自己, 一般紧挨开始节点，常用于发起人信息审核场景
    START_USER_DEPT_LEADER(37, "发起人部门负责人"),
    START_USER_DEPT_LEADER_MULTI(38, "发起人连续多级部门的负责人"),
    USER_GROUP(40, "用户组"),
    FORM_USER(50, "表单内用户字段"),
    FORM_DEPT_LEADER(51, "表单内部门负责人"),
    EXPRESSION(60, "流程表达式"), // 表达式 ExpressionManager
    ASSIGN_EMPTY(1, "审批人为空"),
    /**
     * 上级部门+岗位：查找发起人的上级部门，并获取该部门下指定岗位的用户
     * 参数格式：岗位ID,部门层级（如：100,2 表示查找发起人第2级上级部门下的岗位用户）
     */
    SUPERIOR_DEPT_POST(70, "上级部门+岗位"),
    /**
     * 业务发起人：通过业务ID找到业务创建人，获取其所在部门
     * 适用于整改流程，由省级/国家级在业务列表页面发起，携带业务ID，通过业务创建人找到其部门
     * 参数格式：业务表名,业务ID字段名（如：declare_filing,filingId）
     */
    BUSINESS_START_USER(71, "业务发起人"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(BpmTaskCandidateStrategyEnum::getStrategy).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer strategy;
    /**
     * 描述
     */
    private final String description;

    public static BpmTaskCandidateStrategyEnum valueOf(Integer strategy) {
        return ArrayUtil.firstMatch(o -> o.getStrategy().equals(strategy), values());
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
