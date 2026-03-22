<script lang="ts" setup>
import type { Training } from '#/api/declare/training';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message, Upload, Button as AButton } from 'ant-design-vue';
import { Tinymce as RichTextarea } from '#/components/tinymce';
import dayjs from 'dayjs';

import { create, get, update } from '#/api/declare/training';
import { uploadFile } from '#/api/infra/file';
import { TRAINING_TYPE_OPTIONS, TARGET_SCOPE_OPTIONS } from '../data';

const emit = defineEmits<{
  success: [];
}>();

const dataId = ref<number | null>(null);
const isEdit = computed(() => !!dataId.value);

// 富文本编辑器 ref，用于直接获取内容
const contentEditorRef = ref<InstanceType<typeof RichTextarea> | null>(null);
const meetingMaterialsEditorRef = ref<InstanceType<typeof RichTextarea> | null>(null);

// 附件相关
interface UploadFileItem {
  name: string;
  url?: string;
  uid: string;
  status?: string;
}

const fileList = ref<UploadFileItem[]>([]);

async function handleBeforeUpload(file: any) {
  try {
    const url = await uploadFile({ file, directory: 'declare/training' });
    // 找到组件自动添加的文件（通过 uid 匹配），更新其 URL
    const existingFile = fileList.value.find((f) => f.uid === file.uid);
    if (existingFile) {
      existingFile.url = url;
      existingFile.status = 'done';
    }
    return false; // 阻止默认上传行为
  } catch {
    message.error('上传失败');
    return false;
  }
}

function handleRemove(file: UploadFileItem) {
  fileList.value = fileList.value.filter((f) => f.uid !== file.uid);
}

// 海报相关
const posterList = ref<UploadFileItem[]>([]);

async function handleBeforePosterUpload(file: any) {
  try {
    const url = await uploadFile({ file, directory: 'declare/training/poster' });
    // 找到组件自动添加的文件，更新其 URL
    const existingFile = posterList.value.find((f) => f.uid === file.uid);
    if (existingFile) {
      existingFile.url = url;
      existingFile.status = 'done';
    }
    return false; // 阻止默认上传行为
  } catch {
    message.error('海报上传失败');
    return false;
  }
}

function handlePosterRemove(file: UploadFileItem) {
  posterList.value = posterList.value.filter((f) => f.uid !== file.uid);
}

// 表单数据
const formData = ref({
  name: '',
  type: undefined as number | undefined,
  organizer: '',
  speaker: '',
  startTime: null as dayjs.Dayjs | null | string,
  endTime: null as dayjs.Dayjs | null | string,
  location: '',
  onlineLink: '',
  targetScope: undefined as number | undefined,
  targetProvinces: '',
  registrationDeadline: null as dayjs.Dayjs | null | string,
  maxParticipants: undefined as number | undefined,
  content: '',
  meetingMaterials: '',
  remark: '',
});

/**
 * 格式化日期值
 * 可能是 dayjs 对象或字符串（由于 value-format）
 */
function formatDateValue(value: dayjs.Dayjs | null | string | undefined): string | null {
  if (!value) return null;
  if (typeof value === 'string') return value;
  if (value instanceof dayjs) return value.format('YYYY-MM-DD HH:mm:ss');
  return null;
}

