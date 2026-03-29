<script lang="ts" setup>
import type { DeclareProgressReport } from '#/api/declare/progress-report';
import type { DeclareHospitalApi } from '#/api/declare/hospital';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { message, Spin } from 'ant-design-vue';

import {
  getLatestOpenWindow,
  getProgressReport,
  saveProgressReport,
  submitProgressReport,
} from '#/api/declare/progress-report';
import { getHospital, getHospitalByDeptId } from '#/api/declare/hospital';

import IndicatorInputTable from '../components/IndicatorInputTable.vue';
import ValidationSummaryModal from '../components/ValidationSummaryModal.vue';

const emit = defineEmits(['success']);

/** 加载状态 */
const isLoading = ref(false);

/** 指标表格加载状态 */
const isLoadingIndicators = ref(false);

/** 指标表格组件引用 */
const indicatorTableRef = ref<InstanceType<typeof IndicatorInputTable> | null>(
  null,
);

/** 验证汇总弹窗组件引用 */
const summaryModalRef = ref<InstanceType<typeof ValidationSummaryModal> | null>(
  null,
);

/** 动态容器值缓存（用于保存时传递到 IndicatorInputTable） */
const containerValuesData = ref<Record<string, string>>({});

/** 医院信息 */
const hospitalInfo = ref<DeclareHospitalApi.Hospital | null>(null);

/** 基本医院信息（用于在 API 失败时显示） */
const basicHospitalInfo = ref<{
  hospitalName: string;
  unifiedSocialCreditCode?: string;
  medicalLicenseNo?: string;
  projectType?: number;
  projectTypeName?: string;
} | null>(null);

/** 是否是编辑已有记录 */
const isEditMode = computed(() => !!formData.value.id);

/** 是否只读（状态不允许编辑） */
const isReadonly = computed(() => {
  const status = formData.value.reportStatus;
  return status !== 'DRAFT' && status !== 'SAVED';
});

/** 项目类型展示文案（始终有兜底，永不空字符串） */
const projectTypeDisplayLabel = computed(() => {
  // 优先使用接口返回的 projectTypeName（来自 declare_project_type.title，如"综合型医院"）
  const name = hospitalInfo.value?.projectTypeName?.trim();
  if (name) {
    return name;
  }
  // API 失败时用 basicHospitalInfo
  const fallbackName = basicHospitalInfo.value?.projectTypeName?.trim();
  if (fallbackName) {
    return fallbackName;
  }
  // 字典备用（字典 label 应与 declare_project_type.title 一致）
  const projectType = hospitalInfo.value?.projectType;
  if (projectType !== undefined && projectType !== null) {
    const options = getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number');
    const found = options.find((item) => Number(item.value) === projectType);
    if (found?.label) {
      return found.label;
    }
  }
  const fallbackProjectType = basicHospitalInfo.value?.projectType;
  if (fallbackProjectType !== undefined && fallbackProjectType !== null) {
    const options = getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number');
    const found = options.find((item) => Number(item.value) === fallbackProjectType);
    if (found?.label) {
      return found.label;
    }
  }
  // 最终兜底
  return '项目进度填报';
});

/** 弹窗标题 */
const modalTitle = computed(() => {
  if (isEditMode.value) {
    return '编辑填报';
  }
  // 优先接口返回的 title
  const name = hospitalInfo.value?.projectTypeName?.trim();
  if (name) {
    return `新建${name}填报`;
  }
  // basicHospitalInfo 备用
  const fallbackName = basicHospitalInfo.value?.projectTypeName?.trim();
  if (fallbackName) {
    return `新建${fallbackName}填报`;
  }
  // 字典备用
  const projectType = hospitalInfo.value?.projectType;
  if (projectType !== undefined && projectType !== null) {
    const options = getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number');
    const found = options.find((item) => Number(item.value) === projectType);
    if (found?.label) {
      return `新建${found.label}填报`;
    }
  }
  return '新建填报';
});

/** 弹窗数据 */
const formData = ref({
  id: undefined as number | undefined,
  hospitalId: 0,
  projectType: undefined as number | undefined,
  reportYear: undefined as number | undefined,
  reportBatch: undefined as number | undefined,
  reportStatus: undefined as string | undefined,
});

/** 保存草稿状态 */
const saving = ref(false);

