package cn.gemrun.base.module.declare.framework.datapermission.config;

import cn.gemrun.base.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * declare 模块的数据权限 Configuration
 *
 * @author Gemini
 */
@Configuration(proxyBeanMethods = false)
public class DeclareDataPermissionConfiguration {

    @Bean
    public DeptDataPermissionRuleCustomizer declareDeptDataPermissionRuleCustomizer() {
        return rule -> {
            // 进度报表 - 按部门过滤（使用 dept_id 而非 hospital_id，因为数据权限框架需要用部门ID进行过滤）
            rule.addDeptColumn("declare_progress_report", "dept_id");
        };
    }

}
