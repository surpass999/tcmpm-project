<!-- UserTask 自定义配置：
     1. 审批人与提交人为同一人时
     2. 审批人拒绝时
     3. 审批人为空时
     4. 操作按钮
     5. 字段权限
     6. 审批类型
     7. 是否需要签名
-->
<script lang="ts" setup>
import type { ComponentPublicInstance } from 'vue';

import type { SystemUserApi } from '#/api/system/user';

import { inject, nextTick, onMounted, ref, toRaw, watch } from 'vue';

import { BpmModelFormType } from '@vben/constants';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Divider,
  Form,
  Input,
  Radio,
  RadioGroup,
  Select,
  SelectOption,
  Switch,
} from 'ant-design-vue';

import { getSimpleUserList } from '#/api/system/user';
import {
  APPROVE_TYPE,
  ApproveType,
  ASSIGN_EMPTY_HANDLER_TYPES,
  ASSIGN_START_USER_HANDLER_TYPES,
  AssignEmptyHandlerType,
  DEFAULT_BUTTON_SETTING,
  FieldPermissionType,
  OPERATION_BUTTON_NAME,
  REJECT_HANDLER_TYPES,
  RejectHandlerType,
} from '#/views/bpm/components/simple-process-design/consts';
import { useFormFieldsPermission } from '#/views/bpm/components/simple-process-design/helpers';

defineOptions({ name: 'ElementCustomConfig4UserTask' });
const props = defineProps({
  id: {
    type: String,
    required: false,
    default: '',
  },
  type: {
    type: String,
    required: false,
    default: '',
  },
});
const prefix = inject('prefix');

// 审批人与提交人为同一人时
const assignStartUserHandlerTypeEl = ref<any>();
const assignStartUserHandlerType = ref<any>();

// 审批人拒绝时
const rejectHandlerTypeEl = ref<any>();
const rejectHandlerType = ref<any>();
const returnNodeIdEl = ref<any>();
const returnNodeId = ref<any>();
const returnTaskList = ref<any[]>([]);

// 审批人为空时
const assignEmptyHandlerTypeEl = ref<any>();
const assignEmptyHandlerType = ref<any>();
const assignEmptyUserIdsEl = ref<any>();
const assignEmptyUserIds = ref<any>();

// 操作按钮
// const buttonsSettingEl = ref<any>();
// const { btnDisplayNameEdit, changeBtnDisplayName } = useButtonsSetting();
// const btnDisplayNameBlurEvent = (index: number) => {
//   btnDisplayNameEdit.value[index] = false;
//   const buttonItem = buttonsSettingEl.value[index];
//   buttonItem.displayName =
//     buttonItem.displayName || OPERATION_BUTTON_NAME.get(buttonItem.id)!;
//   updateElementExtensions();
// };

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
  const buttonsSetting = ref<any[]>([]);
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
    el: ComponentPublicInstance | Element | null,
    index: number,
  ) => {
    _btnDisplayNameInputRefs.value[index] = el as HTMLInputElement;
  };

  return {
    buttonsSetting,
    btnDisplayNameEdit,
    changeBtnDisplayName,
    btnDisplayNameBlurEvent,
    setInputRef,
  };
}

// 字段权限
const fieldsPermissionEl = ref<any[]>([]);
const { formType, fieldsPermissionConfig, getNodeConfigFormFields } =
  useFormFieldsPermission(FieldPermissionType.READ);

// 审批类型
const approveType = ref({ value: ApproveType.USER });

// 是否需要签名
const signEnable = ref({ value: false });

// 审批意见
const reasonRequire = ref({ value: false });

// ========== DSL 配置 ==========
const dslConfigEl = ref<any>();
const dslConfig = ref<string>('');

// DSL 配置选项
const dslCapOptions = [
  { label: '单人审批 (AUDIT)', value: 'AUDIT' },
  { label: '会签 (COUNTERSIGN)', value: 'COUNTERSIGN' },
  { label: '选择专家 (EXPERT_SELECT)', value: 'EXPERT_SELECT' },
  { label: '填报 (FILL)', value: 'FILL' },
  { label: '补正 (MODIFY)', value: 'MODIFY' },
  { label: '确认 (CONFIRM)', value: 'CONFIRM' },
  { label: '归档 (ARCHIVE)', value: 'ARCHIVE' },
  { label: '发布 (PUBLISH)', value: 'PUBLISH' },
];

