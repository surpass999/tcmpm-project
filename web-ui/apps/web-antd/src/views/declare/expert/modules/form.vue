<script lang="ts" setup>
import type { DeclareExpertApi } from '#/api/declare/expert';
import type { SystemUserApi } from '#/api/system/user';

import { computed, onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { handleTree } from '@vben/utils';

import { getDictOptions } from '@vben/hooks';

import { message } from 'ant-design-vue';

import { getSimpleDeptList } from '#/api/system/dept';
import { getSimpleUserList } from '#/api/system/user';

import { createExpert, getExpert, updateExpert } from '#/api/declare/expert';

import { $t } from '#/locales';
import { DICT_TYPE } from '@vben/constants';

const emit = defineEmits(['success']);

const formData = ref<Partial<DeclareExpertApi.Expert>>();
const formRef = ref<any>();

// 表单验证规则
const formRules = {
  expertName: [
    { required: true, message: '请输入专家姓名', trigger: 'blur' },
    { required: true, message: '请输入专家姓名', trigger: 'change' },
  ],
  expertType: [
    { required: true, message: '请选择专家类型', trigger: 'change' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
  email: [
    { type: 'email' as any, message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
  idCard: [
    { pattern: /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/, message: '请输入正确的身份证号码', trigger: 'blur' },
  ],
};

// 用户选项（带搜索）
const userOptions = ref<SystemUserApi.User[]>([]);
// 部门树选项
const deptTreeOptions = ref<any[]>([]);

// 加载用户和部门列表
onMounted(async () => {
  const [users, depts] = await Promise.all([
    getSimpleUserList(),
    getSimpleDeptList(),
  ]);
  userOptions.value = users;

  // 过滤掉无效部门并转换为树形结构
  const validDepts = (depts || []).filter((d: any) => d && d.name);

  // 递归处理树节点
  const processNode = (node: any): any => {
    if (!node || !node.name) return null;
    return {
      ...node,
      title: node.name,
      value: node.name,
      key: node.name,
      children: node.children?.filter(Boolean).map(processNode).filter(Boolean) || [],
    };
  };

  const deptTree = handleTree(validDepts);
  deptTreeOptions.value = deptTree.map(processNode).filter(Boolean);
});

// 专家类型选项
const expertTypeOptions = computed(() => {
  const options = getDictOptions(DICT_TYPE.DECLARE_EXPERT_TYPE, 'number');
  return options.length > 0 ? options : [
    { label: '技术专家', value: 1 },
    { label: '财务专家', value: 2 },
    { label: '管理专家', value: 3 },
    { label: '行业专家', value: 4 },
  ];
});

// 性别选项
const genderOptions = [
  { label: '男', value: 1 },
  { label: '女', value: 2 },
];

// 状态选项
const statusOptions = computed(() => {
  const options = getDictOptions(DICT_TYPE.DECLARE_EXPERT_STATUS, 'number');
  return options.length > 0 ? options : [
    { label: '在册', value: 1 },
    { label: '暂停', value: 2 },
    { label: '注销', value: 3 },
  ];
});

// 弹窗标题
const getTitle = computed(() => (formData.value?.id ? '编辑专家' : '新增专家'));

// 使用 Vben Modal
const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    try {
      await formRef.value?.validate();
    } catch {
      message.error('请完善表单信息');
      return;
    }
    modalApi.lock();
    const data = formData.value as DeclareExpertApi.Expert;
    try {
      if (data.id) {
        await updateExpert(data);
        message.success('更新成功');
      } else {
        await createExpert(data);
        message.success('创建成功');
      }
      await modalApi.close();
      emit('success');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }
    // 打开时清除之前的验证状态
    formRef.value?.resetFields();
    const data = modalApi.getData<DeclareExpertApi.Expert>();
    if (!data || !data.id) {
      formData.value = {
        status: 1,
        reviewCount: 0,
      };
    } else {
      modalApi.lock();
      try {
        const result = await getExpert(data.id);
        formData.value = result || {};
      } finally {
        modalApi.unlock();
      }
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[700px]">
    <a-form
      v-if="formData"
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <!-- 基本信息 -->
      <a-divider orientation="left">基本信息</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="专家姓名" name="expertName" required>
            <a-input v-model:value="formData.expertName" placeholder="请输入专家姓名" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="关联用户" name="userId">
            <a-select
              v-model:value="formData.userId"
              placeholder="请选择关联用户"
              show-search
              :filter-option="(input: string, option: any) => option.label?.toLowerCase().includes(input.toLowerCase())"
              allow-clear
              style="width: 100%"
            >
              <a-select-option v-for="item in userOptions" :key="item.id" :value="item.id" :label="item.nickname">
                {{ item.nickname }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="身份证号" name="idCard">
            <a-input v-model:value="formData.idCard" placeholder="请输入身份证号" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="性别" name="gender">
            <a-select v-model:value="formData.gender" placeholder="请选择性别">
              <a-select-option v-for="item in genderOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="出生日期" name="birthDate">
            <a-date-picker
              v-model:value="formData.birthDate"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              placeholder="请选择出生日期"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系电话" name="phone">
            <a-input v-model:value="formData.phone" placeholder="请输入联系电话" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="电子邮箱" name="email">
            <a-input v-model:value="formData.email" placeholder="请输入电子邮箱" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 职业信息 -->
      <a-divider orientation="left">职业信息</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="工作单位" name="workUnit">
            <a-input v-model:value="formData.workUnit" placeholder="请输入工作单位" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="职务" name="jobTitle">
            <a-input v-model:value="formData.jobTitle" placeholder="请输入职务" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="职称" name="professionalTitle">
            <a-input v-model:value="formData.professionalTitle" placeholder="请输入职称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="所在部门" name="department">
            <a-tree-select
              v-model:value="formData.department"
              :tree-data="deptTreeOptions"
              :field-names="{
                label: 'title',
                value: 'value',
                children: 'children',
              }"
              placeholder="请选择所在部门"
              show-search
              allow-clear
              tree-node-filter-prop="title"
              tree-default-expand-all
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 专家资质 -->
      <a-divider orientation="left">专家资质</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="专家类型" name="expertType" required>
            <a-select v-model:value="formData.expertType" placeholder="请选择专家类型">
              <a-select-option v-for="item in expertTypeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="专业领域" name="specialties">
            <a-textarea
              v-model:value="formData.specialties"
              :rows="2"
              placeholder="请输入专业领域（逗号分隔）"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="擅长方向" name="expertiseAreas">
            <a-input v-model:value="formData.expertiseAreas" placeholder="请输入擅长方向" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="学历" name="educationBackground">
            <a-input v-model:value="formData.educationBackground" placeholder="请输入学历" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="学位" name="degree">
            <a-input v-model:value="formData.degree" placeholder="请输入学位" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="毕业院校" name="graduatedFrom">
            <a-input v-model:value="formData.graduatedFrom" placeholder="请输入毕业院校" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 资质证明 -->
      <a-divider orientation="left">资质证明</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="资格证书编号" name="qualificationCert">
            <a-input v-model:value="formData.qualificationCert" placeholder="请输入资格证书编号" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 其他信息 -->
      <a-divider orientation="left">其他信息</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-select v-model:value="formData.status" placeholder="请选择状态">
              <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态说明" name="statusRemark">
            <a-textarea
              v-model:value="formData.statusRemark"
              :rows="2"
              placeholder="请输入状态说明（如暂停原因）"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea
              v-model:value="formData.remark"
              :rows="2"
              placeholder="请输入备注"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </Modal>
</template>
