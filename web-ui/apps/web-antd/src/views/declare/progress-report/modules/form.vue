<script lang="ts" setup>
import type { DeclareProgressReport } from '#/api/declare/progress-report';
import type { DeclareHospitalApi } from '#/api/declare/hospital';

import { computed, h, nextTick, ref, watch } from 'vue';

import { prompt, useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { useIdleSessionStore } from '@vben/stores';
import { useDebounceFn } from '@vueuse/core';

import { message, Modal as AConfirmModal, Spin, Input } from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getLatestOpenWindow,
  getProgressReport,
  saveProgressReport,
  submitProgressReport,
} from '#/api/declare/progress-report';
import { getHospital, getHospitalByDeptId } from '#/api/declare/hospital';

import IndicatorInputTable from '../components/IndicatorInputTable/index.vue';
import ValidationSummaryModal from '../components/ValidationSummaryModal.vue';

const emit = defineEmits(['success']);

/** 空闲会话 Store */
const idleSessionStore = useIdleSessionStore();

/** localStorage 兜底草稿的 key */
const DRAFT_STORAGE_KEY = 'declare_progress_report_draft';

/** 草稿存储结构 */
interface LocalDraft {
  values: Array<{
    indicatorId: number;
    indicatorCode: string;
    valueType: number;
    value: any;
    valueDateStart?: string;
    valueDateEnd?: string;
  }>;
  inputTypeValues: Record<string, string>;
  containerValues: Record<string, any[]>;
  savedAt: number;
}

/** 加载 localStorage 草稿 */
function loadLocalDraft(): LocalDraft | null {
  try {
    const raw = localStorage.getItem(DRAFT_STORAGE_KEY);
    if (!raw) {
      console.log('[loadLocalDraft] localStorage 无草稿');
      return null;
    }
    const draft = JSON.parse(raw) as LocalDraft;
    console.log('[loadLocalDraft] 找到草稿', { valuesCount: draft.values.length, savedAt: dayjs(draft.savedAt).format('HH:mm:ss') });
    return draft;
  } catch {
    console.log('[loadLocalDraft] 解析失败');
    return null;
  }
}

/** 保存草稿到 localStorage */
function saveLocalDraft(values: LocalDraft['values'], inputTypeValues: Record<string, string>, containerValues: Record<string, any[]>) {
  try {
    const draft: LocalDraft = {
      values,
      inputTypeValues,
      containerValues,
      savedAt: Date.now(),
    };
    localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
  } catch {
    // localStorage 满了或不可用，静默忽略
  }
}

/** 清除 localStorage 草稿 */
function clearLocalDraft() {
  localStorage.removeItem(DRAFT_STORAGE_KEY);
}

/** 加载状态 */
const isLoading = ref(false);

/** 指标表格加载状态 */
const isLoadingIndicators = ref(false);

/** 填报进度 */
const fillProgress = ref({ total: 0, filled: 0, percentage: 0 });

/** 指标表格组件引用 */
const indicatorTableRef = ref<InstanceType<typeof IndicatorInputTable> | null>(
  null,
);

/** 验证汇总弹窗组件引用 */
const summaryModalRef = ref<InstanceType<typeof ValidationSummaryModal> | null>(
  null,
);

/** 医院信息 */
const hospitalInfo = ref<DeclareHospitalApi.Hospital | null>(null);

/** 基本医院信息（用于在 API 失败时显示） */
const basicHospitalInfo = ref<{
  hospitalName: string;
  unifiedSocialCreditCode?: string;
  medicalLicenseNo?: string;
  projectType?: number;
  projectTypeName?: string;
  projectTypeTitle?: string;
} | null>(null);

/** 是否是编辑已有记录 */
const isEditMode = computed(() => !!formData.value.id);

/** 是否只读（状态不允许编辑） */
const isReadonly = computed(() => {
  console.log('[是否编辑模式]', formData.value.reportStatus);
  const status = formData.value.reportStatus;
  return status === 'SUBMITTED' || status?.endsWith('APPROVED');
});

