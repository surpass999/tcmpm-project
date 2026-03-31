<script setup lang="ts">
import type { DashboardStats } from '#/api/declare/dashboard';

import { computed, onMounted, ref } from 'vue';
import type { PropType } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';
import { useRouter } from 'vue-router';

import { checkReportWindow } from '#/api/declare/progress-report';
import { getHospitalByDeptId } from '#/api/declare/hospital';
import type { DeclareHospitalApi } from '#/api/declare/hospital';

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

/** 登录医院信息 */
const hospitalInfo = ref<DeclareHospitalApi.Hospital | null>(null);

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

  // 加载登录医院信息
  if (hospitalId.value) {
    try {
      hospitalInfo.value = await getHospitalByDeptId(hospitalId.value);
    } catch {
      hospitalInfo.value = null;
    }
  }
});

/** 打开填报弹窗 */
function openFormModal() {
  formModalApi.setData({ 
    hospitalId: hospitalId.value,
    
   }).open();
}

/** 提示条是否可见 */
const showTip = ref(true);

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
    <!-- 医院基本信息 -->
    <div class="hospital-info-card">
      <div v-if="showTip" class="info-tip-bar">
        <span class="tip-icon">ℹ</span>
        <span class="tip-text">核查机构基本信息，如有问题联系中心后台修改</span>
        <button class="tip-close" @click="showTip = false" title="关闭提示">×</button>
      </div>
      <div class="info-card-header">
        <span class="hospital-name">{{ hospitalInfo?.hospitalName || '-' }}</span>
        <a-tag v-if="hospitalInfo?.projectTypeName" color="blue">
          {{ hospitalInfo.projectTypeName }}
        </a-tag>
        <a-tag v-if="hospitalInfo?.hospitalLevel" color="green">
          {{ hospitalInfo.hospitalLevel }}
        </a-tag>
      </div>
      <div class="info-card-grid">
        <div class="info-item">
          <span class="info-label">统一社会信用代码</span>
          <span class="info-value">{{ hospitalInfo?.unifiedSocialCreditCode || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">医疗机构执业许可证</span>
          <span class="info-value">{{ hospitalInfo?.medicalLicenseNo || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">医院类别</span>
          <span class="info-value">{{ hospitalInfo?.hospitalCategory || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">所属省份</span>
          <span class="info-value">{{ hospitalInfo?.provinceName || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">所在城市</span>
          <span class="info-value">{{ hospitalInfo?.cityName || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">所在区县</span>
          <span class="info-value">{{ hospitalInfo?.districtName || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">编制床位数</span>
          <span class="info-value">{{ hospitalInfo?.bedCount ?? '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">在职职工人数</span>
          <span class="info-value">{{ hospitalInfo?.employeeCount ?? '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">联系人</span>
          <span class="info-value">{{ hospitalInfo?.contactPerson || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">联系电话</span>
          <span class="info-value">{{ hospitalInfo?.contactPhone || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">详细地址</span>
          <span class="info-value">{{ hospitalInfo?.address || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">医院标签</span>
          <span class="info-value">{{ hospitalInfo?.tagNames || '-' }}</span>
        </div>
      </div>
    </div>

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
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
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

/* 医院基本信息卡片 */
.hospital-info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

/* 提示条 */
.info-tip-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 16px;
}

.tip-icon {
  color: #faad14;
  font-size: 14px;
  flex-shrink: 0;
}

.tip-text {
  flex: 1;
  font-size: 13px;
  color: #ad6800;
  line-height: 1.5;
}

.tip-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #d9d9d9;
  font-size: 16px;
  line-height: 1;
  padding: 0;
  flex-shrink: 0;
  transition: color 0.2s;
}

.tip-close:hover {
  color: #ad6800;
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.hospital-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.info-card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px 32px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  word-break: break-all;
}

@media (max-width: 900px) {
  .info-card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .info-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
