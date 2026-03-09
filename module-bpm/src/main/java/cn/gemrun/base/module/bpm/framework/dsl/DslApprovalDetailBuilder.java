package cn.gemrun.base.module.bpm.framework.dsl;

import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslAssign;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslConfig;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DSL 审批详情构建器
 * 基于 DSL 配置构建审批流程详情
 *
 * @author Gemini
 */
@Slf4j
@Component
public class DslApprovalDetailBuilder {

    /**
     * 从 DSL 构建审批详情节点列表
     *
     * @param businessProcess 业务流程记录
     * @param bpmnModel      BPMN 模型
     * @param historicTasks  历史任务列表
     * @param currentTask    当前任务（可能为空）
     * @return 审批详情节点列表
     */
    public List<BpmApprovalDetailRespVO.ActivityNode> buildActivityNodes(
            BpmBusinessProcessDO businessProcess,
            BpmnModel bpmnModel,
            List<BpmApprovalDetailRespVO.ActivityNodeTask> historicTasks,
            BpmApprovalDetailRespVO.ActivityNodeTask currentTask) {

        // 获取流程定义ID
        final String processDefinitionId;
        if (bpmnModel.getProcesses() != null && !bpmnModel.getProcesses().isEmpty()) {
            processDefinitionId = bpmnModel.getProcesses().get(0).getId();
        } else {
            processDefinitionId = null;
        }
        if (processDefinitionId == null) {
            log.error("[DslApprovalDetailBuilder] 无法获取流程定义ID");
            return null;
        }

        String processInstanceId = businessProcess.getProcessInstanceId();

        log.info("[DslApprovalDetailBuilder] 开始构建审批详情: processInstanceId={}", processInstanceId);

        // 1. 解析 DSL 节点列表
        List<DslConfig> dslNodes = parseDslNodes(bpmnModel, businessProcess);
        if (dslNodes == null || dslNodes.isEmpty()) {
            log.warn("[DslApprovalDetailBuilder] 未找到 DSL 节点配置，回退到原有实现");
            return null;
        }

        // 2. 构建节点状态映射
        Map<String, List<BpmApprovalDetailRespVO.ActivityNodeTask>> taskMap = historicTasks.stream()
                .collect(Collectors.groupingBy(task -> {
                    // 优先使用任务定义key，否则使用流程定义key
                    return task.getId() != null ? task.getId() : processDefinitionId;
                }));

        // 3. 构建 ActivityNode 列表
        List<BpmApprovalDetailRespVO.ActivityNode> activityNodes = new ArrayList<>();

        for (DslConfig dslNode : dslNodes) {
            BpmApprovalDetailRespVO.ActivityNode activityNode = new BpmApprovalDetailRespVO.ActivityNode();
            activityNode.setId(dslNode.getNodeKey());
            activityNode.setName(dslNode.getName());
            activityNode.setNodeType(getNodeType(dslNode.getCap()));

            // 设置节点状态
            String nodeKey = dslNode.getNodeKey();
            if (currentTask != null && nodeKey.equals(currentTask.getId())) {
                // 当前进行中的节点
                activityNode.setStatus(1); // 进行中
                activityNode.setStartTime(currentTask.getCreateTime());
            } else if (taskMap.containsKey(nodeKey)) {
                // 已完成的节点
                List<BpmApprovalDetailRespVO.ActivityNodeTask> nodeTasks = taskMap.get(nodeKey);
                activityNode.setTasks(nodeTasks);
                if (nodeTasks != null && !nodeTasks.isEmpty()) {
                    // 取第一个任务的开始时间
                    activityNode.setStartTime(nodeTasks.get(0).getCreateTime());
                    // 检查是否全部完成
                    boolean allCompleted = nodeTasks.stream()
                            .allMatch(t -> t.getStatus() != null && t.getStatus() == 2);
                    if (allCompleted) {
                        activityNode.setStatus(2); // 已完成
                        // 取最后一个完成时间
                        activityNode.setEndTime(nodeTasks.stream()
                                .filter(t -> t.getEndTime() != null)
                                .map(BpmApprovalDetailRespVO.ActivityNodeTask::getEndTime)
                                .reduce((a, b) -> b)
                                .orElse(null));
                    }
                }
            } else {
                // 未来的节点
                activityNode.setStatus(0); // 待处理
            }

            // 设置候选人策略
            activityNode.setCandidateStrategy(getCandidateStrategy(dslNode.getAssign()));

            activityNodes.add(activityNode);
        }

        log.info("[DslApprovalDetailBuilder] 构建完成: processInstanceId={}, 节点数={}", processInstanceId, activityNodes.size());
        return activityNodes;
    }