/** 项目类型展示文案（大标题用全称，始终有兜底，永不空字符串） */
const projectTypeDisplayLabel = computed(() => {
  // 优先使用接口返回的 projectTypeTitle（来自 declare_project_type.title，如"综合型医院"）
  const title = hospitalInfo.value?.projectTypeTitle?.trim();
  if (title) {
    return title;
  }
  // API 失败时用 basicHospitalInfo
  const fallbackTitle = basicHospitalInfo.value?.projectTypeTitle?.trim();
  if (fallbackTitle) {
    return fallbackTitle;
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
  // 优先使用接口返回的 projectTypeTitle（来自 declare_project_type.title，如"综合型医院"）
  const title = hospitalInfo.value?.projectTypeTitle?.trim();
  if (title) {
    return `${title} 填报`;
  }
  // basicHospitalInfo 备用
  const fallbackTitle = basicHospitalInfo.value?.projectTypeTitle?.trim();
  if (fallbackTitle) {
    return `${fallbackTitle} 填报`;
  }
  // 字典备用
  const projectType = hospitalInfo.value?.projectType;
  if (projectType !== undefined && projectType !== null) {
    const options = getDictOptions(DICT_TYPE.DECLARE_PROJECT_TYPE, 'number');
    const found = options.find((item) => Number(item.value) === projectType);
    if (found?.label) {
      return `${found.label} 填报`;
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
  projectTypeShortName: undefined as string | undefined,
  windowStart: undefined as string | undefined,
  windowEnd: undefined as string | undefined,
  reportUserName: undefined as string | undefined,
  /** 真正的医院 ID（用于查询上期值），与 hospitalId/deptId 区分 */
  realHospitalId: undefined as number | undefined,
});

/** 保存状态（用于禁用按钮防重复提交） */
const saving = ref(false);

/** 提交状态（用于禁用提交审核按钮） */
const submitting = ref(false);

/** 是否已保存（可提交审核的状态） */
const isSaved = computed(() => 
  formData.value.reportStatus === 'SAVED' || 
  formData.value.reportStatus?.endsWith('RETURNED') || 
  formData.value.reportStatus?.endsWith('REJECTED')
);

/** 标记表单在本次打开后是否被修改（用于关闭时判断） */
const isFormDirty = ref(false);

/** 监听指标表格内容变化，标记为已修改（用于关闭确认） */
watch(
  () => indicatorTableRef.value?.isDirty,
  (isDirty) => {
    if (isDirty && !saving.value) {
      isFormDirty.value = true;
    }
  },
);

const [Modal, modalApi] = useVbenModal({
  class: '!w-[90%]',
  destroyOnClose: true,
  showCancelButton: true,
  async onBeforeClose() {
    if (!isFormDirty.value) return; // 无修改，直接关闭

    const status = formData.value.reportStatus;

    // SAVED 状态：有修改时提示"保存"
    if (status === 'SAVED') {
      return new Promise((resolve) => {
        AConfirmModal.confirm({
          title: '确认关闭',
          content: '当前有未保存的修改，是否保存？',
          okText: '保存',
          cancelText: '不保存',
          okButtonProps: { loading: saving.value },
          async onOk() {
            await handleSave(); // handleSave 会关闭弹窗
            resolve(true);
          },
          async onCancel() {
            isFormDirty.value = false;
            resolve(true);
          },
        });
      });
    }

    // 新建或草稿(DRAFT)状态：有修改时提示"保存草稿"
    const isNewOrDraft =
      formData.value.id === undefined || status === 'DRAFT';

    if (isNewOrDraft) {
      return new Promise((resolve) => {
        AConfirmModal.confirm({
          title: '确认关闭',
          content: '当前有未保存的填报内容，关闭后将丢失。是否保存为草稿？',
          okText: '保存为草稿',
          cancelText: '不保存',
          okButtonProps: { loading: saving.value },
          async onOk() {
            await handleSaveDraft(); // handleSaveDraft 会关闭弹窗
            resolve(true);
          },
          async onCancel() {
            isFormDirty.value = false;
            resolve(true);
          },
        });
      });
    }
  },
  async onOpenChange(isOpen: boolean) {
    // 立即设置表单打开状态（不等待后续异步操作）
    idleSessionStore.setFormOpened(isOpen);
    // 打开表单时重置空闲计时器，避免用户填写表单时因超时被踢出
    if (isOpen) {
      idleSessionStore.resetActiveTime();
    }

    if (!isOpen) {
      idleSessionStore.syncFormData(null);
      formData.value = {
        id: undefined,
        hospitalId: 0,
        realHospitalId: undefined,
        projectType: undefined,
        reportYear: undefined,
        reportBatch: undefined,
        reportStatus: undefined,
        projectTypeShortName: undefined,
        windowStart: undefined,
        windowEnd: undefined,
        reportUserName: undefined,
      };
      hospitalInfo.value = null;
      basicHospitalInfo.value = null;
      isFormDirty.value = false;
      // 重置子组件的 dirty 状态
      indicatorTableRef.value?.resetDirty?.();
      // 清除超时保存回调
      idleSessionStore.setAutoSaveDraftCallback(() => Promise.resolve());
      return;
    }

    // 注入超时保存回调
    // - 有 id：尝试存数据库（静默）
    // - 无 id（新建）：保存到 localStorage（兜底）
    idleSessionStore.setAutoSaveDraftCallback(async () => {
      console.log('[autoSaveDraft callback] 被调用');
      modalApi.lock();
      try {
        indicatorTableRef.value?.syncContainerValuesToForm?.();
        indicatorTableRef.value?.syncAllAutoEntryContainers?.();
        indicatorTableRef.value?.recalculateComputedIndicators?.();

        const rawValues = indicatorTableRef.value?.getAllIndicatorValues?.() || [];
        const values = rawValues.map((item: any) => ({
          indicatorId: item.indicatorId,
          indicatorCode: item.indicatorCode,
          valueType: item.valueType,
          value: item.valueType === 8 ? undefined : extractValue(item),
          ...(item.valueType === 8 ? { valueDateStart: item.valueDateStart, valueDateEnd: item.valueDateEnd } : {}),
        }));

        if (formData.value.id) {
          // 有 id：存数据库
          const errors = indicatorTableRef.value?.validateFilledData?.() || [];
          if (errors.length > 0) return;
          await saveProgressReport({
            id: formData.value.id,
            reportYear: formData.value.reportYear,
            reportBatch: formData.value.reportBatch,
            reportStatus: 'DRAFT',
            values,
          });
        } else {
          // 新建：无 id，保存到 localStorage
          console.log('[autoSaveDraft] 保存草稿到 localStorage', { valuesCount: values.length });
          saveLocalDraft(values, {}, {});
        }
      } catch {
        // 静默忽略
      } finally {
        modalApi.unlock();
      }
    });

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
          projectTypeTitle: (data as any).projectTypeTitle,
        };

        // 加载医院详细信息（data.hospitalId 实际是 deptId）
        try {
          hospitalInfo.value = await getHospitalByDeptId(data.hospitalId);
          // 同步 projectType 到 formData（用于加载联合验证规则）
          if (hospitalInfo.value?.projectType !== undefined) {
            formData.value.projectType = hospitalInfo.value.projectType;
          }
          // 同步真正的 hospitalId（用于查询上期值）
          if (hospitalInfo.value?.id !== undefined) {
            formData.value.realHospitalId = hospitalInfo.value.id;
          }
        } catch (error) {
          console.error('加载医院详细信息失败，使用基本信息:', error);
          // API 失败时，使用基本信息
          hospitalInfo.value = {
            hospitalName: data.hospitalName || '',
            unifiedSocialCreditCode: '',
            medicalLicenseNo: '',
            projectTypeTitle: basicHospitalInfo.value?.projectTypeTitle,
          } as DeclareHospitalApi.Hospital;
        }
      }

      if (data?.id) {
        // 编辑已有记录
        const result = await getProgressReport(data.id);
        formData.value = { ...result, realHospitalId: result.hospitalId } as typeof formData.value;

        // 编辑时也需要加载医院信息
        if (!hospitalInfo.value && result.hospitalId) {
          basicHospitalInfo.value = {
            hospitalName: result.hospitalName || '',
            unifiedSocialCreditCode: (result as any).unifiedSocialCreditCode,
            medicalLicenseNo: (result as any).medicalLicenseNo,
            projectType: result.projectType,
            projectTypeName: result.projectTypeName,
            projectTypeTitle: (result as any).projectTypeTitle,
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
        formData.value.windowStart = latestWindow.windowStart;
        formData.value.windowEnd = latestWindow.windowEnd;
      }
    } catch (error) {
      console.error('加载数据失败:', error);
      message.error('加载数据失败');
    } finally {
      isLoading.value = false;
      // 指标表格加载完成后，尝试恢复 localStorage 草稿（新建记录场景）
      nextTick(async () => {
        if (!formData.value.id) {
          const draft = loadLocalDraft();
          if (draft && draft.values.length > 0) {
            // 等待 IndicatorInputTable 完全挂载
            await nextTick();
            indicatorTableRef.value?.setValues?.(draft.values);
            message.success(`已恢复本地草稿（保存于 ${dayjs(draft.savedAt).format('HH:mm')}）`);
            clearLocalDraft();
          }
        }
      });
    }
  },
});

/** 保存草稿：状态保持 DRAFT，仅记录填写进度，仅验证已填数据的格式（不做必填验证） */
async function handleSaveDraft() {
  modalApi.lock();
  saving.value = true;
  try {
    // 1. 同步动态容器值到 formValues
    indicatorTableRef.value?.syncContainerValuesToForm?.();

    // 2. 同步所有自动条目容器（根据当前 formValues 清理不需要的条目）
    indicatorTableRef.value?.syncAllAutoEntryContainers?.();

    // 3. 触发自动计算
    indicatorTableRef.value?.recalculateComputedIndicators?.();

    // 3. 验证已填数据的格式、范围、精度（不做非空验证）
    const errors = indicatorTableRef.value?.validateFilledData?.() || [];
    if (errors.length > 0) {
      summaryModalRef.value?.show(errors);
      return;
    }

    // 4. 获取所有指标值
    const rawValues = indicatorTableRef.value?.getAllIndicatorValues?.() || [];
    console.log('[DEBUG handleSaveDraft] rawValues 702:', JSON.stringify(rawValues.find((r: any) => r.indicatorCode === '702')));
    const values = rawValues.map((item: any) => {
      const base: any = {
        indicatorId: item.indicatorId,
        indicatorCode: item.indicatorCode,
        valueType: item.valueType,
        value: extractValue(item),
      };
      if (item.valueType === 8) {
        base.value = undefined;
        base.valueDateStart = item.valueDateStart;
        base.valueDateEnd = item.valueDateEnd;
      }
      return base;
    });
    console.log('[DEBUG handleSaveDraft] values 702:', JSON.stringify(values.find((v: any) => v.indicatorCode === '702')));

    // 5. 调用一体化保存接口，状态为 DRAFT
    const id = await saveProgressReport({
      id: formData.value.id,
      reportYear: formData.value.reportYear,
      reportBatch: formData.value.reportBatch,
      reportStatus: 'DRAFT',
      values,
    });
    formData.value.id = id;
    formData.value.reportStatus = 'DRAFT';
    isFormDirty.value = false;
    clearLocalDraft(); // 成功后清除本地草稿
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

/** 姓名格式校验 */
function validateReportUserName(name: string): string | null {
  const trimmed = name.trim();
  if (!trimmed) return '请输入填报人姓名';
  if (!/^[\u4e00-\u9fa5·•.·]+$/u.test(trimmed) && !/^[a-zA-Z\s·]+$/.test(trimmed)) {
    return '填报人姓名格式不正确，请输入真实姓名';
  }
  if (trimmed.length < 2 || trimmed.length > 20) {
    return '填报人姓名长度应在2-20个字符之间';
  }
  return null;
}

/** 保存：状态设为 SAVED，需验证所有必填指标，确认无误后可在列表页提交审核 */
async function handleSave() {
  modalApi.lock();
  saving.value = true;
  try {
    // 1. 同步动态容器值到 formValues
    indicatorTableRef.value?.syncContainerValuesToForm?.();

    // 2. 同步所有自动条目容器（根据当前 formValues 清理不需要的条目）
    indicatorTableRef.value?.syncAllAutoEntryContainers?.();

    // 3. 【验证必填指标】必须全部通过才能继续
    const firstResult = indicatorTableRef.value?.validateAll?.() as any;
    const firstErrors: any[] = firstResult?.messages ?? (Array.isArray(firstResult) ? firstResult : []);
    if (firstErrors.length > 0) {
      summaryModalRef.value?.show(firstErrors);
      return;
    }

    // 4. 触发自动计算（在校验之后）
    indicatorTableRef.value?.recalculateComputedIndicators?.();

    // 5. 再次校验（确保计算后的值也通过验证）
    const secondResult = indicatorTableRef.value?.validateAll?.() as any;
    const secondErrors: any[] = secondResult?.messages ?? (Array.isArray(secondResult) ? secondResult : []);
    if (secondErrors.length > 0) {
      summaryModalRef.value?.show(secondErrors);
      return;
    }

    // 5. 【验证通过后】弹出填报人姓名输入框
    let reportUserName = formData.value.reportUserName?.trim();
    if (!reportUserName) {
      reportUserName = await new Promise<string>((resolve) => {
        prompt({
          title: '填报人信息',
          content: '请输入填报人真实姓名',
          modelPropName: 'value',
          component: () =>
            h(Input, {
              placeholder: '请输入填报人真实姓名',
              allowClear: true,
            }),
          async beforeClose(scope) {
            if (!scope.isConfirm) {
              resolve('');
              return;
            }
            const name = (scope.value as string) || '';
            const error = validateReportUserName(name);
            if (error) {
              message.warning(error);
              return false;
            }
            resolve(name.trim());
          },
        });
      });
    }

    if (!reportUserName) return; // 用户取消或校验失败

    // 6. 获取所有指标值
    const rawValues = indicatorTableRef.value?.getAllIndicatorValues?.() || [];
    console.log('[DEBUG handleSave] rawValues 702:', JSON.stringify(rawValues.find((r: any) => r.indicatorCode === '702')));
    const values = rawValues.map((item: any) => {
      const base: any = {
        indicatorId: item.indicatorId,
        indicatorCode: item.indicatorCode,
        valueType: item.valueType,
        value: extractValue(item),
      };
      if (item.valueType === 8) {
        base.value = undefined;
        base.valueDateStart = item.valueDateStart;
        base.valueDateEnd = item.valueDateEnd;
      }
      return base;
    });
    console.log('[DEBUG handleSave] values 702:', JSON.stringify(values.find((v: any) => v.indicatorCode === '702')));

    // 7. 调用一体化保存接口，状态为 SAVED，传入填报人姓名
    const id = await saveProgressReport({
      id: formData.value.id,
      reportYear: formData.value.reportYear,
      reportBatch: formData.value.reportBatch,
      reportStatus: 'SAVED',
      reportUserName,
      values,
    });
    formData.value.id = id;
    formData.value.reportStatus = 'SAVED';
    isFormDirty.value = false;
    clearLocalDraft(); // 成功后清除本地草稿
    message.success('保存成功');
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

/** 提交审核：仅在 SAVED 状态下可用，直接发起 BPM 流程，不重新保存 */
async function handleSubmit() {
  modalApi.lock();
  submitting.value = true;
  try {
    // 1. 【验证必填指标】必须全部通过才能提交
    if (indicatorTableRef.value) {
      const firstResult = indicatorTableRef.value.validateAll?.() as any;
      const errors: any[] = firstResult?.messages ?? (Array.isArray(firstResult) ? firstResult : []);
      if (errors.length > 0) {
        summaryModalRef.value?.show(errors);
        return;
      }
    }

    // 2. 触发自动计算（在校验之后）
    indicatorTableRef.value?.recalculateComputedIndicators?.();

    // 3. 再次校验（确保计算后的值也通过验证）
    if (indicatorTableRef.value) {
      const secondResult = indicatorTableRef.value.validateAll?.() as any;
      const errors: any[] = secondResult?.messages ?? (Array.isArray(secondResult) ? secondResult : []);
      if (errors.length > 0) {
        summaryModalRef.value?.show(errors);
        return;
      }
    }

    // 4. 直接发起 BPM 流程（数据已在上一次保存时入库，状态为 SAVED）
    await submitProgressReport(formData.value.id!);
    formData.value.reportStatus = 'SUBMITTED';
    isFormDirty.value = false;
    message.success('提交审核成功');
    await modalApi.close();
    emit('success');
  } catch (error) {
    console.error('提交失败:', error);
    message.error('提交失败');
  } finally {
    modalApi.unlock();
    submitting.value = false;
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
  // type 8 日期区间：valueDateStart/valueDateEnd 通过独立字段传递，不走 value
  return null;
}

/** 取消 */
function handleCancel() {
  modalApi.close();
}

/** 指标表格加载状态变化 */
function handleIndicatorLoadingChange(loading: boolean) {
  isLoadingIndicators.value = loading;
  if (!loading) {
    nextTick(() => updateFillProgress());
  }
}

/** 更新填报进度 */
function updateFillProgress() {
  if (!indicatorTableRef.value?.getFillProgress) {
    fillProgress.value = { total: 0, filled: 0, percentage: 0 };
    return;
  }
  fillProgress.value = indicatorTableRef.value.getFillProgress();
}

defineExpose({ setData: modalApi.setData });

/** 防抖同步表单数据到 Store（用于 idle 超时保存） */
const debouncedSyncFormData = useDebounceFn(() => {
  // 只在有 reportId 时同步（新建记录无 id，无法保存）
  if (!formData.value?.id) return;

  // 同步容器值到 formValues，确保 getAllIndicatorValues 能取到容器数据
  indicatorTableRef.value?.syncContainerValuesToForm?.();

  const rawValues = indicatorTableRef.value?.getAllIndicatorValues?.() || [];
  const values = rawValues.map((item: any) => {
    const base: any = {
      indicatorId: item.indicatorId,
      indicatorCode: item.indicatorCode,
      valueType: item.valueType,
      value: extractValue(item),
    };
    if (item.valueType === 8) {
      base.value = undefined;
      base.valueDateStart = item.valueDateStart;
      base.valueDateEnd = item.valueDateEnd;
    }
    return base;
  });

  idleSessionStore.syncFormData({
    id: formData.value.id,
    reportYear: formData.value.reportYear,
    reportBatch: formData.value.reportBatch,
    reportStatus: 'DRAFT',
    values,
  });
}, 500);

// watch formData 变化，同步到 Store
watch(
  formData,
  () => {
    if (formData.value?.id && idleSessionStore.isFormOpened) {
      debouncedSyncFormData();
    }
  },
  { deep: true },
);
</script>

<template>
  <Modal class="progress-form-modal wide-modal">
    <template #title>
      <span class="modal-title-text">{{ modalTitle }}</span>
      <span
        v-if="fillProgress.total > 0"
        class="modal-title-progress"
      >
        已填 {{ fillProgress.filled }} 项/共 {{ fillProgress.total }} 项, 已完成 {{ fillProgress.percentage }}%
      </span>
    </template>
    <Spin :spinning="isLoading || isLoadingIndicators">
      <!-- 项目类型大标题（与下方「项目类型」不同源展示） -->
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
            <span class="info-value">国家中医药管理局监测统计中心</span>
          </div>
          <div class="info-item">
            <span class="info-label">项目类型：</span>
            <span class="info-value">{{ formData.projectTypeShortName || hospitalInfo?.projectTypeName || '-' }}</span>
          </div>
          <!-- <div class="info-item">
            <span class="info-label">填报窗口：</span>
            <span class="info-value">{{
              formData.reportYear
                ? `${formData.reportYear}年第${formData.reportBatch}期`
                : '-'
            }}</span>
          </div> -->
          <div class="info-item">
            <span class="info-label">填报窗口：</span>
            <span class="info-value">
              <!-- {{ formData.windowStart ? dayjs(formData.windowStart).format('YYYY-MM-DD') : '-' }}
              ~
              {{ formData.windowEnd ? dayjs(formData.windowEnd).format('YYYY-MM-DD') : '-' }} -->
               {{ dayjs().format('YYYY-MM') }}
            </span>
          </div>
        </div>
      </div>

      <!-- 指标表格（仅在 hospitalId 确定后渲染） -->
      <div class="indicator-area">
        <IndicatorInputTable
          ref="indicatorTableRef"
          v-if="formData.hospitalId"
          :hospital-id="formData.hospitalId"
          :real-hospital-id="formData.realHospitalId"
          :project-type="formData.projectType"
          :report-id="formData.id"
          :report-year="formData.reportYear"
          :report-batch="formData.reportBatch"
          :readonly="isReadonly"
          @loading-change="handleIndicatorLoadingChange"
          @update="updateFillProgress"
        />
      </div>
    </Spin>

    <!-- 验证汇总弹窗 -->
    <ValidationSummaryModal ref="summaryModalRef" :scroll-to-field-ref="indicatorTableRef" />

    <template #footer>
      <a-button @click="handleCancel">取消</a-button>
      <template v-if="!isReadonly">
        <!-- SAVED 之前：显示保存草稿 + 保存 -->
        <template v-if="!isSaved">
          <a-button @click="handleSaveDraft" :loading="saving">保存草稿</a-button>
          <a-button type="default" @click="handleSave" :loading="saving">保存</a-button>
        </template>
        <!-- SAVED 之后：显示保存 + 提交审核（不再需要保存草稿） -->
        <template v-else>
          <a-button @click="handleSave" :loading="saving">保存</a-button>
          <!-- <a-button type="primary" @click="handleSubmit" :loading="submitting">提交审核</a-button> -->
        </template>
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

<style>
/* Modal title 容器设为相对定位，作为进度居中的定位基准 */
.progress-form-modal .ant-modal-title {
  position: relative;
}

/* 进度文本绝对居中，不影响左侧标题 */
.modal-title-progress {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 1.125rem;
  line-height: 1.4;
  color: hsl(var(--primary-dark));
  font-weight: 600;
  pointer-events: none;
  white-space: nowrap;
}
</style>
