# 修复进度填报新建流程

## 业务流程梳理

### 新建填报流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                         新建填报流程                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 用户点击"新建填报"按钮（根据 checkReportWindow 状态启用/禁用）     │
│                            ↓                                         │
│  2. 弹窗打开，自动获取最新开放窗口                                     │
│     └─ 无需用户选择年度/批次                                         │
│     └─ 显示窗口信息（如：2026年第1期）                               │
│                            ↓                                         │
│  3. 显示指标填报表格                                                 │
│                            ↓                                         │
│  4. 三个按钮：                                                       │
│     ├─ [取消]     - 关闭弹窗，不保存                                 │
│     ├─ [保存草稿] - 状态=SAVED(0)，可再编辑                          │
│     └─ [提交审核] - 状态=SUBMITTED(1)，不可编辑                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 编辑已有填报

```
┌─────────────────────────────────────────────────────────────────────┐
│                         编辑已有填报                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  点击列表"详情"按钮打开编辑弹窗                                        │
│                            ↓                                         │
│  根据状态判断是否可编辑：                                              │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ SAVED(0) 或 HOSPITAL_REJECTED(4) → 可编辑，显示指标表格        │  │
│  │ 其他状态 → 只读，不可编辑                                        │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                            ↓                                         │
│  按钮逻辑：                                                          │
│  ├─ 可编辑时：[取消] [保存草稿] [提交审核]                           │
│  └─ 只读时：[取消]                                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 状态规则

| 状态 | 值 | 说明 | 可编辑 | 可删除 | 按钮操作 |
|------|-----|------|--------|--------|---------|
| **SAVED** | 0 | 保存草稿 | ✅ | ✅ | 保存草稿 / 提交审核 |
| **SUBMITTED** | 1 | 已提交 | ❌ | ❌ | 仅查看 |
| HOSPITAL_AUDITING | 2 | 医院审核中 | ❌ | ❌ | 仅查看 |
| HOSPITAL_APPROVED | 3 | 医院审核通过 | ❌ | ❌ | 仅查看 |
| HOSPITAL_REJECTED | 4 | 医院审核驳回 | ✅ | ❌ | 保存草稿 / 提交审核 |

---

## 需要修改的文件

### 后端修改

#### 1. `module-declare/.../enums/ReportStatusEnum.java`
```java
public enum ReportStatusEnum {
    SAVED(0, "保存草稿"),        // 原 DRAFT(0, "草稿")
    SUBMITTED(1, "已提交"),      // 保持不变
    HOSPITAL_AUDITING(2, "医院审核中"),
    HOSPITAL_APPROVED(3, "医院审核通过"),
    HOSPITAL_REJECTED(4, "医院审核驳回");
    
    // ... 其余不变
}
```

#### 2. `module-declare/.../service/progress/DeclareReportWindowService.java`
添加接口方法：
```java
/**
 * 获取最新的开放填报窗口
 */
ReportWindowVO getLatestOpenWindow();
```

#### 3. `module-declare/.../service/progress/impl/DeclareReportWindowServiceImpl.java`
实现方法：
```java
@Override
public ReportWindowVO getLatestOpenWindow() {
    LocalDateTime now = LocalDateTime.now();
    LambdaQueryWrapper<DeclareReportWindowDO> wrapper = new LambdaQueryWrapper<DeclareReportWindowDO>()
            .eq(DeclareReportWindowDO::getStatus, 1)
            .le(DeclareReportWindowDO::getWindowStart, now)
            .ge(DeclareReportWindowDO::getWindowEnd, now)
            .orderByDesc(DeclareReportWindowDO::getWindowStart)  // 取最新的
            .last("LIMIT 1");
    DeclareReportWindowDO window = windowMapper.selectOne(wrapper);
    return window != null ? convertToVO(window) : null;
}
```

#### 4. `module-declare/.../controller/admin/progress/DeclareProgressReportController.java`
添加接口：
```java
/**
 * 获取最新开放窗口
 */
@GetMapping("/latest-window")
public CommonResult<ReportWindowVO> getLatestOpenWindow() {
    return success(reportWindowService.getLatestOpenWindow());
}
```

---

### 前端修改

#### 1. `web-ui/apps/web-antd/src/api/declare/progress-report/index.ts`
添加两个 API 函数：
```typescript
/** 获取最新开放窗口 */
export async function getLatestOpenWindow(hospitalId: number) {
  return requestClient.get<ReportWindow | null>(
    `/declare/progress-report/latest-window?hospitalId=${hospitalId}`,
  );
}

/** 保存草稿 */
export async function saveProgressReportDraft(id: number) {
  return requestClient.put(`/declare/progress-report/save-draft?id=${id}`);
}
```

#### 2. `web-ui/apps/web-antd/src/api/declare/indicator/index.ts`
添加函数：
```typescript
/** 获取指标列表（进度填报用） */
export async function getIndicatorList() {
  return requestClient.get<DeclareIndicatorApi.Indicator[]>(
    '/declare/indicator/list-by-business-type?businessType=process',
  );
}
```

#### 3. `web-ui/apps/web-antd/src/views/declare/progress-report/modules/form.vue`
**完全重写核心逻辑**：

```typescript
// === 状态定义 ===
const isLoading = ref(false);
const isEditMode = ref(false);  // 是否是编辑已有记录
const isReadonly = ref(false);  // 是否只读（状态不允许编辑）

