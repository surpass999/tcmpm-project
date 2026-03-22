<script setup lang="ts">
import type { Ref } from 'vue';

import type { ButtonSetting, SimpleFlowNode } from '../../consts';

import type { SystemDeptApi } from '#/api/system/dept';
import type { SystemUserApi } from '#/api/system/user';

import { inject, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { BpmModelFormType, BpmNodeTypeEnum } from '@vben/constants';
import { IconifyIcon } from '@vben/icons';
import { cloneDeep } from '@vben/utils';

import {
  ElButton,
  ElCol,
  ElInput,
  ElRadio,
  ElRadioGroup,
  ElRow,
  ElSwitch,
  ElTabPane,
  ElTabs,
  ElText,
  ElTooltip,
} from 'element-plus';

import {
  AssignStartUserHandlerType,
  FieldPermissionType,
  OPERATION_BUTTON_NAME,
  START_USER_BUTTON_SETTING,
} from '../../consts';
import {
  useFormFieldsPermission,
  useNodeName,
  useWatchNode,
} from '../../helpers';

defineOptions({ name: 'StartUserNodeConfig' });

const props = defineProps({
  flowNode: {
    type: Object as () => SimpleFlowNode,
    required: true,
  },
});

// 可发起流程的用户编号
const startUserIds = inject<Ref<any[]>>('startUserIds');
// 可发起流程的部门编号
const startDeptIds = inject<Ref<any[]>>('startDeptIds');
// 用户列表
const userOptions = inject<Ref<SystemUserApi.User[]>>('userList');
// 部门列表
const deptOptions = inject<Ref<SystemDeptApi.Dept[]>>('deptList');
// 当前节点
const currentNode = useWatchNode(props);
// 节点名称
const { nodeName, showInput, clickIcon, changeNodeName, inputRef } =
  useNodeName(BpmNodeTypeEnum.START_USER_NODE);
// 激活的 Tab 标签页
const activeTabName = ref('user');

// 表单字段权限配置
const { formType, fieldsPermissionConfig, getNodeConfigFormFields } =
  useFormFieldsPermission(FieldPermissionType.WRITE);

// 发起人节点处理方式
const startUserHandlerType = ref<AssignStartUserHandlerType>(
  AssignStartUserHandlerType.START_USER_AUDIT,
);

// 操作按钮设置
const {
  buttonsSetting,
  btnDisplayNameEdit,
  changeBtnDisplayName,
  btnDisplayNameBlurEvent,
  setInputRef,
} = useButtonsSetting();

/** 操作按钮设置 */
function useButtonsSetting() {
  const buttonsSetting = ref<ButtonSetting[]>();
  // 操作按钮显示名称可编辑
  const btnDisplayNameEdit = ref<boolean[]>([]);
  // 输入框的引用数组 - 内部使用，不暴露出去
  const _btnDisplayNameInputRefs = ref<Array<HTMLInputElement | null>>([]);

  const changeBtnDisplayName = (index: number) => {
    btnDisplayNameEdit.value[index] = true;
    // 输入框自动聚集
    nextTick(() => {
      if (_btnDisplayNameInputRefs.value[index]) {
        _btnDisplayNameInputRefs.value[index]?.focus();
      }
    });
  };

  const btnDisplayNameBlurEvent = (index: number) => {
    btnDisplayNameEdit.value[index] = false;
    const buttonItem = buttonsSetting.value![index];
    if (buttonItem)
      buttonItem.displayName =
        buttonItem.displayName || OPERATION_BUTTON_NAME.get(buttonItem.id)!;
  };

  // 设置 ref 引用的方法
  const setInputRef = (
    el: any | null,
    index: number,
  ) => {
    _btnDisplayNameInputRefs.value[index] = el;
  };

  return {
    buttonsSetting,
    btnDisplayNameEdit,
    changeBtnDisplayName,
    btnDisplayNameBlurEvent,
    setInputRef,
  };
}

function getUserNicknames(userIds: number[]): string {
  if (!userIds || userIds.length === 0) {
    return '';
  }
  const nicknames: string[] = [];
  userIds.forEach((userId) => {
    const found = userOptions?.value.find((item) => item.id === userId);
    if (found && found.nickname) {
      nicknames.push(found.nickname);
    }
  });
  return nicknames.join(',');
}

function getDeptNames(deptIds: number[]): string {
  if (!deptIds || deptIds.length === 0) {
    return '';
  }
  const deptNames: string[] = [];
  deptIds.forEach((deptId) => {
    const found = deptOptions?.value.find((item) => item.id === deptId);
    if (found && found.name) {
      deptNames.push(found.name);
    }
  });
  return deptNames.join(',');
}

// 使用 VbenDrawer
const [Drawer, drawerApi] = useVbenDrawer({
  header: true,
  closable: true,
  onCancel() {
    drawerApi.setState({ isOpen: false });
  },
  onConfirm() {
    saveConfig();
  },
});

// 保存配置
async function saveConfig() {
  activeTabName.value = 'user';
  currentNode.value.name = nodeName.value!;
  currentNode.value.showText = '已设置';
  // 设置表单权限
  currentNode.value.fieldsPermission = fieldsPermissionConfig.value;
  // 设置发起人的按钮权限（使用用户配置的按钮设置）
  currentNode.value.buttonsSetting = buttonsSetting.value;
  // 设置发起人节点处理方式
  currentNode.value.assignStartUserHandlerType = startUserHandlerType.value;
  drawerApi.setState({ isOpen: false });
  return true;
}

// 显示发起人节点配置，由父组件传过来
function showStartUserNodeConfig(node: SimpleFlowNode) {
  nodeName.value = node.name;
  // 读取发起人处理方式
  startUserHandlerType.value = node.assignStartUserHandlerType || AssignStartUserHandlerType.START_USER_AUDIT;
  // 表单字段权限
  getNodeConfigFormFields(node.fieldsPermission);
  // 按钮设置
  if (node.buttonsSetting?.length) {
    buttonsSetting.value = cloneDeep(node.buttonsSetting);
  } else {
    buttonsSetting.value = cloneDeep(START_USER_BUTTON_SETTING);
  }
  drawerApi.open();
}

/** 批量更新权限 */
function updatePermission(type: string) {
  fieldsPermissionConfig.value.forEach((field) => {
    if (type === 'READ') {
      field.permission = FieldPermissionType.READ;
    } else if (type === 'WRITE') {
      field.permission = FieldPermissionType.WRITE;
    } else {
      field.permission = FieldPermissionType.NONE;
    }
  });
}

/**
 * 暴露方法给父组件
 */
defineExpose({ showStartUserNodeConfig });
</script>
<template>
  <Drawer>
    <template #title>
      <div class="config-header">
        <ElInput
          ref="inputRef"
          v-if="showInput"
          type="text"
          class="focus:border-blue-500 focus:shadow-[0_0_0_2px_rgba(24,144,255,0.2)] focus:outline-none"
          @blur="changeNodeName()"
          @keyup.enter="changeNodeName()"
          v-model="nodeName"
          :placeholder="nodeName"
        />
        <div v-else class="node-name">
          {{ nodeName }}
          <IconifyIcon
            class="ml-1"
            icon="lucide:edit-3"
            :size="16"
            @click="clickIcon()"
          />
        </div>
      </div>
    </template>
    <ElTabs v-model="activeTabName" type="card">
      <ElTabPane label="权限" name="user">
        <ElText
          v-if="
            (!startUserIds || startUserIds.length === 0) &&
            (!startDeptIds || startDeptIds.length === 0)
          "
        >
          全部成员可以发起流程
        </ElText>
        <div v-else-if="startUserIds && startUserIds.length > 0">
          <ElText v-if="startUserIds.length === 1">
            {{ getUserNicknames(startUserIds) }} 可发起流程
          </ElText>
          <ElText v-else>
            <ElTooltip
              class="box-item"
              effect="dark"
              placement="top"
              :content="getUserNicknames(startUserIds)"
            >
              {{ getUserNicknames(startUserIds.slice(0, 2)) }} 等
              {{ startUserIds.length }} 人可发起流程
            </ElTooltip>
          </ElText>
        </div>
        <div v-else-if="startDeptIds && startDeptIds.length > 0">
          <ElText v-if="startDeptIds.length === 1">
            {{ getDeptNames(startDeptIds) }} 可发起流程
          </ElText>
          <ElText v-else>
            <ElTooltip
              class="box-item"
              effect="dark"
              placement="top"
              :content="getDeptNames(startDeptIds)"
            >
              {{ getDeptNames(startDeptIds.slice(0, 2)) }} 等
              {{ startDeptIds.length }} 个部门可发起流程
            </ElTooltip>
          </ElText>
        </div>
      </ElTabPane>
      <ElTabPane label="处理方式" name="handler">
        <div class="p-1">
          <div class="mb-4 text-base font-bold">发起人节点处理方式</div>
          <ElRadioGroup v-model="startUserHandlerType" class="w-full">
            <ElRow :gutter="16">
              <ElCol :span="24" class="mb-4">
                <ElRadio :value="AssignStartUserHandlerType.START_USER_AUDIT" class="w-full">
                  <div class="ml-2">
                    <div class="font-bold">由发起人对自己审批</div>
                    <div class="text-gray-500 text-sm">发起人需要手动点击"提交"按钮才能继续流程</div>
                  </div>
                </ElRadio>
              </ElCol>
              <ElCol :span="24" class="mb-4">
                <ElRadio :value="AssignStartUserHandlerType.SKIP" class="w-full">
                  <div class="ml-2">
                    <div class="font-bold">自动跳过（推荐）</div>
                    <div class="text-gray-500 text-sm">发起人提交后自动跳过，无需再次操作</div>
                  </div>
                </ElRadio>
              </ElCol>
              <ElCol :span="24">
                <ElRadio :value="AssignStartUserHandlerType.ASSIGN_DEPT_LEADER" class="w-full">
                  <div class="ml-2">
                    <div class="font-bold">转交给部门负责人审批</div>
                    <div class="text-gray-500 text-sm">如果部门负责人为空，则自动通过</div>
                  </div>
                </ElRadio>
              </ElCol>
            </ElRow>
          </ElRadioGroup>
        </div>
      </ElTabPane>
      <ElTabPane label="操作按钮设置" name="buttons">
        <div class="p-1">
          <div class="mb-4 text-base font-bold">操作按钮</div>

          <!-- 表头 -->
          <ElRow class="border border-gray-200 px-4 py-3">
            <ElCol :span="6" class="font-bold">操作按钮</ElCol>
            <ElCol :span="8" class="font-bold">显示名称</ElCol>
            <ElCol :span="7" class="font-bold">业务状态(bizStatus)</ElCol>
            <ElCol :span="3" class="flex items-center justify-center font-bold">
              启用
            </ElCol>
          </ElRow>

          <!-- 表格内容 -->
          <div v-for="(item, index) in buttonsSetting" :key="index">
            <ElRow class="border border-t-0 border-gray-200 px-4 py-2">
              <ElCol :span="6" class="flex items-center truncate">
                {{ OPERATION_BUTTON_NAME.get(item.id) }}
              </ElCol>
              <ElCol :span="8" class="flex items-center">
                <ElInput
                  v-if="btnDisplayNameEdit[index]"
                  :ref="(el: any) => setInputRef(el, index)"
                  type="text"
                  class="max-w-24 focus:border-blue-500 focus:shadow-[0_0_0_2px_rgba(24,144,255,0.2)] focus:outline-none"
                  @blur="btnDisplayNameBlurEvent(index)"
                  @keyup.enter="btnDisplayNameBlurEvent(index)"
                  v-model="item.displayName"
                  :placeholder="item.displayName"
                />
                <ElButton v-else text @click="changeBtnDisplayName(index)">
                  <div class="flex items-center">
                    {{ item.displayName }}
                    <IconifyIcon icon="lucide:edit" class="ml-2" />
                  </div>
                </ElButton>
              </ElCol>
              <ElCol :span="7" class="flex items-center">
                <ElInput
                  type="text"
                  class="max-w-24"
                  v-model="item.bizStatus"
                  placeholder="如: SUBMIT"
                />
              </ElCol>
              <ElCol :span="3" class="flex items-center justify-center">
                <ElSwitch v-model="item.enable" />
              </ElCol>
            </ElRow>
          </div>
        </div>
      </ElTabPane>
      <ElTabPane
        label="表单字段权限"
        name="fields"
        v-if="formType === BpmModelFormType.NORMAL"
      >
        <div class="p-1">
          <div class="mb-4 text-base font-bold">字段权限</div>

          <!-- 表头 -->
          <ElRow class="border border-gray-200 px-4 py-3">
            <ElCol :span="8" class="font-bold">字段名称</ElCol>
            <ElCol :span="16" class="!flex">
              <span
                class="flex-1 cursor-pointer text-center font-bold"
                @click="updatePermission('READ')"
              >
                只读
              </span>
              <span
                class="flex-1 cursor-pointer text-center font-bold"
                @click="updatePermission('WRITE')"
              >
                可编辑
              </span>
              <span
                class="flex-1 cursor-pointer text-center font-bold"
                @click="updatePermission('NONE')"
              >
                隐藏
              </span>
            </ElCol>
          </ElRow>

          <!-- 表格内容 -->
          <div v-for="(item, index) in fieldsPermissionConfig" :key="index">
            <ElRow class="border border-t-0 border-gray-200 px-4 py-2">
              <ElCol :span="8" class="flex items-center truncate">
                {{ item.title }}
              </ElCol>
              <ElCol :span="16">
                <ElRadioGroup v-model="item.permission" class="flex w-full">
                  <div class="flex flex-1 justify-center">
                    <ElRadio :value="FieldPermissionType.READ" />
                  </div>
                  <div class="flex flex-1 justify-center">
                    <ElRadio :value="FieldPermissionType.WRITE" />
                  </div>
                  <div class="flex flex-1 justify-center">
                    <ElRadio :value="FieldPermissionType.NONE" />
                  </div>
                </ElRadioGroup>
              </ElCol>
            </ElRow>
          </div>
        </div>
      </ElTabPane>
    </ElTabs>
  </Drawer>
</template>
