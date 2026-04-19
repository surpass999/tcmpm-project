<script lang="ts" setup>
import type { NotificationItem } from '@vben/layouts';


import { computed, onMounted, onUnmounted, ref, watch } from 'vue';

import { useAccess } from '@vben/access';
import { AuthenticationLoginExpiredModal, useVbenModal } from '@vben/common-ui';
import { VBEN_GITHUB_URL } from '@vben/constants';
import { isTenantEnable, useTabs, useWatermark } from '@vben/hooks';
import {
  AntdProfileOutlined,
  CircleHelp,
  SvgGithubIcon,
} from '@vben/icons';
import {
  BasicLayout,
  Help,
  LockScreen,
  Notification,
  TenantDropdown,
  UserDropdown,
} from '@vben/layouts';
import { preferences } from '@vben/preferences';
import { useAccessStore, useIdleSessionStore, useUserStore } from '@vben/stores';
import { formatDateTime, openWindow } from '@vben/utils';
import { message } from 'ant-design-vue';

import {
  getUnreadNotifyMessageCount,
  getUnreadNotifyMessageList,
  updateAllNotifyMessageRead,
  updateNotifyMessageRead,
} from '#/api/system/notify/message';
import { $t } from '#/locales';
import { router } from '#/router';
import { useAuthStore } from '#/store';
import LoginForm from '#/views/_core/authentication/login.vue';

const userStore = useUserStore();
const authStore = useAuthStore();
const accessStore = useAccessStore();
const idleSessionStore = useIdleSessionStore();
const { hasAccessByCodes } = useAccess();
const { destroyWatermark, updateWatermark } = useWatermark();
const { closeOtherTabs, refreshTab } = useTabs();

// ========== 移动端检测 ==========
const isMobileDevice = computed(() => {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile|mobile|CriOS/i.test(
    navigator.userAgent,
  );
});

// 注册空闲超时登出回调
idleSessionStore.setLogoutCallback(async () => {
  await authStore.logout(false);
});
idleSessionStore.setAutoSaveDraftCallback(async () => {
  // 暂未实现
});

// ========== IdleTimeout 内联逻辑 ==========
const ACTIVITY_EVENTS = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];
const CHECK_INTERVAL = 10 * 1000;
let idleIntervalId: ReturnType<typeof setInterval> | null = null;

function handleIdleActivity() {
  if (!preferences.idleTimeout?.enable) return;
  idleSessionStore.resetActiveTime();
}

function checkIdleTimeout() {
  if (!preferences.idleTimeout?.enable) return;
  const elapsed = Date.now() - idleSessionStore.lastActiveTime;
  if (elapsed >= preferences.idleTimeout.timeout) {
    doIdleLogout();
  }
}

async function doIdleLogout() {
  console.log('[doIdleLogout] isFormOpened=', idleSessionStore.isFormOpened, 'hasCallback=', !!idleSessionStore.onAutoSaveDraft);
  if (idleIntervalId) {
    clearInterval(idleIntervalId);
    idleIntervalId = null;
  }
  if (idleSessionStore.isFormOpened && idleSessionStore.onAutoSaveDraft) {
    console.log('[doIdleLogout] 保存草稿');
    await idleSessionStore.onAutoSaveDraft();
  }
  if (idleSessionStore.onLogout) {
    await idleSessionStore.onLogout();
  }
}

function startIdleTimeout() {
  if (!preferences.idleTimeout?.enable) return;
  ACTIVITY_EVENTS.forEach((e) => document.addEventListener(e, handleIdleActivity, { passive: true }));
  idleIntervalId = setInterval(checkIdleTimeout, CHECK_INTERVAL);
}

function stopIdleTimeout() {
  ACTIVITY_EVENTS.forEach((e) => document.removeEventListener(e, handleIdleActivity));
  if (idleIntervalId) clearInterval(idleIntervalId);
}

const notifications = ref<NotificationItem[]>([]);
const unreadCount = ref(0);
const showDot = computed(() => unreadCount.value > 0);

const [HelpModal, helpModalApi] = useVbenModal({
  connectedComponent: Help,
});

const menus = computed(() => [
  {
    handler: () => {
      router.push({ name: 'Profile' });
    },
    icon: AntdProfileOutlined,
    text: $t('ui.widgets.profile'),
  }
]);

