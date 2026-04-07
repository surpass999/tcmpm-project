<script setup lang="ts">
import type { Recordable } from '@vben/types';

import { $t } from '@vben/locales';

import { message } from 'ant-design-vue';

import { useVbenForm, z } from '#/adapter/form';
import { updateUserPassword } from '#/api/system/user/profile';
import { PASSWORD_RULES } from '#/utils/password';

const [Form, formApi] = useVbenForm({
  commonConfig: {
    labelWidth: 70,
  },
  schema: [
    {
      component: 'InputPassword',
      fieldName: 'oldPassword',
      label: '旧密码',
      rules: z
        .string({ message: '请输入旧密码' })
        .min(PASSWORD_RULES.MIN_LENGTH, `密码长度不能少于 ${PASSWORD_RULES.MIN_LENGTH} 个字符`)
        .max(PASSWORD_RULES.MAX_LENGTH, `密码长度不能超过 ${PASSWORD_RULES.MAX_LENGTH} 个字符`),
    },
    {
      component: 'InputPassword',
      fieldName: 'newPassword',
      label: '新密码',
      dependencies: {
        rules(values) {
          const checks = [
            { pass: /[a-z]/.test(values.newPassword || ''), msg: '小写字母' },
            { pass: /[A-Z]/.test(values.newPassword || ''), msg: '大写字母' },
            { pass: /\d/.test(values.newPassword || ''), msg: '数字' },
            { pass: /[^\da-zA-Z]/.test(values.newPassword || ''), msg: '特殊字符' },
          ];
          const satisfiedTypes = checks.filter((c) => c.pass).length;
          const missing = checks
            .filter((c) => !c.pass)
            .map((c) => c.msg)
            .join('、');
          return z
            .string({ message: '请输入新密码' })
            .min(PASSWORD_RULES.MIN_LENGTH, `密码长度不能少于 ${PASSWORD_RULES.MIN_LENGTH} 个字符`)
            .max(PASSWORD_RULES.MAX_LENGTH, `密码长度不能超过 ${PASSWORD_RULES.MAX_LENGTH} 个字符`)
            .refine(
              () => satisfiedTypes >= PASSWORD_RULES.MIN_COMPLEXITY,
              `密码必须包含 ${missing} 中的至少 ${PASSWORD_RULES.MIN_COMPLEXITY} 种`,
            )
            .refine(
              (value) => value !== values.oldPassword,
              '新旧密码不能相同',
            );
        },
        triggerFields: ['newPassword', 'oldPassword'],
      },
    },
    {
      component: 'InputPassword',
      fieldName: 'confirmPassword',
      label: '确认密码',
      dependencies: {
        rules(values) {
          return z
            .string({ message: '请输入确认密码' })
            .min(PASSWORD_RULES.MIN_LENGTH, `密码长度不能少于 ${PASSWORD_RULES.MIN_LENGTH} 个字符`)
            .max(PASSWORD_RULES.MAX_LENGTH, `密码长度不能超过 ${PASSWORD_RULES.MAX_LENGTH} 个字符`)
            .refine(
              (value) => value === values.newPassword,
              '新密码和确认密码不一致',
            );
        },
        triggerFields: ['newPassword', 'confirmPassword'],
      },
    },
  ],
  resetButtonOptions: {
    show: false,
  },
  submitButtonOptions: {
    content: '修改密码',
  },
  handleSubmit,
});

async function handleSubmit(values: Recordable<any>) {
  try {
    formApi.setLoading(true);
    // 提交表单
    await updateUserPassword({
      oldPassword: values.oldPassword,
      newPassword: values.newPassword,
    });
    message.success($t('ui.actionMessage.operationSuccess'));
  } catch (error) {
    console.error(error);
  } finally {
    formApi.setLoading(false);
  }
}
</script>

<template>
  <div class="mt-4 md:w-full lg:w-1/2 2xl:w-2/5">
    <Form />
  </div>
</template>
