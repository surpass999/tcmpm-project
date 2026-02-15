<script lang="ts" setup>
import type { BpmProcessNodeDslApi } from '#/api/bpm/process-node-dsl';

import { ref, watch } from 'vue';

import { useVbenForm } from '#/adapter/form';
import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { createDsl, getDsl, updateDsl, validateDslConfig } from '#/api/bpm/process-node-dsl';

interface Props {
  /** 弹窗模式 */
  mode?: 'add' | 'edit';
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'add',
});

const emit = defineEmits<{
  success: [];
}>();

// 表单数据
const formData = ref<BpmProcessNodeDslApi.DslSaveParams>({
  id: undefined,
  processDefinitionKey: '',
  nodeKey: '',
  nodeName: '',
  dslConfig: '',
  enabled: 1,
  remark: '',
});

// DSL 配置编辑器相关
const activeTab = ref('basic');
const dslEditorData = ref({
  nodeKey: '',
  cap: 'AUDIT',
  actions: 'agree,reject',
  roles: [] as string[],
  assign: {
    type: 'STATIC_ROLE',
    source: '',
  },
  signRule: 'MAJORITY',
  backStrategy: 'TO_START',
  reSubmit: 'RESTART',
  bizStatus: '',
  vars: {
    targetVar: '',
    min: undefined as number | undefined,
    max: undefined as number | undefined,
    modifyFields: [] as string[],
  },
  ui: {
    form: '',
    readonly: false,
  },
});

// 节点能力选项
const capOptions = [
  { label: '单人审批 (AUDIT)', value: 'AUDIT' },
  { label: '会签 (COUNTERSIGN)', value: 'COUNTERSIGN' },
  { label: '选择专家 (EXPERT_SELECT)', value: 'EXPERT_SELECT' },
  { label: '填报 (FILL)', value: 'FILL' },
  { label: '补正 (MODIFY)', value: 'MODIFY' },
  { label: '确认 (CONFIRM)', value: 'CONFIRM' },
  { label: '归档 (ARCHIVE)', value: 'ARCHIVE' },
  { label: '发布 (PUBLISH)', value: 'PUBLISH' },
];

// 动作选项
const actionOptions = [
  { label: '提交 (submit)', value: 'submit' },
  { label: '通过 (agree)', value: 'agree' },
  { label: '拒绝 (reject)', value: 'reject' },
  { label: '退回 (back)', value: 'back' },
  { label: '撤回 (cancel)', value: 'cancel' },
  { label: '转办 (transfer)', value: 'transfer' },
  { label: '委派 (delegate)', value: 'delegate' },
  { label: '加签 (addSign)', value: 'addSign' },
  { label: '减签 (reduceSign)', value: 'reduceSign' },
  { label: '会签同意 (signAgree)', value: 'signAgree' },
  { label: '会签拒绝 (signReject)', value: 'signReject' },
  { label: '催办 (urge)', value: 'urge' },
  { label: '选择专家 (selectExpert)', value: 'selectExpert' },
  { label: '补正 (modify)', value: 'modify' },
  { label: '确认 (confirm)', value: 'confirm' },
  { label: '归档 (archive)', value: 'archive' },
];

// 分配类型选项
const assignTypeOptions = [
  { label: '固定角色 (STATIC_ROLE)', value: 'STATIC_ROLE' },
  { label: '动态用户 (DYNAMIC_USER)', value: 'DYNAMIC_USER' },
  { label: '用户组 (GROUP)', value: 'GROUP' },
];

// 分配来源选项
const assignSourceOptions = [
  { label: '省级角色 (provinceRole)', value: 'provinceRole' },
  { label: '国家局角色 (nationalRole)', value: 'nationalRole' },
  { label: '专家用户 (expertUsers)', value: 'expertUsers' },
  { label: '发起人部门负责人 (startUserDeptLeader)', value: 'startUserDeptLeader' },
  { label: '用户组 (userGroups)', value: 'userGroups' },
];

// 会签规则选项
const signRuleOptions = [
  { label: '全部通过 (ALL)', value: 'ALL' },
  { label: '任一通过 (ANY)', value: 'ANY' },
  { label: '多数通过 (MAJORITY)', value: 'MAJORITY' },
  { label: '三分之二 (2/3)', value: '2/3' },
  { label: '自定义 (CUSTOM)', value: 'CUSTOM' },
];

// 退回策略选项
const backStrategyOptions = [
  { label: '退回起点 (TO_START)', value: 'TO_START' },
  { label: '退回上一节点 (TO_PREV)', value: 'TO_PREV' },
  { label: '退回任意节点 (TO_ANY)', value: 'TO_ANY' },
  { label: '退回指定角色 (TO_ROLE)', value: 'TO_ROLE' },
];

// 重新提交策略选项
const reSubmitOptions = [
  { label: '重新开始 (RESTART)', value: 'RESTART' },
  { label: '从当前节点继续 (RESUME)', value: 'RESUME' },
];

