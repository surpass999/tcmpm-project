package cn.gemrun.base.module.system.framework.security;

import cn.gemrun.base.framework.security.core.LoginUser;
import cn.gemrun.base.module.infra.api.config.ConfigApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PasswordMustChangeInterceptor implements HandlerInterceptor {

    private static final String CONFIG_KEY = "system.user.force-password-change-enabled";

    /** 不需要强制改密的路径白名单 */
    private static final Set<String> WHITE_LIST = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
        "/system/user/profile/update-password",
        "/system/auth/logout",
        "/system/auth/get-permission-info",
        "/system/user/profile/get",
        "/system/oauth2/token",
        "/assets/",
        "/favicon"
    )));

    private final ConfigApi configApi;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String enabled = configApi.getConfigValueByKey(CONFIG_KEY);
        if (!"true".equalsIgnoreCase(enabled)) {
            return true;
        }

        LoginUser loginUser = cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return true;
        }

        if (Boolean.TRUE.equals(loginUser.getPasswordMustChange())) {
            String path = request.getRequestURI();
            boolean isWhite = WHITE_LIST.stream().anyMatch(path::contains);
            if (!isWhite) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":1002018,\"msg\":\"\\u8bf7\\u5148\\u4fee\\u6539\\u5bc6\\u7801\",\"data\":null}");
                return false;
            }
        }
        return true;
    }
}
