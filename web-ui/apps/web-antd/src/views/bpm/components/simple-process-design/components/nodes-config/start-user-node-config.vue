<script setup lang="ts">
import type { Ref } from 'vue';

import type { ButtonSetting, SimpleFlowNode } from '../../consts';

import type { SystemDeptApi } from '#/api/system/dept';
import type { SystemUserApi } from '#/api/system/user';

import { inject, nextTick, ref } from 'vue';

import { cloneDeep } from '@vben/utils';

import { useVbenDrawer } from '@vben/common-ui';
import { BpmModelFormType, BpmNodeTypeEnum } from '@vben/constants';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Col,
  Input,
  Radio,
  RadioGroup,
  Row,
  Switch,
  TabPane,
  Tabs,
  Tooltip,
  TypographyText,
} from 'ant-design-vue';

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

// 发起人节点处理方式
const startUserHandlerType = ref<AssignStartUserHandlerType>(
  AssignStartUserHandlerType.START_USER_AUDIT,
);

// 表单字段权限配置
const { formType, fieldsPermissionConfig, getNodeConfigFormFields } =
  useFormFieldsPermission(FieldPermissionType.WRITE);

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
        <Input
          ref="inputRef"
          v-if="showInput"
          type="text"
          class="focus:border-blue-500 focus:shadow-[0_0_0_2px_rgba(24,144,255,0.2)] focus:outline-none"
          @blur="changeNodeName()"
          @press-enter="changeNodeName()"
          v-model:value="nodeName"
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
    <Tabs v-model:active-key="activeTabName" type="card">
      <TabPane tab="权限" key="user">
        <TypographyText
          v-if="
            (!startUserIds || startUserIds.length === 0) &&
            (!startDeptIds || startDeptIds.length === 0)
          "
        >
          全部成员可以发起流程
        </TypographyText>
        <div v-else-if="startUserIds && startUserIds.length > 0">
          <TypographyText v-if="startUserIds.length === 1">
            {{ getUserNicknames(startUserIds) }} 可发起流程
          </TypographyText>
          <TypographyText v-else>
            <Tooltip
              class="box-item"
              effect="dark"
              placement="top"
              :content="getUserNicknames(startUserIds)"
            >
              {{ getUserNicknames(startUserIds.slice(0, 2)) }} 等
              {{ startUserIds.length }} 人可发起流程
            </Tooltip>
          </TypographyText>
        </div>
        <div v-else-if="startDeptIds && startDeptIds.length > 0">
          <TypographyText v-if="startDeptIds.length === 1">
            {{ getDeptNames(startDeptIds) }} 可发起流程
          </TypographyText>
          <TypographyText v-else>
            <Tooltip
              class="box-item"
              effect="dark"
              placement="top"
              :content="getDeptNames(startDeptIds)"
            >
              {{ getDeptNames(startDeptIds.slice(0, 2)) }} 等
              {{ startDeptIds.length }} 个部门可发起流程
            </Tooltip>
          </TypographyText>
        </div>
      </TabPane>
      <TabPane tab="处理方式" key="handler">
        <div class="p-1">
          <div class="mb-4 text-base font-bold">发起人节点处理方式</div>
          <RadioGroup v-model:value="startUserHandlerType" class="w-full">
            <Row :gutter="16">
              <Col :span="24" class="mb-4">
                <Radio :value="AssignStartUserHandlerType.START_USER_AUDIT" class="w-full">
                  <div class="ml-2">
                    <div class="font-bold">由发起人对自己审批</div>
                    <div class="text-gray-500 text-sm">发起人需要手动点击"提交"按钮才能继续流程</div>
                  </div>
                </Radio>
              </Col>
              <Col :span="24" class="mb-4">
                <Radio :value="AssignStartUserHandlerType.SKIP" class="w-full">
                  <div class="ml-2">
                    <div class="font-bold">自动跳过（推荐）</div>
                    <div class="text-gray-500 text-sm">发起人提交后自动跳过，无需再次操作</div>
                  </div>
                </Radio>
              </Col>
              <Col :span="24">
                <Radio :value="AssignStartUserHandlerType.ASSIGN_DEPT_LEADER" class="w-full">
                  <div class="ml-2">
                    <div class="font-bold">转交给部门负责人审批</div>
                    <div class="text-gray-500 text-sm">如果部门负责人为空，则自动通过</div>
                  </div>
                </Radio>
              </Col>
            </Row>
          </RadioGroup>
        </div>
      </TabPane>
      <TabPane tab="操作按钮设置" key="buttons">
        <div class="p-1">
          <div class="mb-4 text-base font-bold">操作按钮</div>

          <!-- 表头 -->
          <Row class="border border-gray-200 px-4 py-3">
            <Col :span="6" class="font-bold">操作按钮</Col>
            <Col :span="8" class="font-bold">显示名称</Col>
            <Col :span="7" class="font-bold">业务状态(bizStatus)</Col>
            <Col :span="3" class="flex items-center justify-center font-bold">
              启用
            </Col>
          </Row>

          <!-- 表格内容 -->
          <div v-for="(item, index) in buttonsSetting" :key="index">
            <Row class="border border-t-0 border-gray-200 px-4 py-2">
              <Col :span="6" class="flex items-center truncate">
                {{ OPERATION_BUTTON_NAME.get(item.id) }}
              </Col>
              <Col :span="8" class="flex items-center">
                <Input
                  v-if="btnDisplayNameEdit[index]"
                  :ref="(el) => setInputRef(el, index)"
                  type="text"
                  class="max-w-24 focus:border-blue-500 focus:shadow-[0_0_0_2px_rgba(24,144,255,0.2)] focus:outline-none"
                  @blur="btnDisplayNameBlurEvent(index)"
                  @press-enter="btnDisplayNameBlurEvent(index)"
                  v-model:value="item.displayName"
                  :placeholder="item.displayName"
                />
                <Button v-else text @click="changeBtnDisplayName(index)">
                  <div class="flex items-center">
                    {{ item.displayName }}
                    <IconifyIcon icon="lucide:edit" class="ml-2" />
                  </div>
                </Button>
              </Col>
              <Col :span="7" class="flex items-center">
                <Input
                  type="text"
                  class="max-w-24"
                  v-model:value="item.bizStatus"
                  placeholder="如: SUBMIT"
                />
              </Col>
              <Col :span="3" class="flex items-center justify-center">
                <Switch v-model:checked="item.enable" />
              </Col>
            </Row>
          </div>
        </div>
      </TabPane>
      <TabPane
        tab="表单字段权限"
        key="fields"
        v-if="formType === BpmModelFormType.NORMAL"
      >
        <div class="p-1">
          <div class="mb-4 text-base font-bold">字段权限</div>

          <!-- 表头 -->
          <Row class="border border-gray-200 px-4 py-3">
            <Col :span="8" class="font-bold">字段名称</Col>
            <Col :span="16">
              <Row>
                <Col :span="8" class="flex items-center justify-center">
                  <span
                    class="cursor-pointer font-bold"
                    @click="updatePermission('READ')"
                  >
                    只读
                  </span>
                </Col>
                <Col :span="8" class="flex items-center justify-center">
                  <span
                    class="cursor-pointer font-bold"
                    @click="updatePermission('WRITE')"
                  >
                    可编辑
                  </span>
                </Col>
                <Col :span="8" class="flex items-center justify-center">
                  <span
                    class="cursor-pointer font-bold"
                    @click="updatePermission('NONE')"
                  >
                    隐藏
                  </span>
                </Col>
              </Row>
            </Col>
          </Row>

          <!-- 表格内容 -->
          <div v-for="(item, index) in fieldsPermissionConfig" :key="index">
            <Row class="border border-t-0 border-gray-200 px-4 py-2">
              <Col :span="8" class="flex items-center truncate">
                {{ item.title }}
              </Col>
              <Col :span="16">
                <RadioGroup v-model:value="item.permission" class="w-full">
                  <Row>
                    <Col :span="8" class="flex items-center justify-center">
                      <Radio
                        :value="FieldPermissionType.READ"
                        size="large"
                        :label="FieldPermissionType.READ"
                      />
                    </Col>
                    <Col :span="8" class="flex items-center justify-center">
                      <Radio
                        :value="FieldPermissionType.WRITE"
                        size="large"
                        :label="FieldPermissionType.WRITE"
                      />
                    </Col>
                    <Col :span="8" class="flex items-center justify-center">
                      <Radio
                        :value="FieldPermissionType.NONE"
                        size="large"
                        :label="FieldPermissionType.NONE"
                      />
                    </Col>
                  </Row>
                </RadioGroup>
              </Col>
            </Row>
          </div>
        </div>
      </TabPane>
    </Tabs>
  </Drawer>
</template>
