package cn.gemrun.base.module.bpm.framework.process.service;

import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import cn.gemrun.base.module.bpm.framework.process.annotation.BpmProcessQuery;

import java.util.List;

/**
 * BPM 数据权限服务
 *
 * 用于查询用户有权限看到的业务数据
 *
 * 工作原理：
 * 1. 根据 processCategory 查询该分类下所有 businessType
 * 2. 解析流程定义中的任务分配配置（DSL）
 * 3. 根据当前用户的角色/部门/岗位，判断是否有权限
 * 4. 返回有权限的业务ID列表
 *
 * @author Gemini
 */
public interface BpmDataPermissionService {

    /**
     * 查询用户有权限的业务ID列表
     *
     * @param processCategory 流程分类（不填则自动推断）
     * @param processOnly     是否只查询流程数据
     * @param method          调用的方法（用于自动推断 processCategory）
     * @return 有权限的业务ID列表（processOnly=true时返回流程数据，否则返回业务数据）
     */
    List<Long> getAuthorizedBusinessIds(String processCategory, boolean processOnly, java.lang.reflect.Method method);

    /**
     * 查询用户有权限的业务ID列表（从注解中解析配置）
     *
     * @param bpmProcessQuery 注解
     * @param method          调用的方法
     * @return 有权限的业务ID列表
     */
    List<Long> getAuthorizedBusinessIds(BpmProcessQuery bpmProcessQuery, java.lang.reflect.Method method);

    /**
     * 查询用户参与的流程数据（场景2：我的待办/已办）
     *
     * @param processCategory 流程分类
     * @return 用户参与的流程列表
     */
    List<BpmBusinessProcessDO> getUserProcesses(String processCategory);

    /**
     * 自动推断 processCategory
     *
     * 推断规则：
     * - 类名：DeclareProjectController -> project
     * - 包名：cn.gemrun.base.module.declare.controller.admin.project
     *   -> 模块名(declare) + 业务名(project) = declare-project
     *
     * @param method 方法
     * @return 流程分类
     */
    String parseProcessCategory(java.lang.reflect.Method method);

}
