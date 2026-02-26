# 项目开发计划

## 一、已完成 ✅

### 1. 指标管理模块 (Indicator Management)
- [x] 后端 CRUD 接口
  - 指标定义表 (declare_indicator) 的增删改查
  - 分页查询、条件筛选（项目类型、业务类型）
- [x] 前端页面
  - 指标列表页（支持排序、字典显示）
  - 指标新增/编辑表单（支持选项定义动态添加/删除）

### 2. 备案管理模块 - 基础框架
- [x] 备案信息表 (declare_filing) 的后端 CRUD
- [x] 前端两步表单
  - 第一步：基本信息填写
  - 第二步：动态指标填报（折叠面板按分类显示）

---

## 二、开发中 🔄

### 3. 备案模块 - 指标值管理
- [ ] 后端：指标值 CRUD 接口
  - 指标值表 (declare_indicator_value) 的保存/查询
  - 根据业务类型和业务ID查询指标值
- [ ] 前端：指标值提交
  - 保存备案时同时提交指标值到后端
  - 编辑模式加载已保存的指标值

---

## 三、待开发 📋

### 4. 指标口径管理 (Indicator Caliber)
- [ ] 后端接口
  - 口径定义表 (declare_indicator_caliber) CRUD
  - 按指标ID查询口径
- [ ] 前端页面
  - 口径管理列表
  - 口径配置表单

### 5. 指标联合规则 (Indicator Joint Rule)
- [ ] 后端接口
  - 联合规则表 (declare_indicator_joint_rule) CRUD
  - 规则校验逻辑
- [ ] 前端页面
  - 规则管理列表
  - 规则配置表单

### 6. 备案流程管理 (Filing Process)
- [ ] 业务流程配置
  - 业务类型表 (declare_business_type)
  - 业务流程表 (declare_business_process)
- [ ] 流程动态表单
  - 根据业务类型动态加载指标
  - 流程状态管理

### 7. 字典数据维护
- [ ] 完善字典数据
  - 项目类型 (declare_project_type)
  - 指标分类 (declare_indicator_category)
  - 值类型 (declare_indicator_value_type)
  - 备案状态 (declare_filing_status)

---

## 四、技术架构

### 后端模块
```
module-declare/
├── api/                    # 暴露给其他模块的接口
├── controller/admin/       # 管理后台接口
│   ├── indicator/         # 指标管理
│   └── filing/             # 备案管理
├── dal/
│   ├── dataobject/         # 实体类
│   └── mysql/              # Mapper
├── service/                # 业务逻辑
├── framework/              # 框架扩展
│   └── process/            # 流程处理
├── convert/                # 对象转换
└── enums/                  # 枚举定义
```

### 前端页面
```
web-ui/apps/web-antd/src/views/declare/
├── indicator/              # 指标管理
│   ├── index.vue           # 列表页
│   ├── data.ts             # 配置
│   └── modules/form.vue    # 表单
└── filing/                 # 备案管理
    ├── index.vue           # 列表页
    └── modules/form.vue    # 两步表单
```

---

## 五、开发优先级

| 优先级 | 模块 | 描述 |
|--------|------|------|
| P0 | 指标值保存 | 备案表单指标值提交到后端 |
| P1 | 指标值查询 | 编辑时加载已保存的指标值 |
| P2 | 指标口径 | 指标口径管理 |
| P3 | 联合规则 | 指标联合规则 |
| P4 | 流程配置 | 业务流程配置 |
| P5 | 字典维护 | 完善字典数据 |

---

## 六、数据库表

| 表名 | 说明 | 状态 |
|------|------|------|
| declare_indicator | 指标定义 | ✅ 完成 |
| declare_indicator_value | 指标值 | 🔄 待开发 |
| declare_indicator_caliber | 指标口径 | 📋 待开发 |
| declare_indicator_joint_rule | 联合规则 | 📋 待开发 |
| declare_filing | 备案信息 | ✅ 完成 |
| declare_business_type | 业务类型 | 📋 待开发 |
| declare_business_process | 业务流程 | 📋 待开发 |
