<script lang="ts" setup>
import type { DeclareAchievementApi } from '#/api/declare/achievement';
import type { DeclareProjectApi } from '#/api/declare/project';

import { nextTick, ref, onMounted } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message, Upload, Button as AButton } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { Tinymce as RichTextarea } from '#/components/tinymce';
import {
  createAchievement,
  getAchievement,
  updateAchievement,
} from '#/api/declare/achievement';
import {
  ACHIEVEMENT_TYPE_OPTIONS,
  DATA_QUALITY_OPTIONS,
  FLOW_TYPE_OPTIONS,
  FORM_RULES,
  PROMOTION_SCOPE_OPTIONS,
  REPLICATION_VALUE_OPTIONS,
  SHARE_SCOPE_OPTIONS,
  TRANSFORM_TYPE_OPTIONS,
} from '../data';
import { getProjectSimpleList } from '#/api/declare/project';
import { uploadFile } from '#/api/infra/file';

const emit = defineEmits(['success']);

// 项目列表数据
const projectList = ref<DeclareProjectApi.Project[]>([]);

// 加载项目列表
async function loadProjectList() {
  try {
    const res = await getProjectSimpleList();
    projectList.value = res || [];
  } catch (error) {
    console.error('加载项目列表失败:', error);
    projectList.value = [];
  }
}

// 页面加载时获取项目列表
onMounted(() => {
  loadProjectList();
});

// 表单数据 - 使用 reactive 确保对象始终存在
const formData = ref<DeclareAchievementApi.Achievement>({
  projectId: undefined,
  achievementName: '',
  achievementType: undefined,
  applicationField: '',
  description: '',
  effectDescription: '',
  replicationValue: undefined,
  promotionScope: undefined,
  promotionCount: 0,
  transformType: undefined,
  attachmentIds: '',
  status: 'DRAFT',
  recommendStatus: 0,
});

// 富文本编辑器 ref，用于直接获取内容
const descriptionEditorRef = ref<InstanceType<typeof RichTextarea> | null>(null);
const effectDescriptionEditorRef = ref<InstanceType<typeof RichTextarea> | null>(null);
const flowPurposeEditorRef = ref<InstanceType<typeof RichTextarea> | null>(null);

// 附件上传相关
interface UploadFileItem {
  name: string;
  url: string;
  uid?: string;
  status?: string;
  fileId?: string; // 存储文件标识符
}

const fileList = ref<UploadFileItem[]>([]);

// 解析存储的附件列表（attachmentIds 存储的格式：完整的文件访问 URL，多个以逗号分隔）
function parseStoredFileList(value: string | undefined): UploadFileItem[] {
  if (!value) return [];
  try {
    const urls = value.split(',').map(s => s.trim()).filter(Boolean);
    return urls.map((url, index) => {
      // 尝试从 URL 中提取文件名
      let filename = `附件${index + 1}`;
      try {
        // 移除 URL 中的查询参数
        const urlWithoutQuery = url.split('?')[0];
        if (urlWithoutQuery.includes('/')) {
          const parts = urlWithoutQuery.split('/');
          const lastPart = parts[parts.length - 1];
          // 如果有文件名后缀，使用解码后的文件名
          if (lastPart && (lastPart.includes('.') || !/^\d+$/.test(lastPart))) {
            filename = decodeURIComponent(lastPart);
          }
        }
      } catch {
        // 解码失败，使用默认文件名
      }
      return {
        name: filename,
        url: url, // 使用完整 URL
        uid: `existing-${index}-${Date.now()}`, // 使用唯一的 uid
        status: 'done',
        fileId: url,
      };
    });
  } catch {
    return [];
  }
}

// 将文件列表转换为附件存储字符串（存储完整的访问 URL）
function convertToAttachmentIds(files: UploadFileItem[]): string {
  return files.map(f => f.url || f.fileId).filter(Boolean).join(',');
}