const [Modal, modalApi] = useVbenModal({
  class: '!w-[90%]',
  destroyOnClose: true,
  showCancelButton: true,
  confirmText: '提交审核',
  async onConfirm() {
    // 提交审核
    await handleSubmit();
    return false;
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = {
        id: undefined,
        hospitalId: 0,
        projectType: undefined,
        reportYear: undefined,
        reportBatch: undefined,
        reportStatus: undefined,
      };
      hospitalInfo.value = null;
      basicHospitalInfo.value = null;
      return;
    }

    isLoading.value = true;
    try {
      const data = modalApi.getData<
        { hospitalId: number } & DeclareProgressReport
      >();

      // hospitalId 字段歧义：新建时传的是 deptId，编辑时存的是 hospital.id
      if (data?.hospitalId) {
        formData.value.hospitalId = data.hospitalId;

        // 保存基本信息用于备用显示（projectType 等字段来自 DeclareProgressReport，unifiedSocialCreditCode/medicalLicenseNo 需强转）
        basicHospitalInfo.value = {
          hospitalName: data.hospitalName || '',
          unifiedSocialCreditCode: (data as any).unifiedSocialCreditCode,
          medicalLicenseNo: (data as any).medicalLicenseNo,
          projectType: data.projectType,
          projectTypeName: data.projectTypeName,
        };

        // 加载医院详细信息（data.hospitalId 实际是 deptId）
        try {
          hospitalInfo.value = await getHospitalByDeptId(data.hospitalId);
          // 同步 projectType 到 formData（用于加载联合验证规则）
          if (hospitalInfo.value?.projectType !== undefined) {
            formData.value.projectType = hospitalInfo.value.projectType;
          }
        } catch (error) {
          console.error('加载医院详细信息失败，使用基本信息:', error);
          // API 失败时，使用基本信息
          hospitalInfo.value = {
            hospitalName: data.hospitalName || '',
            unifiedSocialCreditCode: '',
            medicalLicenseNo: '',
          } as DeclareHospitalApi.Hospital;
        }
      }

      if (data?.id) {
        // 编辑已有记录
        const result = await getProgressReport(data.id);
        formData.value = { ...result } as typeof formData.value;

        // 编辑时也需要加载医院信息
        if (!hospitalInfo.value && result.hospitalId) {
          basicHospitalInfo.value = {
            hospitalName: result.hospitalName || '',
            unifiedSocialCreditCode: (result as any).unifiedSocialCreditCode,
            medicalLicenseNo: (result as any).medicalLicenseNo,
            projectType: result.projectType,
            projectTypeName: result.projectTypeName,
          };
          try {
            hospitalInfo.value = await getHospital(result.hospitalId);
          } catch (error) {
            console.error('加载医院信息失败:', error);
          }
        }
        // 同步 projectType 到 formData（用于加载联合验证规则）
        if (hospitalInfo.value?.projectType !== undefined) {
          formData.value.projectType = hospitalInfo.value.projectType;
        } else if (result.projectType !== undefined) {
          formData.value.projectType = result.projectType;
        }
      } else {
        // 新建记录：获取最新开放窗口
        const latestWindow = await getLatestOpenWindow();
        if (!latestWindow) {
          message.error('当前没有开放的填报窗口');
          await modalApi.close();
          return;
        }
        formData.value.reportYear = latestWindow.reportYear;
        formData.value.reportBatch = latestWindow.reportBatch;
        formData.value.reportStatus = 'DRAFT';
      }
    } catch (error) {
      console.error('加载数据失败:', error);
      message.error('加载数据失败');
    } finally {
      isLoading.value = false;
    }
  },
});