const dslActionOptions = [
  { label: '提交 (submit)', value: 'submit' },
  { label: '通过 (agree)', value: 'agree' },
  { label: '拒绝 (reject)', value: 'reject' },
  { label: '退回 (back)', value: 'back' },
  { label: '撤回 (cancel)', value: 'cancel' },
  { label: '转办 (transfer)', value: 'transfer' },
  { label: '委派 (delegate)', value: 'delegate' },
  { label: '加签 (addSign)', value: 'addSign' },
  { label: '减签 (reduceSign)', value: 'reduceSign' },
  { label: '补正 (modify)', value: 'modify' },
];

const dslAssignTypeOptions = [
  { label: '固定角色 (STATIC_ROLE)', value: 'STATIC_ROLE' },
  { label: '发起人 (START_USER)', value: 'START_USER' },
  { label: '动态用户 (DYNAMIC_USER)', value: 'DYNAMIC_USER' },
  { label: '用户组 (GROUP)', value: 'GROUP' },
];

const dslAssignSourceOptions = [
  // 固定角色
  { label: '省局角色 (provinceRole)', value: 'provinceRole' },
  { label: '国家局角色 (nationalRole)', value: 'nationalRole' },
  { label: '专家角色 (expertRole)', value: 'expertRole' },
  { label: '医院角色 (hospitalRole)', value: 'hospitalRole' },
  // 发起人
  { label: '发起人 (startUser)', value: 'startUser' },
  { label: '发起人部门负责人 (startUserDeptLeader)', value: 'startUserDeptLeader' },
  { label: '发起人部门成员 (startUserDeptMembers)', value: 'startUserDeptMembers' },
  // 动态用户
  { label: '专家用户 (expertUsers)', value: 'expertUsers' },
  { label: '用户组 (userGroups)', value: 'userGroups' },
];

const dslSignRuleOptions = [
  { label: '全部通过 (ALL)', value: 'ALL' },
  { label: '任一通过 (ANY)', value: 'ANY' },
  { label: '多数通过 (MAJORITY)', value: 'MAJORITY' },
];

const dslBackStrategyOptions = [
  { label: '不可退回 (NONE)', value: 'NONE' },
  { label: '退回起点 (TO_START)', value: 'TO_START' },
  { label: '退回上一节点 (TO_PREV)', value: 'TO_PREV' },
  { label: '退回任意节点 (TO_ANY)', value: 'TO_ANY' },
  { label: '退回指定角色 (TO_ROLE)', value: 'TO_ROLE' },
];

const dslRoleOptions = [
  { label: '省局 (PROVINCE)', value: 'PROVINCE' },
  { label: '国家局 (NATION)', value: 'NATION' },
  { label: '专家 (EXPERT)', value: 'EXPERT' },
  { label: '医院 (HOSPITAL)', value: 'HOSPITAL' },
];

// DSL 配置表单数据
const dslFormData = ref({
  cap: 'AUDIT',
  actions: ['agree', 'reject'] as string[],
  roles: [] as string[],
  assignType: 'STATIC_ROLE',
  assignSource: '',
  signRule: 'MAJORITY',
  backStrategy: 'TO_START',
  bizStatus: '',
  enable: true,
  // 扩展字段
  expertMin: undefined as number | undefined,
  expertMax: undefined as number | undefined,
  modifyFields: [] as string[],
});

// 解析 DSL JSON
function parseDslJson(jsonStr: string) {
  if (!jsonStr) return;
  try {
    const config = JSON.parse(jsonStr);
    // actions 可能是字符串或数组，统一转为数组供表单使用
    let actionsArr: string[] = [];
    if (Array.isArray(config.actions)) {
      actionsArr = config.actions;
    } else if (typeof config.actions === 'string') {
      actionsArr = config.actions.split(',').filter((a: string) => a.trim());
    }
    dslFormData.value = {
      cap: config.cap || 'AUDIT',
      actions: actionsArr,
      roles: config.roles || [],
      assignType: config.assign?.type || 'STATIC_ROLE',
      assignSource: config.assign?.source || '',
      signRule: config.signRule || 'MAJORITY',
      backStrategy: config.backStrategy || 'TO_START',
      bizStatus: config.bizStatus || '',
      enable: config.enable !== false,
      // 扩展字段
      expertMin: config.expertMin,
      expertMax: config.expertMax,
      modifyFields: config.vars?.modifyFields || [],
    };
  } catch (e) {
    console.error('解析 DSL 配置失败', e);
  }
}

