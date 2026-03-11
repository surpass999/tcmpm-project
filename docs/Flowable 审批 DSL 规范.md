# Flowable 审批 DSL 规范（完整版）

## 目标

* 前端 100% 由 DSL 驱动
* 会签 / 退回 / 补正全部由流程配置决定
* 兼容 9 个菜单所有审批场景

## 一、DSL 总体结构

```json
{
  "nodeKey": "EXPERT_SELECT",
  "cap": "EXPERT_SELECT",
  "actions": "selectExpert,submit",
  "roles": ["PROVINCE","HOSP","EXPERT","NATION"],
  "assign": {
    "type": "DEPT_POST",
    "source": "2"
  },
  "signRule": "ALL | ANY | MAJORITY | 2/3 | CUSTOM",
  "backStrategy": "TO_START | TO_PREV | TO_ANY | TO_ROLE",
  "reSubmit": "RESTART | RESUME",
  "bizStatus": "PRO_AUDIT",
  "vars": {},
  "ui": {
    "form": "expertSelect",
    "readonly": false
  }
}
```

## 二、cap（节点能力）—— 最核心

### 审批类

| cap            | 说明   |
| -------------- | ---- |
| AUDIT          | 单人审批 |
| COUNTERSIGN    | 会签   |
| EXPERT_SELECT  | 选择专家 |
| FILL           | 填报   |
| MODIFY         | 补正   |
| CONFIRM        | 确认   |
| ARCHIVE        | 归档   |
| PUBLISH        | 发布   |

## 三、actions（全部按钮定义）

### 1. 流程控制

| key          | 含义   | 默认 bizStatus | 说明 |
| ------------ | ------ | ---------------| ---- |
| submit       | 提交   | SUBMITTED      | 提交到下一步 |
| agree        | 通过   | APPROVED       | 审批通过 |
| reject       | 拒绝   | REJECTED       | 审批拒绝 |
| back         | 退回   | RETURNED       | 退回修改 |
| selectExpert | 选择专家 | EXPERT_REVIEWING | 选择专家 |
| cancel       | 撤回   | CANCELLED      | 撤回 |
| transfer     | 转办   | (不改变)        | 转办他人 |
| delegate     | 委派   | (不改变)        | 委派他人 |
| suspend      | 挂起   | SUSPENDED      | 挂起流程 |
| resume       | 恢复   | (恢复)          | 恢复流程 |

### 2. 会签专用

| key          | 含义     | 默认 bizStatus | 说明 |
| ------------ | -------- | ---------------| ---- |
| addSign      | 加签     | (不改变)        | 增加会签人 |
| reduceSign   | 减签     | (不改变)        | 减少会签人 |
| signAgree    | 会签通过 | SIGN_APPROVED  | 会签同意 |
| signReject   | 会签拒绝 | SIGN_REJECTED  | 会签拒绝 |
| urge         | 催办     | (不改变)        | 催办 |

### 3. 业务专用

| key          | 含义   | 默认 bizStatus | 说明 |
| ------------ | ------ | ---------------| ---- |
| selectExpert | 选择专家 | EXPERT_REVIEWING | 选择评审专家 |
| reSelect     | 重选   | EXPERT_REVIEWING | 重新选择专家 |
| modify       | 修改   | MODIFYING      | 补正修改 |
| confirm      | 确认   | CONFIRMED      | 确认 |
| archive      | 归档   | ARCHIVED       | 归档 |
| toProject    | 转项目 | (业务自定义)    | 转为项目 |
| publish      | 发布   | PUBLISHED      | 发布 |

## 四、assign（任务分配）

### 配置示例

```json
{
  "assign": {
    "type": "DEPT_POST",
    "source": "2"
  }
}
```

### 类型说明

