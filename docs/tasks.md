# 项目开发计划

## 一、已完成 ✅

### 1. 指标管理模块 (Indicator Management)
- [x] 后端 CRUD 接口
  - 指标定义表 (declare_indicator) 的增删改查
  - 分页查询、条件筛选（项目类型、业务类型）
- [x] 前端页面
  - 指标列表页（支持排序、字典显示）
  - 指标新增/编辑表单（支持选项定义动态添加/删除）

### 2. 备案管理模块 (Filing Management)
- [x] 备案信息表 (declare_filing) 后端 CRUD
- [x] 前端两步表单
  - 第一步：基本信息填写
  - 第二步：动态指标填报（折叠面板按分类显示）
- [x] 备案列表页（支持分页、筛选）
- [x] 数据权限控制（按部门过滤）

### 3. 指标口径管理 (Indicator Caliber)
- [x] 后端接口
  - 口径定义表 (declare_indicator_caliber) CRUD
  - 按指标ID查询口径
- [x] 前端页面
  - 口径管理列表
  - 口径配置表单

### 4. 指标联合规则 (Indicator Joint Rule)
- [x] 后端接口
  - 联合规则表 (declare_indicator_joint_rule) CRUD
  - 规则校验逻辑
- [x] 前端页面
  - 规则管理列表
  - 规则配置表单

### 5. 指标值管理 (Indicator Value)
- [x] 后端接口
  - 指标值表 (declare_indicator_value) 的保存/查询
  - 根据业务类型和业务ID查询指标值
- [x] 前端页面
  - 指标值提交（保存备案时同时提交指标值）
  - 编辑模式加载已保存的指标值

### 6. 业务流程配置 (Business Process)
- [x] 业务类型表 (declare_business_type) CRUD
- [x] 业务流程关联表 (declare_business_process)
  - 记录业务与流程实例的关联
  - 追踪当前节点 (currentNodeKey)
- [x] 流程启动/更新服务
- [x] 流程处理 AOP 切面 (@DeclareProcess)

### 7. BPM 任务候选人策略 (Candidate Strategy)
- [x] DEPT_POST（上级部门+岗位）策略实现
  - 根据前序任务执行人的部门，查找上级部门的特定岗位人员
  - 支持 postId 配置
- [x] DSL 配置支持
  - 任务分配 DSL 配置（DEPT_POST, DEPT_LEADER, START_USER, USER 等）
  - DSL 预览功能
- [x] 前端 UI
  - 任务分配下拉选择
  - 岗位列表联动

### 8. DSL 流程引擎核心实现 ✅ (新增)
- [x] DSL 配置解析 (DslConfig)
- [x] 节点能力处理器 (DslCapHandler)
  - AUDIT（审批）、COUNTERSIGN（会签）、FILL（填报）、EXPERT_SELECT（选择专家）等
- [x] UI 处理器 (DslUiHandler)
  - 根据 cap 动态渲染表单
- [x] 退回策略处理器 (DslBackStrategyHandler)
- [x] 重新提交策略处理器 (DslReSubmitStrategyHandler)
- [x] 下一节点预测器 (DslNextNodePredictor)
- [x] 审批详情构建器 (DslApprovalDetailBuilder)

### 9. 流程审批功能 ✅ (新增)
- [x] 审批操作接口 (BpmActionController)
  - 提交 (submit)
  - 通过 (agree)
  - 拒绝 (reject)
  - 退回 (back)
  - 撤回 (cancel/withdraw)
  - 选择专家 (selectExpert)
  - 转办/委派 (transfer/delegate)
- [x] 流程实例服务 (BpmProcessInstanceServiceImpl)
- [x] 流程任务处理

### 10. 专家管理模块 ✅ (新增)
- [x] 专家信息管理 (ExpertController/ExpertService)
- [x] 专家分页查询
- [x] 专家新增/编辑/删除
- [x] 专家 API 暴露

### 11. 备案状态监听器 ✅ (新增)
- [x] DeclareFilingStatusListener
- [x] 流程状态变更时自动更新业务状态

