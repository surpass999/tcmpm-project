package cn.gemrun.base.module.bpm.framework.flowable.core.candidate.strategy.business;

import cn.gemrun.base.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;

/**
 * 业务相关审批人策略的抽象类
 *
 * @author Gemini
 */
public abstract class AbstractBpmTaskCandidateBusinessStrategy implements BpmTaskCandidateStrategy {

    /**
     * 流程变量中业务创建人ID的Key
     */
    protected static final String BUSINESS_CREATOR_ID_VAR = "businessCreatorId";

}