// 构建 DSL JSON
function buildDslJson(): string {
  const { cap, actions, roles, assignType, assignSource, signRule, backStrategy, bizStatus, enable, expertMin, expertMax, modifyFields } = dslFormData.value;

  // actions 必须是逗号分隔的字符串，不能是数组
  const actionsStr = Array.isArray(actions) ? actions.join(',') : actions;

  // 构建 vars 对象
  const vars: Record<string, any> = {};
  if (expertMin !== undefined) vars.expertMin = expertMin;
  if (expertMax !== undefined) vars.expertMax = expertMax;
  if (modifyFields && modifyFields.length > 0) vars.modifyFields = modifyFields;

  return JSON.stringify({
    nodeKey: props.id,
    cap,
    actions: actionsStr,
    roles,
    assign: assignSource ? { type: assignType, source: assignSource } : undefined,
    signRule: cap === 'COUNTERSIGN' ? signRule : undefined,
    backStrategy: cap !== 'FILL' ? backStrategy : 'NONE',
    bizStatus,
    enable,
    ...(Object.keys(vars).length > 0 ? { vars } : {}),
  });
}

const elExtensionElements = ref<any>();
const otherExtensions = ref<any>();
const bpmnElement = ref<any>();
const bpmnInstances = () => (window as any)?.bpmnInstances;