// === 弹窗打开时 ===
async function onOpenChange(isOpen: boolean) {
  if (!isOpen) return;
  
  isLoading.value = true;
  try {
    const data = modalApi.getData<{ hospitalId: number } & DeclareProgressReport>();
    
    if (data?.hospitalId) {
      formData.value.hospitalId = data.hospitalId;
    }
    
    if (data?.id) {
      // 编辑已有记录
      isEditMode.value = true;
      const result = await getProgressReport(data.id);
      formData.value = { ...result };
      // 判断是否可编辑
      isReadonly.value = ![0, 4].includes(result.reportStatus);
    } else {
      // 新建记录
      isEditMode.value = false;
      // 获取最新开放窗口
      const latestWindow = await getLatestOpenWindow(formData.value.hospitalId);
      if (!latestWindow) {
        message.error('当前没有开放的填报窗口');
        modalApi.close();
        return;
      }
      formData.value.reportYear = latestWindow.reportYear;
      formData.value.reportBatch = latestWindow.reportBatch;
      isReadonly.value = false;
    }
  } finally {
    isLoading.value = false;
  }
}

// === 保存草稿 ===
async function handleSaveDraft() {
  modalApi.lock();
  try {
    if (!formData.value.id) {
      // 新建时先创建记录
      const id = await createProgressReport({
        reportYear: formData.value.reportYear!,
        reportBatch: formData.value.reportBatch!,
      });
      formData.value.id = id;
    }
    // TODO: 调用保存草稿接口（目前状态已是DRAFT/SAVED）
    message.success('保存草稿成功');
  } finally {
    modalApi.unlock();
  }
}

// === 提交审核 ===
async function handleSubmit() {
  if (!formData.value.id) {
    message.error('请先保存草稿');
    return;
  }
  modalApi.lock();
  try {
    await submitProgressReport(formData.value.id);
    message.success('提交审核成功');
    await modalApi.close();
    emit('success');
  } finally {
    modalApi.unlock();
  }
}

// === 取消 ===
function handleCancel() {
  modalApi.close();
}
```

#### 4. `web-ui/apps/web-antd/src/views/declare/progress-report/modules/form.vue` 模板部分

```vue
<template>
  <Modal :title="isEditMode ? '编辑填报' : '新建填报'" class="progress-form-modal">
    <a-spin :spinning="isLoading">
      <!-- 窗口信息（只读显示） -->
      <a-alert
        v-if="formData.reportYear"
        :message="`当前填报窗口：${formData.reportYear}年第${formData.reportBatch}期`"
        type="info"
        show-icon
        class="mb-4"
      />
      
      <a-divider>指标填报</a-divider>
      
      <!-- 指标表格 -->
      <IndicatorInputTable
        v-if="formData.id"
        :hospital-id="formData.hospitalId"
        :report-id="formData.id"
        :readonly="isReadonly"
      />
      <a-alert v-else type="info" show-icon message="保存后可填报指标数据" />
    </a-spin>
    
    <template #footer>
      <a-button @click="handleCancel">取消</a-button>
      <template v-if="!isReadonly">
        <a-button @click="handleSaveDraft" :loading="saving">保存草稿</a-button>
        <a-button type="primary" @click="handleSubmit">提交审核</a-button>
      </template>
    </template>
  </Modal>
</template>
```

#### 5. `web-ui/apps/web-antd/src/views/declare/progress-report/index.vue`
更新操作列逻辑：
```vue
<!-- 操作列 -->
<template #actions="{ row }">
  <TableAction
    :actions="[
      {
        label: '详情',
        type: 'link',
        onClick: handleEdit.bind(null, row),
      },
      // 可编辑状态时显示保存草稿/提交审核
      ...(canEdit(row) ? [
        {
          label: '提交',
          type: 'link',
          onClick: handleSubmit.bind(null, row),
        },
      ] : []),
    ]"
  />
</template>

<script>
// 判断是否可以编辑
function canEdit(row: DeclareProgressReport): boolean {
  return row.reportStatus === 0 || row.reportStatus === 4;
}
</script>
```

#### 6. `IndicatorInputTable.vue`
添加 readonly 属性支持：
```typescript
const props = defineProps<{
  hospitalId: number;
  reportId?: number;
  readonly?: boolean;  // 新增
}>();

// 模板中根据 readonly 禁用输入
<a-input-number
  v-model:value="formValues[record.indicatorId]"
  :disabled="props.readonly"
  ...
/>
```

---

## 修改文件清单

### 后端 (Java)
1. `module-declare/src/main/java/cn/gemrun/base/module/declare/enums/ReportStatusEnum.java`
2. `module-declare/src/main/java/cn/gemrun/base/module/declare/service/progress/DeclareReportWindowService.java`
3. `module-declare/src/main/java/cn/gemrun/base/module/declare/service/progress/impl/DeclareReportWindowServiceImpl.java`
4. `module-declare/src/main/java/cn/gemrun/base/module/declare/controller/admin/progress/DeclareProgressReportController.java`

### 前端 (Vue/TypeScript)
1. `web-ui/apps/web-antd/src/api/declare/progress-report/index.ts`
2. `web-ui/apps/web-antd/src/api/declare/indicator/index.ts`
3. `web-ui/apps/web-antd/src/views/declare/progress-report/modules/form.vue`
4. `web-ui/apps/web-antd/src/views/declare/progress-report/index.vue`
5. `web-ui/apps/web-antd/src/views/declare/progress-report/components/IndicatorInputTable.vue`

---

## 注意事项

1. **状态枚举修改**：后端 `DRAFT` 改为 `SAVED`，会影响所有使用该枚举的地方
2. **提交审核时机**：目前是点击"提交审核"直接提交，也可以改为先保存草稿再提交
3. **指标保存**：需要后端提供保存指标数据的接口
