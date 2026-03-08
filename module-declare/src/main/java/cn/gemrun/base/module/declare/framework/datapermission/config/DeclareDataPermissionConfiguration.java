package cn.gemrun.base.module.declare.framework.datapermission.config;

import cn.gemrun.base.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
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
            // 备案表 - 按部门过滤（使用数据库列名 dept_id）
            rule.addDeptColumn(FilingDO.class, "dept_id");
        };
    }

}
