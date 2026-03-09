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
  Collapse,
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
import { getSimplePostList } from '#/api/system/post';
import { getSimpleRoleList } from '#/api/system/role';
import { getBpmActionList } from '#/api/bpm/action';
import {
  APPROVE_TYPE,
  ApproveType,
  ASSIGN_EMPTY_HANDLER_TYPES,
  ASSIGN_START_USER_HANDLER_TYPES,
  AssignEmptyHandlerType,
  CandidateStrategy,
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

// 任务分配（CandidateStrategy + CandidateParam）
const candidateStrategyEl = ref<any>();
const candidateParamEl = ref<any>();

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

const dslActionOptions = ref<{ label: string; value: string }[]>([]);

// 按钮说明列表（从接口获取）
const bpmActionDescriptions = ref<{ key: string; label: string; bizStatus?: string; bizStatusLabel?: string; bpmAction?: string }[]>([]);

// 新增动作时选择的 key
const newActionKey = ref<string>('');

// 折叠面板展开的 key
const activeActionKeys = ref<string[]>([]);

// 处理添加动作
function handleAddAction(value: string) {
  if (!value) return;
  // 检查是否已存在
  const exists = dslFormData.value.actions.some((a) => a.key === value);
  if (exists) {
    console.warn('动作已存在:', value);
    newActionKey.value = '';
    return;
  }
  // 从 bpmActionDescriptions 中获取默认的 label、bizStatus、bizStatusLabel 和 bpmAction
  const actionDesc = bpmActionDescriptions.value.find((a) => a.key === value);
  const newIndex = dslFormData.value.actions.length;
  dslFormData.value.actions.push({
    key: value,
    label: actionDesc?.label || value,
    bizStatus: actionDesc?.bizStatus || value,
    bizStatusLabel: actionDesc?.bizStatusLabel || actionDesc?.bizStatus || '',
    bpmAction: actionDesc?.bpmAction || '',
  });
  newActionKey.value = '';
  // 自动展开新添加的动作
  activeActionKeys.value = [String(newIndex)];
  updateElementExtensions();
}

// 处理删除动作
function handleRemoveAction(index: number) {
  dslFormData.value.actions.splice(index, 1);
  updateElementExtensions();
}

// 按钮选项加载状态
const bpmActionOptionsLoaded = ref(false);

const dslAssignTypeOptions = [
  { label: '本部门岗位 (DEPT_POST)', value: 'DEPT_POST' },
  { label: '部门负责人 (DEPT_LEADER)', value: 'DEPT_LEADER' },
  { label: '发起人 (START_USER)', value: 'START_USER' },
  { label: '发起人自选 (START_USER_SELECT)', value: 'START_USER_SELECT' },
  { label: '指定用户 (USER)', value: 'USER' },
];

// 岗位列表（用于 DEPT_POST 类型）
const postOptions = ref<{ label: string; value: string }[]>([]);

// 角色列表（用于角色限制）
const roleOptions = ref<{ label: string; value: string }[]>([]);

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

// 角色列表从系统接口获取，见 onMounted

// DSL 配置表单数据
const dslFormData = ref({
  cap: 'AUDIT',
  actions: [
    { key: 'agree', label: '同意', bizStatus: 'agree', bizStatusLabel: '已同意', bpmAction: 'AGREE', vars: {} },
    { key: 'reject', label: '驳回', bizStatus: 'rejected', bizStatusLabel: '已驳回', bpmAction: 'REJECT', vars: {} }
  ] as Array<{ key: string; label?: string; bizStatus?: string; bizStatusLabel?: string; bpmAction?: string; vars?: Record<string, any> }>,
  roles: [] as string[],
  assignType: 'DEPT_POST',
  assignSource: '',
  signRule: 'MAJORITY',
  backStrategy: 'TO_START',
  enable: true,
});

// 解析 DSL JSON
function parseDslJson(jsonStr: string) {
  if (!jsonStr) return;
  try {
    const config = JSON.parse(jsonStr);
    // actions 可能是字符串、简单数组或对象数组，统一转为对象数组供表单使用
    let actionsArr: Array<{ key: string; label?: string; bizStatus?: string; bizStatusLabel?: string; bpmAction?: string; vars?: Record<string, any> }> = [];
    if (Array.isArray(config.actions)) {
      // 检查是否是对象数组（新格式）
      if (config.actions.length > 0 && typeof config.actions[0] === 'object' && config.actions[0].key) {
        // 兼容旧数据：如果没有 label 和 bizStatusLabel，从 bpmActionDescriptions 补充
        actionsArr = config.actions.map((action: any) => {
          const actionDesc = bpmActionDescriptions.value.find((a) => a.key === action.key);
          // 新格式：vars 在 action 对象内部
          // 兼容旧格式：vars 在顶层，先检查 action.vars，没有则用顶层的
          const vars = action.vars || config.vars || {};
          return {
            key: action.key,
            label: action.label || actionDesc?.label || action.key,
            bizStatus: action.bizStatus || actionDesc?.bizStatus || '',
            bizStatusLabel: action.bizStatusLabel || actionDesc?.bizStatusLabel || actionDesc?.bizStatus || '',
            bpmAction: action.bpmAction || actionDesc?.bpmAction || '',
            // 提取 vars 中的特殊字段到顶层供表单使用
            expertMin: vars.expertMin,
            expertMax: vars.expertMax,
            modifyFields: vars.modifyFields || [],
          };
        });
      } else {
        // 简单字符串数组（旧格式），转换为对象数组
        actionsArr = (config.actions as string[]).map((key: string) => {
          // 尝试从 bpmActionDescriptions 中查找对应的 label、bizStatus、bizStatusLabel 和 bpmAction
          const actionDesc = bpmActionDescriptions.value.find((a) => a.key === key);
          return {
            key,
            label: actionDesc?.label || key,
            bizStatus: actionDesc?.bizStatus || '',
            bizStatusLabel: actionDesc?.bizStatusLabel || actionDesc?.bizStatus || '',
            bpmAction: actionDesc?.bpmAction || '',
            vars: {},
          };
        });
      }
    } else if (typeof config.actions === 'string') {
      // 逗号分隔的字符串（旧格式）
      const actionKeys = config.actions.split(',').filter((a: string) => a.trim());
      actionsArr = actionKeys.map((key: string) => {
        const actionDesc = bpmActionDescriptions.value.find((a) => a.key === key);
        return {
          key,
          label: actionDesc?.label || key,
          bizStatus: actionDesc?.bizStatus || '',
          bizStatusLabel: actionDesc?.bizStatusLabel || actionDesc?.bizStatus || '',
          bpmAction: actionDesc?.bpmAction || '',
          vars: {},
        };
      });
    }

    // 根据类型获取不同的 source
    const assignType = config.assign?.type || 'DEPT_POST';
    let assignSource = '';
    if (assignType === 'DEPT_POST') {
      // 兼容旧数据：优先使用 source，其次使用 postCode
      assignSource = config.assign?.source || config.assign?.postCode || '';
    } else if (assignType === 'USER') {
      assignSource = config.assign?.userIds || config.assign?.source || '';
    } else if (assignType === 'START_USER_SELECT') {
      assignSource = config.assign?.userSource || config.assign?.source || '';
    } else {
      assignSource = config.assign?.source || '';
    }

    dslFormData.value = {
      cap: config.cap || 'AUDIT',
      actions: actionsArr,
      roles: config.roles || [],
      assignType,
      assignSource,
      signRule: config.signRule || 'MAJORITY',
      backStrategy: config.backStrategy || 'TO_START',
      enable: config.enable !== false,
    };
  } catch (e) {
    console.error('解析 DSL 配置失败', e);
  }
}

// 切换分配类型时清空二级下拉
function handleAssignTypeChange() {
  dslFormData.value.assignSource = '';
  updateElementExtensions();
}

// 构建 DSL JSON
function buildDslJson(): string {
  const { cap, actions, roles, assignType, assignSource, signRule, backStrategy, enable } = dslFormData.value;

  // actions 转换为带 label、bizStatus、bizStatusLabel、bpmAction 和 vars 的对象数组
  const actionsWithMeta = actions.map((action) => {
    if (typeof action === 'string') {
      // 兼容字符串格式
      const actionDesc = bpmActionDescriptions.value.find((a) => a.key === action);
      return {
        key: action,
        label: actionDesc?.label || action,
        bizStatus: actionDesc?.bizStatus || '',
        bizStatusLabel: actionDesc?.bizStatusLabel || actionDesc?.bizStatus || '',
        bpmAction: actionDesc?.bpmAction || '',
        vars: {},
      };
    }
    // 已有 label、bizStatus、bizStatusLabel、bpmAction、vars 的对象
    // vars 单独提取处理
    const { expertMin, expertMax, modifyFields, ...actionBasic } = action as any;
    const vars: Record<string, any> = {};
    if (expertMin !== undefined) vars.expertMin = expertMin;
    if (expertMax !== undefined) vars.expertMax = expertMax;
    if (modifyFields && modifyFields.length > 0) vars.modifyFields = modifyFields;

    return {
      ...actionBasic,
      vars: Object.keys(vars).length > 0 ? vars : undefined,
    };
  });

  // 构建 assign 对象
  let assign: any = undefined;
  if (assignType === 'DEPT_POST' && assignSource) {
    // DEPT_POST: source 就是岗位ID，后端通过 parent_id 自动查找上级部门
    assign = { type: assignType, source: assignSource };
  } else if (assignType === 'USER' && assignSource) {
    assign = { type: assignType, userIds: assignSource, source: assignSource };
  } else if (assignType === 'START_USER_SELECT' && assignSource) {
    assign = { type: assignType, userSource: assignSource, source: assignSource };
  } else if (assignType) {
    // DEPT_LEADER, START_USER 等无参数的类型
    assign = { type: assignType };
  }

  return JSON.stringify({
    nodeKey: props.id,
    cap,
    actions: actionsWithMeta,
    roles,
    assign,
    signRule: cap === 'COUNTERSIGN' ? signRule : undefined,
    backStrategy: cap !== 'FILL' ? backStrategy : 'NONE',
    enable,
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

  // 任务分配（CandidateStrategy + CandidateParam，用于后端计算候选人）
  candidateStrategyEl.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:CandidateStrategy`,
    )?.[0] ||
    bpmnInstances().moddle.create(`${prefix}:CandidateStrategy`, {
      value: undefined,
    });
  candidateParamEl.value =
    elExtensionElements.value.values?.find(
      (ex: any) => ex.$type === `${prefix}:CandidateParam`,
    )?.[0] ||
    bpmnInstances().moddle.create(`${prefix}:CandidateParam`, {
      value: '',
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
      actions: [
        { key: 'agree', label: '同意', bizStatus: 'agree', bizStatusLabel: '已同意', bpmAction: 'AGREE', vars: {} },
        { key: 'reject', label: '驳回', bizStatus: 'rejected', bizStatusLabel: '已驳回', bpmAction: 'REJECT', vars: {} },
      ],
      roles: [],
      assignType: 'DEPT_POST',
      assignSource: '',
      signRule: 'MAJORITY',
      backStrategy: 'TO_START',
      enable: false,
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
  console.log('[updateElementExtensions] 当前节点 dslFormData:', JSON.stringify(dslFormData.value));

  // 更新候选人策略：根据 DSL assignType 设置 CandidateStrategy 和 CandidateParam
  const { assignType, assignSource } = dslFormData.value;
  let strategyValue: number | undefined;
  let paramValue = '';

  if (assignType === 'DEPT_POST' && assignSource) {
    // DEPT_POST: 策略值为 24，参数为岗位ID
    strategyValue = CandidateStrategy.DEPT_POST; // 24
    paramValue = assignSource; // 岗位ID
  } else if (assignType === 'DEPT_LEADER') {
    strategyValue = CandidateStrategy.DEPT_LEADER; // 21
  } else if (assignType === 'START_USER') {
    strategyValue = CandidateStrategy.START_USER; // 36
  } else if (assignType === 'USER') {
    strategyValue = CandidateStrategy.USER; // 30
    paramValue = assignSource; // 用户ID
  }

  // 更新 CandidateStrategy
  if (candidateStrategyEl.value) {
    candidateStrategyEl.value.value = strategyValue;
  }
  // 更新 CandidateParam
  if (candidateParamEl.value) {
    candidateParamEl.value.value = paramValue;
  }

  console.log('[updateElementExtensions] candidateStrategy:', strategyValue, 'candidateParam:', paramValue);

  const extensions = bpmnInstances().moddle.create('bpmn:ExtensionElements', {
    values: [
      ...otherExtensions.value,
      assignStartUserHandlerTypeEl.value,
      rejectHandlerTypeEl.value,
      returnNodeIdEl.value,
      assignEmptyHandlerTypeEl.value,
      assignEmptyUserIdsEl.value,
      approveType.value,
      candidateStrategyEl.value,
      candidateParamEl.value,
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
  // 获得岗位列表（用于 DEPT_POST 类型）
  const postList = await getSimplePostList();
  postOptions.value = postList.map((post: any) => ({
    // 固定使用 id 作为值
    label: `${post.name}${post.code ? ` (${post.code})` : ''}`,
    value: post.id?.toString(),
  }));
  // 获得角色列表（用于角色限制）
  const roleList = await getSimpleRoleList();
  roleOptions.value = roleList.map((role: any) => ({
    label: role.name,
    value: role.code,
  }));

  // 加载按钮选项（从接口获取）
  try {
    const actionList = await getBpmActionList();
    bpmActionDescriptions.value = actionList;
    dslActionOptions.value = actionList.map((action: any) => ({
      label: `${action.label} (${action.key})`,
      value: action.key,
    }));
    bpmActionOptionsLoaded.value = true;
  } catch (error) {
    console.error('加载按钮选项失败:', error);
  }
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
      <div class="mb-3">
        <div class="ant-form-item-label" style="margin-bottom: 10px;">
          <label class="ant-form-item-required">可用动作</label>
        </div>
        <!-- 添加动作按钮 -->
        <div class="mb-2">
          <Select
            v-model:value="newActionKey"
            :options="dslActionOptions"
            placeholder="选择动作添加"
            style="width: 200px"
            @change="handleAddAction"
          >
            <template #suffixIcon><span class="text-blue-500 cursor-pointer">+ 添加</span></template>
          </Select>
        </div>

        <!-- 动作列表折叠面板 -->
        <div v-if="dslFormData.actions && dslFormData.actions.length > 0" class="mb-3">
          <Collapse v-model:activeKey="activeActionKeys" :bordered="false" class="action-collapse">
            <Collapse.Panel
              v-for="(action, index) in dslFormData.actions"
              :key="index"
              :header="`${action.label || action.key} (${action.key})`"
            >
              <template #extra>
                <Button
                  type="link"
                  danger
                  size="small"
                  @click.stop="handleRemoveAction(index)"
                >
                  删除
                </Button>
              </template>

              <!-- 动作配置表单 -->
              <div class="space-y-3">
                <!-- 动作显示名称 -->
                <div class="flex items-center">
                  <span class="w-24 text-xs text-gray-500">动作显示名称</span>
                  <Input
                    v-model:value="action.label"
                    placeholder="如: 同意"
                    size="small"
                    @change="updateElementExtensions"
                  />
                </div>

                <!-- 业务状态 (值 + 显示名) -->
                <div class="flex items-center">
                  <span class="w-24 text-xs text-gray-500">业务状态</span>
                  <div class="flex items-center gap-2">
                    <Input
                      v-model:value="action.bizStatus"
                      placeholder="值 如: agree"
                      size="small"
                      style="width: 120px"
                      @change="updateElementExtensions"
                    />
                    <Input
                      v-model:value="action.bizStatusLabel"
                      placeholder="显示名 如: 已同意"
                      size="small"
                      style="width: 120px"
                      @change="updateElementExtensions"
                    />
                  </div>
                </div>

                <!-- BPM 操作 -->
                <div class="flex items-center">
                  <span class="w-24 text-xs text-gray-500">BPM 操作</span>
                  <Input
                    v-model:value="action.bpmAction"
                    placeholder="如: AGREE"
                    size="small"
                    @change="updateElementExtensions"
                  />
                </div>

                <!-- action.vars 配置区域 -->
                <!-- 选择专家 (selectExpert) 需要配置专家数量 -->
                <template v-if="action.key === 'selectExpert'">
                  <div class="border border-gray-200 rounded p-2 mt-2">
                    <div class="text-xs font-medium text-gray-600 mb-2">专家数量配置</div>
                    <div class="flex items-center gap-2">
                      <span class="text-xs text-gray-500 w-16">最少:</span>
                      <a-input-number
                        v-model:value="action.expertMin"
                        :min="1"
                        placeholder="最少"
                        size="small"
                        style="width: 80px"
                        @change="updateElementExtensions"
                      />
                      <span class="text-xs text-gray-500 w-16 ml-2">最多:</span>
                      <a-input-number
                        v-model:value="action.expertMax"
                        :min="1"
                        placeholder="最多"
                        size="small"
                        style="width: 80px"
                        @change="updateElementExtensions"
                      />
                    </div>
                  </div>
                </template>

                <!-- 补正/填报 (modify/fill) 需要配置可修改字段 -->
                <template v-if="action.key === 'modify' || action.key === 'fill'">
                  <div class="border border-gray-200 rounded p-2 mt-2">
                    <div class="text-xs font-medium text-gray-600 mb-2">可修改字段</div>
                    <Select
                      v-model:value="action.modifyFields"
                      mode="tags"
                      placeholder="输入字段名后回车确认"
                      size="small"
                      @change="updateElementExtensions"
                    />
                    <div class="text-xs text-gray-400 mt-1">允许用户修改的字段列表</div>
                  </div>
                </template>
              </div>
            </Collapse.Panel>
          </Collapse>
        </div>
        <div v-else class="text-gray-400 text-xs py-2">请添加动作</div>
      </div>

      <!-- 按钮说明 -->
      <div v-if="bpmActionDescriptions.length > 0" class="mb-4">
        <div class="mb-2 text-xs font-medium text-gray-500">按钮说明</div>
        <div class="max-h-48 overflow-y-auto rounded border border-gray-200">
          <table class="w-full text-xs">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-2 py-1 text-left font-medium text-gray-600">按钮</th>
                <th class="px-2 py-1 text-left font-medium text-gray-600">业务状态</th>
                <th class="px-2 py-1 text-left font-medium text-gray-600">BPM操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="action in bpmActionDescriptions" :key="action.key" class="border-t border-gray-100 hover:bg-gray-50">
                <td class="px-2 py-1">{{ action.label }} ({{ action.key }})</td>
                <td class="px-2 py-1">
                  <span v-if="action.bizStatus" class="text-green-600">
                    {{ action.bizStatusLabel || action.bizStatus }}({{ action.bizStatus }})
                  </span>
                  <span v-else class="text-gray-400">-</span>
                </td>
                <td class="px-2 py-1">
                  <span v-if="action.bpmAction" class="text-blue-600">
                    {{ action.bpmAction }}
                  </span>
                  <span v-else class="text-gray-400">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 角色限制 -->
      <Form.Item label="角色限制">
        <Select
          v-model:value="dslFormData.roles"
          mode="multiple"
          :options="roleOptions"
          placeholder="选择允许操作的角色"
          @change="updateElementExtensions"
        />
      </Form.Item>

      <!-- 任务分配 -->
      <Form.Item label="任务分配">
        <!-- 第一级：分配类型 -->
        <a-row :gutter="8">
          <a-col :span="12">
            <Select
              v-model:value="dslFormData.assignType"
              :options="dslAssignTypeOptions"
              placeholder="分配类型"
              @change="handleAssignTypeChange"
            />
          </a-col>
          <!-- 第二级：根据类型显示不同选项 -->
          <a-col :span="12">
            <!-- DEPT_POST: 显示岗位列表 -->
            <Select
              v-if="dslFormData.assignType === 'DEPT_POST'"
              v-model:value="dslFormData.assignSource"
              :options="postOptions"
              placeholder="选择岗位"
              allowClear
              @change="updateElementExtensions"
            />
            <!-- USER: 显示用户列表 -->
            <Select
              v-else-if="dslFormData.assignType === 'USER'"
              v-model:value="dslFormData.assignSource"
              :options="userOptions"
              placeholder="选择用户"
              allowClear
              @change="updateElementExtensions"
            />
            <!-- START_USER_SELECT: 显示用户来源 -->
            <Select
              v-else-if="dslFormData.assignType === 'START_USER_SELECT'"
              v-model:value="dslFormData.assignSource"
              :options="dslAssignSourceOptions.filter(o => o.value === 'startUser' || o.value === 'startUserDeptLeader' || o.value === 'expertUsers')"
              placeholder="选择用户来源"
              allowClear
              @change="updateElementExtensions"
            />
            <!-- DEPT_LEADER, START_USER 无需选择 -->
            <Select
              v-else-if="dslFormData.assignType === 'DEPT_LEADER' || dslFormData.assignType === 'START_USER'"
              :value="dslFormData.assignSource"
              disabled
              placeholder="无需配置"
            />
            <!-- 其他类型 -->
            <Select
              v-else-if="dslFormData.assignType"
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
      </template>

      <!-- 退回策略（首节点 FILL 不需要退回） -->
      <Form.Item v-if="dslFormData.cap !== 'FILL'" label="退回策略">
        <Select
          v-model:value="dslFormData.backStrategy"
          :options="dslBackStrategyOptions"
          @change="updateElementExtensions"
        />
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

// 动作折叠面板样式
.action-collapse {
  :deep(.ant-collapse-header) {
    padding: 8px 12px !important;
  }

  :deep(.ant-collapse-content-box) {
    padding: 12px !important;
  }

  :deep(.ant-collapse-expand-icon) {
    order: -1;
    margin-right: 8px;
  }

  :deep(.ant-collapse-header-text) {
    flex: 1;
  }

  :deep(.ant-collapse-extra) {
    margin-left: auto;
  }
}
</style>