const resetCustomConfigList = () => {
  // 确保 BPMN 实例已准备好
  if (!bpmnInstances() || !bpmnInstances().bpmnElement) {
    console.log('[DSL] BPMN 实例未准备好，等待...');
    setTimeout(() => resetCustomConfigList(), 100);
    return;
  }
  
  bpmnElement.value = bpmnInstances().bpmnElement;

  // 获取当前节点ID，优先使用 props.id（更可靠）
  const currentNodeId = props.id || bpmnElement.value?.id;
  console.log('[DSL] resetCustomConfigList:', currentNodeId, 'props.id:', props.id);
  
  // 获取可回退的列表
  returnTaskList.value = findAllPredecessorsExcludingStart(
    bpmnElement.value.id,
    bpmnInstances().modeler,
  );
  // 获取元素扩展属性 或者 创建扩展属性
  elExtensionElements.value =
    bpmnElement.value.businessObject?.extensionElements ??
    bpmnInstances().moddle.create('bpmn:ExtensionElements', { values: [] });

  // 审批类型
  approveType.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:ApproveType`,
    )?.[0] ||
    bpmnInstances().moddle.create(`${prefix}:ApproveType`, {
      value: ApproveType.USER,
    });

  // 审批人与提交人为同一人时
  assignStartUserHandlerTypeEl.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:AssignStartUserHandlerType`,
    )?.[0] ||
    bpmnInstances().moddle.create(`${prefix}:AssignStartUserHandlerType`, {
      value: 1,
    });
  assignStartUserHandlerType.value = assignStartUserHandlerTypeEl.value.value;

  // 审批人拒绝时
  rejectHandlerTypeEl.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:RejectHandlerType`,
    )?.[0] ||
    bpmnInstances().moddle.create(`${prefix}:RejectHandlerType`, { value: 1 });
  rejectHandlerType.value = rejectHandlerTypeEl.value.value;
  returnNodeIdEl.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:RejectReturnTaskId`,
    )?.[0] ||
    bpmnInstances().moddle.create(`${prefix}:RejectReturnTaskId`, {
      value: '',
    });
  returnNodeId.value = returnNodeIdEl.value.value;

  // 审批人为空时
  assignEmptyHandlerTypeEl.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:AssignEmptyHandlerType`,
    )?.[0] ||
    bpmnInstances().moddle.create(`${prefix}:AssignEmptyHandlerType`, {
      value: 1,
    });
  assignEmptyHandlerType.value = assignEmptyHandlerTypeEl.value.value;
  assignEmptyUserIdsEl.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:AssignEmptyUserIds`,
    )?.[0] ||
    bpmnInstances().moddle.create(`${prefix}:AssignEmptyUserIds`, {
      value: '',
    });
  assignEmptyUserIds.value = assignEmptyUserIdsEl.value.value
    ?.split(',')
    .map((item: string) => {
      // 如果数字超出了最大安全整数范围，则将其作为字符串处理
      const num = Number(item);
      return num > Number.MAX_SAFE_INTEGER || num < -Number.MAX_SAFE_INTEGER
        ? item
        : num;
    });

  // 操作按钮
  buttonsSetting.value = elExtensionElements.value.values?.filter(
    (ex: any) => ex.$type === `${prefix}:ButtonsSetting`,
  );
  if (buttonsSetting.value.length === 0) {
    DEFAULT_BUTTON_SETTING.forEach((item) => {
      buttonsSetting.value.push(
        bpmnInstances().moddle.create(`${prefix}:ButtonsSetting`, {
          'flowable:id': item.id,
          'flowable:displayName': item.displayName,
          'flowable:enable': item.enable,
        }),
      );
    });
  }

  // 字段权限
  if (formType.value === BpmModelFormType.NORMAL) {
    const fieldsPermissionList = elExtensionElements.value.values?.filter(
      (ex: any) => ex.$type === `${prefix}:FieldsPermission`,
    );
    fieldsPermissionEl.value = [];
    getNodeConfigFormFields();
    fieldsPermissionConfig.value.forEach((element: any) => {
      element.permission =
        fieldsPermissionList?.find((obj: any) => obj.field === element.field)
          ?.permission ?? '1';
      fieldsPermissionEl.value.push(
        bpmnInstances().moddle.create(`${prefix}:FieldsPermission`, element),
      );
    });
  }

  // 是否需要签名
  signEnable.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:SignEnable`,
    ) ||
    bpmnInstances().moddle.create(`${prefix}:SignEnable`, { value: false });

  // 审批意见
  reasonRequire.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:ReasonRequire`,
    ) ||
    bpmnInstances().moddle.create(`${prefix}:ReasonRequire`, { value: false });

  // DSL 配置 - 直接读取现有配置，不创建新对象
  const foundDslConfig = elExtensionElements.value.values?.find(
    (ex: any) => ex.$type === `${prefix}:DslConfig`,
  );
  
  console.log('[resetCustomConfigList] foundDslConfig:', foundDslConfig);
  console.log('[resetCustomConfigList] elExtensionElements:', elExtensionElements.value);
  
  if (foundDslConfig) {
    dslConfigEl.value = foundDslConfig;
  } else {
    // 如果没有找到 DSL 配置，创建新的
    dslConfigEl.value = bpmnInstances().moddle.create(`${prefix}:DslConfig`, { value: '' });
    // 添加到扩展元素中
    elExtensionElements.value.values.push(dslConfigEl.value);
    console.log('[resetCustomConfigList] created new DslConfig');
  }
  
  // 直接读取 DSL 配置值
  const existingDslValue = dslConfigEl.value?.value || '';
  console.log('[DSL] 读取配置, nodeId:', currentNodeId, 'value:', existingDslValue);
  
  // 加载已保存的配置或使用默认值
  if (existingDslValue && existingDslValue.trim()) {
    console.log('[DSL] 加载已有配置:', currentNodeId);
    dslConfig.value = existingDslValue;
    parseDslJson(existingDslValue);
  } else {
    console.log('[DSL] 使用默认配置:', currentNodeId);
    dslConfig.value = '';
    dslFormData.value = {
      cap: 'AUDIT',
      actions: ['agree', 'reject'],
      roles: [],
      assignType: 'STATIC_ROLE',
      assignSource: '',
      signRule: 'MAJORITY',
      backStrategy: 'TO_START',
      bizStatus: '',
      enable: false,
      expertMin: undefined,
      expertMax: undefined,
      modifyFields: [],
    };
  }

  // 保留剩余扩展元素，便于后面更新该元素对应属性
  otherExtensions.value =
    elExtensionElements.value.values?.filter(
      (ex: any) =>
        ex.$type !== `${prefix}:AssignStartUserHandlerType` &&
        ex.$type !== `${prefix}:RejectHandlerType` &&
        ex.$type !== `${prefix}:RejectReturnTaskId` &&
        ex.$type !== `${prefix}:AssignEmptyHandlerType` &&
        ex.$type !== `${prefix}:AssignEmptyUserIds` &&
        ex.$type !== `${prefix}:ButtonsSetting` &&
        ex.$type !== `${prefix}:FieldsPermission` &&
        ex.$type !== `${prefix}:ApproveType` &&
        ex.$type !== `${prefix}:SignEnable` &&
        ex.$type !== `${prefix}:ReasonRequire` &&
        ex.$type !== `${prefix}:DslConfig`,
    ) ?? [];

  // 更新元素扩展属性，避免后续报错
  updateElementExtensions();
};