// 角色选项
const roleOptions = [
  { label: '省级 (PROVINCE)', value: 'PROVINCE' },
  { label: '国家局 (NATION)', value: 'NATION' },
  { label: '专家 (EXPERT)', value: 'EXPERT' },
  { label: '医院 (HOSPITAL)', value: 'HOSPITAL' },
];

// 监听 dslEditorData 变化，更新 dslConfig
watch(
  () => dslEditorData.value,
  (val) => {
    formData.value.dslConfig = JSON.stringify(val, null, 2);
  },
  { deep: true }
);

// 从 JSON 解析到编辑器数据
function parseDslConfig(configStr: string) {
  if (!configStr) return;
  try {
    const config = JSON.parse(configStr);
    dslEditorData.value = {
      nodeKey: config.nodeKey || '',
      cap: config.cap || 'AUDIT',
      actions: config.actions || '',
      roles: config.roles || [],
      assign: config.assign || { type: 'STATIC_ROLE', source: '' },
      signRule: config.signRule || 'MAJORITY',
      backStrategy: config.backStrategy || 'TO_START',
      reSubmit: config.reSubmit || 'RESTART',
      bizStatus: config.bizStatus || '',
      vars: config.vars || { targetVar: '', min: undefined, max: undefined, modifyFields: [] },
      ui: config.ui || { form: '', readonly: false },
    };
  } catch (e) {
    console.error('解析DSL配置失败', e);
  }
}

// 加载数据
async function loadData(id: number) {
  const data = await getDsl(id);
  if (data) {
    formData.value = { ...data };
    parseDslConfig(data.dslConfig);
    // 设置表单值
    await formApi.setValues(formData.value);
  }
}

// 提交
async function handleSubmit() {
  try {
    // 更新 dslConfig
    dslEditorData.value.nodeKey = formData.value.nodeKey;
    formData.value.dslConfig = JSON.stringify(dslEditorData.value, null, 2);

    // 验证 DSL 配置
    await validateDslConfig(formData.value.dslConfig);

    if (formData.value.id) {
      await updateDsl(formData.value);
      message.success('更新成功');
    } else {
      await createDsl(formData.value);
      message.success('创建成功');
    }
    emit('success');
  } catch (e: any) {
    message.error(e.message || '操作失败');
  }
}

const [Form, formApi] = useVbenForm({
  layout: 'horizontal',
  showDefaultActions: false,
  commonConfig: {
    labelWidth: 120,
    componentProps: {
      class: 'w-full',
    },
  },
  schema: [
    {
      fieldName: 'processDefinitionKey',
      label: '流程定义Key',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入流程定义Key，如 proc_filing',
      },
    },
    {
      fieldName: 'nodeKey',
      label: '节点Key',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入节点Key，如 province_audit',
      },
    },
    {
      fieldName: 'nodeName',
      label: '节点名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入节点名称，如 省级审核',
      },
    },
    {
      fieldName: 'enabled',
      label: '是否启用',
      component: 'RadioGroup',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入备注',
        rows: 2,
      },
    },
  ],
});

const [Modal, modalApi] = useVbenModal({
  class: 'w-2/5',
  title: 'DSL配置',
  destroyOnClose: true,
  onConfirm: async () => {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    try {
      await handleSubmit();
      await modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      return;
    }
    const data = modalApi.getData<BpmProcessNodeDslApi.Dsl>();
    if (data?.id) {
      await loadData(data.id);
      // 设置表单值
      await formApi.setValues(formData.value);
    } else {
      // 重置表单
      formData.value = {
        id: undefined,
        processDefinitionKey: '',
        nodeKey: '',
        nodeName: '',
        dslConfig: '',
        enabled: 1,
        remark: '',
      };
      dslEditorData.value = {
        nodeKey: '',
        cap: 'AUDIT',
        actions: 'agree,reject',
        roles: [],
        assign: { type: 'STATIC_ROLE', source: '' },
        signRule: 'MAJORITY',
        backStrategy: 'TO_START',
        reSubmit: 'RESTART',
        bizStatus: '',
        vars: { targetVar: '', min: undefined, max: undefined, modifyFields: [] },
        ui: { form: '', readonly: false },
      };
      // 设置表单默认值
      await formApi.setValues(formData.value);
    }
  },
});

defineExpose({
  setData: modalApi.setData,
  open: modalApi.open,
});
</script>