    /**
     * 解析 DSL 节点列表
     * 优先级：
     * 1. 业务表 dslJson 中的 nodes 数组
     * 2. 从 BPMN 解析 UserTask，使用业务表 DSL 配置匹配节点
     */
    private List<DslConfig> parseDslNodes(BpmnModel bpmnModel, BpmBusinessProcessDO businessProcess) {
        List<DslConfig> nodes = new ArrayList<>();

        // 0. 解析业务表 DSL（可能只有当前节点配置）
        DslConfig currentNodeDsl = null;
        String dslJson = businessProcess.getDslJson();
        if (StrUtil.isNotBlank(dslJson)) {
            try {
                JSONObject jsonObject = JSONUtil.parseObj(dslJson);
                // 检查是否有 nodes 数组（完整流程配置）
                if (jsonObject.containsKey("nodes")) {
                    JSONArray nodesArray = jsonObject.getJSONArray("nodes");
                    if (nodesArray != null && !nodesArray.isEmpty()) {
                        for (Object obj : nodesArray) {
                            DslConfig node = JSONUtil.toBean((JSONObject) obj, DslConfig.class);
                            nodes.add(node);
                        }
                        log.info("[DslApprovalDetailBuilder] 从业务表 DSL 解析到 nodes: count={}", nodes.size());
                        return nodes;
                    }
                }

                // 当前节点 DSL 配置（只有当前节点）
                if (jsonObject.containsKey("nodeKey")) {
                    currentNodeDsl = JSONUtil.toBean(jsonObject, DslConfig.class);
                    log.info("[DslApprovalDetailBuilder] 解析到当前节点 DSL: nodeKey={}", currentNodeDsl.getNodeKey());
                }
            } catch (Exception e) {
                log.warn("[DslApprovalDetailBuilder] 解析业务表 DSL 失败: {}", e.getMessage());
            }
        }

        // 从 BPMN 按流程执行顺序解析 UserTask（使用 simulateProcess 保证顺序正确）
        if (bpmnModel != null) {
            List<FlowElement> flowOrderElements = BpmnModelUtils.simulateProcess(bpmnModel, Collections.emptyMap());
            for (FlowElement element : flowOrderElements) {
                if (!(element instanceof UserTask)) {
                    continue;
                }
                UserTask userTask = (UserTask) element;
                String nodeKey = userTask.getId();

                // 优先使用业务表当前节点 DSL 配置
                DslConfig node = new DslConfig();
                node.setNodeKey(nodeKey);
                node.setName(userTask.getName());

                if (currentNodeDsl != null && nodeKey.equals(currentNodeDsl.getNodeKey())) {
                    // 使用业务表中的 DSL 配置
                    node.setCap(currentNodeDsl.getCap());
                    node.setRoles(currentNodeDsl.getRoles());
                    node.setAssign(currentNodeDsl.getAssign());
                    node.setActions(currentNodeDsl.getActions());
                    node.setSignRule(currentNodeDsl.getSignRule());
                    node.setBackStrategy(currentNodeDsl.getBackStrategy());
                    node.setReSubmit(currentNodeDsl.getReSubmit());
                    node.setBizStatus(currentNodeDsl.getBizStatus());
                    node.setEnable(currentNodeDsl.getEnable());
                    log.info("[DslApprovalDetailBuilder] 节点 {} 使用业务表 DSL 配置", nodeKey);
                } else {
                    // 其他节点：尝试从 BPMN 解析 DSL（目前 BPMN 扩展元素 DSL 不完整，暂用默认值）
                    node.setCap("AUDIT");
                    log.info("[DslApprovalDetailBuilder] 节点 {} 使用默认 AUDIT 配置", nodeKey);
                }
                nodes.add(node);
            }
        }

        log.info("[DslApprovalDetailBuilder] 解析节点数: count={}", nodes.size());
        return nodes;
    }

