/**
 * 表单值状态管理 composable
 */

import { reactive, ref, watch } from 'vue';
import dayjs from 'dayjs';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';

export function useFormValues() {
  /** 表单值 Map */
  const formValues = reactive<Record<string, any>>({});

  /** 是否已修改 */
  const isDirty = ref(false);

  /** 文件列表 Map */
  const fileListMap = reactive<Record<string, UploadFile[]>>({});

  /** 标记为已修改 */
  function markDirty() {
    isDirty.value = true;
  }

  /** 重置 dirty */
  function resetDirty() {
    isDirty.value = false;
  }

  /** 监听 formValues 变化，标记 dirty */
  watch(
    () => formValues,
    () => {
      const hasAnyValue = Object.values(formValues).some(
        (v) => v !== undefined && v !== null && v !== '',
      );
      if (hasAnyValue) {
        markDirty();
      }
    },
    { deep: true },
  );

  /** 获取文件列表 */
  function getFileList(indicatorCode: string): UploadFile[] {
    return fileListMap[indicatorCode] || [];
  }

  /** 设置文件列表 */
  function setFileList(indicatorCode: string, files: UploadFile[]) {
    fileListMap[indicatorCode] = files;
  }

  return {
    formValues,
    isDirty,
    fileListMap,
    markDirty,
    resetDirty,
    getFileList,
    setFileList,
  };
}