| type           | 说明                    | 参数                          |
| -------------- | --------------------- | --------------------------- |
| STATIC_ROLE    | 固定角色                | `source`: 角色编码列表        |
| DEPT_POST      | 本部门岗位            | `source`: 岗位ID              |
| DEPT_LEADER    | 部门负责人              | 无参数，自动查找                   |
| START_USER     | 发起人本人              | 无参数                         |
| START_USER_SELECT | 发起人自选           | `source`: 用户来源           |
| DYNAMIC_USER   | 运行时用户              | `source`: expertUsers/startUserDeptLeader |
| USER           | 指定用户                | `source`: 用户ID列表            |
| EXPRESSION     | 流程表达式              | `source`: 表达式            |

### 来源说明（source）

根据 type 的不同类型，source的值代表不同的数据

### 前端联动示例

```
┌────────────────────────────────────────────────────────────────┐
│  分配类型：  [本部门岗位               ▼]                       │
│                                                             │
│  岗  位：    [项目管理处              ▼]                       │
│              ↑                                                │
│              └── 只显示岗位，不区分部门                         │
│                  运行时自动注入：上一节点用户所在部门的           │
│                  上级部门，然后在该部门下查找对应岗位的用户        │
└────────────────────────────────────────────────────────────────┘
```

前端两联动说明：
1. 第一个下拉框选择"分配类型"（如本部门岗位）
2. 第二个下拉框根据类型显示不同选项（如岗位列表）
3. 保存时参数格式：`source`（岗位ID）

**运行时逻辑**：
- 找到上一个完成任务的用户
- 获取他的所在部门的**上级部门**
- 在该上级部门下查找有指定岗位的用户

## 五、signRule（会签规则）

| 规则       | 含义   |
| -------- | ---- |
| ALL      | 全部   |
| ANY      | 任一   |
| MAJORITY | 多数   |
| 2/3      | 三分之二 |
| CUSTOM   | 自定义  |

## 六、backStrategy（退回）

| 策略        |
| --------- |
| TO_START |
| TO_PREV  |
| TO_ANY   |
| TO_ROLE  |

## 七、vars（关键变量）

### 1. 专家相关

```json
{
  "targetVar": "expertUsers",
  "min": 1,
  "max": 7
}
```

### 2. 补正

```json
{
  "modifyFields": ["name", "file"]
}
```

## 八、九菜单节点示例

### 1. 备案→省局审核

```json
{
  "cap": "AUDIT",
  "actions": "agree,reject,back",
  "assign": {
    "type": "DEPT_POST",
    "source": "3"
  },
  "bizStatus": "PRO_AUDIT"
}
```

> 说明：上海某医院提交备案 → 自动找上海市卫生健康委员会的项目管理员审批

### 2. 备案→国家局审核

```json
{
  "cap": "AUDIT",
  "actions": "agree,reject",
  "assign": {
    "type": "DEPT_POST",
    "source": "2"
  },
  "bizStatus": "NATION_AUDIT"
}
```

### 3. 选择专家

```json
{
  "cap": "EXPERT_SELECT",
  "actions": "selectExpert,submit",
  "assign": {
    "type": "START_USER_SELECT",
    "source": "expertUsers"
  },
  "vars": {
    "min": 2,
    "max": 5
  }
}
```

### 4. 会签（多专家评审）

```json
{
  "nodeKey": "Activity_0isavu8",
  "cap": "COUNTERSIGN",
  "actions": [
    {
      "key": "signAgree",
      "label": "同意",
      "bizStatus": "EXPERT_APPROVED",
      "bizStatusLabel": "专家已同意",
      "bpmAction": "complete",
      "vars": {}
    },
    {
      "key": "signReject",
      "label": "拒绝",
      "bizStatus": "EXPERT_REJECTED",
      "bizStatusLabel": "专家已拒绝",
      "bpmAction": "reject",
      "vars": {"reason": "审核意见", "reasonRequired": true}
    }
  ],
  "assign": {
    "type": "DYNAMIC_USER",
    "source": "expertUsers"
  },
  "signRule": "MAJORITY",
  "rejectRule": "ANY_REJECT"
}
```

**配置说明**：

| 字段 | 说明 | 可选值 |
|------|------|--------|
| `signRule` | 会签通过规则 | `ALL` / `ANY` / `MAJORITY` / `2/3` |
| `rejectRule` | 会签拒绝规则 | `ANY_REJECT`（默认，任一拒绝）/ `ALL_REJECT`（全部拒绝）|
| `actions[].bizStatus` | 该动作执行后的业务状态 | 业务定义的状态码 |