    /**
     * 获取节点类型
     */
    private Integer getNodeType(String cap) {
        if (cap == null) {
            return 1; // 默认审批节点
        }
        switch (cap.toUpperCase()) {
            case "FILL":
                return 4; // 填报
            case "MODIFY":
                return 5; // 补正
            case "CONFIRM":
                return 6; // 确认
            case "ARCHIVE":
                return 7; // 归档
            case "PUBLISH":
                return 8; // 发布
            case "COUNTERSIGN":
                return 2; // 会签
            case "EXPERT_SELECT":
                return 3; // 选择专家
            default:
                return 1; // 审批
        }
    }

    /**
     * 获取候选人策略
     */
    private Integer getCandidateStrategy(DslAssign assign) {
        if (assign == null || StrUtil.isBlank(assign.getType())) {
            return BpmTaskCandidateStrategyEnum.ASSIGN_EMPTY.getStrategy();
        }

        String type = assign.getType().toUpperCase();
        switch (type) {
            case "STATIC_ROLE":
                return BpmTaskCandidateStrategyEnum.ROLE.getStrategy();
            case "DEPT_POST":
                return BpmTaskCandidateStrategyEnum.DEPT_POST.getStrategy();
            case "DEPT_LEADER":
                return BpmTaskCandidateStrategyEnum.DEPT_LEADER.getStrategy();
            case "START_USER":
                return BpmTaskCandidateStrategyEnum.START_USER.getStrategy();
            case "START_USER_SELECT":
                return BpmTaskCandidateStrategyEnum.START_USER_SELECT.getStrategy();
            case "START_USER_DEPT_LEADER":
                return BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy();
            case "START_USER_DEPT_LEADER_MULTI":
                return BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER_MULTI.getStrategy();
            case "USER":
                return BpmTaskCandidateStrategyEnum.USER.getStrategy();
            case "USER_GROUP":
                return BpmTaskCandidateStrategyEnum.USER_GROUP.getStrategy();
            case "EXPRESSION":
                return BpmTaskCandidateStrategyEnum.EXPRESSION.getStrategy();
            case "DYNAMIC_USER":
                // DYNAMIC_USER 可能对应多种情况，根据 source 进一步判断
                return resolveDynamicUserStrategy(assign.getSource());
            case "FORM_USER":
                return BpmTaskCandidateStrategyEnum.FORM_USER.getStrategy();
            case "FORM_DEPT_LEADER":
                return BpmTaskCandidateStrategyEnum.FORM_DEPT_LEADER.getStrategy();
            default:
                log.warn("[DslApprovalDetailBuilder] 未知的 assign type: {}, 使用默认策略", type);
                return BpmTaskCandidateStrategyEnum.ASSIGN_EMPTY.getStrategy();
        }
    }

    /**
     * 解析动态用户类型对应的策略
     *
     * @param source 动态用户来源
     * @return 候选人策略
     */
    private Integer resolveDynamicUserStrategy(String source) {
        if (StrUtil.isBlank(source)) {
            return BpmTaskCandidateStrategyEnum.ASSIGN_EMPTY.getStrategy();
        }

        switch (source.toLowerCase()) {
            case "expertusers":
                // 专家用户 - 使用用户类型
                return BpmTaskCandidateStrategyEnum.USER.getStrategy();
            case "startuserdeptleader":
                return BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy();
            case "startuserdeptleadermulti":
                return BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER_MULTI.getStrategy();
            default:
                return BpmTaskCandidateStrategyEnum.USER.getStrategy();
        }
    }

    /**
     * 检查是否支持 DSL 审批详情
     *
     * @param businessProcess 业务流程记录
     * @return 是否支持
     */
    public boolean isSupportDslApprovalDetail(BpmBusinessProcessDO businessProcess) {
        if (businessProcess == null) {
            return false;
        }

        // 有 DSL 配置
        if (StrUtil.isNotBlank(businessProcess.getDslJson())) {
            return true;
        }

        return false;
    }

    /**
     * 获取当前节点的 DSL 配置
     *
     * @param businessProcess 业务流程记录
     * @return 当前节点 DSL 配置
     */
    public DslConfig getCurrentNodeDsl(BpmBusinessProcessDO businessProcess) {
        if (businessProcess == null || StrUtil.isBlank(businessProcess.getDslJson())) {
            return null;
        }

        try {
            return JSONUtil.toBean(businessProcess.getDslJson(), DslConfig.class);
        } catch (Exception e) {
            log.warn("[DslApprovalDetailBuilder] 解析当前节点 DSL 失败: {}", e.getMessage());
            return null;
        }
    }

}