const avatar = computed(() => {
  return userStore.userInfo?.avatar ?? preferences.app.defaultAvatar;
});

async function handleLogout() {
  await authStore.logout(false);
}

/** 获得未读消息数 */
async function handleNotificationGetUnreadCount() {
  unreadCount.value = await getUnreadNotifyMessageCount();
}

/** 获得消息列表 */
async function handleNotificationGetList() {
  const list = await getUnreadNotifyMessageList();
  notifications.value = list.map((item) => ({
    avatar: preferences.app.defaultAvatar,
    date: formatDateTime(item.createTime) as string,
    isRead: false,
    id: item.id,
    message: item.templateContent,
    title: item.templateNickname,
  }));
}

/** 跳转我的站内信 */
function handleNotificationViewAll() {
  router.push({
    name: 'MyNotifyMessage',
  });
}

/** 标记所有已读 */
async function handleNotificationMakeAll() {
  await updateAllNotifyMessageRead();
  unreadCount.value = 0;
  notifications.value = [];
}

/** 清空通知 */
async function handleNotificationClear() {
  await handleNotificationMakeAll();
}

/** 标记单个已读 */
async function handleNotificationRead(item: NotificationItem) {
  if (!item.id) {
    return;
  }
  await updateNotifyMessageRead([item.id]);
  await handleNotificationGetUnreadCount();
  notifications.value = notifications.value.filter((n) => n.id !== item.id);
}

/** 处理通知打开 */
function handleNotificationOpen(open: boolean) {
  if (!open) {
    return;
  }
  handleNotificationGetList();
  handleNotificationGetUnreadCount();
}



// ========== 初始化 ==========
onMounted(() => {
  startIdleTimeout();
  handleNotificationGetUnreadCount();
  // 轮询刷新未读数量
  setInterval(
    () => {
      if (userStore.userInfo) {
        handleNotificationGetUnreadCount();
      }
    },
    1000 * 60 * 2,
  );
});

onUnmounted(() => {
  stopIdleTimeout();
});

watch(
  () => ({
    enable: preferences.app.watermark,
    content: preferences.app.watermarkContent,
  }),
  async ({ enable, content }) => {
    if (enable) {
      await updateWatermark({
        content:
          content ||
          `${userStore.userInfo?.id} - ${userStore.userInfo?.nickname}`,
      });
    } else {
      destroyWatermark();
    }
  },
  {
    immediate: true,
  },
);
</script>

<template>
  <!-- 移动端提示遮罩 -->
  <div v-if="isMobileDevice" class="mobile-blocker">
    <div class="mobile-blocker__content">
      <div class="mobile-blocker__icon">💻</div>
      <h2 class="mobile-blocker__title">请使用电脑端访问</h2>
      <p class="mobile-blocker__desc">
        系统暂不支持移动端操作，请在电脑端打开继续使用
      </p>
    </div>
  </div>

  <BasicLayout v-else @clear-preferences-and-logout="handleLogout">
    <template #user-dropdown>
      <UserDropdown
        :avatar
        :menus
        :text="userStore.userInfo?.nickname"
        :description="userStore.userInfo?.email"
        :tag-text="userStore.userInfo?.username"
        @logout="handleLogout"
      />
    </template>
    <template #notification>
      <Notification
        :dot="showDot"
        :notifications="notifications"
        @clear="handleNotificationClear"
        @make-all="handleNotificationMakeAll"
        @view-all="handleNotificationViewAll"
        @open="handleNotificationOpen"
        @read="handleNotificationRead"
      />
    </template>
    <template #extra>
      <AuthenticationLoginExpiredModal
        v-model:open="accessStore.loginExpired"
        :avatar
      >
        <LoginForm />
      </AuthenticationLoginExpiredModal>
    </template>
    <template #lock-screen>
      <LockScreen :avatar @to-login="handleLogout" />
    </template>
  </BasicLayout>
  <HelpModal />
</template>

<style lang="scss" scoped>
.mobile-blocker {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;

  &__content {
    text-align: center;
    padding: 40px;
  }

  &__icon {
    font-size: 64px;
    margin-bottom: 16px;
  }

  &__title {
    margin: 0 0 12px;
    font-size: 20px;
    font-weight: 600;
    color: #1f1f1f;
  }

  &__desc {
    margin: 0;
    font-size: 14px;
    color: #666;
  }
}
</style>
