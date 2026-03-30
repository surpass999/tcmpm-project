<script lang="ts" setup>
import { computed } from 'vue';

import { preferences, usePreferences } from '@vben/preferences';

const { isDark } = usePreferences();

const logoSrc = computed(() => {
  const src = isDark.value
    ? preferences.logo.sourceDark
    : preferences.logo.source;
  return src || preferences.logo.source;
});

const pageTitle = '智慧中医医院试点项目过程管理平台';
</script>

<template>
  <div class="auth-root">
    <!-- 顶部装饰条：庄重的红色顶部条 -->
    <div class="gov-topbar">
      <div class="gov-topbar-inner">
        <div class="gov-topbar-logo-area">
          <img v-if="logoSrc" :src="logoSrc" :alt="preferences.app.name" class="gov-topbar-logo" />
          <div class="gov-topbar-titles">
            <h1 class="gov-topbar-main-title">{{ pageTitle }}</h1>
          </div>
        </div>
      </div>
      <!-- 底部红色强调线 -->
      <div class="gov-topbar-accent-line"></div>
    </div>

    <!-- 登录表单区域（标题与表单均在子路由内，避免与顶部红头重复、风格割裂） -->
    <div class="auth-form-area">
      <div class="auth-form-card">
        <div class="auth-form-card-body">
          <RouterView />
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="auth-footer">
      <p class="auth-footer-text">© 2026 {{ preferences.app.name }} · 仅限授权用户访问</p>
    </div>
  </div>
</template>

<style scoped>
/* ========== 整体布局 ========== */
.auth-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: hsl(var(--background));
  background-image:
    radial-gradient(circle at 20% 80%, hsl(var(--primary) / 0.06) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, hsl(var(--primary) / 0.04) 0%, transparent 50%);
}

/* ========== 顶部导航条 ========== */
.gov-topbar {
  background: linear-gradient(180deg, hsl(var(--primary)) 0%, hsl(var(--primary-dark)) 100%);
  box-shadow: 0 2px 12px hsl(var(--primary) / 0.25);
  position: relative;
  
  flex-shrink: 0;
}

.gov-topbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 40px;
  height: 72px;
  display: flex;
  align-items: center;
}

.gov-topbar-logo-area {
  display: flex;
  align-items: center;
  gap: 16px;
}

.gov-topbar-logo {
  height: 48px;
  width: auto;
  object-fit: contain;
  filter: brightness(0) invert(1);
  opacity: 0.95;
}

.gov-topbar-titles {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.gov-topbar-main-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: hsl(var(--primary-foreground));
  letter-spacing: 0.08em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  margin: 0;
  line-height: 1.2;
}

/* 主题色强调线 */
.gov-topbar-accent-line {
  height: 4px;
  background: linear-gradient(
    90deg,
    hsl(var(--primary-dark)) 0%,
    hsl(var(--primary)) 30%,
    hsl(var(--primary-glow)) 50%,
    hsl(var(--primary)) 70%,
    hsl(var(--primary-dark)) 100%
  );
}

/* ========== 登录表单区域：宽度600px，距顶部约20% ========== */
.auth-form-area {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 40px 16px;
}

.auth-form-card {
  position: relative;
  width: 600px;
  max-width: calc(100vw - 32px);
  /* 距登录区顶边约20%，向下溢出部分自动撑开高度 */
  margin-top: calc((100vh - 72px - 40px - 32px) * 0.20);
  background: hsl(var(--card));
  border-radius: 2px;
  border: 1px solid hsl(var(--border));
  box-shadow: 0 2px 8px hsl(var(--primary) / 0.08);
  overflow: hidden;
}

/* 左侧竖条：与顶部导航同色系 */
.auth-form-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, hsl(var(--primary)) 0%, hsl(var(--primary-dark)) 100%);
}

/* 顶部横线装饰 */
.auth-form-card::after {
  content: '';
  display: block;
  height: 3px;
  background: linear-gradient(
    90deg,
    hsl(var(--primary-dark)) 0%,
    hsl(var(--primary)) 40%,
    hsl(var(--primary-glow)) 100%
  );
}

.auth-form-card-body {
  padding: 36px 44px 40px 48px;
}

/* ========== 底部版权 ========== */
.auth-footer {
  padding: 16px;
  text-align: center;
  border-top: 1px solid hsl(var(--border));
  flex-shrink: 0;
}

.auth-footer-text {
  font-size: 0.75rem;
  color: hsl(var(--muted-foreground));
  margin: 0;
  letter-spacing: 0.02em;
}
</style>