---

## 二、开发中 🔄

### 12. 备案列表动态按钮
- [x] 后端：备案列表返回 currentNodeKey
- [x] 前端：根据 DSL 配置动态显示按钮（部分完成）
- [ ] 流程节点更新：任务流转时更新 currentNodeKey

### 13. 流程待办任务列表
- [x] 待办任务列表 API
- [ ] 前端待办任务页面
- [ ] 任务签收/处理
- [ ] 流程审批表单

### 14. 流程历史
- [ ] 已办任务列表
- [ ] 流程轨迹查看

---

## 三、待开发 📋

### 15. 项目管理模块
- [ ] 项目列表页
- [ ] 项目详情页
- [ ] 建设过程填报
- [ ] 年度总结
- [ ] 中期评估
- [ ] 整改记录
- [ ] 验收申请

### 16. 成果管理模块
- [ ] 成果列表
- [ ] 成果编辑
- [ ] 成果详情
- [ ] 成果统计

### 17. 审核管理模块
- [ ] 备案审核（省局/国家局）
- [ ] 项目监管
- [ ] 成果审核

### 18. 评审管理模块
- [ ] 评审任务列表
- [ ] 评审页面（材料查阅、评分、意见）
- [ ] 成果评审

### 19. 专家评审模块
- [ ] 评审任务
- [ ] 评审页面
- [ ] 成果评审

### 20. 系统管理模块
- [ ] 用户管理
- [ ] 角色权限
- [ ] 流程配置
- [ ] 专家库管理
- [ ] 系统监控
- [ ] 数据治理

### 21. 通知公告模块
- [ ] 通知列表
- [ ] 通知详情
- [ ] 通知发布

### 22. 成果与推广模块
- [ ] 推广成果列表
- [ ] 成果详情
- [ ] 工具包下载

---

## 四、技术架构

### 后端模块
```
module-declare/
├── api/                      # 暴露给其他模块的接口
│   ├── ExpertApi.java        # 专家 API
├── controller/admin/         # 管理后台接口
│   ├── indicator/            # 指标管理
│   ├── filing/               # 备案管理
│   └── expert/               # 专家管理
├── dal/
│   ├── dataobject/           # 实体类
│   │   ├── filing/           # 备案相关
│   │   ├── indicator/       # 指标相关
│   │   ├── process/          # 流程相关
│   │   └── expert/           # 专家相关
│   └── mysql/                # Mapper
├── service/                  # 业务逻辑
│   ├── filing/              # 备案服务
│   ├── indicator/           # 指标服务
│   └── expert/              # 专家服务
├── framework/                # 框架扩展
│   ├── process/              # 流程处理
│   │   ├── aspect/           # AOP 切面
│   │   ├── annotation/       # 注解
│   │   └── service/         # 流程服务
│   ├── datapermission/       # 数据权限
│   └── listener/            # 监听器
├── convert/                  # 对象转换
└── enums/                    # 枚举定义
```

### BPM DSL 模块扩展
```
module-bpm/
├── framework/
│   ├── flowable/core/
│   │   ├── candidate/        # 任务候选人策略
│   │   │   ├── strategy/
│   │   │   │   └── dept/
│   │   │   │       └── BpmTaskCandidateDeptPostStrategy.java  # DEPT_POST 策略
│   │   │   └── invoker/
│   │   └── dsl/
│   │       ├── config/       # DSL 配置
│   │       │   └── DslConfig.java
│   │       ├── handler/      # DSL 处理器
│   │       │   ├── DslCapHandler.java          # 节点能力处理
│   │       │   ├── DslUiHandler.java            # UI 处理
│   │       │   ├── DslBackStrategyHandler.java # 退回策略处理
│   │       │   └── DslReSubmitStrategyHandler.java # 重新提交策略
│   │       ├── predictor/
│   │       │   └── DslNextNodePredictor.java   # 下一节点预测
│   │       └── builder/
│   │           └── DslApprovalDetailBuilder.java # 审批详情构建
│   └── process/
│       ├── service/
│       │   ├── BpmProcessService.java
│       │   └── impl/
│       └── listener/
├── controller/admin/
│   ├── action/              # 审批操作
│   │   └── BpmActionController.java
│   └── task/                # 任务管理
│       └── BpmProcessInstanceController.java
└── api/
    ├── task/                # 任务 API
    └── event/               # 事件
```

