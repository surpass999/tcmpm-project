<script lang="ts" setup>
import type { DeclareProjectApi } from '#/api/declare/project';

import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Spin, Tabs } from 'ant-design-vue';

import { getProject, getProjectProcessListByProjectId } from '#/api/declare/project';

import ProjectBasicInfo from './components/ProjectBasicInfo.vue';
import ProjectProcessList from './components/ProjectProcessList.vue';

// 弹窗 API
const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  showCancelButton: false,
  confirmText: '关闭',
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      // 关闭时清理数据
      projectDetail.value = null;
      processList.value = [];
      return;
    }
    // 打开时加载数据
    await loadProjectDetail();
  },
});

// 导出 setData 方法供父组件使用
defineExpose({ setData: modalApi.setData });

const loading = ref(false);

// 项目详情数据
const projectDetail = ref<DeclareProjectApi.Project | null>(null);

// 过程记录列表
const processList = ref<DeclareProjectApi.ProjectProcess[]>([]);

// 当前激活的 Tab
const activeTab = ref('basic');

// 加载项目详情
async function loadProjectDetail() {
  const data = modalApi.getData<{ projectId: number }>();
  if (!data?.projectId) return;

  loading.value = true;
  try {
    const result = await getProject(data.projectId);
    projectDetail.value = result;
    // 加载过程记录
    const processData = await getProjectProcessListByProjectId(data.projectId);
    processList.value = processData;
  } catch (e) {
    console.error('加载项目详情失败', e);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <Modal
    class="w-[90%]"
    :title="`项目详情 - ${projectDetail?.projectName || ''}`"
  >
    <Spin :spinning="loading">
      <Tabs v-model:activeKey="activeTab" class="detail-tabs">
        <Tabs.TabPane key="basic" tab="基本信息">
          <ProjectBasicInfo v-if="projectDetail" :project="projectDetail" />
        </Tabs.TabPane>
        <Tabs.TabPane key="process" tab="过程记录">
          <ProjectProcessList
            v-if="projectDetail"
            :project-id="projectDetail.id"
            :process-list="processList"
            @refresh="loadProjectDetail"
          />
        </Tabs.TabPane>
      </Tabs>
    </Spin>
  </Modal>
</template>

<style scoped>
.detail-tabs :deep(.ant-tabs-content) {
  padding-top: 16px;
}
</style>
