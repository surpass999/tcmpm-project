package cn.gemrun.base.module.system.util;

import cn.gemrun.base.framework.common.exception.ServiceException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static cn.gemrun.base.module.system.enums.ErrorCodeConstants.*;

/**
 * 密码强度校验工具类
 *
 * 校验规则：
 * 1. 长度：8-32 位
 * 2. 复杂度：至少包含大写字母、小写字母、数字、特殊字符中的 3 种
 * 3. 黑名单：不允许使用常用弱密码
 * 4. 关联校验：不允许密码包含用户名
 */
public class PasswordUtils {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 32;

    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "1234567", "letmein", "trustno1", "dragon",
            "baseball", "iloveyou", "master", "sunshine", "ashley",
            "football", "password1", "123123", "654321", "111111",
            "12345", "123456789", "1234", "000000", "admin",
            "login", "welcome", "passw0rd", "hello", "shadow",
            "superman", "qwerty123", "test", "guest", "love"
    ));

    private PasswordUtils() {
    }

    public static void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new ServiceException(PASSWORD_VALIDATION_FAILED);
        }
        if (password.length() < MIN_LENGTH) {
            throw new ServiceException(PASSWORD_TOO_SHORT);
        }
        if (password.length() > MAX_LENGTH) {
            throw new ServiceException(PASSWORD_TOO_LONG);
        }
        int complexityScore = calculateComplexity(password);
        if (complexityScore < 3) {
            throw new ServiceException(PASSWORD_COMPLEXITY_INSUFFICIENT);
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            throw new ServiceException(PASSWORD_TOO_WEAK);
        }
    }

    public static void validatePassword(String password, String username) {
        validatePassword(password);
        if (username != null && !username.isEmpty()
                && password.toLowerCase().contains(username.toLowerCase())) {
            throw new ServiceException(PASSWORD_CONTAINS_USERNAME);
        }
    }

    public static int calculateComplexity(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        int score = 0;
        if (password.length() >= MIN_LENGTH) score++;
        if (password.length() >= 12) score++;
        if (Pattern.compile("[a-z]").matcher(password).find()) score++;
        if (Pattern.compile("[A-Z]").matcher(password).find()) score++;
        if (Pattern.compile("\\d").matcher(password).find()) score++;
        if (Pattern.compile("[^\\da-zA-Z]").matcher(password).find()) score++;
        return score;
    }

    public static int getStrengthLevel(String password) {
        int score = calculateComplexity(password);
        if (score <= 1) return 0;
        if (score == 2) return 1;
        if (score == 3) return 2;
        if (score == 4) return 3;
        return 4;
    }

    public static String getStrengthText(int level) {
        switch (level) {
            case 0: return "非常弱";
            case 1: return "弱";
            case 2: return "中等";
            case 3: return "强";
            case 4: return "非常强";
            default: return "未知";
        }
    }

    public static boolean isValidLength(String password) {
        return password != null && password.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH;
    }
}