/** 保存草稿 */
async function handleSaveDraft() {
  modalApi.lock();
  saving.value = true;
  try {
    // 同步动态容器值
    if (indicatorTableRef.value) {
      const containerVals = indicatorTableRef.value.getContainerValues();
      Object.assign(containerValuesData.value, containerVals);
    }

    // 验证所有必填指标
    if (indicatorTableRef.value) {
      const indicators = indicatorTableRef.value.getAllIndicators?.();
      console.log('[handleSaveDraft] 获取到指标数量:', indicators?.length);
      if (indicators) {
        const errors = indicatorTableRef.value.validateAll(indicators);
        console.log('[handleSaveDraft] 验证错误:', errors);
        if (errors.length > 0) {
          summaryModalRef.value?.show(errors);
          return;
        }
      }
    }

    // 1. 触发自动计算，确保所有计算指标的值是最新的
    indicatorTableRef.value?.recalculateComputedIndicators?.();

    // 2. 获取所有指标值并转换为统一格式
    const rawValues = indicatorTableRef.value?.getAllIndicatorValues?.() || [];
    const values = rawValues.map((item: any) => ({
      indicatorId: item.indicatorId,
      indicatorCode: item.indicatorCode,
      valueType: item.valueType,
      value: extractValue(item),
    }));
    console.log('[handleSaveDraft] 保存指标值，数量:', values.length, 'reportId:', formData.value.id);

    // 3. 调用一体化保存接口
    const id = await saveProgressReport({
      id: formData.value.id,
      reportYear: formData.value.reportYear,
      reportBatch: formData.value.reportBatch,
      reportStatus: 'SAVED',
      values,
    });
    formData.value.id = id;
    formData.value.reportStatus = 'SAVED';

    message.success('保存草稿成功');
    await modalApi.close();
    emit('success');
  } catch (error) {
    console.error('保存失败:', error);
    message.error('保存失败');
  } finally {
    modalApi.unlock();
    saving.value = false;
  }
}

/** 提交审核 */
async function handleSubmit() {
  modalApi.lock();
  try {
    // 同步动态容器值
    if (indicatorTableRef.value) {
      const containerVals = indicatorTableRef.value.getContainerValues();
      Object.assign(containerValuesData.value, containerVals);
    }

    // 指标验证
    if (indicatorTableRef.value) {
      const indicators = indicatorTableRef.value.getAllIndicators?.();
      if (indicators) {
        const errors = indicatorTableRef.value.validateAll(indicators);
        if (errors.length > 0) {
          summaryModalRef.value?.show(errors);
          return;
        }
      }
    }

    // 1. 触发自动计算
    indicatorTableRef.value?.recalculateComputedIndicators?.();

    // 2. 获取所有指标值并转换为统一格式
    const rawValues = indicatorTableRef.value?.getAllIndicatorValues?.() || [];
    const values = rawValues.map((item: any) => ({
      indicatorId: item.indicatorId,
      indicatorCode: item.indicatorCode,
      valueType: item.valueType,
      value: extractValue(item),
    }));
    console.log('[handleSubmit] 保存指标值，数量:', values.length, 'reportId:', formData.value.id);

    // 3. 调用一体化保存接口
    const id = await saveProgressReport({
      id: formData.value.id,
      reportYear: formData.value.reportYear,
      reportBatch: formData.value.reportBatch,
      reportStatus: 'SUBMITTED',
      values,
    });
    formData.value.id = id;

    // 4. 提交流程
    await submitProgressReport(id);
    message.success('提交审核成功');
    await modalApi.close();
    emit('success');
  } catch (error) {
    console.error('提交失败:', error);
    message.error('提交失败');
  } finally {
    modalApi.unlock();
  }
}

/** 从 getAllIndicatorValues 的返回项中提取统一的 value 字段 */
function extractValue(item: any): any {
  if (item.value !== undefined) return item.value;
  if (item.valueNum !== undefined) return item.valueNum;
  if (item.valueStr !== undefined) return item.valueStr;
  if (item.valueText !== undefined) return item.valueText;
  if (item.valueBool !== undefined) return item.valueBool;
  if (item.valueDate !== undefined) return item.valueDate;
  if (item.valueDateStart !== undefined) {
    return [item.valueDateStart, item.valueDateEnd || ''];
  }
  return null;
}

/** 取消 */
function handleCancel() {
  modalApi.close();
}

/** 指标表格加载状态变化 */
function handleIndicatorLoadingChange(loading: boolean) {
  isLoadingIndicators.value = loading;
}

defineExpose({ setData: modalApi.setData });
</script>

