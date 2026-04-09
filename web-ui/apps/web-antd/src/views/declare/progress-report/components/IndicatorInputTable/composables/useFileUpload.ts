/**
 * 文件上传 composable
 */

import { reactive } from 'vue';
import { message } from 'ant-design-vue';
import { uploadFile } from '#/api/infra/file';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';

export interface UseFileUploadOptions {
  formValues: Record<string, any>;
  fileListMap: Record<string, UploadFile[]>;
}

export function useFileUpload() {
  /** 获取文件上传最大数量 */
  function getMaxFileCount(indicator: DeclareIndicatorApi.Indicator): number {
    const extraConfig = indicator.extraConfig ? JSON.parse(indicator.extraConfig) : {};
    if (extraConfig.maxCount !== undefined) {
      return Number(extraConfig.maxCount);
    }
    return indicator.maxValue ? Number(indicator.maxValue) : 5;
  }

  /** 获取指标允许的上传文件类型 */
  function getAcceptTypes(indicator: DeclareIndicatorApi.Indicator): string {
    const extraConfig = indicator.extraConfig ? JSON.parse(indicator.extraConfig) : {};
    return extraConfig.accept || '';
  }

  /** 解析已存储的文件列表 */
  function parseStoredFileList(value: string | undefined): UploadFile[] {
    if (!value) return [];
    try {
      const parsed = JSON.parse(value);
      if (Array.isArray(parsed)) {
        return parsed.map((item: any, index: number) => ({
          uid: String(index),
          name: item.name || item.url?.split('/').pop() || '文件',
          url: item.url,
          status: 'done' as const,
        }));
      }
    } catch {
      if (value.includes(',')) {
        return value.split(',').map((url, index) => ({
          uid: String(index),
          name: url.split('/').pop() || url,
          url: url.trim(),
          status: 'done' as const,
        }));
      }
      if (value.startsWith('http') || value.startsWith('/')) {
        return [
          {
            uid: '0',
            name: value.split('/').pop() || value,
            url: value,
            status: 'done' as const,
          },
        ];
      }
    }
    return [];
  }

  /** 文件上传处理 */
  async function handleFileUpload(
    file: File,
    indicator: DeclareIndicatorApi.Indicator,
    formValues: Record<string, any>,
    fileListMap: Record<string, UploadFile[]>,
  ): Promise<boolean> {
    const indicatorCode = indicator.indicatorCode;
    const maxCount = getMaxFileCount(indicator);
    const acceptTypes = getAcceptTypes(indicator);

    // 前端格式校验
    if (acceptTypes) {
      const fileExt = file.name.split('.').pop()?.toLowerCase() || '';
      const allowedExts = acceptTypes.split(',').map((ext: string) => ext.trim().toLowerCase());
      if (!allowedExts.includes(fileExt)) {
        message.warning(`仅支持上传以下格式：${acceptTypes}`);
        return false;
      }
    }

    try {
      const result = await uploadFile({
        file: file as any,
        directory: 'declare/indicator',
      });

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
      return true;
    } catch (error) {
      console.error('文件上传错误:', error);
      message.error('文件上传失败');
      return false;
    }
  }

  /** 删除文件 */
  function handleFileRemove(
    indicatorCode: string,
    file: UploadFile,
    formValues: Record<string, any>,
    fileListMap: Record<string, UploadFile[]>,
  ) {
    const currentList = fileListMap[indicatorCode] || [];
    fileListMap[indicatorCode] = currentList.filter((f) => f.uid !== file.uid);
    formValues[indicatorCode] = JSON.stringify(fileListMap[indicatorCode]);
    message.success('文件已删除');
  }

  return {
    getMaxFileCount,
    getAcceptTypes,
    parseStoredFileList,
    handleFileUpload,
    handleFileRemove,
  };
}