const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  showCancelButton: true,
  confirmText: '保存',
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData<Training>();
      if (data?.id) {
        dataId.value = data.id;
        const detail = await get(data.id);
        formData.value = {
          name: detail.name || '',
          type: detail.type,
          organizer: detail.organizer || '',
          speaker: detail.speaker || '',
          startTime: detail.startTime ? dayjs(detail.startTime) : null,
          endTime: detail.endTime ? dayjs(detail.endTime) : null,
          location: detail.location || '',
          onlineLink: detail.onlineLink || '',
          targetScope: detail.targetScope,
          targetProvinces: detail.targetProvinces || '',
          registrationDeadline: detail.registrationDeadline ? dayjs(detail.registrationDeadline) : null,
          maxParticipants: detail.maxParticipants,
          content: detail.content || '',
          meetingMaterials: detail.meetingMaterials || '',
          remark: detail.remark || '',
        };
        // 等待 formData 更新后，手动设置富文本编辑器内容
        await nextTick();
        setTimeout(() => {
          contentEditorRef.value?.setContent(detail.content || '');
          meetingMaterialsEditorRef.value?.setContent(detail.meetingMaterials || '');
        }, 100);
        // 回填附件列表
        if (detail.attachmentUrls) {
          const urls = detail.attachmentUrls.split(',').filter(Boolean);
          fileList.value = urls.map((url: string, index: number) => ({
            name: `附件${index + 1}`,
            url,
            uid: url,
            status: 'done',
          }));
        } else {
          fileList.value = [];
        }
        if (detail.posterUrls) {
          const urls = detail.posterUrls.split(',').filter(Boolean);
          posterList.value = urls.map((url: string, index: number) => ({
            name: `海报${index + 1}`,
            url,
            uid: url,
            status: 'done',
          }));
        } else {
          posterList.value = [];
        }
      } else {
        dataId.value = null;
        fileList.value = [];
        posterList.value = [];
        formData.value = {
          name: '',
          type: undefined,
          organizer: '',
          speaker: '',
          startTime: null,
          endTime: null,
          location: '',
          onlineLink: '',
          targetScope: undefined,
          targetProvinces: '',
          registrationDeadline: null,
          maxParticipants: undefined,
          content: '',
          meetingMaterials: '',
          remark: '',
        };
      }
    }
  },
  async onConfirm() {
    modalApi.lock();
    try {
      // 简单的必填字段验证
      if (!formData.value.name) {
        message.error('请输入活动名称');
        modalApi.unlock();
        return false;
      }
      if (!formData.value.type) {
        message.error('请选择活动类型');
        modalApi.unlock();
        return false;
      }
      if (!formData.value.startTime) {
        message.error('请选择开始时间');
        modalApi.unlock();
        return false;
      }
      if (!formData.value.endTime) {
        message.error('请选择结束时间');
        modalApi.unlock();
        return false;
      }
      if (!formData.value.targetScope) {
        message.error('请选择目标范围');
        modalApi.unlock();
        return false;
      }

      // 直接从编辑器获取内容（通过 expose 的方法）
      let content = formData.value.content;
      let meetingMaterials = formData.value.meetingMaterials;
      
      // 尝试从编辑器 ref 获取最新内容
      if (contentEditorRef.value) {
        try {
          // 通过 expose 获取编辑器内容
          const editorContent = (contentEditorRef.value as any).getContent?.();
          if (editorContent !== undefined) {
            content = editorContent;
          }
        } catch (e) {
          // 忽略错误，使用 formData 的值
        }
      }
      if (meetingMaterialsEditorRef.value) {
        try {
          const editorContent = (meetingMaterialsEditorRef.value as any).getContent?.();
          if (editorContent !== undefined) {
            meetingMaterials = editorContent;
          }
        } catch (e) {
          // 忽略错误，使用 formData 的值
        }
      }

      const data = {
        name: formData.value.name,
        type: formData.value.type,
        organizer: formData.value.organizer,
        speaker: formData.value.speaker,
        startTime: formatDateValue(formData.value.startTime),
        endTime: formatDateValue(formData.value.endTime),
        location: formData.value.location,
        onlineLink: formData.value.onlineLink,
        targetScope: formData.value.targetScope,
        targetProvinces: formData.value.targetProvinces,
        registrationDeadline: formatDateValue(formData.value.registrationDeadline),
        maxParticipants: formData.value.maxParticipants,
        content,
        meetingMaterials,
        remark: formData.value.remark,
        attachmentUrls: fileList.value.map((f) => f.url).filter(Boolean).join(','),
        posterUrls: posterList.value.map((f) => f.url).filter(Boolean).join(','),
      };
      if (dataId.value) {
        await update({ ...data, id: dataId.value });
        message.success('更新成功');
      } else {
        await create(data);
        message.success('创建成功');
      }
      await modalApi.close();
      emit('success');
    } catch (error) {
      console.error('保存失败:', error);
      message.error('保存失败');
    } finally {
      modalApi.unlock();
    }
    return false;
  },
});

