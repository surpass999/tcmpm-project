<script lang="ts" setup>
import type { DeclareFilingApi } from '#/api/declare/filing';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DeclareIndicatorValueApi } from '#/api/declare/indicatorValue';
import type { DeclareProjectApi } from '#/api/declare/project';

import { computed, onMounted, ref } from 'vue';

import { Descriptions, DescriptionsItem, Progress, Collapse, CollapsePanel, Card, Row, Col, Statistic, Tag } from 'ant-design-vue';

import { formatDateTime } from '@vben/utils';

import { getFiling } from '#/api/declare/filing';
import { getIndicatorsByProjectType } from '#/api/declare/indicator';
import { getIndicatorValues } from '#/api/declare/indicatorValue';

interface Props {
  project: DeclareProjectApi.Project;
}

// 指标业务类型（数字）- 项目=2
const BUSINESS_TYPE_PROJECT = 2;

const props = defineProps<Props>();

// 加载状态
const loading = ref(false);

// 备案信息
const filingData = ref<DeclareFilingApi.Filing | null>(null);

// 指标列表（按分类分组）
const indicatorGroups = ref<{
  category: number;
  categoryName: string;
  indicators: DeclareIndicatorApi.Indicator[];
}[]>([]);

// 指标值映射
const indicatorValuesMap = ref<Record<number, any>>({});

// 折叠面板展开的key
const activeCollapseKeys = ref<(string | number)[]>([]);

// 项目类型名称映射
const projectTypeMap: Record<number, string> = {
  1: '综合型',
  2: '中医电子病历型',
  3: '智慧中药房型',
  4: '名老中医传承型',
  5: '中医临床科研型',
  6: '中医智慧医共体型',
};

// 加载指标数据
async function loadIndicators() {
  if (!props.project?.filingId || !props.project?.id) return;

  loading.value = true;
  try {
    // 1. 获取备案信息（获取项目类型）
    const filing = await getFiling(props.project.filingId);
    filingData.value = filing;

    // 2. 根据项目类型获取指标列表
    if (filing?.projectType) {
      const indicators = await getIndicatorsByProjectType(
        filing.projectType,
        'project'
      );

      // 按分类分组
      const categoryMap = new Map<number, typeof indicatorGroups.value[0]>();
      indicators.forEach((indicator) => {
        if (!categoryMap.has(indicator.category)) {
          categoryMap.set(indicator.category, {
            category: indicator.category,
            categoryName: getCategoryName(indicator.category),
            indicators: [],
          });
        }
        categoryMap.get(indicator.category)!.indicators.push(indicator);
      });
      indicatorGroups.value = Array.from(categoryMap.values()).sort(
        (a, b) => a.category - b.category
      );

      // 默认展开所有分类
      if (indicatorGroups.value.length > 0) {
        activeCollapseKeys.value = indicatorGroups.value.map((g) => g.category);
      }

      // 3. 使用 business_type=2 和 business_id=项目ID 获取指标值
      const values = await getIndicatorValues(
        BUSINESS_TYPE_PROJECT,
        props.project.id
      );
      const map: Record<number, any> = {};
      values.forEach((v) => {
        map[v.indicatorId] = parseIndicatorValue(v);
      });
      indicatorValuesMap.value = map;
    }
  } catch (e) {
    console.error('加载指标数据失败', e);
  } finally {
    loading.value = false;
  }
}

// 获取分类名称
function getCategoryName(category: number): string {
  const categoryNames: Record<number, string> = {
    1: '基本信息',
    2: '业务功能',
    3: '技术实现',
    4: '安全防护',
    5: '其他',
  };
  return categoryNames[category] || `分类${category}`;
}