const updateAssignStartUserHandlerType = () => {
  assignStartUserHandlerTypeEl.value.value = assignStartUserHandlerType.value;

  updateElementExtensions();
};

const updateRejectHandlerType = () => {
  rejectHandlerTypeEl.value.value = rejectHandlerType.value;

  returnNodeId.value = returnTaskList.value[0]?.id;
  returnNodeIdEl.value.value = returnNodeId.value;

  updateElementExtensions();
};

const updateReturnNodeId = () => {
  returnNodeIdEl.value.value = returnNodeId.value;

  updateElementExtensions();
};

const updateAssignEmptyHandlerType = () => {
  assignEmptyHandlerTypeEl.value.value = assignEmptyHandlerType.value;

  updateElementExtensions();
};

const updateAssignEmptyUserIds = () => {
  assignEmptyUserIdsEl.value.value = assignEmptyUserIds.value.toString();

  updateElementExtensions();
};

const updateElementExtensions = () => {
  // 更新 DSL 配置的值
  if (!dslConfigEl.value) {
    console.error('[updateElementExtensions] dslConfigEl is undefined!');
    return;
  }
  dslConfigEl.value.value = buildDslJson();
  console.log('[updateElementExtensions] dslConfig value:', dslConfigEl.value.value);

  const extensions = bpmnInstances().moddle.create('bpmn:ExtensionElements', {
    values: [
      ...otherExtensions.value,
      assignStartUserHandlerTypeEl.value,
      rejectHandlerTypeEl.value,
      returnNodeIdEl.value,
      assignEmptyHandlerTypeEl.value,
      assignEmptyUserIdsEl.value,
      approveType.value,
      ...buttonsSetting.value,
      ...fieldsPermissionEl.value,
      signEnable.value,
      reasonRequire.value,
      dslConfigEl.value,
    ],
  });
  bpmnInstances().modeling.updateProperties(toRaw(bpmnElement.value), {
    extensionElements: extensions,
  });
};

watch(
  () => props.id,
  (val, oldVal) => {
    if (val && val.length > 0 && val !== oldVal) {
      console.log('[Config] 节点切换:', oldVal, '->', val);
      // 强制等待 DOM 更新后再加载新节点数据
      nextTick(() => {
        // 短暂延迟确保组件完全重建
        setTimeout(() => {
          resetCustomConfigList();
        }, 50);
      });
    } else if (val && val.length > 0) {
      // 首次加载
      nextTick(() => {
        resetCustomConfigList();
      });
    }
  },
  { immediate: true },
);

function findAllPredecessorsExcludingStart(elementId: string, modeler: any) {
  const elementRegistry = modeler.get('elementRegistry');
  const allConnections = elementRegistry.filter(
    (element: any) => element.type === 'bpmn:SequenceFlow',
  );
  const predecessors = new Set(); // 使用 Set 来避免重复节点
  const visited = new Set(); // 用于记录已访问的节点

  // 检查是否是开始事件节点
  function isStartEvent(element: any) {
    return element.type === 'bpmn:StartEvent';
  }

  function findPredecessorsRecursively(element: any) {
    // 如果该节点已经访问过，直接返回，避免循环
    if (visited.has(element)) {
      return;
    }

    // 标记当前节点为已访问
    visited.add(element);

    // 获取与当前节点相连的所有连接
    const incomingConnections = allConnections.filter(
      (connection: any) => connection.target === element,
    );

    incomingConnections.forEach((connection: any) => {
      const source = connection.source; // 获取前置节点

      // 只添加不是开始事件的前置节点
      if (!isStartEvent(source)) {
        predecessors.add(source.businessObject);
        // 递归查找前置节点
        findPredecessorsRecursively(source);
      }
    });
  }

  const targetElement = elementRegistry.get(elementId);
  if (targetElement) {
    findPredecessorsRecursively(targetElement);
  }

  return [...predecessors]; // 返回前置节点数组
}

// function useButtonsSetting() {
//   const buttonsSetting = ref<ButtonSetting[]>();
//   // 操作按钮显示名称可编辑
//   const btnDisplayNameEdit = ref<boolean[]>([]);
//   const changeBtnDisplayName = (index: number) => {
//     btnDisplayNameEdit.value[index] = true;
//   };
//   return {
//     buttonsSetting,
//     btnDisplayNameEdit,
//     changeBtnDisplayName,
//   };
// }

