import { requestClient } from '#/api/request';

// ========== Hospital Tag ==========

/**
 * 标签定义
 */
export interface HospitalTag {
  id: number;
  tagCode: string;
  tagName: string;
  tagCategory: string;
  tagType: number;
  parentId?: number;
  sort: number;
  status: number;
  hospitalCount?: number;
}

/**
 * 创建标签
 */
export function createHospitalTag(data: {
  tagCode: string;
  tagName: string;
  tagCategory: string;
  tagType?: number;
  parentId?: number;
  sort?: number;
}) {
  return requestClient.post<number>('/declare/hospital-tag/create', data);
}

/**
 * 更新标签
 */
export function updateHospitalTag(data: {
  id: number;
  tagName: string;
  tagType?: number;
  parentId?: number;
  sort?: number;
  status?: number;
}) {
  return requestClient.put('/declare/hospital-tag/update', data);
}

/**
 * 删除标签
 */
export function deleteHospitalTag(id: number) {
  return requestClient.delete(`/declare/hospital-tag/delete?id=${id}`);
}

/**
 * 获取标签详情
 */
export function getHospitalTag(id: number) {
  return requestClient.get<HospitalTag>(`/declare/hospital-tag/get?id=${id}`);
}

/**
 * 获取标签列表
 */
export function getHospitalTagList() {
  return requestClient.get<HospitalTag[]>('/declare/hospital-tag/list');
}

/**
 * 获取标签列表（按分类）
 */
export function getHospitalTagListByCategory(category: string) {
  return requestClient.get<HospitalTag[]>(
    `/declare/hospital-tag/list-by-category?category=${category}`,
  );
}

/**
 * 获取标签统计
 */
export function getHospitalTagStatistics() {
  return requestClient.get<HospitalTag[]>('/declare/hospital-tag/statistics');
}

/**
 * 分配医院标签
 */
export function assignHospitalTags(data: {
  hospitalCode: string;
  tagIds: number[];
}) {
  return requestClient.post('/declare/hospital-tag/assign', data);
}

/**
 * 获取医院的标签列表
 */
export function getHospitalTags(hospitalCode: string) {
  return requestClient.get<HospitalTag[]>(
    `/declare/hospital-tag/hospital-tags?hospitalCode=${hospitalCode}`,
  );
}

/**
 * 标签分类选项
 */
export const TAG_CATEGORY_OPTIONS = [
  { label: '区域', value: 'region' },
  { label: '等级', value: 'level' },
  { label: '特征', value: 'feature' },
  { label: '属性', value: 'attribute' },
];
