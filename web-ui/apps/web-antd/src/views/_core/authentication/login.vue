<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';

import type { AuthApi } from '#/api/core/auth';

import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { AuthenticationLogin, Verification, z } from '@vben/common-ui';
import { isCaptchaEnable, isTenantEnable } from '@vben/hooks';
import { $t } from '@vben/locales';
import { useAccessStore } from '@vben/stores';

import {
  checkCaptcha,
  getCaptcha,
  getTenantByWebsite,
  getTenantSimpleList,
  socialAuthRedirect,
} from '#/api/core/auth';
import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const { query } = useRoute();
const authStore = useAuthStore();
const accessStore = useAccessStore();
const tenantEnable = isTenantEnable();
const captchaEnable = isCaptchaEnable();

const loginRef = ref();
const verifyRef = ref();

const captchaType = 'blockPuzzle'; // 验证码类型：'blockPuzzle' | 'clickWord'

/** 获取租户列表，并默认选中 */
const tenantList = ref<AuthApi.TenantResult[]>([]); // 租户列表
async function fetchTenantList() {
  if (!tenantEnable) {
    return;
  }
  try {
    // 获取租户列表、域名对应租户
    const websiteTenantPromise = getTenantByWebsite(window.location.hostname);
    tenantList.value = await getTenantSimpleList();

    // 选中租户：域名 > store 中的租户 > 首个租户
    let tenantId: null | number = null;
    const websiteTenant = await websiteTenantPromise;
    if (websiteTenant?.id) {
      tenantId = websiteTenant.id;
    }
    // 如果没有从域名获取到租户，尝试从 store 中获取
    if (!tenantId && accessStore.tenantId) {
      tenantId = accessStore.tenantId;
    }
    // 如果还是没有租户，使用列表中的第一个
    if (!tenantId && tenantList.value?.[0]?.id) {
      tenantId = tenantList.value[0].id;
    }

    // 设置选中的租户编号
    accessStore.setTenantId(tenantId);
    loginRef.value.getFormApi().setFieldValue('tenantId', tenantId?.toString());
  } catch (error) {
    console.error('获取租户列表失败:', error);
  }
}

/** 处理登录 */
async function handleLogin(values: any) {
  // 如果开启验证码，则先验证验证码
  if (captchaEnable) {
    verifyRef.value.show();
    return;
  }
  // 无验证码，直接登录
  await authStore.authLogin('username', values);
}

/** 验证码通过，执行登录 */
async function handleVerifySuccess({ captchaVerification }: any) {
  try {
    await authStore.authLogin('username', {
      ...(await loginRef.value.getFormApi().getValues()),
      captchaVerification,
    });
  } catch (error) {
    console.error('Error in handleLogin:', error);
  }
}

/** 处理第三方登录 */
const redirect = query?.redirect;
async function handleThirdLogin(type: number) {
  if (type <= 0) {
    return;
  }
  try {
    // 计算 redirectUri
    // tricky: type、redirect 需要先 encode 一次，否则钉钉回调会丢失。配合 social-login.vue#getUrlValue() 使用
    const redirectUri = `${
      location.origin
    }/auth/social-login?${encodeURIComponent(
      `type=${type}&redirect=${redirect || '/'}`,
    )}`;

    // 进行跳转
    window.location.href = await socialAuthRedirect(type, redirectUri);
  } catch (error) {
    console.error('第三方登录处理失败:', error);
  }
}

/** 组件挂载时获取租户信息 */
onMounted(() => {
  fetchTenantList();
});

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenSelect',
      componentProps: {
        options: tenantList.value.map((item) => ({
          label: item.name,
          value: item.id.toString(),
        })),
        placeholder: $t('authentication.tenantTip'),
      },
      fieldName: 'tenantId',
      label: $t('authentication.tenant'),
      rules: z.string().min(1, { message: $t('authentication.tenantTip') }),
      dependencies: {
        triggerFields: ['tenantId'],
        if: tenantEnable,
        trigger(values) {
          if (values.tenantId) {
            accessStore.setTenantId(Number(values.tenantId));
          }
        },
      },
    },
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      fieldName: 'username',
      label: $t('authentication.username'),
      rules: z
        .string()
        .min(1, { message: $t('authentication.usernameTip') })
        .default(import.meta.env.VITE_APP_DEFAULT_USERNAME),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('authentication.passwordTip'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z
        .string()
        .min(1, { message: $t('authentication.passwordTip') })
        .default(import.meta.env.VITE_APP_DEFAULT_PASSWORD),
    },
  ];
});
</script>

