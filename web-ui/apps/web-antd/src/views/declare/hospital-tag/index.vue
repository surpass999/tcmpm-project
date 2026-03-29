<script lang="ts" setup>
import type { HospitalTag } from '#/api/declare/hospital-tag';

import { ref, computed } from 'vue';

import { ContentWrap, Page, useVbenModal } from '@vben/common-ui';

import {
  Card,
  Col,
  Form,
  FormItem,
  Input,
  InputNumber,
  message as antMessage,
  Modal,
  Radio,
  RadioGroup,
  Row,
  Select,
  Tabs,
  TabPane,
} from 'ant-design-vue';

import {
  createHospitalTag,
  deleteHospitalTag,
  getHospitalTagStatistics,
  updateHospitalTag,
} from '#/api/declare/hospital-tag';

/** 当前激活的分类 Tab */
const activeCategory = ref('region');

/** 所有标签 */
const allTags = ref<HospitalTag[]>([]);

/** 按分类过滤 */
const regionTags = computed(() =>
  allTags.value.filter((t) => t.tagCategory === 'region'),
);
const levelTags = computed(() =>
  allTags.value.filter((t) => t.tagCategory === 'level'),
);
const featureTags = computed(() =>
  allTags.value.filter((t) => t.tagCategory === 'feature'),
);

/** 加载标签数据 */
const loadTags = async () => {
  try {
    allTags.value = await getHospitalTagStatistics();
  } catch {
    antMessage.error('加载标签失败');
  }
};

/** Tab 切换 */
const handleTabChange = (key: string) => {
  activeCategory.value = key;
};

// ===== 新增/编辑表单弹窗 =====
const formRef = ref();
const formVisible = ref(false);
const formTitle = ref('新增标签');
const editingTag = ref<HospitalTag | undefined>();

const formData = ref({
  tagCode: '',
  tagName: '',
  tagCategory: 'region',
  tagType: 1,
  sort: 0,
});

const openForm = (tag?: HospitalTag) => {
  editingTag.value = tag;
  formTitle.value = tag ? '编辑标签' : '新增标签';
  formData.value = tag
    ? {
        tagCode: tag.tagCode,
        tagName: tag.tagName,
        tagCategory: tag.tagCategory,
        tagType: tag.tagType,
        sort: tag.sort,
      }
    : {
        tagCode: '',
        tagName: '',
        tagCategory: activeCategory.value,
        tagType: 1,
        sort: 0,
      };
  formVisible.value = true;
};

const submitForm = async () => {
  try {
    if (editingTag.value?.id) {
      await updateHospitalTag({
        id: editingTag.value.id,
        tagName: formData.value.tagName,
        tagType: formData.value.tagType,
        sort: formData.value.sort,
      });
    } else {
      await createHospitalTag({
        tagCode: formData.value.tagCode,
        tagName: formData.value.tagName,
        tagCategory: formData.value.tagCategory,
        tagType: formData.value.tagType,
        sort: formData.value.sort,
      });
    }
    formVisible.value = false;
    antMessage.success('操作成功');
    loadTags();
  } catch {
    antMessage.error('操作失败');
  }
};

const handleDelete = async (id: number) => {
  try {
    await deleteHospitalTag(id);
    antMessage.success('删除成功');
    loadTags();
  } catch {
    antMessage.error('删除失败');
  }
};

// ===== 分配标签弹窗 =====
const assignVisible = ref(false);
const searchHospital = ref('');
const assignTagIds = ref<number[]>([]);

const openAssignDialog = () => {
  assignVisible.value = true;
};

const submitAssign = async () => {
  if (!searchHospital.value) {
    antMessage.warning('请输入医院编码');
    return;
  }
  try {
    // TODO: 调用实际的分配 API，接口参数待确认
    antMessage.success('分配成功');
    assignVisible.value = false;
  } catch {
    antMessage.error('分配失败');
  }
};

// 初始化
loadTags();
</script>

