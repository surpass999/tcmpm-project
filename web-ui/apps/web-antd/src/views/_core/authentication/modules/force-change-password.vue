<script setup lang="ts">
import type { Recordable } from '@vben/types';

import { $t } from '@vben/locales';

import { message } from 'ant-design-vue';

import { useVbenForm, z } from '#/adapter/form';
import { updateUserPassword } from '#/api/system/user/profile';

const emit = defineEmits<{ success: [] }>();

const [Form, formApi] = useVbenForm({
  commonConfig: {
    labelWidth: 80,
  },
  schema: [
    {
      component: 'InputPassword',
      fieldName: 'oldPassword',
      label: '旧密码',
      rules: z
        .string({ message: '请输入旧密码' })
        .min(5, '密码长度不能少于 5 个字符')
        .max(20, '密码长度不能超过 20 个字符'),
    },
    {
      component: 'InputPassword',
      fieldName: 'newPassword',
      label: '新密码',
      dependencies: {
        rules(values) {
          return z
            .string({ message: '请输入新密码' })
            .min(5, '密码长度不能少于 5 个字符')
            .max(20, '密码长度不能超过 20 个字符')
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
            .min(5, '密码长度不能少于 5 个字符')
            .max(20, '密码长度不能超过 20 个字符')
            .refine(
              (value) => value === values.newPassword,
              '新密码和确认密码不一致',
            );
        },
        triggerFields: ['newPassword', 'confirmPassword'],
      },
    },
  ],
  resetButtonOptions: { show: false },
  submitButtonOptions: { content: '确认修改' },
  handleSubmit,
});

async function handleSubmit(values: Recordable<any>) {
  try {
    formApi.setLoading(true);
    await updateUserPassword({
      oldPassword: values.oldPassword,
      newPassword: values.newPassword,
    });
    message.success('密码修改成功');
    emit('success');
  } catch (error) {
    console.error(error);
  } finally {
    formApi.setLoading(false);
  }
}
</script>

<template>
  <div class="force-change-password-wrapper">
    <div class="force-change-password-header">
      <span class="header-icon">🛡️</span>
      <h3>首次登录需修改密码</h3>
      <p class="header-tip">为保障账户安全，请修改您的登录密码</p>
    </div>
    <Form />
  </div>
</template>

<style scoped>
.force-change-password-wrapper {
  padding: 0 8px;
}
.force-change-password-header {
  text-align: center;
  margin-bottom: 24px;
}
.header-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 8px;
}
.force-change-password-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px;
}
.header-tip {
  color: #666;
  font-size: 13px;
  margin: 0;
}
</style>
