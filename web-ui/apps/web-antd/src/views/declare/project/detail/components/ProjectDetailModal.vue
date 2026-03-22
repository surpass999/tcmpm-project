<script lang="ts" setup>
import type { DeclareProjectApi } from '#/api/declare/project';

import { nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Spin, Tabs } from 'ant-design-vue';

import { getProject, getProjectProcessListByProjectId } from '#/api/declare/project';

import ProjectBasicInfo from './ProjectBasicInfo.vue';
import ProjectProcessList from './ProjectProcessList.vue';
import ProjectRectificationList from './ProjectRectificationList.vue';
import ProjectReviewRecords from './ProjectReviewRecords.vue';

// 业务类型 - 项目 (1=备案，2=项目，3=成果)
const BUSINESS_TYPE = 2;

// 弹窗 API
const [Modal, modalApi] = useVbenModal({
  destroyOnClose: true,
  showCancelButton: false,
  confirmText: '关闭',
  onConfirm() {
    // 点击关闭按钮时关闭弹窗
    modalApi.close();
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      // 关闭时清理数据 - 使用 nextTick 确保在正确的渲染周期执行
      await nextTick();
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
    // 注意：评审记录和整改记录组件会通过 watch 自动加载
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
            :project-id="projectDetail.id!"
            :process-list="processList"
            @refresh="loadProjectDetail"
          />
        </Tabs.TabPane>
        <Tabs.TabPane key="review" tab="评审记录">
          <ProjectReviewRecords
            v-if="projectDetail"
            :business-type="BUSINESS_TYPE"
            :business-id="projectDetail.id!"
          />
        </Tabs.TabPane>
        <Tabs.TabPane key="rectification" tab="整改记录">
          <ProjectRectificationList
            v-if="projectDetail"
            :project-id="projectDetail.id!"
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