**说明**：通过 `signAgree` 和 `signReject` 动作单独配置 `bizStatus`，可以灵活控制会签通过和拒绝后的业务状态。

### 5. 固定角色（传统方式）

```json
{
  "cap": "AUDIT",
  "actions": "agree,reject",
  "assign": {
    "type": "STATIC_ROLE",
    "source": ["PROVINCE"]
  },
  "bizStatus": "PRO_AUDIT"
}
```

## 九、前端解释规范

### 1. 按钮渲染

```javascript
buttons = actions.split(',')
```

### 2. 会签判断

```
cap == COUNTERSIGN:
  显示 signAgree / signReject 按钮
```

### 3. 选专家

```
cap == EXPERT_SELECT:
  打开专家选择器
```

### 4. assign 联动

```javascript
// 根据 type 动态显示第二个下拉框
switch (assign.type) {
  case 'STATIC_ROLE':
    show: 角色列表
  case 'DEPT_POST':
    show: 岗位列表（不区分部门，运行时自动找发起人所属省局）
  case 'USER':
    show: 用户列表
  case 'USER_GROUP':
    show: 用户组列表
}
```

## 十、与 Flowable 的映射

* DSL → extensionElements
* expertUsers → 多实例
* signRule → 完成条件

## 十一、部门岗位联动执行逻辑

### 1. 执行流程

```
1. 获取发起人信息
   └── 从流程变量获取 startUserId

2. 获取发起人所属部门
   └── 从用户信息获取 deptId

3. 向上查找目标层级部门
   └── 根据 用户部门 遍历 parent_id 树
       - 查找上一个部门

4. 查询该部门下的岗位用户
   └── SELECT u.id FROM system_user u
       JOIN system_user_post up ON u.id = up.user_id
       JOIN system_post p ON up.post_id = p.id
       WHERE u.dept_id = targetDeptId
         AND p.code = source
```

### 2. 降级处理

当目标部门下没有该岗位的用户时：
1. 查找该部门的部门负责人（leader_user_id）
2. 如果仍没有，查找上级部门

### 3. 部门层级判断方式（使用 parent_id 递归查找）

由于 `system_dept` 表没有 `level` 字段，通过 `parent_id` 递归向上查找目标层级部门。

#### 3.1 递归查找逻辑

```
查找流程：
┌─────────────────────────────────────────────────────────────┐
│  上海某医院部门 (deptId = 103，parent_id = 101)           │
│       ↓                                                   │
│  上海市卫生健康委员会 (deptId = 101，parent_id = 0) ← 找到！│
└─────────────────────────────────────────────────────────────┘
```

## 十二、流程引擎整体设计

### 1. 核心表结构

#### 1.1 bpm_business_type（流程业务类型配置）

| 字段 | 类型 | 说明 |
|-----|------|------|
| business_type | varchar | 业务类型，如 `declare:filing:submit` |
| name | varchar | 流程名称 |
| process_definition_key | varchar | Flowable 流程定义Key |
| enabled | int | 是否启用 |

**配置示例**：

| business_type | process_definition_key |
|--------------|----------------------|
| declare:filing:submit | projectFiling |

#### 1.2 declare_business_process（业务与流程关联）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | bigint | 主键 |
| business_type | varchar | 业务类型 |
| business_id | bigint | 业务ID（如备案ID） |
| process_instance_id | varchar | Flowable 流程实例ID |
| process_definition_key | varchar | 流程定义Key |
| current_node_key | varchar | 当前节点Key |
| current_status | varchar | 当前流程状态 |
| initiator_id | bigint | 发起人ID |
| initiator_ids | text | 参与者ID列表，逗号分隔 |
| current_assign_type | varchar | 分配类型：DEPT_POST / USER / START_USER / DEPT_LEADER 等 |
| current_assign_source | varchar | 分配来源：岗位ID / 用户ID / 角色编码 等 |
| start_time | datetime | 开始时间 |
| end_time | datetime | 结束时间 |
| result | varchar | 流程结果（agree/reject/back） |