// 解析指标值
function parseIndicatorValue(
  item: DeclareIndicatorValueApi.IndicatorValue
): any {
  switch (item.valueType) {
    case 1: // 数字
      return item.valueNum;
    case 2: // 字符串
    case 6: // 单选
    case 10: // 单选下拉
      return item.valueStr;
    case 7: // 多选
    case 11: // 多选下拉
      return item.valueStr ? item.valueStr.split(',') : [];
    case 3: // 布尔
      return item.valueBool;
    case 4: // 日期
      return item.valueDate;
    case 5: // 长文本
      return item.valueText;
    case 8: // 日期区间
      if (item.valueDateStart || item.valueDateEnd) {
        return [item.valueDateStart, item.valueDateEnd];
      }
      return undefined;
    case 9: // 文件上传
      return item.valueStr;
    default:
      return item.valueStr;
  }
}

// 解析选项
function parseOptions(
  valueOptions: string
): Array<{ label: string; value: string }> {
  if (!valueOptions) return [];
  try {
    return JSON.parse(valueOptions);
  } catch {
    return [];
  }
}

// 获取选项数组
function getOptions(valueOptions: string): Array<{ label: string; value: string }> {
  return parseOptions(valueOptions);
}

// 项目状态映射（使用字典值）
const statusMap: Record<string, { text: string; color: string; bg: string; border: string }> = {
  INITIATION: { text: '初始化', color: '#6C737A', bg: '#F3F4F6', border: '#D4D9DF' },
  FILING: { text: '立项中', color: '#1890FF', bg: '#E6F7FF', border: '#91D5FF' },
  CONSTRUCTION: { text: '建设中', color: '#FA8C16', bg: '#FFF7E6', border: '#FFD591' },
  MIDTERM: { text: '中期评估', color: '#722ED1', bg: '#F9F0FF', border: '#D3ADF7' },
  RECTIFICATION: { text: '整改中', color: '#FF4D4F', bg: '#FFF2F0', border: '#FFCCC7' },
  ACCEPTANCE: { text: '验收中', color: '#13C2C2', bg: '#E6FFFB', border: '#87E8DE' },
  ACCEPTED: { text: '已验收', color: '#52C41A', bg: '#F6FFED', border: '#B7EB8F' },
  TERMINATED: { text: '已终止', color: '#8C8C8C', bg: '#F5F5F5', border: '#D9D9D9' },
};

const projectStatus = computed(() => {
  const status = props.project?.projectStatus;
  return status !== undefined && status !== null
    ? statusMap[status] || { text: '未知', color: '#8C8C8C', bg: '#F5F5F5', border: '#D9D9D9' }
    : { text: '未知', color: '#8C8C8C', bg: '#F5F5F5', border: '#D9D9D9' };
});

// 资金执行率
const fundExecutionRate = computed(() => {
  if (!props.project?.totalInvestment || props.project.totalInvestment <= 0) {
    return 0;
  }
  const rate = ((props.project?.accumulatedInvestment || 0) / props.project.totalInvestment) * 100;
  return Math.min(Math.round(rate), 100);
});

// 项目类型名称
const projectTypeName = computed(() => {
  const type = filingData.value?.projectType || props.project?.projectType;
  return type ? projectTypeMap[type] || '未知' : '未知';
});

// 格式化金额
function formatMoney(value: number | string | undefined) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return Number(value).toFixed(2);
}

// 组件挂载时加载指标数据
onMounted(() => {
  loadIndicators();
});
</script>

