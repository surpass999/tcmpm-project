<script lang="ts" setup>
import type { DeclareProjectApi } from '#/api/declare/project';

import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';

import { Card, Tabs, Spin } from 'ant-design-vue';

import { getProject, getProjectProcessListByProjectId } from '#/api/declare/project';

import ProjectBasicInfo from './components/ProjectBasicInfo.vue';
import ProjectProcessList from './components/ProjectProcessList.vue';

// 路由
const route = useRoute();
const router = useRouter();

// 项目ID
const projectId = computed(() => Number(route.params?.id));

// 加载状态
const loading = ref(false);

// 项目详情数据
const projectDetail = ref<DeclareProjectApi.Project | null>(null);

// 过程记录列表
const processList = ref<DeclareProjectApi.ProjectProcess[]>([]);

// 当前激活的 Tab
const activeTab = ref('basic');

// 加载项目详情
async function loadProjectDetail() {
  if (!projectId.value) return;

  loading.value = true;
  try {
    const data = await getProject(projectId.value);
    projectDetail.value = data;
  } catch (e) {
    console.error('加载项目详情失败', e);
  } finally {
    loading.value = false;
  }
}

// 加载过程记录列表
async function loadProcessList() {
  if (!projectId.value) return;

  try {
    const data = await getProjectProcessListByProjectId(projectId.value);
    processList.value = data || [];
  } catch (e) {
    console.error('加载过程记录失败', e);
  }
}

// 返回列表页
function goBack() {
  router.push('/declare/project');
}

// 页面加载时获取数据
onMounted(() => {
  if (projectId.value) {
    loadProjectDetail();
    loadProcessList();
  }
});
</script>

<template>
  <Page :auto-content-height="false" content-class="project-detail-page">
    <Spin :spinning="loading">
      <div class="page-header">
        <div class="header-left">
          <a-button @click="goBack">
            <template #icon>
              <span class="anticon">←</span>
            </template>
            返回列表
          </a-button>
          <h2 class="page-title">{{ projectDetail?.projectName || '项目详情' }}</h2>
        </div>
      </div>

      <Card :bordered="false" class="detail-card">
        <Tabs v-model:activeKey="activeTab" type="card">
          <Tabs.TabPane key="basic" tab="基本信息">
            <ProjectBasicInfo
              v-if="projectDetail"
              :project="projectDetail"
            />
          </Tabs.TabPane>

          <Tabs.TabPane key="process" tab="过程记录">
            <ProjectProcessList
              v-if="projectId"
              :project-id="projectId"
              :process-list="processList"
              @refresh="loadProcessList"
            />
          </Tabs.TabPane>

          <Tabs.TabPane key="rectification" tab="整改记录">
            <div class="empty-tip">暂无整改记录</div>
          </Tabs.TabPane>

          <Tabs.TabPane key="fund" tab="资金使用">
            <div class="empty-tip">暂无资金使用记录</div>
          </Tabs.TabPane>

          <Tabs.TabPane key="document" tab="项目文档">
            <div class="empty-tip">暂无项目文档</div>
          </Tabs.TabPane>
        </Tabs>
      </Card>
    </Spin>
  </Page>
</template>

<style lang="scss" scoped>
.project-detail-page {
  padding: 16px;
  background: #f5f5f5;
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: white;
  border-radius: 4px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.detail-card {
  :deep(.ant-card-body) {
    padding: 0;
  }

  :deep(.ant-tabs-nav) {
    padding: 0 16px;
    margin-bottom: 0;
  }

  :deep(.ant-tabs-content-holder) {
    padding: 16px;
    background: white;
  }
}

.empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: #999;
  font-size: 14px;
}
</style>