### 2. 流程启动流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          流程启动流程                                    │
└─────────────────────────────────────────────────────────────────────────┘

1. 用户调用接口（如提交备案）
   │
   ▼
2. Controller 方法带 @DeclareProcess 注解
   │   - businessType 自动解析（如 declare:filing:submit）
   │   - 也可以手动指定
   ▼
3. AOP 拦截（DeclareProcessAspect）
   │   - 解析 businessType
   │   - 执行业务方法
   │   - 调用 processService.startProcessIfConfigured()
   ▼
4. 查询流程配置（bpm_business_type 表）
   │   - 根据 businessType 查询
   │   - 获取 process_definition_key
   ▼
5. 启动 Flowable 流程
   │   - runtimeService.startProcessInstanceByKey()
   │   - 传入流程变量：businessType, businessId, initiator
   ▼
6. 创建业务流程关联记录
   │   - 写入 declare_business_process 表
   │   - 记录 processInstanceId, processDefinitionKey
   │   - 记录 initiator_id, initiator_ids
   ▼
7. Flowable 自动创建第一个用户任务
   │   - 根据 BPMN 中的 UserTask 配置
   │   - 调用 BpmTaskCandidateInvoker 计算候选人
   │   - 根据 DSL 的 assign 配置确定任务处理人
   ▼
8. 任务完成
   │   - 更新 declare_business_process.current_node_key
   │   - 更新 current_assign_type/source
   │   - 追加当前用户到 initiator_ids
   │   - 根据下一个节点的 DSL 配置创建新任务
   ▼
9. 流程结束
   │   - 更新 declare_business_process.end_time
   │   - 更新 declare_business_process.result
   ▼
结束
```

### 3. 任务分配执行时机

任务分配（候选人计算）发生在 **Flowable 创建用户任务时**：

```
Flowable 创建任务
    │
    ▼
BpmUserTaskActivityBehavior.handleAssignments()
    │
    ▼
BpmTaskCandidateInvoker.calculateUsersByTask(execution)
    │
    ▼
解析 BPMN 节点 DSL 配置
    │   - 获取 assign.type（如 DEPT_POST）
    │   - 获取 assign.source（如岗位ID）
    ▼
调用对应策略计算候选人
    │   - DEPT_POST → BpmTaskCandidateDeptPostStrategy
    │   - START_USER → BpmTaskCandidateStartUserStrategy
    │   - USER → BpmTaskCandidateUserStrategy
    ▼
设置任务候选人 / 处理人
```

### 4. assign 配置的是当前节点的处理人规则

**重要说明**：DSL 中的 `assign` 配置定义的是**当前节点**的任务处理人规则，供 `BpmTaskCandidateInvoker` 在**任务创建时**使用。

**核心逻辑**：
- 根据 `assign.type` 类型，从**上一个完成任务的人**所在部门向上查找目标部门
- 在目标部门下根据 `assign.source`（岗位ID/用户ID等）查找处理人

```
当前节点：省局审核
  assign: { type: DEPT_POST, source: "2" }
  → 找上一个完成任务的人（医院）的上级部门（省局）+ 岗位ID=2 的人来处理这个任务

当前节点：国家局审核
  assign: { type: DEPT_POST, source: "1" }
  → 找上一个完成任务的人（省局）的上级部门（国家局）+ 岗位ID=1 的人来处理这个任务
