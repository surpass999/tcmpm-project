<script setup lang="ts">
import type { NationalExportParams } from '#/api/declare/progress-report';
import { exportNationalReport, exportNationalReportAdvanced } from '#/api/declare/progress-report';
import { downloadFileFromBlobPart } from '@vben/utils';

import { computed, ref } from 'vue';
import {
  message,
  Spin,
  Divider,
  Form,
  FormItem,
  Row,
  Col,
  Select,
  Checkbox,
  Input,
} from 'ant-design-vue';

// ========== 状态 ==========
const visible = ref(false);
const loading = ref(false);

// ========== 表单数据 ==========
const formData = ref<NationalExportParams>({
  includeContainerDetail: true,
});

// ========== 下拉选项 ==========
const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear();
  return Array.from({ length: 5 }, (_, i) => ({
    label: `${currentYear - i}年`,
    value: currentYear - i,
  }));
});

const batchOptions = [
  { label: '第1期', value: 1 },
  { label: '第2期', value: 2 },
  { label: '第3期', value: 3 },
  { label: '第4期', value: 4 },
  { label: '第5期', value: 5 },
  { label: '第6期', value: 6 },
  { label: '第7期', value: 7 },
  { label: '第8期', value: 8 },
  { label: '第9期', value: 9 },
  { label: '第10期', value: 10 },
];

const projectTypeOptions = [
  { label: '综合型', value: 1 },
  { label: '中医电子病历型', value: 2 },
  { label: '智慧中药房型', value: 3 },
  { label: '名老中医传承型', value: 4 },
  { label: '中医临床科研型', value: 5 },
  { label: '中医智慧医共体型', value: 6 },
];

const reportStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已保存', value: 'SAVED' },
  { label: '待审批', value: 'SUBMITTED' },
  { label: '国家局通过', value: 'NATION_APPROVED' },
  { label: '国家局驳回', value: 'NATION_REJECTED' },
];

const provinceStatusOptions = [
  { label: '未提交', value: 0 },
  { label: '审核中', value: 1 },
  { label: '已通过', value: 2 },
  { label: '已驳回', value: 3 },
];

const nationalStatusOptions = [
  { label: '国家局审批中', value: 1 },
  { label: '已上报', value: 2 },
];

// ========== 方法 ==========

/** 打开弹窗 */
function open(initialData?: Record<string, any> | null) {
  // 如果有传入参数，使用它作为初始值
  if (initialData) {
    formData.value = {
      ...(initialData as NationalExportParams),
      includeContainerDetail: true,
    };
  } else {
    formData.value = {
      includeContainerDetail: true,
    };
  }
  visible.value = true;
}

/** 关闭弹窗 */
function handleClose() {
  visible.value = false;
}

/** 导出 */
async function handleExport() {
  loading.value = true;
  try {
    // 生成文件名
    const year = formData.value.reportYear || '全部';
    const projectTypeName =
      projectTypeOptions.find((o) => o.value === formData.value.projectType)?.label ||
      '全类型';
    const filename = `国家局填报数据_${year}_${projectTypeName}.xlsx`;

    let data: Blob;

    // 根据是否有高级搜索条件选择接口
    if (
      formData.value.indicatorGroups &&
      formData.value.indicatorGroups.length > 0
    ) {
      // 高级条件导出
      data = await exportNationalReportAdvanced(formData.value);
    } else {
      // 简单条件导出
      data = await exportNationalReport(formData.value);
    }

    // 下载文件
    await downloadFileFromBlobPart({ fileName: filename, source: data });

    message.success('导出成功');
    visible.value = false;
  } catch (error: any) {
    console.error('Export error:', error);
    message.error(error?.message || '导出失败，请重试');
  } finally {
    loading.value = false;
  }
}

// ========== 暴露方法 ==========
defineExpose({ open });
</script>

<template>
  <a-modal
    v-model:open="visible"
    title="导出填报数据"
    width="600px"
    :confirm-loading="loading"
    :mask-closable="false"
    @ok="handleExport"
    @cancel="handleClose"
  >
    <Spin :spinning="loading">
      <Form layout="vertical" class="export-form">
        <!-- 筛选条件 -->
        <Divider orientation="left">筛选条件</Divider>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="填报年度">
              <Select
                v-model:value="formData.reportYear"
                placeholder="全部年度"
                allow-clear
                :options="yearOptions"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="填报批次">
              <Select
                v-model:value="formData.reportBatch"
                placeholder="全部批次"
                allow-clear
                :options="batchOptions"
              />
            </FormItem>
          </Col>
        </Row>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="项目类型">
              <Select
                v-model:value="formData.projectType"
                placeholder="全部类型"
                allow-clear
                :options="projectTypeOptions"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="上报状态">
              <Select
                v-model:value="formData.nationalReportStatus"
                placeholder="全部状态"
                allow-clear
                :options="nationalStatusOptions"
              />
            </FormItem>
          </Col>
        </Row>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="填报状态">
              <Select
                v-model:value="formData.reportStatus"
                placeholder="全部状态"
                allow-clear
                :options="reportStatusOptions"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="省级审核">
              <Select
                v-model:value="formData.provinceStatus"
                placeholder="全部状态"
                allow-clear
                :options="provinceStatusOptions"
              />
            </FormItem>
          </Col>
        </Row>

        <FormItem label="医院名称">
          <Input
            v-model:value="formData.hospitalName"
            placeholder="支持模糊搜索"
            allow-clear
          />
        </FormItem>

        <!-- 导出选项 -->
        <Divider orientation="left">导出选项</Divider>

        <FormItem label="导出范围说明">
          <div class="export-tip">
            <ul>
              <li>不指定项目类型时，将按每个项目类型生成独立的 Sheet</li>
              <li>每个 Sheet 包含: 医院基础信息 + 指标值 (三行表头)</li>
              <li>
                动态容器指标的每个子字段将展开为独立的列（如 601.01 姓名、601.02
                职称）
              </li>
              <li>
                如果当前页面有高级搜索条件，将自动带入并使用高级导出接口
              </li>
            </ul>
          </div>
        </FormItem>

        <FormItem label="数据内容">
          <Checkbox v-model:checked="formData.includeContainerDetail">
            包含动态容器明细
          </Checkbox>
          <template #extra>
            <span class="checkbox-extra">
              勾选后，动态容器指标的每个子字段将展开为独立列；不勾选则只导出行数统计
            </span>
          </template>
        </FormItem>
      </Form>
    </Spin>
  </a-modal>
</template>

<style scoped>
.export-form {
  max-width: 100%;
}

.export-tip {
  background: #f5f5f5;
  padding: 12px 16px;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
}

.export-tip ul {
  margin: 0;
  padding-left: 20px;
}

.export-tip li {
  margin-bottom: 4px;
  line-height: 1.6;
}

.export-tip li:last-child {
  margin-bottom: 0;
}

.checkbox-extra {
  color: #999;
  font-size: 12px;
}
</style>
