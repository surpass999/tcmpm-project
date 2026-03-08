package cn.gemrun.base.module.bpm.framework.process.aspect;

import java.util.Collections;
import java.util.List;

/**
 * BPM 数据权限上下文
 *
 * 用于在 AOP 切面中传递权限数据到 Service/Mapper 层
 *
 * 使用方式：
 * 1. 在 Controller 方法上添加 @BpmProcessQuery
 * 2. 在 Service/Mapper 中通过 BpmDataPermissionContext.getAuthorizedBusinessIds() 获取有权限的业务ID
 *
 * @author Gemini
 */
public class BpmDataPermissionContext {

    /**
     * ThreadLocal 存储权限数据
     */
    private static final ThreadLocal<List<Long>> AUTHORIZED_BUSINESS_IDS = new ThreadLocal<>();

    /**
     * 流程分类
     */
    private static final ThreadLocal<String> PROCESS_CATEGORY = new ThreadLocal<>();

    /**
     * 是否只查询流程数据
     */
    private static final ThreadLocal<Boolean> PROCESS_ONLY = new ThreadLocal<>();

    /**
     * 获取有权限的业务ID列表
     */
    public static List<Long> getAuthorizedBusinessIds() {
        List<Long> ids = AUTHORIZED_BUSINESS_IDS.get();
        return ids != null ? ids : Collections.emptyList();
    }

    /**
     * 设置有权限的业务ID列表
     */
    public static void setAuthorizedBusinessIds(List<Long> businessIds) {
        AUTHORIZED_BUSINESS_IDS.set(businessIds);
    }

    /**
     * 获取流程分类
     */
    public static String getProcessCategory() {
        return PROCESS_CATEGORY.get();
    }

    /**
     * 设置流程分类
     */
    public static void setProcessCategory(String category) {
        PROCESS_CATEGORY.set(category);
    }

    /**
     * 是否只查询流程数据
     */
    public static boolean isProcessOnly() {
        Boolean flag = PROCESS_ONLY.get();
        return flag != null && flag;
    }

    /**
     * 设置是否只查询流程数据
     */
    public static void setProcessOnly(boolean processOnly) {
        PROCESS_ONLY.set(processOnly);
    }

    /**
     * 检查是否有数据权限
     * 如果返回空列表，表示没有权限或未配置
     */
    public static boolean hasDataPermission() {
        List<Long> ids = getAuthorizedBusinessIds();
        return ids != null && !ids.isEmpty();
    }

    /**
     * 清理 ThreadLocal
     */
    public static void clear() {
        AUTHORIZED_BUSINESS_IDS.remove();
        PROCESS_CATEGORY.remove();
        PROCESS_ONLY.remove();
    }

}