```

#### 4.1 assign.type 详解

| assign.type | source | 处理逻辑 |
|------------|--------|---------|
| DEPT_POST | 岗位ID | 找上一个完成任务的人的上级部门，在该部门下查找有指定岗位ID的用户 |
| DEPT_LEADER | 无 | 找上一个完成任务的人的上级部门，查找该部门的负责人 |
| START_USER | 无 | 发起人本人处理 |
| USER | 用户ID列表 | 指定的用户处理 |
| START_USER_SELECT | 用户来源 | 发起人自选用户处理 |
| EXPRESSION | 表达式 | 通过表达式计算处理人 |

#### 4.2 assign.type 与 source 对照表

| assign.type | source 含义 | 示例 |
|-------------|-------------|------|
| DEPT_POST | 岗位ID | "2" |
| USER | 用户ID列表（逗号分隔） | "101,102,103" |
| DEPT_LEADER | 无 | - |
| START_USER | 无 | - |
| START_USER_SELECT | 用户来源 | "formField" |
| EXPRESSION | 表达式 | "${var}" |

### 5. 列表页面按钮显示

备案列表页面需要根据当前节点的 DSL 配置，动态显示操作按钮：

```
查询条件：
1. declare_business_process.current_node_key
2. 根据 process_definition_key 获取 BPMN 模型
3. 解析当前节点的 DSL 配置
4. 根据 dsl.actions 显示对应按钮
5. 根据 dsl.roles 过滤用户权限
```

**按钮映射**：

| actions 值 | 显示按钮 | 说明 |
|-----------|---------|------|
| submit | 提交审核 | 提交到下一步审批 |
| agree | 通过 | 审批通过 |
| reject | 拒绝 | 审批拒绝 |
| back | 退回 | 退回修改 |
| selectExpert | 选择专家 | 选择评审专家 |

### 6. 完整示例：医院提交备案

**Step 1：配置流程**

bpm_business_type 表：

| business_type | process_definition_key |
|--------------|----------------------|
| declare:filing:create | projectFiling |

BPMN 流程设计：

```
开始 → 医院提交(FILL) → 省局审核(AUDIT) → 国家局审核(AUDIT) → 结束
```

**Step 2：医院创建备案**

1. 医院用户调用 `POST /declare/filing/create`
2. AOP 拦截 `@DeclareProcess`，触发启动 Flowable 流程
3. 写入业务数据到 `declare_filing` 表
4. 写入 `declare_business_process` 表（记录流程实例、当前节点、发起人等）
5. Flowable 自动创建第一个用户任务"医院提交"
6. 根据 DSL `assign: { "type": "START_USER" }`，任务直接分配给医院用户

**医院看到的 DSL**：

```json
{
  "nodeKey": "Activity_1ftwrrv",
  "cap": "FILL",
  "actions": "submit",
  "roles": ["HOSPITAL"],
  "assign": { "type": "START_USER" }
}
```

**列表按钮**：显示"提交审核"按钮

**Step 3：医院提交审核**

1. 医院点击"提交审核"按钮，弹出提交弹窗
2. 填写完提交后，调用后端完成当前任务接口
3. Flowable 完成当前任务，推进到下一个节点"省局审核"
4. **任务完成监听器**触发：
   - 解析下一个节点 DSL，获取 `assign.type` 和 `assign.source`
   - 更新 `declare_business_process` 表：
     - `current_node_key` → 省局审核节点 Key
     - `current_assign_type` → DEPT_POST
     - `current_assign_source` → 2（岗位ID）
     - `initiator_ids` 追加当前用户ID
   - 同步更新业务表 `declare_filing` 状态为省局审核中
5. Flowable 根据新的 DSL 计算省局审批人，创建新任务

**Step 4：省局审核**

1. 省局用户登录后，在备案列表通过 declare_business_process 表的 current_assign_type 、current_assign_source 查询判断看到该条数据
2. 根据 `declare_business_process.current_node_key` 获取当前节点
3. 解析 BPMN 模型获取 DSL 配置
4. 根据 DSL 的 `roles` 判断当前用户是否有权限操作
5. 根据 DSL 的 `actions` 显示对应按钮

**省局看到的 DSL**：

```json
{
  "nodeKey": "Activity_province_audit",
  "cap": "AUDIT",
  "actions": "agree,reject,back",
  "roles": ["PROVINCE"],
  "assign": { "type": "DEPT_POST", "source": "2" }
}
```

**列表按钮**：显示"通过"、"拒绝"、"退回"按钮

---

## 十三、评审列表方案（专家专用）

### 13.1 方案背景

原方案中，备案列表需要根据 `current_assign_type/source` 复杂过滤，且专家用户难以区分。

**新方案**：将审批功能拆分为两个独立列表：
1. **备案列表** - 所有用户查看自己参与过的备案
2. **评审列表** - 专家专用，类似 BPM 待办任务

### 13.2 列表拆分

| 列表 | 访问用户 | 功能 |
|-----|---------|------|
| 备案列表 | 医院、省局、国家局 | 查看参与的备案，只读，显示流程状态 |
| 评审列表 | 专家 | 待审批任务，有审批按钮 |

### 13.3 评审列表实现

```java
// 专家评审列表 API
GET /admin-api/declare/review/page

