package cn.gemrun.base.module.bpm.framework.process.annotation;

import java.lang.annotation.*;

/**
 * BPM 流程处理注解
 *
 * 使用方式：
 * 1. 自动解析 businessType：
 *    @BpmProcess
 *    public void submitFiling(Long id) { }
 *    // 自动解析为：模块名:业务名:方法名
 *
 * 2. 手动指定 businessType：
 *    @BpmProcess(businessType = "declare:filing:submit")
 *    public void submitFiling(Long id) { }
 *
 * 3. 指定是否必须启动流程：
 *    @BpmProcess(required = false)  // 没有配置就跳过
 *    public void withdrawFiling(Long id) { }
 *
 * @author Gemini
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BpmProcess {

    /**
     * 业务类型
     * 格式：{模块名}:{业务名}:{动作}
     * 例如：declare:filing:submit
     *
     * 不填则自动解析：
     * - 模块名：取 controller 路径第一部分
     * - 业务名：取类名（去掉 Controller 后缀）
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