<template>
  <Page auto-content-height>
    <ContentWrap title="医院标签管理">
      <!-- 工具栏 -->
      <Row :gutter="10" class="mb-4">
        <Col :span="1.5">
          <a-button type="primary" @click="openForm()">
            新增
          </a-button>
        </Col>
        <Col :span="1.5">
          <a-button type="warning" @click="openAssignDialog()">
            分配标签
          </a-button>
        </Col>
      </Row>

      <!-- 分类卡片 -->
      <Tabs v-model:activeKey="activeCategory" @change="handleTabChange">
        <TabPane key="region" tab="区域">
          <Row :gutter="20">
            <Col v-for="tag in regionTags" :key="tag.id" :span="4">
              <Card hoverable class="tag-card">
                <div class="tag-name">{{ tag.tagName }}</div>
                <div class="tag-count">{{ tag.hospitalCount || 0 }} 家医院</div>
                <div class="tag-actions">
                  <a-button type="link" size="small" @click="openForm(tag)">编辑</a-button>
                  <a-button type="link" size="small" danger @click="handleDelete(tag.id!)">删除</a-button>
                </div>
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane key="level" tab="等级">
          <Row :gutter="20">
            <Col v-for="tag in levelTags" :key="tag.id" :span="4">
              <Card hoverable class="tag-card">
                <div class="tag-name">{{ tag.tagName }}</div>
                <div class="tag-count">{{ tag.hospitalCount || 0 }} 家医院</div>
                <div class="tag-actions">
                  <a-button type="link" size="small" @click="openForm(tag)">编辑</a-button>
                  <a-button type="link" size="small" danger @click="handleDelete(tag.id!)">删除</a-button>
                </div>
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane key="feature" tab="特征">
          <Row :gutter="20">
            <Col v-for="tag in featureTags" :key="tag.id" :span="4">
              <Card hoverable class="tag-card">
                <div class="tag-name">{{ tag.tagName }}</div>
                <div class="tag-count">{{ tag.hospitalCount || 0 }} 家医院</div>
                <div class="tag-actions">
                  <a-button type="link" size="small" @click="openForm(tag)">编辑</a-button>
                  <a-button type="link" size="small" danger @click="handleDelete(tag.id!)">删除</a-button>
                </div>
              </Card>
            </Col>
          </Row>
        </TabPane>
      </Tabs>
    </ContentWrap>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:open="formVisible"
      :title="formTitle"
      width="500px"
      :footer="null"
    >
      <Form layout="horizontal" class="mt-4">
        <FormItem label="标签编码" name="tagCode">
          <Input
            v-model:value="formData.tagCode"
            placeholder="请输入标签编码，如 REGION_EAST"
            :disabled="!!editingTag"
          />
        </FormItem>
        <FormItem label="标签名称" name="tagName">
          <Input v-model:value="formData.tagName" placeholder="请输入标签名称，如 华东区" />
        </FormItem>
        <FormItem label="标签分类" name="tagCategory">
          <Select v-model:value="formData.tagCategory" :disabled="!!editingTag">
            <Select.Option value="region">区域</Select.Option>
            <Select.Option value="level">等级</Select.Option>
            <Select.Option value="feature">特征</Select.Option>
            <Select.Option value="attribute">属性</Select.Option>
          </Select>
        </FormItem>
        <FormItem label="标签类型" name="tagType">
          <RadioGroup v-model:value="formData.tagType">
            <Radio :value="1">单选</Radio>
            <Radio :value="2">多选</Radio>
          </RadioGroup>
        </FormItem>
        <FormItem label="排序" name="sort">
          <InputNumber v-model:value="formData.sort" :min="0" :max="9999" />
        </FormItem>
        <FormItem class="text-right">
          <a-button @click="formVisible = false">取 消</a-button>
          <a-button type="primary" class="ml-2" @click="submitForm">确 定</a-button>
        </FormItem>
      </Form>
    </Modal>

    <!-- 分配标签弹窗 -->
    <Modal
      v-model:open="assignVisible"
      title="分配医院标签"
      width="800px"
      :footer="null"
    >
      <Form layout="vertical" class="mt-4">
        <FormItem label="医院编码">
          <Input v-model:value="searchHospital" placeholder="请输入医院编码" />
        </FormItem>
        <FormItem label="选择标签">
          <Select v-model:value="assignTagIds" multiple placeholder="请选择标签">
            <Select.Option v-for="tag in allTags" :key="tag.id" :value="tag.id!">
              {{ tag.tagName }} ({{ tag.tagCategory }})
            </Select.Option>
          </Select>
        </FormItem>
        <FormItem class="text-right">
          <a-button @click="assignVisible = false">取 消</a-button>
          <a-button type="primary" class="ml-2" @click="submitAssign">确 定</a-button>
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.tag-card {
  margin-bottom: 15px;
  text-align: center;
}
.tag-name {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 8px;
}
.tag-count {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}
.tag-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style>