// 返回参数
{
  "taskId": "xxx",           // 任务ID，审批时需要传回
  "taskName": "专家会签",      // 任务名称
  "filingId": "123",         // 备案ID
  "filingTitle": "xxx项目",   // 备案标题
  "status": "待评审",          // 状态
  "assignee": "101",         // 分配给专家
  "createTime": "2026-01-01" // 任务创建时间
}
```

**实现逻辑**：直接查询 Flowable 任务表 `act_ru_task`，根据 `assignee_` 或 `act_ru_identitylink` 获取专家待办任务。

### 13.4 评审列表的数据权限

评审列表是**独立的**，直接查询 Flowable 任务表，无需通过 `current_assign_type/source` 过滤：

```sql
-- 专家评审列表：直接查 Flowable 任务表
SELECT * FROM act_ru_task
WHERE assignee_ = '101'  -- 当前专家

-- 或者查候选任务
SELECT * FROM act_ru_identitylink
WHERE user_id_ = '101'
```

### 13.5 备案列表查询逻辑

```sql
-- 用户查询自己参与过的备案（不区分角色）
SELECT * FROM declare_filing f
JOIN declare_business_process p ON f.id = p.business_id
WHERE
  p.initiator_id = #{userId}  -- 自己发起的
  OR p.initiator_ids LIKE '%用户ID%'  -- 参与者（自己参与过的节点）
  OR f.dept_id IN (用户所属部门及下级)  -- 部门权限
```

---

## 十四、initiator_ids 字段（解决参与者可见性）

### 14.1 问题场景

```
省局选择专家前：
  current_assign_type: DEPT_POST
  current_assign_source: "2"（省局岗位）
  → 省局能看见 ✓

省局选择专家后：
  current_assign_type: USER
  current_assign_source: "101,102,103"（3个专家）
  → 省局看不见了 ✗（因为不是这3个专家）
