package cn.gemrun.base.module.bpm.framework.dsl;

import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslConfig;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DSL 下一节点预测器
 * 基于 DSL 节点顺序预测下一个审批节点
 *
 * @author Gemini
 */
@Slf4j
@Component
public class DslNextNodePredictor {

    /**
     * 预测下一审批节点
     *
     * @param currentNodeKey    当前节点 Key
     * @param dslNodes         DSL 节点列表（按顺序排列）
     * @param businessProcess  业务流程记录
     * @param bpmnModel       BPMN 模型
     * @return 下一节点列表
     */
    public List<BpmApprovalDetailRespVO.ActivityNode> predictNextNodes(
            String currentNodeKey,
            List<DslConfig> dslNodes,
            BpmBusinessProcessDO businessProcess,
            BpmnModel bpmnModel) {

        List<BpmApprovalDetailRespVO.ActivityNode> nextNodes = new ArrayList<>();

        if (dslNodes == null || dslNodes.isEmpty()) {
            log.warn("[DslNextNodePredictor] DSL 节点列表为空，无法预测");
            return nextNodes;
        }

        // 找到当前节点的索引
        int currentIndex = -1;
        for (int i = 0; i < dslNodes.size(); i++) {
            if (dslNodes.get(i).getNodeKey().equals(currentNodeKey)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0) {
            log.warn("[DslNextNodePredictor] 未找到当前节点: {}", currentNodeKey);
            return nextNodes;
        }

        // 预测下一个节点
        if (currentIndex + 1 < dslNodes.size()) {
            DslConfig nextDslNode = dslNodes.get(currentIndex + 1);
            BpmApprovalDetailRespVO.ActivityNode nextNode = convertToActivityNode(nextDslNode);
            nextNodes.add(nextNode);
            log.info("[DslNextNodePredictor] 预测下一节点: current={}, next={}", currentNodeKey, nextDslNode.getNodeKey());
        } else {
            log.info("[DslNextNodePredictor] 当前节点是最后一个节点，无下一节点: {}", currentNodeKey);
        }

        return nextNodes;
    }

    /**
     * 预测所有后续节点
     *
     * @param currentNodeKey   当前节点 Key
     * @param dslNodes        DSL 节点列表
     * @return 后续节点列表
     */
    public List<BpmApprovalDetailRespVO.ActivityNode> predictAllFollowingNodes(
            String currentNodeKey,
            List<DslConfig> dslNodes) {

        List<BpmApprovalDetailRespVO.ActivityNode> followingNodes = new ArrayList<>();

        if (dslNodes == null || dslNodes.isEmpty()) {
            return followingNodes;
        }

        // 找到当前节点的索引
        int currentIndex = -1;
        for (int i = 0; i < dslNodes.size(); i++) {
            if (dslNodes.get(i).getNodeKey().equals(currentNodeKey)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0) {
            return followingNodes;
        }

        // 添加所有后续节点
        for (int i = currentIndex + 1; i < dslNodes.size(); i++) {
            DslConfig dslNode = dslNodes.get(i);
            followingNodes.add(convertToActivityNode(dslNode));
        }

        log.info("[DslNextNodePredictor] 预测后续节点: current={}, count={}", currentNodeKey, followingNodes.size());

        return followingNodes;
    }

    /**
     * 从 BPMN 模型中提取 DSL 节点列表
     * 按流程顺序排列
     *
     * @param bpmnModel BPMN 模型
     * @return DSL 节点列表
     */
    public List<DslConfig> extractDslNodesFromBpmn(BpmnModel bpmnModel) {
        List<DslConfig> dslNodes = new ArrayList<>();

        if (bpmnModel == null || bpmnModel.getProcesses() == null) {
            return dslNodes;
        }

        // 从 BPMN 模型中提取所有 UserTask
        // 这里简化处理，实际需要按流程顺序排序
        for (org.flowable.bpmn.model.Process process : bpmnModel.getProcesses()) {
            for (FlowElement element : process.getFlowElements()) {
                if (element instanceof UserTask) {
                    UserTask userTask = (UserTask) element;

                    DslConfig dslNode = new DslConfig();
                    dslNode.setNodeKey(userTask.getId());
                    dslNode.setName(userTask.getName());
                    dslNode.setCap("AUDIT"); // 默认审批节点
                    // TODO: 从扩展属性中读取 DSL 配置

                    dslNodes.add(dslNode);
                }
            }
        }

        return dslNodes;
    }

    /**
     * 从业务表中提取 DSL 节点列表
     *
     * @param businessProcess 业务流程记录
     * @return DSL 节点列表
     */
    public List<DslConfig> extractDslNodesFromBusiness(BpmBusinessProcessDO businessProcess) {
        List<DslConfig> dslNodes = new ArrayList<>();

        if (businessProcess == null || StrUtil.isBlank(businessProcess.getDslJson())) {
            return dslNodes;
        }

        // 解析 DSL JSON
        try {
            cn.hutool.json.JSONObject jsonObject = cn.hutool.json.JSONUtil.parseObj(businessProcess.getDslJson());

            // 检查是否有 nodes 数组
            if (jsonObject.containsKey("nodes")) {
                cn.hutool.json.JSONArray nodesArray = jsonObject.getJSONArray("nodes");
                if (nodesArray != null) {
                    for (Object obj : nodesArray) {
                        DslConfig node = cn.hutool.json.JSONUtil.toBean((cn.hutool.json.JSONObject) obj, DslConfig.class);
                        dslNodes.add(node);
                    }
                }
            } else if (jsonObject.containsKey("nodeKey")) {
                // 单节点配置，添加当前节点
                DslConfig node = cn.hutool.json.JSONUtil.toBean(jsonObject, DslConfig.class);
                dslNodes.add(node);
            }
        } catch (Exception e) {
            log.warn("[DslNextNodePredictor] 解析 DSL 节点列表失败: {}", e.getMessage());
        }

        return dslNodes;
    }

    /**
     * 合并多个来源的 DSL 节点列表
     *
     * @param businessNodes 业务表节点列表
     * @param bpmnNodes    BPMN 模型节点列表
     * @return 合并后的节点列表
     */
    public List<DslConfig> mergeDslNodes(List<DslConfig> businessNodes, List<DslConfig> bpmnNodes) {
        if (businessNodes != null && !businessNodes.isEmpty()) {
            return businessNodes;
        }

        if (bpmnNodes != null && !bpmnNodes.isEmpty()) {
            return bpmnNodes;
        }

        return new ArrayList<>();
    }

    /**
     * 将 DSL 节点转换为 ActivityNode
     */
    private BpmApprovalDetailRespVO.ActivityNode convertToActivityNode(DslConfig dslNode) {
        BpmApprovalDetailRespVO.ActivityNode node = new BpmApprovalDetailRespVO.ActivityNode();
        node.setId(dslNode.getNodeKey());
        node.setName(dslNode.getName());
        node.setStatus(0); // 待处理
        node.setNodeType(getNodeType(dslNode.getCap()));

        return node;
    }

    /**
     * 获取节点类型
     */
    private Integer getNodeType(String cap) {
        if (cap == null) {
            return 1;
        }
        switch (cap.toUpperCase()) {
            case "FILL":
                return 4;
            case "MODIFY":
                return 5;
            case "CONFIRM":
                return 6;
            case "ARCHIVE":
                return 7;
            case "PUBLISH":
                return 8;
            case "COUNTERSIGN":
                return 2;
            case "EXPERT_SELECT":
                return 3;
            default:
                return 1;
        }
    }

}