<template>
  <div class="project-basic-info">
    <!-- 项目核心信息区 -->
    <div class="project-header">
      <div class="project-title-section">
        <h1 class="project-title">
          <i class="fas fa-project-diagram title-icon" />
          {{ project?.projectName || '项目详情' }}
        </h1>
        <div class="project-tags">
          <span class="project-type-tag">
            <i class="fas fa-tag" />
            {{ projectTypeName }}
          </span>
          <span class="project-status-tag" :style="{
            color: projectStatus.color,
            backgroundColor: projectStatus.bg,
            borderColor: projectStatus.border
          }">
            <i class="fas fa-circle" style="font-size: 8px" />
            {{ projectStatus.text }}
          </span>
        </div>
      </div>
    </div>

    <!-- 关键指标卡片 -->
    <Row :gutter="[16, 16]" class="indicator-cards">
      <Col :xs="24" :sm="12" :lg="6">
        <Card class="indicator-card" :bordered="false">
          <Statistic
            title="总投资 (201指标)"
            :value="project?.totalInvestment || 0"
            :precision="2"
            suffix="万元"
            value-style="{ color: '#2A5C45' }"
          >
            <template #prefix>
              <i class="fas fa-coins stat-icon" />
            </template>
          </Statistic>
        </Card>
      </Col>
      <Col :xs="24" :sm="12" :lg="6">
        <Card class="indicator-card" :bordered="false">
          <Statistic
            title="中央资金到账 (20101指标)"
            :value="project?.centralFundArrive || 0"
            :precision="2"
            suffix="万元"
            value-style="{ color: '#1890FF' }"
          >
            <template #prefix>
              <i class="fas fa-university stat-icon" style="color: #1890FF" />
            </template>
          </Statistic>
        </Card>
      </Col>
      <Col :xs="24" :sm="12" :lg="6">
        <Card class="indicator-card" :bordered="false">
          <Statistic
            title="累计完成投资 (202指标)"
            :value="project?.accumulatedInvestment || 0"
            :precision="2"
            suffix="万元"
            value-style="{ color: '#52C41A' }"
          >
            <template #prefix>
              <i class="fas fa-chart-line stat-icon" style="color: #52C41A" />
            </template>
          </Statistic>
        </Card>
      </Col>
      <Col :xs="24" :sm="12" :lg="6">
        <Card class="indicator-card indicator-card-highlight" :bordered="false">
          <Statistic
            title="资金执行率"
            :value="fundExecutionRate"
            suffix="%"
            :value-style="{ color: fundExecutionRate >= 70 ? '#52C41A' : '#FA8C16' }"
          >
            <template #prefix>
              <i class="fas fa-percentage stat-icon" :style="{ color: fundExecutionRate >= 70 ? '#52C41A' : '#FA8C16' }" />
            </template>
          </Statistic>
          <div class="execution-bar">
            <div class="execution-bar-inner" :style="{ width: `${fundExecutionRate}%`, backgroundColor: fundExecutionRate >= 70 ? '#52C41A' : '#FA8C16' }" />
          </div>
        </Card>
      </Col>
    </Row>

    <!-- 项目进度卡片 -->
    <Card class="progress-card" :bordered="false">
      <div class="progress-content">
        <div class="progress-info">
          <div class="progress-label">
            <i class="fas fa-tasks" />
            项目进度
          </div>
          <div class="progress-percent">{{ project?.actualProgress || 0 }}%</div>
        </div>
        <Progress
          :percent="project?.actualProgress || 0"
          :stroke-color="project?.actualProgress && project.actualProgress >= 70 ? '#52C41A' : {
            '0%': '#2A5C45',
            '100%': '#4A8F72'
          }"
          :trail-color="'#E6EEE8'"
          :show-info="false"
          class="progress-bar-custom"
        />
        <div class="progress-timeline">
          <div class="timeline-item">
            <span class="timeline-label">立项时间</span>
            <span class="timeline-value">{{ formatDateTime(project?.startTime) || '-' }}</span>
          </div>
          <div class="timeline-item">
            <span class="timeline-label">计划完成</span>
            <span class="timeline-value">{{ formatDateTime(project?.planEndTime) || '-' }}</span>
          </div>
          <div class="timeline-item">
            <span class="timeline-label">实际完成</span>
            <span class="timeline-value">{{ formatDateTime(project?.actualEndTime) || '-' }}</span>
          </div>
        </div>
      </div>
    </Card>

    <!-- 基本信息卡片 -->
    <Card class="info-card" :bordered="false" title="">
      <template #title>
        <div class="card-title">
          <i class="fas fa-info-circle" />
          基本信息
        </div>
      </template>
      <Descriptions :column="3" bordered size="small" class="info-table">
        <DescriptionsItem label="项目名称" :span="2">
          <span class="info-value-primary">{{ project?.projectName || '-' }}</span>
        </DescriptionsItem>
        <DescriptionsItem label="项目类型">
          <Tag color="#E6EEE8" class="type-tag">{{ projectTypeName }}</Tag>
        </DescriptionsItem>

        <DescriptionsItem label="总投资(万元)">
          {{ formatMoney(project?.totalInvestment) }}
        </DescriptionsItem>
        <DescriptionsItem label="中央资金到账(万元)">
          {{ formatMoney(project?.centralFundArrive) }}
        </DescriptionsItem>
        <DescriptionsItem label="累计完成投资(万元)">
          <span class="highlight-value">{{ formatMoney(project?.accumulatedInvestment) }}</span>
        </DescriptionsItem>

        <DescriptionsItem label="中央资金使用(万元)">
          {{ formatMoney(project?.centralFundUsed) }}
        </DescriptionsItem>
        <DescriptionsItem label="计划完成时间">
          <span class="date-value"><i class="fas fa-calendar-alt" /> {{ formatDateTime(project?.planEndTime) || '-' }}</span>
        </DescriptionsItem>
        <DescriptionsItem label="实际完成时间">
          <span class="date-value"><i class="fas fa-calendar-check" /> {{ formatDateTime(project?.actualEndTime) || '-' }}</span>
        </DescriptionsItem>

        <DescriptionsItem label="项目负责人" :span="2">
          <span class="leader-value"><i class="fas fa-user" /> {{ project?.leaderName || '-' }}</span>
        </DescriptionsItem>
        <DescriptionsItem label="联系电话">
          <span class="phone-value"><i class="fas fa-phone" /> {{ project?.leaderMobile || '-' }}</span>
        </DescriptionsItem>

        <DescriptionsItem label="创建时间">
          <span class="time-value">{{ formatDateTime(project?.createTime) || '-' }}</span>
        </DescriptionsItem>
        <DescriptionsItem label="更新时间">
          <span class="time-value">{{ formatDateTime(project?.updateTime) || '-' }}</span>
        </DescriptionsItem>
      </Descriptions>
    </Card>

    <!-- 指标信息 -->
    <div v-if="indicatorGroups.length" class="indicator-section">
      <Card class="indicator-card-section" :bordered="false">
        <template #title>
          <div class="card-title">
            <i class="fas fa-chart-bar" />
            项目指标
          </div>
        </template>
        <Collapse v-model:activeKey="activeCollapseKeys" class="indicator-collapse">
          <CollapsePanel
            v-for="group in indicatorGroups"
            :key="group.category"
            :header="`${group.categoryName} (${group.indicators.length}个指标)`"
          >
            <div class="indicator-grid">
              <div
                v-for="indicator in group.indicators"
                :key="indicator.id"
                class="indicator-item"
                :class="{ 'col-span-2': indicator.valueType === 5 }"
              >
                <div class="indicator-label">
                  <i class="fas fa-circle indicator-dot" />
                  {{ indicator.indicatorName }}：
                </div>
                <div class="indicator-value-wrap">
                  <!-- 数字类型 -->
                  <template v-if="indicator.valueType === 1">
                    <span class="indicator-value number-value">
                      {{ indicatorValuesMap[indicator.id] || '-' }}
                    </span>
                    <span v-if="indicator.unit" class="unit">{{ indicator.unit }}</span>
                  </template>

                  <!-- 字符串类型 -->
                  <template v-else-if="indicator.valueType === 2">
                    <span class="indicator-value">{{ indicatorValuesMap[indicator.id] || '-' }}</span>
                  </template>

                  <!-- 布尔类型 -->
                  <template v-else-if="indicator.valueType === 3">
                    <Tag :color="indicatorValuesMap[indicator.id] ? 'success' : 'default'" class="bool-tag">
                      <i :class="['fas', indicatorValuesMap[indicator.id] ? 'fa-check' : 'fa-times']" />
                      {{ indicatorValuesMap[indicator.id] ? '是' : '否' }}
                    </Tag>
                  </template>

                  <!-- 日期类型 -->
                  <template v-else-if="indicator.valueType === 4">
                    <span class="indicator-value date-value">
                      <i class="fas fa-calendar" />
                      {{ indicatorValuesMap[indicator.id] || '-' }}
                    </span>
                  </template>

                  <!-- 长文本类型 -->
                  <template v-else-if="indicator.valueType === 5">
                    <span class="indicator-value long-text">
                      {{ indicatorValuesMap[indicator.id] || '-' }}
                    </span>
                  </template>

                  <!-- 单选类型 -->
                  <template v-else-if="indicator.valueType === 6">
                    <div class="option-list">
                      <span
                        v-for="opt in getOptions(indicator.valueOptions)"
                        :key="opt.value"
                        class="option-tag"
                        :class="{
                          'option-selected': String(opt.value) === String(indicatorValuesMap[indicator.id])
                        }"
                      >
                        {{ opt.label }}
                      </span>
                    </div>
                  </template>

                  <!-- 多选类型 -->
                  <template v-else-if="indicator.valueType === 7">
                    <div class="option-list">
                      <span
                        v-for="opt in getOptions(indicator.valueOptions)"
                        :key="opt.value"
                        class="option-tag"
                        :class="{
                          'option-selected': (indicatorValuesMap[indicator.id] || []).includes(opt.value)
                        }"
                      >
                        {{ opt.label }}
                      </span>
                    </div>
                  </template>

                  <!-- 默认显示 -->
                  <template v-else>
                    <span class="indicator-value">{{ indicatorValuesMap[indicator.id] || '-' }}</span>
                  </template>
                </div>
              </div>
            </div>
          </CollapsePanel>
        </Collapse>
      </Card>
    </div>

    <!-- 无指标时显示提示 -->
    <div v-else-if="!loading" class="empty-indicator">
      <i class="fas fa-inbox empty-icon" />
      <p>暂无指标数据</p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.project-basic-info {
  // 项目头部
  .project-header {
    margin-bottom: 20px;
    padding: 20px 24px;
    background: linear-gradient(135deg, #2A5C45 0%, #4A8F72 100%);
    border-radius: 12px;
    color: #fff;

    .project-title-section {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .project-title {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 10px;

      .title-icon {
        font-size: 24px;
      }
    }

    .project-tags {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }

    .project-type-tag {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 20px;
      font-size: 13px;
    }

    .project-status-tag {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 13px;
      border: 1px solid;
    }
  }

  // 指标卡片
  .indicator-cards {
    margin-bottom: 16px;

    .indicator-card {
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      transition: all 0.3s ease;

      &:hover {
        box-shadow: 0 4px 16px rgba(42, 92, 69, 0.15);
        transform: translateY(-2px);
      }

      :deep(.ant-card-body) {
        padding: 20px;
      }

      :deep(.ant-statistic-title) {
        color: #8C8C8C;
        font-size: 13px;
      }

      :deep(.ant-statistic-content) {
        font-size: 24px;
      }

      .stat-icon {
        font-size: 18px;
        color: #2A5C45;
        margin-right: 8px;
      }
    }

    .indicator-card-highlight {
      background: linear-gradient(135deg, #F6FFED 0%, #F9F0FF 100%);
    }

    .execution-bar {
      height: 6px;
      background: #E6EEE8;
      border-radius: 3px;
      margin-top: 12px;
      overflow: hidden;

      .execution-bar-inner {
        height: 100%;
        border-radius: 3px;
        transition: width 0.5s ease;
      }
    }
  }

  // 进度卡片
  .progress-card {
    margin-bottom: 16px;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    :deep(.ant-card-body) {
      padding: 20px;
    }

    .progress-content {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .progress-info {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .progress-label {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 500;
        color: #262626;

        i {
          color: #2A5C45;
        }
      }

      .progress-percent {
        font-size: 28px;
        font-weight: 700;
        color: #2A5C45;
      }
    }

    .progress-bar-custom {
      :deep(.ant-progress-inner) {
        border-radius: 10px;
      }

      :deep(.ant-progress-bg) {
        border-radius: 10px;
      }
    }

    .progress-timeline {
      display: flex;
      gap: 32px;
      padding-top: 8px;
      border-top: 1px solid #F0F0F0;

      .timeline-item {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .timeline-label {
          font-size: 12px;
          color: #8C8C8C;
        }

        .timeline-value {
          font-size: 14px;
          color: #262626;
          font-weight: 500;
        }
      }
    }
  }

  // 基本信息卡片
  .info-card {
    margin-bottom: 16px;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    :deep(.ant-card-body) {
      padding: 0;
    }

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 600;
      color: #262626;
      padding: 16px 20px;
      border-bottom: 1px solid #F0F0F0;
      margin: 0;

      i {
        color: #2A5C45;
      }
    }

    .info-table {
      :deep(.ant-descriptions-item-label) {
        width: 140px;
        background: #FAFAFA;
        font-weight: 500;
        color: #595959;
      }

      :deep(.ant-descriptions-item-content) {
        min-width: 150px;
        color: #262626;
      }

      .info-value-primary {
        font-weight: 600;
        color: #262626;
      }

      .type-tag {
        border-radius: 12px;
        border: none;
      }

      .highlight-value {
        color: #52C41A;
        font-weight: 600;
      }

      .date-value, .time-value {
        display: flex;
        align-items: center;
        gap: 6px;
        color: #595959;

        i {
          color: #8C8C8C;
        }
      }

      .leader-value, .phone-value {
        display: flex;
        align-items: center;
        gap: 8px;

        i {
          color: #2A5C45;
        }
      }
    }
  }

  // 指标部分
  .indicator-section {
    margin-bottom: 16px;

    .indicator-card-section {
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

      :deep(.ant-card-head) {
        border-bottom: 1px solid #F0F0F0;
      }

      :deep(.ant-card-head-title) {
        padding: 12px 0;
      }

      .card-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 600;
        color: #262626;

        i {
          color: #2A5C45;
        }
      }
    }
  }

  .indicator-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }

  .indicator-item {
    display: flex;
    align-items: flex-start;
    padding: 8px 0;

    &.col-span-2 {
      flex-direction: row;
      grid-column: span 2;
    }
  }

  .indicator-label {
    flex-shrink: 0;
    width: 200px;
    color: #595959;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;

    .indicator-dot {
      font-size: 6px;
      color: #2A5C45;
    }
  }

  .indicator-value-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
  }

  .indicator-value {
    color: #262626;
    font-size: 14px;
    word-break: break-all;

    &.number-value {
      font-weight: 600;
      color: #2A5C45;
    }

    &.long-text {
      white-space: pre-wrap;
      max-height: 100px;
      overflow-y: auto;
      background: #F9FAFB;
      padding: 12px;
      border-radius: 8px;
      font-size: 13px;
      line-height: 1.6;
    }

    &.date-value {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #595959;

      i {
        color: #8C8C8C;
      }
    }
  }

  .unit {
    margin-left: 4px;
    color: #8C8C8C;
    font-size: 12px;
  }

  .bool-tag {
    border-radius: 12px;
  }

  .option-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .option-tag {
    padding: 4px 12px;
    border-radius: 16px;
    font-size: 13px;
    background: #F5F5F5;
    color: #595959;
    border: 1px solid #E8E8E8;
    transition: all 0.2s ease;

    &.option-selected {
      background: linear-gradient(135deg, #E6F7EF 0%, #D4F0E6 100%);
      color: #2A5C45;
      border-color: #2A5C45;
      font-weight: 500;
    }
  }

  .empty-indicator {
    text-align: center;
    color: #BFbfbf;
    padding: 40px 0;

    .empty-icon {
      font-size: 48px;
      margin-bottom: 12px;
      opacity: 0.5;
    }

    p {
      margin: 0;
      font-size: 14px;
    }
  }
}

:deep(.ant-collapse) {
  background: transparent;
  border: none;

  .ant-collapse-item {
    border: 1px solid #E8E8E8;
    margin-bottom: 12px;
    border-radius: 8px;
    overflow: hidden;
  }

  .ant-collapse-header {
    background: #FAFAFA;
    padding: 14px 16px !important;
    font-weight: 500;
    font-size: 14px;
  }

  .ant-collapse-content-box {
    padding: 16px !important;
  }
}
</style>