### 前端页面
```
web-ui/apps/web-antd/src/views/declare/
├── indicator/                # 指标管理
│   ├── index.vue             # 列表页
│   ├── data.ts               # 配置
│   └── modules/form.vue      # 表单
├── caliber/                  # 指标口径
│   ├── index.vue
│   └── modules/form.vue
├── jointRule/                # 联合规则
│   ├── index.vue
│   ├── components/RuleConfig.vue
│   └── modules/form.vue
├── filing/                   # 备案管理
│   ├── index.vue             # 列表页
│   └── modules/form.vue      # 两步表单
└── expert/                   # 专家管理（新增）
    ├── index.vue
    └── modules/form.vue
```

---

## 五、开发优先级

| 优先级 | 模块 | 描述 | 状态 |
|--------|------|------|------|
| P0 | 流程待办任务 | 任务签收、处理、审批 | 🔄 开发中 |
| P1 | 备案列表动态按钮 | 根据 DSL 配置显示操作按钮 | 🔄 开发中 |
| P2 | 流程历史 | 已办任务、流程轨迹 | 📋 待开发 |
| P3 | 项目管理 | 项目全生命周期管理 | 📋 待开发 |
| P3 | 成果管理 | 成果登记与转化 | 📋 待开发 |
| P4 | 审核管理 | 省局/国家局审核 | 📋 待开发 |
| P4 | 专家评审 | 评审任务与评分 | 📋 待开发 |

---

## 六、数据库表

| 表名 | 说明 | 状态 |
|------|------|------|
| declare_indicator | 指标定义 | ✅ 完成 |
| declare_indicator_value | 指标值 | ✅ 完成 |
| declare_indicator_caliber | 指标口径 | ✅ 完成 |
| declare_indicator_joint_rule | 联合规则 | ✅ 完成 |
| declare_filing | 备案信息 | ✅ 完成 |
| bpm_business_type | 业务类型 | ✅ 完成 |
| declare_business_process | 业务流程 | ✅ 完成 |
| declare_expert | 专家信息 | ✅ 完成 |
| system_dict_data | 字典数据 | ✅ 完成 |

---

## 七、BPM 任务候选人策略

| 策略 | 说明 | 状态 |
|------|------|------|
| DEPT_POST | 上级部门+岗位 | ✅ 完成 |
| DEPT_LEADER | 部门负责人 | ✅ 完成 |
| START_USER | 发起人本人 | ✅ 完成 |
| START_USER_SELECT | 发起人自选 | ✅ 完成 |
| USER | 指定用户 | ✅ 完成 |
| USER_GROUP | 用户组 | ✅ 完成 |
| EXPRESSION | 流程表达式 | ✅ 完成 |

---

## 八、DSL 配置示例

```json
{
  "nodeKey": "PROVINCE_AUDIT",
  "cap": "AUDIT",
  "actions": "agree,reject,back",
  "roles": ["PROVINCE"],
  "assign": {
    "type": "DEPT_POST",
    "source": "2"
  },
  "backStrategy": "NONE",
  "bizStatus": "SUBMITTED",
  "enable": true
}
```

### DSL 字段说明

| 字段 | 说明 | 必填 |
|------|------|------|
| nodeKey | 节点Key | 是 |
| cap | 能力（如 AUDIT, FILL） | 是 |
| actions | 允许的操作（agree,reject,back,submit） | 是 |
| roles | 允许的角色 | 否 |
| assign | 任务分配策略 | 否 |
| assign.type | 分配类型 | 否 |
| assign.source | 来源（根据type不同含义不同） | 否 |
| backStrategy | 退回策略 | 是 |
| bizStatus | 业务状态 | 是 |
| enable | 是否启用 | 是 |