<template>
  <Modal>
    <div class="dsl-editor">
      <!-- 基本信息 -->
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="basic" tab="基本信息">
          <Form />
        </a-tab-pane>

        <a-tab-pane key="dsl" tab="DSL配置">
          <div class="dsl-config-panel">
            <a-row :gutter="16">
              <!-- 左侧：DSL 配置表单 -->
              <a-col :span="14">
                <a-form layout="vertical">
                  <!-- 节点能力 -->
                  <a-form-item label="节点能力 (cap)">
                    <a-select
                      v-model:value="dslEditorData.cap"
                      :options="capOptions"
                      placeholder="请选择节点能力"
                    />
                  </a-form-item>

                  <!-- 可用动作 -->
                  <a-form-item label="可用动作 (actions)">
                    <a-select
                      v-model:value="dslEditorData.actions"
                      mode="tags"
                      :options="actionOptions"
                      placeholder="输入动作后回车确认"
                    />
                    <div class="form-help-text">
                      多个动作用逗号分隔，如：agree,reject,back
                    </div>
                  </a-form-item>

                  <!-- 角色限制 -->
                  <a-form-item label="角色限制 (roles)">
                    <a-select
                      v-model:value="dslEditorData.roles"
                      mode="multiple"
                      :options="roleOptions"
                      placeholder="选择允许操作的角色"
                    />
                  </a-form-item>

                  <!-- 任务分配 -->
                  <a-form-item label="任务分配 (assign)">
                    <a-row :gutter="8">
                      <a-col :span="12">
                        <a-select
                          v-model:value="dslEditorData.assign.type"
                          :options="assignTypeOptions"
                          placeholder="分配类型"
                        />
                      </a-col>
                      <a-col :span="12">
                        <a-select
                          v-model:value="dslEditorData.assign.source"
                          :options="assignSourceOptions"
                          placeholder="分配来源"
                          allowClear
                        />
                      </a-col>
                    </a-row>
                  </a-form-item>

                  <!-- 条件显示：根据节点能力显示不同配置 -->
                  <template v-if="dslEditorData.cap === 'COUNTERSIGN'">
                    <!-- 会签规则 -->
                    <a-form-item label="会签规则 (signRule)">
                      <a-select
                        v-model:value="dslEditorData.signRule"
                        :options="signRuleOptions"
                      />
                    </a-form-item>

                    <!-- 专家数量 -->
                    <a-form-item label="专家数量范围">
                      <a-row :gutter="8">
                        <a-col :span="12">
                          <a-input-number
                            v-model:value="dslEditorData.vars.min"
                            :min="1"
                            placeholder="最少"
                            style="width: 100%"
                          />
                        </a-col>
                        <a-col :span="12">
                          <a-input-number
                            v-model:value="dslEditorData.vars.max"
                            :min="1"
                            placeholder="最多"
                            style="width: 100%"
                          />
                        </a-col>
                      </a-row>
                    </a-form-item>
                  </template>

                  <template v-if="dslEditorData.cap === 'MODIFY'">
                    <!-- 补正时可修改字段 -->
                    <a-form-item label="可修改字段 (modifyFields)">
                      <a-select
                        v-model:value="dslEditorData.vars.modifyFields"
                        mode="tags"
                        placeholder="输入字段名后回车确认"
                      />
                      <div class="form-help-text">
                        补正时允许用户修改的字段列表
                      </div>
                    </a-form-item>
                  </template>

                  <!-- 退回策略 -->
                  <a-form-item label="退回策略 (backStrategy)">
                    <a-select
                      v-model:value="dslEditorData.backStrategy"
                      :options="backStrategyOptions"
                    />
                  </a-form-item>

                  <!-- 重新提交策略 -->
                  <a-form-item label="重新提交策略 (reSubmit)">
                    <a-select
                      v-model:value="dslEditorData.reSubmit"
                      :options="reSubmitOptions"
                    />
                  </a-form-item>

                  <!-- 业务状态 -->
                  <a-form-item label="业务状态 (bizStatus)">
                    <a-input
                      v-model:value="dslEditorData.bizStatus"
                      placeholder="审批通过后更新的业务状态值"
                    />
                    <div class="form-help-text">
                      审批通过后更新业务表的状态字段值
                    </div>
                  </a-form-item>

                  <!-- UI 配置 -->
                  <a-divider>UI 配置</a-divider>
                  <a-form-item label="表单标识">
                    <a-input
                      v-model:value="dslEditorData.ui.form"
                      placeholder="关联的表单配置"
                    />
                  </a-form-item>
                  <a-form-item label="是否只读">
                    <a-switch v-model:checked="dslEditorData.ui.readonly" />
                  </a-form-item>
                </a-form>
              </a-col>

              <!-- 右侧：JSON 预览 -->
              <a-col :span="10">
                <div class="json-preview">
                  <div class="json-preview-title">JSON 预览</div>
                  <a-textarea
                    :value="formData.dslConfig"
                    :rows="30"
                    :readonly="true"
                    class="json-textarea"
                  />
                </div>
              </a-col>
            </a-row>
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>
  </Modal>
</template>

<style scoped>
.dsl-editor {
  padding: 16px;
}

.dsl-config-panel {
  min-height: 500px;
}

.form-help-text {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.json-preview {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
}

.json-preview-title {
  font-weight: bold;
  margin-bottom: 8px;
}

.json-textarea {
  font-family: 'Monaco', 'Menlo', 'Ubuntu', 'Consolas', monospace;
  font-size: 12px;
}
</style>
