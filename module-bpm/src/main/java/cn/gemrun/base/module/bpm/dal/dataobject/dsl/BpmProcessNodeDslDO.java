package cn.gemrun.base.module.bpm.dal.dataobject.dsl;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BPM 流程节点 DSL 配置 DO
 *
 * 存储每个流程节点的 DSL 配置，用于驱动前端动态渲染审批页面
 *
 * DSL 配置示例：
 * {
 *   "nodeKey": "province_audit",
 *   "cap": "AUDIT",
 *   "actions": "agree,reject,back",
 *   "roles": ["PROVINCE"],
 *   "assign": {
 *     "type": "STATIC_ROLE",
 *     "source": "provinceRole"
 *   },
 *   "signRule": "MAJORITY",
 *   "backStrategy": "TO_START",
 *   "bizStatus": "PRO_AUDIT"
 * }
 *
 * @author Gemini
 */
@TableName("bpm_process_node_dsl")
@KeySequence("bpm_process_node_dsl_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmProcessNodeDslDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 流程定义Key（对应 Flowable 的 processDefinitionKey）
     */
    private String processDefinitionKey;

    /**
     * 节点Key（对应 Flowable 的 nodeId）
     */
    private String nodeKey;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * DSL 配置 JSON
     * 包含：cap, actions, roles, assign, signRule, backStrategy, reSubmit, bizStatus, vars, ui
     */
    private String dslConfig;

    /**
     * 是否启用
     * 0 = 禁用
     * 1 = 启用
     */
    private Integer enabled;

    /**
     * 备注
     */
    private String remark;

}