```

### 14.2 解决方案

在 `declare_business_process` 表新增 `initiator_ids` 字段，记录所有参与者：

| 字段 | 类型 | 说明 |
|-----|------|------|
| initiator_ids | text | 参与者ID列表，逗号分隔 |

### 14.3 更新逻辑

```java
// 任务完成时，追加当前用户到 initiator_ids
public void updateProcessInstance(DelegateTask task) {
    Long userId = getCurrentUserId();
    String processInstanceId = task.getProcessInstanceId();

    // 追加用户ID到 initiator_ids
    DeclareBusinessProcessDO process = getByProcessInstanceId(processInstanceId);
    String initiatorIds = process.getInitiatorIds();
    if (initiatorIds == null) {
        initiatorIds = userId.toString();
    } else if (!initiatorIds.contains(userId.toString())) {
        initiatorIds += "," + userId;
    }
    process.setInitiatorIds(initiatorIds);
    updateById(process);
}
```

### 14.4 数据变化示例

| 操作 | initiator_ids | current_assign_type | current_assign_source |
|-----|---------------|---------------------|----------------------|
| 医院提交 | "1001" | DEPT_POST | "2" |
| 省局选择专家 | "1001,1002" | USER | "101,102,103" |
| 专家1会签 | "1001,1002,101" | ... | ... |

### 14.5 完整查询逻辑

```java
// 备案列表查询 - 综合权限判断
public List<FilingDO> getFilingsForCurrentUser(LoginUser user) {
    Long userId = user.getId();
    Long deptId = user.getDeptId();

    // 1. 部门权限 - 用户所属部门及下级
    List<Long> deptIds = deptService.getDeptAndChildren(deptId);

    // 2. 获取用户关联的岗位ID列表
    List<Long> userPostIds = userPostMapper.getPostIdsByUserId(userId);

    // 3. 查询 declare_business_process
    List<DeclareBusinessProcessDO> processes = businessProcessMapper.selectList(
        new QueryWrapper<DeclareBusinessProcessDO>()
            // 条件A：部门权限
            .in("dept_id", deptIds)
            // 条件B：发起人或参与者
            .and(w -> w
                .eq("initiator_id", userId)  // 自己发起的
                .or()
                .like("initiator_ids", userId)  // 自己参与过的
            )
            // 条件C：当前审批人（如果有岗位）
            .and(w -> w
                .isNull("current_assign_type")  // 无分配类型
                .or()
                .and(o -> o  // 有分配类型且匹配
                    .eq("current_assign_type", "DEPT_POST")
                    .in("current_assign_source", userPostIds)
                )
                .or(o -> o
                    .eq("current_assign_type", "USER")
                    .like("current_assign_source", userId.toString())
                )
                .or(o -> o
                    .eq("current_assign_type", "DEPT_LEADER")
                )
            )
            .isNull("end_time")  // 流程未结束
    );

    // 4. 返回业务数据
    List<Long> businessIds = processes.stream()
        .map(DeclareBusinessProcessDO::getBusinessId)
        .collect(Collectors.toList());

    if (businessIds.isEmpty()) {
        return Collections.emptyList();
    }

    return filingMapper.selectBatchIds(businessIds);
}
```

---

## 十五、数据权限过滤（按岗位精准过滤）

### 15.1 适用场景

| assign.type | assign.source | 过滤逻辑 |
|------------|--------------|---------|
| DEPT_POST | 岗位ID | 用户的岗位ID 在 current_assign_source 中 |
| USER | 用户ID | 用户ID = current_assign_source |
| START_USER | 无 | 只有发起人能看到（businessId + initiatorId 匹配） |
| DEPT_LEADER | 无 | 用户是该部门的负责人 |
| STATIC_ROLE | 角色编码 | 用户拥有该角色 |

这样设计后：
- **省局用户**（岗位ID=2）只能看到「省局待审批」的数据
- **国家局用户**（岗位ID=3）只能看到「国家局待审批」的数据
- **医院用户** 看不到任何待审批数据（因为他们不是审批角色）

### 15.2 流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                    数据权限过滤流程                                │
└─────────────────────────────────────────────────────────────────┘

当前用户（省局用户）
    │
    ▼
获取用户关联的岗位列表
    │   例如：用户关联了岗位ID = 2（项目管理处）
    ▼
查询 declare_business_process 表
    │
    ├─ current_assign_type = 'DEPT_POST'
    └─ current_assign_source IN (2, 3, ...)  ← 用户岗位ID匹配
    │
    ▼
返回匹配的 business_id 列表
    │
    ▼
查询 declare_filing 表，返回这些ID的数据
```

---

## 十六、bizStatus（业务状态）说明

### 16.1 设计原则

`bizStatus` 字段用于审批通过后**更新业务数据的状态值**。由于不同业务（备案、半年报、年报）有不同的状态，因此设计为**自由输入**，由流程设计人员根据实际业务字典填写。

### 16.2 状态值参考

以下是项目中各类流程的 `bizStatus` 参考值，供流程设计时使用：

#### 16.2.1 备案流程 (declare:filing)

| bizStatus 值 | 说明 | 适用节点 |
|-------------|------|---------|
| `DRAFT` | 草稿 | 初始状态 |
| `SUBMITTED` | 已提交（待省级审核） | 医院提交后 |
| `PROVINCE_APPROVED` | 省级审核通过（待专家评审） | 省局审核通过 |
| `PROVINCE_REJECTED` | 省级审核拒绝 | 省局审核拒绝 |
| `EXPERT_APPROVED` | 专家论证通过（待国家局审核） | 专家会签通过 |
| `EXPERT_REJECTED` | 专家论证拒绝 | 专家会签拒绝 |
| `NATION_APPROVED` | 国家局审核通过（待归档） | 国家局审核通过 |
| `NATION_REJECTED` | 国家局审核拒绝 | 国家局审核拒绝 |
| `ARCHIVED` | 已归档 | 归档完成 |
| `RETURNED` | 退回修改 | 被退回 |

