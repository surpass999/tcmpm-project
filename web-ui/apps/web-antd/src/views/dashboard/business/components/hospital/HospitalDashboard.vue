<script setup lang="ts">
import type { DashboardStats } from '#/api/declare/dashboard';

import { computed, onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';
import { useRouter } from 'vue-router';

import { checkReportWindow } from '#/api/declare/progress-report';

import Form from '../../../../declare/progress-report/modules/form.vue';


const router = useRouter();


const props = defineProps({
  stats: Object as PropType<DashboardStats>,
});



defineEmits(['refresh']);

/** 填报弹窗 */
const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
  closeOnClickModal: false,
});

const userStore = useUserStore();

/** 当前用户所属医院ID，从 deptId 获取 */
const hospitalId = computed(() => Number(userStore.userInfo?.deptId) || 0);

/** 是否有开放的填报窗口（优先用 stats 里的值，兜底用 API 校验） */
const canCreate = ref(false);

onMounted(async () => {
  // stats.hospitalStats.hasUnfilledOpenWindow 是后端已计算好的，直接用
  if (props.stats?.hospitalStats?.hasUnfilledOpenWindow !== undefined) {
    canCreate.value = !!props.stats.hospitalStats.hasUnfilledOpenWindow;
  } else if (hospitalId.value) {
    try {
      canCreate.value = await checkReportWindow(hospitalId.value);
    } catch {
      canCreate.value = false;
    }
  }
});

/** 打开填报弹窗 */
function openFormModal() {
  formModalApi.setData({ 
    hospitalId: hospitalId.value,
    
   }).open();
}

/** 填报弹窗提交成功后刷新状态 */
async function handleFormSuccess() {
  // 重新检查填报窗口状态
  if (hospitalId.value) {
    try {
      canCreate.value = await checkReportWindow(hospitalId.value);
    } catch {
      canCreate.value = false;
    }
  }
}
</script>

<template>
  <div class="hospital-dashboard">
    <!-- 填报窗口状态提醒 -->
    <div class="window-status-panel">
      <div v-if="canCreate" class="window-open">
        <div class="status-icon">
          <span class="icon-badge">!</span>
        </div>
        <div class="status-content">
          <h3>当前有填报窗口开放</h3>
          <p>国家局已发布新的填报任务，请及时完成进度填报。</p>
        </div>
        <a-button type="primary" size="large" @click="openFormModal">
          立即填报
        </a-button>
      </div>
      <div v-else class="window-closed">
        <div class="status-icon">
          <span class="icon-badge icon-badge--ok">✓</span>
        </div>
        <div class="status-content">
          <h3>当前无开放填报窗口</h3>
          <p>暂无可填报任务，请耐心等待国家局发布新的填报通知。</p>
        </div>
        <a-button type="default" class="bg-background-lighter text-red-500" size="large" @click="router.push('/progress-report')">
          查看填报记录
        </a-button>
      </div>
    </div>

    <!-- 填报弹窗（挂载在此，随时可打开） -->
    <FormModal @success="handleFormSuccess" />
  </div>
</template>

<style scoped>
.hospital-dashboard {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 120px);
}

.window-status-panel {
  background: #fff;
  border-radius: 12px;
  padding: 48px 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.window-open {
  display: flex;
  align-items: center;
  gap: 28px;
  max-width: 680px;
  width: 100%;
}

.window-closed {
  display: flex;
  align-items: center;
  gap: 28px;
  max-width: 680px;
  width: 100%;
}

.status-icon {
  flex-shrink: 0;
}

.icon-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #fff1f0;
  border: 2px solid #ffccc7;
  font-size: 28px;
  font-weight: 700;
  color: #ff4d4f;
}

.icon-badge--ok {
  background: #f6ffed;
  border-color: #b7eb8f;
  color: #52c41a;
}

.status-content {
  flex: 1;
}

.status-content h3 {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.status-content p {
  margin: 0;
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
}
</style>
