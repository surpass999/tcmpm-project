package cn.gemrun.base.module.bpm.framework.process.aspect;

import cn.gemrun.base.module.bpm.framework.process.annotation.BpmProcessQuery;
import cn.gemrun.base.module.bpm.framework.process.service.BpmDataPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * BPM 流程数据权限查询 AOP 切面
 *
 * 工作流程：
 * 1. 拦截带有 @BpmProcessQuery 注解的方法
 * 2. 自动解析 processCategory（如果不填）
 * 3. 查询用户有权限的业务ID列表
 * 4. 将权限信息存入 ThreadLocal，供后续查询使用
 * 5. 执行业务方法
 * 6. 清理 ThreadLocal
 *
 * 使用方式：
 * 在 Controller 方法上添加 @BpmProcessQuery 注解
 * 然后在 Service/Mapper 中通过 BpmDataPermissionContext 获取有权限的业务ID
 *
 * @author Gemini
 */
@Slf4j
@Aspect
@Component
public class BpmProcessQueryAspect {

    @Resource
    private BpmDataPermissionService dataPermissionService;

    @Around("@annotation(bpmProcessQuery)")
    public Object around(ProceedingJoinPoint joinPoint, BpmProcessQuery bpmProcessQuery) throws Throwable {
        // 1. 获取方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 2. 解析 processCategory
        String processCategory = bpmProcessQuery.processCategory();
        if (org.apache.commons.lang3.StringUtils.isBlank(processCategory)) {
            processCategory = dataPermissionService.parseProcessCategory(method);
        }

        log.info("BPM数据权限查询: processCategory={}, processOnly={}, method={}",
                processCategory, bpmProcessQuery.processOnly(), method.getName());

        // 3. 查询用户有权限的业务ID列表
        List<Long> authorizedBusinessIds = Collections.emptyList();
        try {
            authorizedBusinessIds = dataPermissionService.getAuthorizedBusinessIds(
                    processCategory, bpmProcessQuery.processOnly(), method);

            // 4. 将权限信息存入 ThreadLocal
            BpmDataPermissionContext.setAuthorizedBusinessIds(authorizedBusinessIds);
            BpmDataPermissionContext.setProcessCategory(processCategory);
            BpmDataPermissionContext.setProcessOnly(bpmProcessQuery.processOnly());

            // 5. 执行业务方法
            Object result = joinPoint.proceed();

            return result;
        } catch (Exception e) {
            log.error("BPM数据权限查询失败: processCategory={}, error={}",
                    processCategory, e.getMessage());
            throw e;
        } finally {
            // 6. 清理 ThreadLocal
            BpmDataPermissionContext.clear();
        }
    }

}