#### 16.2.2 半年报流程 (declare:semi-annual)

| bizStatus 值 | 说明 | 适用节点 |
|-------------|------|---------|
| `DRAFT` | 草稿 | 初始状态 |
| `SUBMITTED` | 已提交（待省级医院提交后 |
审核） | | `PROVINCE_APPROVED` | 省级审核通过 | 省局审核通过 |
| `PROVINCE_REJECTED` | 省级审核拒绝 | 省局审核拒绝 |
| `ARCHIVED` | 已归档 | 归档完成 |
| `RETURNED` | 退回修改 | 被退回 |

#### 16.2.3 年报流程 (declare:annual)

| bizStatus 值 | 说明 | 适用节点 |
|-------------|------|---------|
| `DRAFT` | 草稿 | 初始状态 |
| `SUBMITTED` | 已提交（待省级审核） | 医院提交后 |
| `PROVINCE_APPROVED` | 省级审核通过（待国家局审核） | 省局审核通过 |
| `PROVINCE_REJECTED` | 省级审核拒绝 | 省局审核拒绝 |
| `NATION_APPROVED` | 国家局审核通过 | 国家局审核通过 |
| `NATION_REJECTED` | 国家局审核拒绝 | 国家局审核拒绝 |
| `ARCHIVED` | 已归档 | 归档完成 |
| `RETURNED` | 退回修改 | 被退回 |

#### 16.2.4 通用状态（所有流程适用）

| bizStatus 值 | 说明 | 适用场景 |
|-------------|------|---------|
| `IN_PROGRESS` | 进行中 | 流程进行中 |
| `PENDING` | 待处理 | 等待处理 |
| `COMPLETED` | 已完成 | 流程完成 |
| `CANCELLED` | 已取消 | 流程取消 |
| `SUSPENDED` | 已挂起 | 流程挂起 |

### 16.3 前端 placeholder 提示

在 BPMN 设计器中，建议配置如下 placeholder：

```
placeholder: "如: SUBMITTED, PROVINCE_APPROVED, ARCHIVED"
帮助文本: "审批通过后更新的业务状态值，请根据实际业务字典填写"
```

### 16.4 后端更新逻辑

当任务完成时，根据 `bizStatus` 值更新业务表状态：

```java
// 任务完成监听器中
String bizStatus = dslConfig.getBizStatus();
if (StringUtils.isNotEmpty(bizStatus)) {
    // 根据业务类型调用不同的更新方法
    switch (businessType) {
        case "declare:filing:submit":
            filingService.updateFilingStatus(businessId, bizStatus);
            break;
        case "declare:semi-annual:submit":
            semiAnnualService.updateStatus(businessId, bizStatus);
            break;
        case "declare:annual:submit":
            annualService.updateStatus(businessId, bizStatus);
            break;
    }
}
```

### 16.5 配置示例

**省局审核节点配置**：

```json
{
  "cap": "AUDIT",
  "actions": "agree,reject,back",
  "assign": {
    "type": "DEPT_POST",
    "source": "2"
  },
  "bizStatus": "PROVINCE_APPROVED",
  "backStrategy": "TO_START"
}
```

> 说明：审核通过后，业务状态更新为 `PROVINCE_APPROVED`（省级审核通过），进入下一节点（专家评审）

**专家会签节点配置**：

```json
{
  "cap": "COUNTERSIGN",
  "actions": "signAgree,signReject",
  "assign": {
    "type": "USER",
    "source": "101,102,103"
  },
  "signRule": "MAJORITY",
  "bizStatus": "EXPERT_APPROVED"
}
```

> 说明：会签通过后，业务状态更新为 `EXPERT_APPROVED`（专家论证通过），进入下一节点（国家局审核）
