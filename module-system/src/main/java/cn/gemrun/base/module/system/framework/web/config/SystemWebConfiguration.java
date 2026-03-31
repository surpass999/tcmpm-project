package cn.gemrun.base.module.system.framework.web.config;

import cn.gemrun.base.framework.swagger.config.BaseSwaggerAutoConfiguration;
import cn.gemrun.base.module.system.framework.security.PasswordMustChangeInterceptor;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * system 模块的 web 组件的 Configuration
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
public class SystemWebConfiguration implements WebMvcConfigurer {

    /**
     * system 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi systemGroupedOpenApi() {
        return BaseSwaggerAutoConfiguration.buildGroupedOpenApi("system");
    }

    @Resource
    private PasswordMustChangeInterceptor passwordMustChangeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(passwordMustChangeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/logout", "/assets/**", "/favicon.ico");
    }
}