/** 批量更新权限 */
const updatePermission = (type: string) => {
  fieldsPermissionEl.value.forEach((field: any) => {
    if (type === 'READ') {
      field.permission = FieldPermissionType.READ;
    } else if (type === 'WRITE') {
      field.permission = FieldPermissionType.WRITE;
    } else {
      field.permission = FieldPermissionType.NONE;
    }
  });
};

const userOptions = ref<SystemUserApi.User[]>([]); // 用户列表
onMounted(async () => {
  // 获得用户列表
  userOptions.value = await getSimpleUserList();
});
</script>

<template>
  <div :key="props.id">
    <Divider orientation="left">审批类型</Divider>
    <Form.Item name="approveType" label="审批类型">
      <RadioGroup v-model:value="approveType.value">
        <Radio
          v-for="(item, index) in APPROVE_TYPE"
          :key="index"
          :value="item.value"
        >
          {{ item.label }}
        </Radio>
      </RadioGroup>
    </Form.Item>

    <Divider orientation="left">审批人拒绝时</Divider>
    <Form.Item name="rejectHandlerType" label="处理方式">
      <RadioGroup
        v-model:value="rejectHandlerType"
        :disabled="returnTaskList.length === 0"
        @change="updateRejectHandlerType"
      >
        <Radio
          v-for="(item, index) in REJECT_HANDLER_TYPES"
          :key="index"
          :value="item.value"
        >
          {{ item.label }}
        </Radio>
      </RadioGroup>
    </Form.Item>
    <Form.Item
      v-if="rejectHandlerType === RejectHandlerType.RETURN_USER_TASK"
      name="returnNodeId"
      label="驳回节点"
    >
      <Select
        v-model:value="returnNodeId"
        allow-clear
        style="width: 100%"
        @change="updateReturnNodeId"
        placeholder="请选择驳回节点"
      >
        <SelectOption
          v-for="item in returnTaskList"
          :key="item.id"
          :value="item.id"
        >
          {{ item.name }}
        </SelectOption>
      </Select>
    </Form.Item>

    <Divider orientation="left">审批人为空时</Divider>
    <Form.Item name="assignEmptyHandlerType">
      <RadioGroup
        v-model:value="assignEmptyHandlerType"
        @change="updateAssignEmptyHandlerType"
      >
        <div class="flex flex-col gap-2">
          <div v-for="(item, index) in ASSIGN_EMPTY_HANDLER_TYPES" :key="index">
            <Radio :key="item.value" :value="item.value">
              {{ item.label }}
            </Radio>
          </div>
        </div>
      </RadioGroup>
    </Form.Item>
    <Form.Item
      v-if="assignEmptyHandlerType === AssignEmptyHandlerType.ASSIGN_USER"
      label="指定用户"
      name="assignEmptyHandlerUserIds"
    >
      <Select
        v-model:value="assignEmptyUserIds"
        allow-clear
        mode="multiple"
        style="width: 100%"
        @change="updateAssignEmptyUserIds"
      >
        <SelectOption
          v-for="item in userOptions"
          :key="item.id"
          :value="item.id"
        >
          {{ item.nickname }}
        </SelectOption>
      </Select>
    </Form.Item>

    <Divider orientation="left">审批人与提交人为同一人时</Divider>
    <RadioGroup
      v-model:value="assignStartUserHandlerType"
      @change="updateAssignStartUserHandlerType"
    >
      <div class="flex flex-col gap-2">
        <div
          v-for="(item, index) in ASSIGN_START_USER_HANDLER_TYPES"
          :key="index"
        >
          <Radio :key="item.value" :value="item.value">
            {{ item.label }}
          </Radio>
        </div>
      </div>
    </RadioGroup>

    <Divider orientation="left">操作按钮</Divider>
    <div class="mt-2 text-sm">
      <!-- 头部标题行 -->
      <div
        class="flex items-center justify-between border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-900"
      >
        <div class="w-28 text-left">操作按钮</div>
        <div class="w-40 pl-2 text-left">显示名称</div>
        <div class="w-20 text-center">启用</div>
      </div>

      <!-- 按钮配置行 -->
      <div
        v-for="(item, index) in buttonsSetting"
        :key="index"
        class="flex items-center justify-between border border-t-0 border-slate-200 px-3 py-2 text-sm"
      >
        <div class="w-28 truncate text-left">
          {{ OPERATION_BUTTON_NAME.get(item.id) }}
        </div>
        <div class="flex w-40 items-center truncate text-left">
          <Input
            v-if="btnDisplayNameEdit[index]"
            :ref="(el) => setInputRef(el, index)"
            @blur="btnDisplayNameBlurEvent(index)"
            @press-enter="btnDisplayNameBlurEvent(index)"
            type="text"
            v-model:value="item.displayName"
            :placeholder="item.displayName"
            class="max-w-32 focus:border-blue-500 focus:shadow-[0_0_0_2px_rgba(24,144,255,0.2)] focus:outline-none"
          />
          <Button v-else @click="changeBtnDisplayName(index)">
            <div class="flex items-center">
              {{ item.displayName }}
              <IconifyIcon icon="lucide:edit" class="ml-2" />
            </div>
          </Button>
        </div>
        <div class="flex w-20 items-center justify-center">
          <Switch
            v-model:checked="item.enable"
            @change="updateElementExtensions"
          />
        </div>
      </div>
    </div>

    <Divider orientation="left">字段权限</Divider>
    <div v-if="formType === BpmModelFormType.NORMAL" class="mt-2 text-sm">
      <!-- 头部标题行 -->
      <div
        class="flex items-center justify-between border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-900"
      >
        <div class="w-28 text-left">字段名称</div>
        <div class="flex flex-1 justify-between">
          <span
            class="inline-block w-24 cursor-pointer text-center hover:text-blue-500"
            @click="updatePermission('READ')"
          >
            只读
          </span>
          <span
            class="inline-block w-24 cursor-pointer text-center hover:text-blue-500"
            @click="updatePermission('WRITE')"
          >
            可编辑
          </span>
          <span
            class="inline-block w-24 cursor-pointer text-center hover:text-blue-500"
            @click="updatePermission('NONE')"
          >
            隐藏
          </span>
        </div>
      </div>

      <!-- 字段权限行 -->
      <div
        v-for="(item, index) in fieldsPermissionEl"
        :key="index"
        class="flex items-center justify-between border border-t-0 border-slate-200 px-3 py-2 text-sm"
      >
        <div class="w-28 truncate text-left" :title="item.title">
          {{ item.title }}
        </div>
        <RadioGroup
          v-model:value="item.permission"
          class="flex flex-1 justify-between"
        >
          <div class="flex w-24 items-center justify-center">
            <Radio
              class="ml-5"
              :value="FieldPermissionType.READ"
              size="large"
              @change="updateElementExtensions"
            >
              <span></span>
            </Radio>
          </div>
          <div class="flex w-24 items-center justify-center">
            <Radio
              class="ml-5"
              :value="FieldPermissionType.WRITE"
              size="large"
              @change="updateElementExtensions"
            >
              <span></span>
            </Radio>
          </div>
          <div class="flex w-24 items-center justify-center">
            <Radio
              class="ml-5"
              :value="FieldPermissionType.NONE"
              size="large"
              @change="updateElementExtensions"
            >
              <span></span>
            </Radio>
          </div>
        </RadioGroup>
      </div>
    </div>

    <Divider orientation="left">是否需要签名</Divider>
    <Form.Item name="signEnable">
      <Switch
        v-model:checked="signEnable.value"
        checked-children="是"
        un-checked-children="否"
        @change="updateElementExtensions"
      />
    </Form.Item>

    <Divider orientation="left">审批意见</Divider>
    <Form.Item name="reasonRequire">
      <Switch
        v-model:checked="reasonRequire.value"
        checked-children="必填"
        un-checked-children="非必填"
        @change="updateElementExtensions"
      />
    </Form.Item>

    <!-- ========== DSL 配置 ========== -->
    <Divider orientation="left">DSL 配置</Divider>
    <Form.Item name="dslEnable" label="启用DSL">
      <Switch
        v-model:checked="dslFormData.enable"
        checked-children="启用"
        un-checked-children="禁用"
        @change="updateElementExtensions"
      />
    </Form.Item>

    <template v-if="dslFormData.enable">
      <!-- 节点能力 -->
      <Form.Item label="节点能力 (cap)">
        <Select
          v-model:value="dslFormData.cap"
          :options="dslCapOptions"
          placeholder="请选择节点能力"
          @change="updateElementExtensions"
        />
      </Form.Item>

      <!-- 可用动作 -->
      <Form.Item label="可用动作">
        <Select
          v-model:value="dslFormData.actions"
          mode="tags"
          :options="dslActionOptions"
          placeholder="输入动作后回车确认"
          @change="updateElementExtensions"
        />
        <div class="form-help-text">多个动作用逗号分隔</div>
      </Form.Item>

      <!-- 角色限制 -->
      <Form.Item label="角色限制">
        <Select
          v-model:value="dslFormData.roles"
          mode="multiple"
          :options="dslRoleOptions"
          placeholder="选择允许操作的角色"
          @change="updateElementExtensions"
        />
      </Form.Item>

      <!-- 任务分配 -->
      <Form.Item label="任务分配">
        <a-row :gutter="8">
          <a-col :span="12">
            <Select
              v-model:value="dslFormData.assignType"
              :options="dslAssignTypeOptions"
              placeholder="分配类型"
              @change="updateElementExtensions"
            />
          </a-col>
          <a-col :span="12">
            <Select
              v-model:value="dslFormData.assignSource"
              :options="dslAssignSourceOptions"
              placeholder="分配来源"
              allowClear
              @change="updateElementExtensions"
            />
          </a-col>
        </a-row>
      </Form.Item>

      <!-- 会签规则（当 cap 为 COUNTERSIGN 时显示） -->
      <template v-if="dslFormData.cap === 'COUNTERSIGN'">
        <Form.Item label="会签规则">
          <Select
            v-model:value="dslFormData.signRule"
            :options="dslSignRuleOptions"
            @change="updateElementExtensions"
          />
        </Form.Item>
        <!-- 专家数量范围 -->
        <Form.Item label="专家数量">
          <a-row :gutter="8">
            <a-col :span="12">
              <a-input-number
                v-model:value="dslFormData.expertMin"
                :min="1"
                placeholder="最少"
                style="width: 100%"
                @change="updateElementExtensions"
              />
            </a-col>
            <a-col :span="12">
              <a-input-number
                v-model:value="dslFormData.expertMax"
                :min="1"
                placeholder="最多"
                style="width: 100%"
                @change="updateElementExtensions"
              />
            </a-col>
          </a-row>
        </Form.Item>
      </template>

      <!-- 选择专家（当 cap 为 EXPERT_SELECT 时显示） -->
      <template v-if="dslFormData.cap === 'EXPERT_SELECT'">
        <Form.Item label="专家数量">
          <a-row :gutter="8">
            <a-col :span="12">
              <a-input-number
                v-model:value="dslFormData.expertMin"
                :min="1"
                placeholder="最少"
                style="width: 100%"
                @change="updateElementExtensions"
              />
            </a-col>
            <a-col :span="12">
              <a-input-number
                v-model:value="dslFormData.expertMax"
                :min="1"
                placeholder="最多"
                style="width: 100%"
                @change="updateElementExtensions"
              />
            </a-col>
          </a-row>
        </Form.Item>
      </template>

      <!-- 补正可修改字段（当 cap 为 MODIFY 或 FILL 时显示） -->
      <template v-if="dslFormData.cap === 'MODIFY' || dslFormData.cap === 'FILL'">
        <Form.Item label="可修改字段">
          <Select
            v-model:value="dslFormData.modifyFields"
            mode="tags"
            placeholder="输入字段名后回车确认"
            @change="updateElementExtensions"
          />
          <div class="form-help-text">允许用户修改的字段列表</div>
        </Form.Item>
      </template>

      <!-- 退回策略（首节点 FILL 不需要退回） -->
      <Form.Item v-if="dslFormData.cap !== 'FILL'" label="退回策略">
        <Select
          v-model:value="dslFormData.backStrategy"
          :options="dslBackStrategyOptions"
          @change="updateElementExtensions"
        />
      </Form.Item>

      <!-- 业务状态 -->
      <Form.Item label="业务状态">
        <Input
          v-model:value="dslFormData.bizStatus"
          placeholder="审批通过后更新的状态值"
          @change="updateElementExtensions"
        />
        <div class="form-help-text">如：PRO_AUDIT, NATION_AUDIT</div>
      </Form.Item>

      <!-- DSL JSON 预览 -->
      <Form.Item label="DSL预览">
        <Input.TextArea
          :value="buildDslJson()"
          :rows="6"
          readonly
          class="dsl-json-preview"
        />
      </Form.Item>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.form-help-text {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.dsl-json-preview {
  font-family: 'Monaco', 'Menlo', 'Ubuntu', 'Consolas', monospace;
  font-size: 11px;
  background: #f5f5f5;
}
</style>