<template>
  <Modal :title="modalTitle" class="progress-form-modal wide-modal">
    <Spin :spinning="isLoading || isLoadingIndicators">
      <!-- 项目类型大标题（与下方「项目类型」同源展示） -->
      <div class="project-type-title">
        {{ projectTypeDisplayLabel }}
      </div>

      <!-- 医院信息头部 -->
      <div class="hospital-info-header">
        <div class="hospital-info-row">
          <div class="info-item">
            <span class="info-label">统一社会信用代码：</span>
            <span class="info-value">{{
              hospitalInfo?.unifiedSocialCreditCode ||
              basicHospitalInfo?.unifiedSocialCreditCode ||
              '-'
            }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">医疗机构执业许可证登记号：</span>
            <span class="info-value">{{
              hospitalInfo?.medicalLicenseNo ||
              basicHospitalInfo?.medicalLicenseNo ||
              '-'
            }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">机构名称：</span>
            <span class="info-value">{{
              hospitalInfo?.hospitalName ||
              basicHospitalInfo?.hospitalName ||
              '-'
            }}</span>
          </div>
        </div>
        <div class="hospital-info-row">
          <div class="info-item">
            <span class="info-label">制定机关：</span>
            <span class="info-value">国家中医药管理局监统中心</span>
          </div>
          <div class="info-item">
            <span class="info-label">项目类型：</span>
            <span class="info-value">{{ projectTypeDisplayLabel }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">填报年度：</span>
            <span class="info-value">{{
              formData.reportYear
                ? `${formData.reportYear}年第${formData.reportBatch}期`
                : '-'
            }}</span>
          </div>
        </div>
      </div>

      <!-- 指标表格（仅在 hospitalId 确定后渲染） -->
      <div class="indicator-area">
        <IndicatorInputTable
          ref="indicatorTableRef"
          v-if="formData.hospitalId"
          :hospital-id="formData.hospitalId"
          :project-type="formData.projectType"
          :report-id="formData.id"
          :report-year="formData.reportYear"
          :report-batch="formData.reportBatch"
          :readonly="isReadonly"
          @loading-change="handleIndicatorLoadingChange"
        />
      </div>
    </Spin>

    <!-- 验证汇总弹窗 -->
    <ValidationSummaryModal ref="summaryModalRef" />

    <template #footer>
      <a-button @click="handleCancel">取消</a-button>
      <template v-if="!isReadonly">
        <a-button @click="handleSaveDraft" :loading="saving">
          保存草稿
        </a-button>
        <a-button type="primary" @click="handleSubmit"> 提交审核 </a-button>
      </template>
    </template>
  </Modal>
</template>

<style>
/* 非 scoped：穿透 Teleport，强制 DialogContent 宽度为 90% */
.wide-modal {
  width: 90% !important;
  max-width: 90vw !important;
}
</style>

<style scoped>
/* ===== 弹窗主体 ===== */
.progress-form-modal :deep(.ant-modal-body) {
  padding: 0;
}

/* ===== 项目类型大标题 ===== */
.project-type-title {
  font-size: 20px;
  font-weight: 700;
  color: hsl(var(--primary));
  text-align: center;
  margin-bottom: 0;
  padding: 20px 24px 16px;
  background: linear-gradient(135deg, hsl(var(--primary) / 0.06) 0%, hsl(var(--background)) 100%);
  border-bottom: 1px solid hsl(var(--border) / 0.5);
  letter-spacing: 0.05em;
  position: relative;
}

.project-type-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 3px;
  background: hsl(var(--primary));
  border-radius: 2px 2px 0 0;
}

/* ===== 医院信息头部 ===== */
.hospital-info-header {
  background: hsl(var(--background));
  border-bottom: 1px solid hsl(var(--border) / 0.5);
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.hospital-info-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 32px;
  align-items: stretch;
}

.hospital-info-row + .hospital-info-row {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed hsl(var(--border));
}

.info-item {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  text-align: left;
  min-width: 0;
  gap: 3px;
}

.info-label {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  font-weight: 500;
  line-height: 1.4;
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-label::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 10px;
  background: hsl(var(--primary));
  border-radius: 2px;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: hsl(var(--foreground));
  font-weight: 600;
  line-height: 1.4;
  word-break: break-all;
}

/* ===== 指标区域内边距 ===== */
.indicator-area {
  padding: 20px 24px 24px;
}

/* ===== 加载状态 ===== */
.loading-wrapper {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 900px) {
  .hospital-info-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .hospital-info-row {
    grid-template-columns: 1fr;
  }
}
</style>
