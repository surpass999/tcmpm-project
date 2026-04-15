import type { AuthPermissionInfo, Recordable, UserInfo } from '@vben/types';

import type { AuthApi } from '#/api';

import { ref } from 'vue';
import { h } from 'vue';
import { useRouter } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { preferences } from '@vben/preferences';
import { resetAllStores, useAccessStore, useUserStore } from '@vben/stores';

import { notification } from 'ant-design-vue';
import { defineStore } from 'pinia';

import {
  getAuthPermissionInfoApi,
  loginApi,
  logoutApi,
  register,
  smsLogin,
  socialLogin,
} from '#/api';
import { useIdleSessionStore } from '@vben/stores';
import { $t } from '#/locales';

export const useAuthStore = defineStore('auth', () => {
  const accessStore = useAccessStore();
  const userStore = useUserStore();
  const router = useRouter();

  const loginLoading = ref(false);

  /**
   * 异步处理登录操作
   * Asynchronously handle the login process
   * @param type 登录类型
   * @param params 登录表单数据
   * @param onSuccess 登录成功后的回调函数
   */
  async function authLogin(
    type: 'mobile' | 'register' | 'social' | 'username',
    params: Recordable<any>,
    onSuccess?: () => Promise<void> | void,
  ) {
    // 异步处理用户登录操作并获取 accessToken
    let userInfo: null | UserInfo = null;
    try {
      let loginResult: AuthApi.LoginResult;
      loginLoading.value = true;
      switch (type) {
        case 'mobile': {
          loginResult = await smsLogin(params as AuthApi.SmsLoginParams);
          break;
        }
        case 'register': {
          loginResult = await register(params as AuthApi.RegisterParams);
          break;
        }
        case 'social': {
          loginResult = await socialLogin(params as AuthApi.SocialLoginParams);
          break;
        }
        default: {
          loginResult = await loginApi(params);
        }
      }
      const { accessToken, refreshToken } = loginResult;

      // 如果成功获取到 accessToken
      if (accessToken) {
        accessStore.setAccessToken(accessToken);
        accessStore.setRefreshToken(refreshToken);

        // 初始化空闲会话时间
        const idleSessionStore = useIdleSessionStore();
        idleSessionStore.setLoginTime();

        // 获取用户信息并存储到 userStore、accessStore 中
        // TODO @芋艿：清理掉 accessCodes 相关的逻辑
        // const [fetchUserInfoResult, accessCodes] = await Promise.all([
        //   fetchUserInfo(),
        //   // getAccessCodesApi(),
        // ]);
        const fetchUserInfoResult = await fetchUserInfo();

        userInfo = fetchUserInfoResult.user;

        // 从 userInfo 的 extraInfo 中获取 passwordMustChange 标志
        const passwordMustChange =
          (fetchUserInfoResult.user as any)?.extraInfo?.passwordMustChange === true;

        if (accessStore.loginExpired) {
          accessStore.setLoginExpired(false);
        } else if (passwordMustChange) {
          // 开启强制改密弹窗，禁止跳转到其他页面
          const { Modal } = await import('ant-design-vue');
          const ForceChangePassword = await import(
            '#/views/_core/authentication/modules/force-change-password.vue'
          );
          Modal.confirm({
            title: '修改密码',
            closable: false,
            maskClosable: false,
            keyboard: false,
            icon: null,
            content: h(ForceChangePassword.default, {
              onSuccess: async () => {
                Modal.destroyAll();
                // 刷新用户信息（清除标志）
                await fetchUserInfo();
                // 跳转到首页
                await router.push(
                  userInfo.homePath || preferences.app.defaultHomePath,
                );
              },
            }),
            footer: null,
            width: 420,
          });
        } else {
          onSuccess
            ? await onSuccess?.()
            : await router.push(
                userInfo.homePath || preferences.app.defaultHomePath,
              );
        }

        if (userInfo?.nickname) {
          notification.success({
            description: `${$t('authentication.loginSuccessDesc')}:${userInfo?.nickname}`,
            duration: 3,
            message: $t('authentication.loginSuccess'),
          });
        }
      }
    } finally {
      loginLoading.value = false;
    }

    return {
      userInfo,
    };
  }

  async function logout(redirect: boolean = true) {
    try {
      const accessToken = accessStore.accessToken as string;
      // 只有在 token 存在且不是明显过期的情况下才调用登出接口
      if (accessToken && !accessToken.startsWith('expired_')) {
        await logoutApi(accessToken);
      }
    } catch {
      // 登出接口调用失败不做任何处理（token 已过期的情况）
    }
    resetAllStores();
    accessStore.setLoginExpired(false);

    // 回登录页带上当前路由地址
    await router.replace({
      path: LOGIN_PATH,
      query: redirect
        ? {
            redirect: encodeURIComponent(router.currentRoute.value.fullPath),
          }
        : {},
    });
  }

  async function fetchUserInfo() {
    // 加载
    let authPermissionInfo: AuthPermissionInfo | null = null;
    authPermissionInfo = await getAuthPermissionInfoApi();
    // userStore
    userStore.setUserInfo(authPermissionInfo.user);
    userStore.setUserRoles(authPermissionInfo.roles);
    // accessStore
    accessStore.setAccessMenus(authPermissionInfo.menus);
    accessStore.setAccessCodes(authPermissionInfo.permissions);
    return authPermissionInfo;
  }

  function $reset() {
    loginLoading.value = false;
  }

  return {
    $reset,
    authLogin,
    fetchUserInfo,
    loginLoading,
    logout,
  };
});
