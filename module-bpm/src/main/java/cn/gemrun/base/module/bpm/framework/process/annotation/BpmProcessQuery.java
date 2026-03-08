package cn.gemrun.base.module.bpm.framework.process.annotation;

import java.lang.annotation.*;

/**
 * BPM 流程数据权限查询注解
 *
 * 用于查询用户有权限看到的业务数据
 *
 * 使用方式：
 * 1. 自动推断分类：
 *    @BpmProcessQuery
 *    public List<Project> getProjectList() { }
 *    // 自动解析：DeclareProjectController -> processCategory = "declare-project"
 *
 * 2. 手动指定分类：
 *    @BpmProcessQuery(processCategory = "project")
 *    public List<Project> getProjectList() { }
 *
 * 3. 只查流程数据（不关联业务表）：
 *    @BpmProcessQuery(processOnly = true)
 *    public List<BpmBusinessProcessDO> getMyProcesses() { }
 *
 * @author Gemini
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BpmProcessQuery {

    /**
     * 流程分类
     * 用于分组管理，如：declare-project、project、achievement 等
     *
     * 不填则自动推断：
     * - 从类名推断：如 DeclareProjectController -> project
     * - 从包名推断：如 cn.gemrun.base.module.declare.controller.admin.project
     *   -> 模块名(declare) + 业务名(project) = declare-project
     */
    String processCategory() default "";

    /**
     * 是否只查询流程数据（不关联业务表）
     * true - 只返回 bpm_business_process 数据，用于场景2（我的待办/已办）
     * false - 关联业务表，返回业务数据 + 流程状态，用于场景1（业务列表）
     *
     * 默认 false
     */
    boolean processOnly() default false;

}