<template>
  <div class="gov-auth-form">
    <AuthenticationLogin
      ref="loginRef"
      :form-schema="formSchema"
      :loading="authStore.loginLoading"
      sub-title="请输入账号与密码登录系统，请妥善保管账户信息，勿向他人泄露。"
      :show-code-login="false"
      :show-qrcode-login="false"
      :show-third-party-login="false"
      :show-register="false"
      title="账户登录"
      @submit="handleLogin"
      @third-login="handleThirdLogin"
    />
    <Verification
      ref="verifyRef"
      v-if="captchaEnable"
      :captcha-type="captchaType"
      :check-captcha-api="checkCaptcha"
      :get-captcha-api="getCaptcha"
      :img-size="{ width: '400px', height: '200px' }"
      mode="pop"
      @on-success="handleVerifySuccess"
    />
  </div>
</template>

<style scoped>
/* 与顶部红头统一的政企风登录区（仅本页） */
.gov-auth-form :deep(h2) {
  margin-bottom: 0.5rem !important;
  font-size: 1.25rem !important;
  font-weight: 600 !important;
  line-height: 1.4 !important;
  color: hsl(var(--foreground)) !important;
  letter-spacing: 0.06em;
}

.gov-auth-form :deep(.text-muted-foreground) {
  font-size: 0.8125rem;
  line-height: 1.5;
  color: hsl(var(--muted-foreground)) !important;
}

/* 收紧标题区块与表单的间距 */
.gov-auth-form :deep(.mb-7) {
  margin-bottom: 1.25rem !important;
}

/* 主按钮：跟随当前主题主色 */
.gov-auth-form :deep(button.inline-flex.w-full) {
  height: 40px;
  font-size: 0.9375rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  border-radius: 4px;
  border: none;
  background: linear-gradient(180deg, hsl(var(--primary)) 0%, hsl(var(--primary-dark)) 100%) !important;
  color: hsl(var(--primary-foreground)) !important;
  box-shadow: 0 2px 6px hsl(var(--primary) / 0.35);
}

.gov-auth-form :deep(button.inline-flex.w-full:hover) {
  background: linear-gradient(180deg, hsl(var(--primary-light)) 0%, hsl(var(--primary)) 100%) !important;
}

.gov-auth-form :deep(button.inline-flex.w-full:disabled) {
  opacity: 0.65;
}

/* 链接：跟随主色 */
.gov-auth-form :deep(.vben-link) {
  color: hsl(var(--primary)) !important;
}

.gov-auth-form :deep(.vben-link:hover) {
  color: hsl(var(--primary-light)) !important;
}

/* Ant Design 输入框：跟随边框色 */
.gov-auth-form :deep(.ant-input),
.gov-auth-form :deep(.ant-input-affix-wrapper) {
  background-color: hsl(var(--input-background)) !important;
  border-color: hsl(var(--input)) !important;
  border-radius: 4px !important;
}

.gov-auth-form :deep(.ant-input-affix-wrapper:not(.ant-input-affix-wrapper-disabled):hover),
.gov-auth-form :deep(.ant-input:hover) {
  border-color: hsl(var(--primary)) !important;
}

.gov-auth-form :deep(.ant-input-affix-wrapper-focused),
.gov-auth-form :deep(.ant-input:focus),
.gov-auth-form :deep(.ant-input-focused) {
  border-color: hsl(var(--primary)) !important;
  box-shadow: 0 0 0 2px hsl(var(--primary) / 0.15) !important;
}

.gov-auth-form :deep(.ant-select:not(.ant-select-disabled):hover .ant-select-selector) {
  border-color: hsl(var(--primary)) !important;
}

.gov-auth-form :deep(.ant-select-focused .ant-select-selector) {
  border-color: hsl(var(--primary)) !important;
  box-shadow: 0 0 0 2px hsl(var(--primary) / 0.15) !important;
}

.gov-auth-form :deep(.ant-select-selector) {
  background-color: hsl(var(--input-background)) !important;
  border-color: hsl(var(--input)) !important;
  border-radius: 4px !important;
}
</style>