// 上传前处理 - 使用自定义方式，不依赖 Upload 组件的自动添加
async function handleBeforeUpload(file: File) {
  try {
    // 调用上传接口，返回文件访问 URL
    const fileUrl = await uploadFile({
      file,
      directory: 'declare/achievement',
    });

    // 生成唯一标识符
    const fileUid = `upload-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    // 创建文件项（手动添加到列表，不依赖 Upload 组件自动添加）
    const newFileItem: UploadFileItem = {
      name: file.name,
      url: fileUrl, // 使用返回的 URL
      uid: fileUid,
      fileId: fileUrl,
      status: 'done',
    };

    // 直接添加文件项
    fileList.value = [...fileList.value, newFileItem];

    // 同步更新 attachmentIds
    formData.value.attachmentIds = convertToAttachmentIds(fileList.value);

    return false; // 阻止默认上传行为
  } catch (error) {
    console.error('上传失败:', error);
    message.error('上传失败');
    return false;
  }
}

// 删除附件
function handleRemove(file: UploadFileItem) {
  fileList.value = fileList.value.filter(f => f.uid !== file.uid);
  formData.value.attachmentIds = convertToAttachmentIds(fileList.value);
}

// 表单校验规则
const formRules = FORM_RULES;

// 折叠面板默认展开（包含附件面板）
const activeKeys = ref(['basic', 'data', 'certificate', 'attachment']);

const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  footer: true,
  confirmText: '保存',
  showCancelButton: true,
  async onConfirm() {
    modalApi.lock();
    try {
      // 简单的必填字段验证
      if (!formData.value.projectId) {
        message.error('请选择关联项目');
        modalApi.unlock();
        return false;
      }
      if (!formData.value.achievementName) {
        message.error('请输入成果名称');
        modalApi.unlock();
        return false;
      }
      if (!formData.value.achievementType) {
        message.error('请选择成果类型');
        modalApi.unlock();
        return false;
      }

      // 直接从编辑器获取内容（通过 expose 的方法）
      let description = formData.value.description || '';
      let effectDescription = formData.value.effectDescription || '';
      let flowPurpose = formData.value.flowPurpose || '';

      // 尝试从编辑器 ref 获取最新内容
      if (descriptionEditorRef.value) {
        try {
          const editorContent = (descriptionEditorRef.value as any).getContent?.();
          if (editorContent !== undefined) {
            description = editorContent;
          }
        } catch (e) {
          // 忽略错误，使用 formData 的值
        }
      }
      if (effectDescriptionEditorRef.value) {
        try {
          const editorContent = (effectDescriptionEditorRef.value as any).getContent?.();
          if (editorContent !== undefined) {
            effectDescription = editorContent;
          }
        } catch (e) {
          // 忽略错误，使用 formData 的值
        }
      }
      if (flowPurposeEditorRef.value) {
        try {
          const editorContent = (flowPurposeEditorRef.value as any).getContent?.();
          if (editorContent !== undefined) {
            flowPurpose = editorContent;
          }
        } catch (e) {
          // 忽略错误，使用 formData 的值
        }
      }

      const data = {
        ...formData.value,
        description,
        effectDescription,
        flowPurpose,
      };
      if (data.id) {
        await updateAchievement(data);
        message.success('更新成功');
      } else {
        await createAchievement(data);
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
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      fileList.value = [];
      formData.value = {
        projectId: undefined,
        achievementName: '',
        achievementType: undefined,
        applicationField: '',
        description: '',
        effectDescription: '',
        replicationValue: undefined,
        promotionScope: undefined,
        promotionCount: 0,
        transformType: undefined,
        attachmentIds: '',
        status: 'DRAFT',
        recommendStatus: 0,
      };
      return;
    }

    const data = modalApi.getData<DeclareAchievementApi.Achievement>();
    if (data?.id) {
      // 编辑模式：加载数据
      try {
        const result = await getAchievement(data.id);
        console.log('[编辑模式] 后端返回的 result:', result);
        console.log('[编辑模式] attachmentIds:', result?.attachmentIds);

        // 先设置附件列表
        const parsedFiles = parseStoredFileList(result?.attachmentIds);
        console.log('[编辑模式] 解析后的文件列表:', parsedFiles);
        fileList.value = parsedFiles;
        console.log('[编辑模式] 设置后的 fileList:', fileList.value);

        formData.value = { ...result } || {
          projectId: undefined,
          achievementName: '',
          achievementType: undefined,
          applicationField: '',
          description: '',
          effectDescription: '',
          replicationValue: undefined,
          promotionScope: undefined,
          promotionCount: 0,
          transformType: undefined,
          attachmentIds: '',
          status: 'DRAFT',
          recommendStatus: 0,
        };
        // 等待 formData 更新后，手动设置富文本编辑器内容
        // 等待编辑器加载完成后再设置内容（TinyMCE init 事件异步，需适当延迟）
        setTimeout(async () => {
          await nextTick();
          if (descriptionEditorRef.value) {
            (descriptionEditorRef.value as any).setContent(result?.description || '');
          }
          if (effectDescriptionEditorRef.value) {
            (effectDescriptionEditorRef.value as any).setContent(result?.effectDescription || '');
          }
          if (flowPurposeEditorRef.value) {
            (flowPurposeEditorRef.value as any).setContent(result?.flowPurpose || '');
          }
        }, 500);
      } catch (error) {
        console.error('加载数据失败:', error);
        message.error('加载数据失败');
      }
    }
  },
});

defineExpose({ setData: modalApi.setData });
</script>

<template>
  <Modal
    :title="formData.id ? '编辑成果与流通' : '新增成果与流通'"
    class="achievement-modal w-[80%]"
    :centered="true"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      class="achievement-form"
    >
      <!-- 基本信息 -->
      <a-collapse v-model:activeKey="activeKeys" :bordered="false" class="form-collapse">
        <a-collapse-panel key="basic" :force-render="true">
          <template #header>
            <div class="collapse-header">
              <span class="header-badge basic-badge">1</span>
              <span class="header-title">基本信息</span>
              <span class="header-desc">成果的基本信息</span>
            </div>
          </template>

          <a-form-item label="关联项目" name="projectId" class="form-item-highlight">
            <a-select
              v-model:value="formData.projectId"
              placeholder="请选择关联项目"
              :options="projectList.map(p => ({ label: p.projectName, value: p.id }))"
              allow-clear
            />
          </a-form-item>

          <a-form-item label="成果名称" name="achievementName" class="form-item-highlight">
            <a-input v-model:value="formData.achievementName" placeholder="请输入成果名称" allow-clear />
          </a-form-item>

          <a-form-item label="成果类型" name="achievementType" class="form-item-highlight">
            <a-select v-model:value="formData.achievementType" placeholder="请选择成果类型" allow-clear>
              <a-select-option v-for="item in ACHIEVEMENT_TYPE_OPTIONS" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="应用领域" name="applicationField">
            <a-input v-model:value="formData.applicationField" placeholder="请输入应用领域" allow-clear />
          </a-form-item>

          <a-form-item label="成果描述" name="description">
            <RichTextarea ref="descriptionEditorRef" v-model:value="formData.description" height="200px" />
          </a-form-item>

          <a-form-item label="应用效果描述" name="effectDescription">
            <RichTextarea ref="effectDescriptionEditorRef" v-model:value="formData.effectDescription" height="200px" />
          </a-form-item>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="可复制性评估" name="replicationValue">
                <a-select v-model:value="formData.replicationValue" placeholder="请选择" allow-clear>
                  <a-select-option v-for="item in REPLICATION_VALUE_OPTIONS" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="推广范围" name="promotionScope">
                <a-select v-model:value="formData.promotionScope" placeholder="请选择" allow-clear>
                  <a-select-option v-for="item in PROMOTION_SCOPE_OPTIONS" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="推广次数" name="promotionCount">
                <a-input-number v-model:value="formData.promotionCount" :min="0" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="转化类型" name="transformType">
                <a-select v-model:value="formData.transformType" placeholder="请选择" allow-clear>
                  <a-select-option v-for="item in TRANSFORM_TYPE_OPTIONS" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </a-collapse-panel>

        <!-- 数据流通信息 -->
        <a-collapse-panel key="data" :force-render="true">
          <template #header>
            <div class="collapse-header">
              <span class="header-badge data-badge">2</span>
              <span class="header-title">数据流通信息</span>
              <span class="header-desc">数据的流通与共享情况</span>
            </div>
          </template>

          <a-form-item label="数据名称" name="dataName">
            <a-input v-model:value="formData.dataName" placeholder="请输入数据名称" allow-clear />
          </a-form-item>

          <a-form-item label="数据类型" name="dataType">
            <a-input v-model:value="formData.dataType" placeholder="请输入数据类型" allow-clear />
          </a-form-item>

          <a-form-item label="数据来源" name="dataSource">
            <a-input v-model:value="formData.dataSource" placeholder="请输入数据来源" allow-clear />
          </a-form-item>

          <a-form-item label="数据量规模" name="dataVolume">
            <a-input v-model:value="formData.dataVolume" placeholder="如：100万条、50GB" allow-clear />
          </a-form-item>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="数据质量评级" name="dataQuality">
                <a-select v-model:value="formData.dataQuality" placeholder="请选择" allow-clear>
                  <a-select-option v-for="item in DATA_QUALITY_OPTIONS" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="共享范围" name="shareScope">
                <a-select v-model:value="formData.shareScope" placeholder="请选择" allow-clear>
                  <a-select-option v-for="item in SHARE_SCOPE_OPTIONS" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="流通类型" name="flowType">
                <a-select v-model:value="formData.flowType" placeholder="请选择" allow-clear>
                  <a-select-option v-for="item in FLOW_TYPE_OPTIONS" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="流通对象" name="flowObject">
                <a-input v-model:value="formData.flowObject" placeholder="请输入流通对象" allow-clear />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="流通目的" name="flowPurpose">
            <RichTextarea ref="flowPurposeEditorRef" v-model:value="formData.flowPurpose" height="150px" />
          </a-form-item>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="流通开始时间" name="startTime">
                <a-date-picker v-model:value="formData.startTime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="流通结束时间" name="endTime">
                <a-date-picker v-model:value="formData.endTime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" allow-clear />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="安全备案编号" name="securityFilingNo">
                <a-input v-model:value="formData.securityFilingNo" placeholder="请输入安全备案编号" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="备案时间" name="securityFilingTime">
                <a-date-picker v-model:value="formData.securityFilingTime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" allow-clear />
              </a-form-item>
            </a-col>
          </a-row>
        </a-collapse-panel>

        <!-- 证书与交易信息 -->
        <a-collapse-panel key="certificate" :force-render="true">
          <template #header>
            <div class="collapse-header">
              <span class="header-badge cert-badge">3</span>
              <span class="header-title">证书与交易信息</span>
              <span class="header-desc">相关证书和交易数据</span>
            </div>
          </template>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="数据产品证书数（601）" name="certificateCount">
                <a-input-number v-model:value="formData.certificateCount" :min="0" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="数据产权登记证数（602）" name="propertyCount">
                <a-input-number v-model:value="formData.propertyCount" :min="0" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="完成交易数（603）" name="transactionCount">
                <a-input-number v-model:value="formData.transactionCount" :min="0" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="累计交易金额（万元，604）" name="transactionAmount">
                <a-input-number v-model:value="formData.transactionAmount" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="交易对象" name="transactionObject">
            <a-input v-model:value="formData.transactionObject" placeholder="请输入交易对象" allow-clear />
          </a-form-item>

          <a-form-item label="交易合同" name="transactionContract">
            <a-input v-model:value="formData.transactionContract" placeholder="请输入交易合同路径" allow-clear />
          </a-form-item>
        </a-collapse-panel>

        <!-- 附件上传 -->
        <a-collapse-panel key="attachment" :force-render="true">
          <template #header>
            <div class="collapse-header">
              <span class="header-badge upload-badge">4</span>
              <span class="header-title">附件上传</span>
              <span class="header-desc">上传相关证明材料</span>
            </div>
          </template>

          <a-form-item label="附件" name="attachmentIds" class="attachment-item">
            <Upload
              v-model:file-list="fileList"
              :before-upload="handleBeforeUpload"
              @remove="handleRemove"
              :max-count="10"
              multiple
              action="#"
              class="attachment-upload"
              :show-upload-list="true"
            >
              <a-button type="dashed" class="upload-button">
                <IconifyIcon icon="lucide:cloud-upload" />
                上传附件
              </a-button>
            </Upload>
          </a-form-item>
        </a-collapse-panel>
      </a-collapse>
    </a-form>
  </Modal>
</template>

<style scoped>
.achievement-modal :deep(.ant-modal-body) {
  padding: 16px 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.achievement-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.achievement-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: #4a4a4a;
}

.form-collapse {
  background: transparent;
}

.form-collapse :deep(.ant-collapse-item) {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  transition: all 0.3s ease;
}

.form-collapse :deep(.ant-collapse-item:hover) {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}

.form-collapse :deep(.ant-collapse-header) {
  padding: 14px 16px !important;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
}

.form-collapse :deep(.ant-collapse-content-box) {
  padding: 16px 20px !important;
  background: #fff;
}

.collapse-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
}

.header-badge.basic-badge {
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
}

.header-badge.data-badge {
  background: linear-gradient(135deg, #52c41a 0%, #95de64 100%);
}

.header-badge.cert-badge {
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
}

.header-badge.upload-badge {
  background: linear-gradient(135deg, #722ed1 0%, #b37feb 100%);
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
}

.header-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-left: auto;
}

.form-item-highlight :deep(.ant-form-item-label > label::before) {
  content: '•';
  color: #1890ff;
  margin-right: 4px;
  font-weight: bold;
}

.attachment-item :deep(.ant-form-item-label) {
  padding-bottom: 0;
}

.attachment-upload :deep(.ant-upload-list) {
  margin-top: 12px;
}

.upload-button {
  width: 100%;
  height: 80px;
  border-style: dashed;
  border-color: #1890ff;
  color: #1890ff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.upload-button:hover {
  border-color: #40a9ff;
  background: rgba(24, 144, 255, 0.05);
  color: #40a9ff;
}

:deep(.ant-select),
:deep(.ant-input),
:deep(.ant-input-number),
:deep(.ant-picker) {
  border-radius: 6px;
}

:deep(.ant-select:not(.ant-select-disabled):hover .ant-select-selector),
:deep(.ant-input:hover),
:deep(.ant-input-number:hover),
:deep(.ant-picker:hover) {
  border-color: #1890ff;
}

:deep(.ant-select-focused .ant-select-selector),
:deep(.ant-input:focus),
:deep(.ant-input-number-focused),
:deep(.ant-picker-focused) {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

/* 滚动条样式 */
.achievement-modal :deep(.ant-modal-body)::-webkit-scrollbar {
  width: 6px;
}

.achievement-modal :deep(.ant-modal-body)::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.achievement-modal :deep(.ant-modal-body)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.achievement-modal :deep(.ant-modal-body)::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
