package cn.gemrun.base.module.declare.framework.process.annotation;

import java.lang.annotation.*;

/**
 * 流程处理注解
 *
 * 使用方式：
 * 1. 自动解析 businessType：
 *    @DeclareProcess
 *    public void submitFiling(Long id) { }
 *    // 自动解析为：declare:filing:submit
 *
 * 2. 手动指定 businessType：
 *    @DeclareProcess(businessType = "declare:filing:submit")
 *    public void submitFiling(Long id) { }
 *
 * 3. 指定是否必须启动流程：
 *    @DeclareProcess(required = false)  // 没有配置就跳过
 *    public void startRectification(Long projectId) { }
 *
 * @author Gemini
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeclareProcess {

    /**
     * 业务类型
     * 格式：{模块名}:{业务名}:{动作}
     * 例如：declare:filing:submit
     *
     * 不填则自动解析：
     * - 模块名：取 controller 路径第一部分
     * - 业务名：取类名（去掉 Controller/DO 后缀）
     * - 动作名：取方法名
     */
    String businessType() default "";

    /**
     * 是否必须启动流程
     * true - 没有配置则抛出异常
     * false - 没有配置则跳过流程（默认值）
     */
    boolean required() default false;

}
