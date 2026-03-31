<script lang="ts" setup>
import type { DeclareIndicatorGroupApi } from '#/api/declare/indicator-group';
import { ref, computed, watch } from 'vue';
import { PlusOutlined } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import {
  createIndicatorGroup,
  updateIndicatorGroup,
  getIndicatorGroup,
  getLevelOneIndicatorGroupList,
} from '#/api/declare/indicator-group';
import { getProjectTypeSimpleList } from '#/api/declare/project-type';

// 项目类型选项（从 API 加载）
const projectTypeOptions = ref<{ label: string; value: number }[]>([]);

// 加载项目类型选项
const loadProjectTypeOptions = async () => {
  try {
    const list = await getProjectTypeSimpleList();
    projectTypeOptions.value = list.map((item) => ({
      label: item.title || item.name,
      value: item.typeValue,
    }));
  } catch {
    projectTypeOptions.value = [];
  }
};

const LEVEL_OPTIONS = [
  { label: '一级分组', value: 1 },
  { label: '二级分组', value: 2 },
];

const STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
];

// 父分组选项（用于下拉）
const parentGroupOptions = ref<{ label: string; value: number }[]>([]);

const emit = defineEmits(['success']);

const formData = ref<DeclareIndicatorGroupApi.IndicatorGroupSaveParams>();
const formRef = ref<any>();

// 表单字段配置
const formRules = {
  groupCode: [{ required: true, message: '请输入分组编码' }],
  groupName: [{ required: true, message: '请输入分组名称' }],
  groupLevel: [{ required: true, message: '请选择分组层级' }],
};

// 表单布局
const formLayout = {
  labelCol: { span: 6 },
  wrapperCol: { span: 16 },
};

// 根据项目类型获取名称
const getProjectTypeName = (value: number) => {
  const option = projectTypeOptions.find((item) => item.value === value);
  return option ? option.label : '';
};

// 根据层级获取名称
const getGroupLevelName = (value: number) => {
  const option = LEVEL_OPTIONS.find((item) => item.value === value);
  return option ? option.label : '';
};

// 获取父分组列表
const loadParentGroups = async () => {
  try {
    const list = await getLevelOneIndicatorGroupList();
    parentGroupOptions.value = list.map((item) => ({
      label: `${item.groupName}（${getProjectTypeName(item.projectType)}）`,
      value: item.id as number,
    }));
    // 添加"顶级"选项
    parentGroupOptions.value.unshift({ label: '顶级分组', value: 0 });
  } catch {
    parentGroupOptions.value = [{ label: '顶级分组', value: 0 }];
  }
};

const getTitle = computed(() => {
  return formData.value?.id
    ? '编辑分组'
    : '新建分组';
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    try {
      await formRef.value?.validate();
    } catch (error) {
      message.error('请完善表单信息');
      return;
    }
    modalApi.lock();
    // 提交表单
    const data = formData.value as DeclareIndicatorGroupApi.IndicatorGroupSaveParams;
    try {
      if (data.id) {
        await updateIndicatorGroup(data);
      } else {
        // 创建时处理父分组ID
        if (data.parentId === 0 || data.parentId === undefined) {
          data.parentId = 0;
        }
        await createIndicatorGroup(data);
      }
      await modalApi.close();
      emit('success');
      message.success('操作成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }
    // 加载项目类型选项
    await loadProjectTypeOptions();
    // 加载父分组列表
    await loadParentGroups();

    const data = modalApi.getData<DeclareIndicatorGroupApi.IndicatorGroupSaveParams>();
    if (!data || !data.id) {
      // 新建
      formData.value = {
        groupCode: '',
        groupName: '',
        groupLevel: 1,
        projectType: undefined,
        groupPrefix: '',
        description: '',
        sort: 0,
        status: 1,
        parentId: 0,
      };
      return;
    }

    // 编辑
    modalApi.lock();
    try {
      const detail = await getIndicatorGroup(data.id);
      formData.value = {
        id: detail.id,
        groupCode: detail.groupCode,
        groupName: detail.groupName,
        groupLevel: detail.groupLevel,
        projectType: detail.projectType,
        groupPrefix: detail.groupPrefix || '',
        description: detail.description || '',
        sort: detail.sort || 0,
        status: detail.status || 1,
        parentId: detail.parentId || 0,
      };
    } finally {
      modalApi.unlock();
    }
  },
});

const handleProjectTypeChange = (value: number) => {
  if (formData.value) {
    formData.value.projectType = value;
  }
};

const handleGroupLevelChange = (value: number) => {
  if (formData.value) {
    formData.value.groupLevel = value;
    // 切换到一级分组时，清空父分组
    if (value === 1) {
      formData.value.parentId = 0;
    }
  }
};
</script>

<template>
  <Modal :title="getTitle" class="w-2/3">
    <a-form
      v-if="formData"
      ref="formRef"
      :model="formData"
      :rules="formRules"
      v-bind="formLayout"
      class="mx-4"
    >
      <a-form-item label="分组编码" name="groupCode">
        <a-input
          v-model:value="formData.groupCode"
          placeholder="请输入分组编码，如 GROUP_101"
        />
      </a-form-item>
      <a-form-item label="分组名称" name="groupName">
        <a-input
          v-model:value="formData.groupName"
          placeholder="请输入分组名称，如 101基本情况"
        />
      </a-form-item>
      <a-form-item label="分组层级" name="groupLevel">
        <a-radio-group
          v-model:value="formData.groupLevel"
          @change="(e: any) => handleGroupLevelChange(e.target.value)"
        >
          <a-radio :value="1">一级分组</a-radio>
          <a-radio :value="2">二级分组</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item v-if="formData.groupLevel === 2" label="父分组" name="parentId">
        <a-select
          v-model:value="formData.parentId"
          placeholder="请选择父分组"
        >
          <a-select-option
            v-for="option in parentGroupOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="关联项目类型" name="projectType">
        <a-select
          v-model:value="formData.projectType"
          placeholder="一级分组必须选择项目类型"
          allowClear
          @change="handleProjectTypeChange"
        >
          <a-select-option
            v-for="option in projectTypeOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="分组前缀" name="groupPrefix">
        <a-input
          v-model:value="formData.groupPrefix"
          placeholder="如：101、10101，用于描述分组特征"
        />
      </a-form-item>
      <a-form-item label="分组描述" name="description">
        <a-textarea
          v-model:value="formData.description"
          placeholder="请输入分组描述"
          :rows="3"
        />
      </a-form-item>
      <a-form-item label="排序" name="sort">
        <a-input-number
          v-model:value="formData.sort"
          :min="0"
          :max="9999"
          class="w-full"
        />
      </a-form-item>
      <a-form-item label="状态" name="status">
        <a-radio-group v-model:value="formData.status">
          <a-radio :value="1">启用</a-radio>
          <a-radio :value="0">禁用</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
  </Modal>
</template>
