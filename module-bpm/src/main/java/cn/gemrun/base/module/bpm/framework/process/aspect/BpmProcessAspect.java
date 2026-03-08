package cn.gemrun.base.module.bpm.framework.process.aspect;

import cn.gemrun.base.module.bpm.framework.process.annotation.BpmProcess;
import cn.gemrun.base.module.bpm.framework.process.service.BpmProcessService;
import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * BPM 流程处理 AOP 切面
 *
 * 工作流程：
 * 1. 拦截带有 @BpmProcess 注解的方法
 * 2. 自动解析 businessType（如果不填）
 * 3. 查询流程配置
 * 4. 执行业务方法
 * 5. 如果有流程配置，启动流程
 *
 * @author Gemini
 */
@Slf4j
@Aspect
@Component
public class BpmProcessAspect {

    @Resource
    private BpmProcessService processService;

    /**
     * 环绕通知：拦截 @BpmProcess 注解
     */
    @Around("@annotation(bpmProcess)")
    public Object around(ProceedingJoinPoint joinPoint, BpmProcess bpmProcess) throws Throwable {
        // 1. 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 2. 解析 businessType
        String businessType = bpmProcess.businessType();
        if (StrUtil.isBlank(businessType)) {
            // 自动解析
            String className = method.getDeclaringClass().getName();
            businessType = processService.parseBusinessType(method.getName(), className);
        }

        // 3. 获取业务ID（从方法参数中获取，或从返回值中获取）
        Long businessId = extractBusinessId(args);

        // 4. 打印日志
        log.info("BPM流程处理: businessType={}, businessId={}, method={}",
                businessType, businessId, method.getName());

        // 5. 执行业务方法
        Object result = joinPoint.proceed();

        // 6. 如果参数中没有businessId，尝试从返回值中获取（适用于create方法）
        if (businessId == null && result != null) {
            businessId = extractBusinessIdFromResult(result);
            if (businessId != null) {
                log.info("从返回值获取到businessId: {}", businessId);
            }
        }

        // 7. 启动流程（如果有配置）
        if (businessId != null) {
            try {
                Long userId = WebFrameworkUtils.getLoginUserId();
                String processInstanceId = processService.startProcessIfConfigured(
                        businessType, businessId, userId);

                if (processInstanceId != null) {
                    log.info("BPM流程已启动: businessType={}, businessId={}, processInstanceId={}",
                            businessType, businessId, processInstanceId);
                }
            } catch (Exception e) {
                // 流程启动失败不影响业务操作
                log.error("BPM流程启动失败: businessType={}, businessId={}, error={}",
                        businessType, businessId, e.getMessage());
            }
        } else {
            log.warn("无法获取业务ID，跳过BPM流程启动: method={}", method.getName());
        }

        return result;
    }

    /**
     * 从方法参数中提取业务ID
     *
     * 优先提取规则：
     * 1. 第一个 Long 类型的参数
     * 2. 第一个 String 类型的参数（能转成 Long）
     * 3. 查找名为 id、filingId、projectId、achievementId 等的参数
     */
    private Long extractBusinessId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            // 1. Long 类型
            if (arg instanceof Long) {
                return (Long) arg;
            }

            // 2. String 类型（能转成 Long）
            if (arg instanceof String) {
                try {
                    return Long.parseLong((String) arg);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }

            // 3. Integer 类型
            if (arg instanceof Integer) {
                return ((Integer) arg).longValue();
            }
        }

        return null;
    }

    /**
     * 从方法返回值中提取业务ID
     *
     * 适用于 create 方法，返回值是新创建的ID
     * 支持：Long、Integer、String、CommonResult<Long>
     */
    private Long extractBusinessIdFromResult(Object result) {
        if (result == null) {
            return null;
        }

        // 1. Long 类型直接返回
        if (result instanceof Long) {
            return (Long) result;
        }

        // 2. Integer 类型
        if (result instanceof Integer) {
            return ((Integer) result).longValue();
        }

        // 3. String 类型（能转成 Long）
        if (result instanceof String) {
            try {
                return Long.parseLong((String) result);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }

        // 4. CommonResult<Long> 类型（Controller 层最常见的返回类型）
        if (result instanceof CommonResult) {
            Object data = ((CommonResult<?>) result).getData();
            if (data instanceof Long) {
                return (Long) data;
            }
            if (data instanceof Integer) {
                return ((Integer) data).longValue();
            }
        }

        return null;
    }

}
