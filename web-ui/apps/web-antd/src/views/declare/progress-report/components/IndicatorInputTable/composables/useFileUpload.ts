/**
 * useFileUpload - 文件上传
 *
 * 负责：
 * - 文件列表状态（fileListMap）
 * - 文件上传/删除操作
 */

import { reactive } from 'vue';
import { message } from 'ant-design-vue';
import { Upload } from 'ant-design-vue';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';
import { uploadFile } from '#/api/infra/file';
import { getAcceptTypes, getMaxFileCount } from '../utils/indicator';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { formValues } from './useFormValues';
import { fieldErrors, setFieldError, toTopLevelKey } from './useErrorKeys';

// ==================== 文件列表状态 ====================

/** 文件列表（key: indicatorCode → UploadFile[]） */
export const fileListMap = reactive<Record<string, UploadFile[]>>({});

// ==================== 文件上传 ====================

async function handleFileUpload(file: File, indicator: DeclareIndicatorApi.Indicator): Promise<boolean> {
  const indicatorCode = indicator.indicatorCode;
  const maxCount = getMaxFileCount(indicator);
  const acceptTypes = getAcceptTypes(indicator);

  if (acceptTypes) {
    const fileExt = file.name.split('.').pop()?.toLowerCase() || '';
    const allowedExts = acceptTypes.split(',').map((ext: string) => ext.trim().toLowerCase());
    if (!allowedExts.includes(fileExt)) {
      message.warning(`仅支持上传以下格式：${acceptTypes}`);
      return false;
    }
  }

  try {
    const result = await uploadFile({ file: file as any, directory: 'declare/indicator' });
    const fileUrl = result.url || result;
    const fileName = file.name;
    const currentList = fileListMap[indicatorCode] || [];

    if (currentList.some((f) => f.name === fileName)) {
      message.warning('文件已存在');
      return false;
    }

    if (currentList.length >= maxCount) {
      message.warning(`最多上传${maxCount}个文件`);
      return false;
    }

    const newFile: UploadFile = {
      uid: Date.now().toString(),
      name: fileName,
      url: fileUrl,
      status: 'done',
    };

    fileListMap[indicatorCode] = [...currentList, newFile];
    formValues[indicatorCode] = JSON.stringify(fileListMap[indicatorCode]);
    message.success('文件上传成功');

    // 上传成功后强制清除错误（直接删除，不走 clearFieldError 的保护逻辑）
    if (indicator.id !== undefined) {
      const key = toTopLevelKey(indicator.id);
      if (fieldErrors[key]) {
        delete fieldErrors[key];
      }
    }
  } catch (error) {
    console.error('文件上传错误:', error);
    message.error('文件上传失败');
  }

  return false;
}

// ==================== 文件删除 ====================

function handleFileRemove(indicatorCode: string, file: UploadFile, indicator?: DeclareIndicatorApi.Indicator) {
  const currentList = fileListMap[indicatorCode] || [];
  fileListMap[indicatorCode] = currentList.filter((f) => f.uid !== file.uid);
  formValues[indicatorCode] = JSON.stringify(fileListMap[indicatorCode]);
  message.success('文件已删除');

  // 如果删除后文件列表为空，重新触发必填错误提示
  if (fileListMap[indicatorCode].length === 0 && indicator?.id !== undefined && indicator.isRequired) {
    setFieldError(toTopLevelKey(indicator.id), '此项为必填', 'required', true);
  }
}

// ==================== 导出 ====================

// fileListMap 已在声明时导出
export { handleFileUpload, handleFileRemove };