defineExpose({ setData: modalApi.setData });
</script>

<template>
  <Modal
    :title="isEdit ? '编辑活动' : '新增活动'"
    class="training-modal w-[80%]"
    :centered="true"
  >
    <a-form
      :model="formData"
      :label-col="{ span: 4 }"
      :wrapper-col="{ span: 18 }"
      class="training-form"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="活动名称" name="name" required>
            <a-input v-model:value="formData.name" placeholder="请输入活动名称" :maxlength="200" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="活动类型" name="type" required>
            <a-select v-model:value="formData.type" :options="TRAINING_TYPE_OPTIONS" placeholder="请选择活动类型" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="组织单位" name="organizer">
            <a-input v-model:value="formData.organizer" placeholder="请输入组织单位" :maxlength="200" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="主讲人/嘉宾" name="speaker">
            <a-input v-model:value="formData.speaker" placeholder="请输入主讲人或嘉宾" :maxlength="200" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="开始时间" name="startTime" required>
            <a-date-picker
              v-model:value="formData.startTime"
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
              placeholder="请选择开始时间"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="结束时间" name="endTime" required>
            <a-date-picker
              v-model:value="formData.endTime"
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
              placeholder="请选择结束时间"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="活动地点" name="location">
            <a-input v-model:value="formData.location" placeholder="请输入活动地点" :maxlength="200" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="线上链接" name="onlineLink">
            <a-input v-model:value="formData.onlineLink" placeholder="请输入线上参与链接" :maxlength="500" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="目标范围" name="targetScope" required>
            <a-select v-model:value="formData.targetScope" :options="TARGET_SCOPE_OPTIONS" placeholder="请选择目标范围" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="最大人数" name="maxParticipants">
            <a-input-number v-model:value="formData.maxParticipants" :min="1" placeholder="留空表示不限人数" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="目标省份" name="targetProvinces" v-if="formData.targetScope === 2">
            <a-input v-model:value="formData.targetProvinces" placeholder="请输入目标省份（多个用逗号分隔）" :maxlength="500" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="报名截止时间" name="registrationDeadline">
            <a-date-picker
              v-model:value="formData.registrationDeadline"
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
              placeholder="请选择报名截止时间"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="活动详情" name="content">
        <RichTextarea ref="contentEditorRef" v-model="formData.content" height="200px" />
      </a-form-item>

      <a-form-item label="会议资料" name="meetingMaterials">
        <RichTextarea ref="meetingMaterialsEditorRef" v-model="formData.meetingMaterials" height="200px" />
      </a-form-item>

      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注信息" :rows="3" />
      </a-form-item>

      <a-form-item label="附件上传" name="attachmentUrls">
        <Upload
          v-model:file-list="fileList"
          :before-upload="handleBeforeUpload"
          multiple
          @remove="handleRemove"
        >
          <AButton type="dashed">上传附件</AButton>
        </Upload>
      </a-form-item>

      <a-form-item label="活动海报" name="posterUrl">
        <Upload
          v-model:file-list="posterList"
          :before-upload="handleBeforePosterUpload"
          multiple
          list-type="picture"
          @remove="handlePosterRemove"
        >
          <AButton type="dashed">上传海报</AButton>
        </Upload>
      </a-form-item>
    </a-form>
  </Modal>
</template>

<style scoped>
.training-modal :deep(.ant-modal-body) {
  padding: 16px 24px;
  max-height: 70vh;
  overflow-y: auto;
  max-width: 1200px;
  margin: 0 auto;
}

.training-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.training-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: #4a4a4a;
}
</style>
