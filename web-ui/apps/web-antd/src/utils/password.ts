/**
 * 密码强度工具类
 */

export enum PasswordStrength {
  VERY_WEAK = 0,
  WEAK = 1,
  MEDIUM = 2,
  STRONG = 3,
  VERY_STRONG = 4,
}

export const PASSWORD_RULES = {
  MIN_LENGTH: 8,
  MAX_LENGTH: 32,
  MIN_COMPLEXITY: 3,
};

export function checkPasswordComplexity(password: string): number {
  let score = 0;

  if (password.length >= PASSWORD_RULES.MIN_LENGTH) score++;
  if (password.length >= 12) score++;
  if (/[a-z]/.test(password)) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^\da-zA-Z]/.test(password)) score++;

  return score;
}

export function getPasswordStrengthLevel(password: string): PasswordStrength {
  const score = checkPasswordComplexity(password);
  if (score <= 1) return PasswordStrength.VERY_WEAK;
  if (score === 2) return PasswordStrength.WEAK;
  if (score === 3) return PasswordStrength.MEDIUM;
  if (score === 4) return PasswordStrength.STRONG;
  return PasswordStrength.VERY_STRONG;
}

export function isPasswordValid(password: string): {
  valid: boolean;
  errors: string[];
} {
  const errors: string[] = [];

  if (password.length < PASSWORD_RULES.MIN_LENGTH) {
    errors.push(`密码长度不能少于 ${PASSWORD_RULES.MIN_LENGTH} 个字符`);
  }

  if (password.length > PASSWORD_RULES.MAX_LENGTH) {
    errors.push(`密码长度不能超过 ${PASSWORD_RULES.MAX_LENGTH} 个字符`);
  }

  const checks = [
    { pass: /[a-z]/.test(password), msg: '小写字母' },
    { pass: /[A-Z]/.test(password), msg: '大写字母' },
    { pass: /\d/.test(password), msg: '数字' },
    { pass: /[^\da-zA-Z]/.test(password), msg: '特殊字符' },
  ];

  const satisfiedTypes = checks.filter((c) => c.pass).length;

  if (satisfiedTypes < PASSWORD_RULES.MIN_COMPLEXITY) {
    const missing = checks
      .filter((c) => !c.pass)
      .map((c) => c.msg)
      .join('、');
    errors.push(`密码必须包含 ${missing} 中的至少 ${PASSWORD_RULES.MIN_COMPLEXITY} 种`);
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

export const passwordSchema = {
  minLength: PASSWORD_RULES.MIN_LENGTH,
  maxLength: PASSWORD_RULES.MAX_LENGTH,
  minComplexity: PASSWORD_RULES.MIN_COMPLEXITY,
};
