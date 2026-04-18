<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { storeToRefs } from 'pinia';

import { preferences } from '@vben/preferences';
import { useIdleSessionStore } from '@vben/stores';

const ACTIVITY_EVENTS = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];
const CHECK_INTERVAL = 10 * 1000; // 10 秒检查一次

const idleSessionStore = useIdleSessionStore();
const idleTimeout = preferences.idleTimeout;
const { isFormOpened } = storeToRefs(idleSessionStore);

let intervalId: ReturnType<typeof setInterval> | null = null;

/** 重置活跃时间 */
function handleActivity() {
  if (!idleTimeout.enable) return;
  idleSessionStore.resetActiveTime();
}

/** 检查是否超时 */
function checkIdle() {
  if (!idleTimeout.enable) return;
  const elapsed = Date.now() - idleSessionStore.lastActiveTime;
  if (elapsed >= idleTimeout.timeout) {
    handleTimeout();
  }
}

/** 超时处理 */
async function handleTimeout() {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
  }

  // 如果表单打开且注入了保存回调，尝试保存草稿
  if (isFormOpened.value && idleSessionStore.onAutoSaveDraft) {
    try {
      await idleSessionStore.onAutoSaveDraft();
    } catch {
      // 静默忽略保存失败
    }
  }

  // 执行登出回调
  if (idleSessionStore.onLogout) {
    await idleSessionStore.onLogout();
  }
}

onMounted(() => {
  if (!idleTimeout.enable) return;

  // 注册事件监听（静默，无 UI）
  ACTIVITY_EVENTS.forEach((event) => {
    document.addEventListener(event, handleActivity, { passive: true });
  });

  // 启动定时检查（10 秒一次）
  intervalId = setInterval(checkIdle, CHECK_INTERVAL);
});

onUnmounted(() => {
  ACTIVITY_EVENTS.forEach((event) => {
    document.removeEventListener(event, handleActivity);
  });
  if (intervalId) {
    clearInterval(intervalId);
  }
});
</script>

<template>
  <!-- 无渲染内容，纯逻辑组件 -->
</template>
