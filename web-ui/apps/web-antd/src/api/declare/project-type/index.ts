import { requestClient } from '#/api/request';

/**
 * 项目类型定义
 */
export interface ProjectType {
  id: number;
  typeCode: string;
  typeValue: number;
  name: string;
  title: string;
  description?: string;
  icon?: string;
  color?: string;
  sort: number;
  status: number;
}

/**
 * 创建项目类型
 */
export function createProjectType(data: {
  typeCode: string;
  typeValue: number;
  name: string;
  title: string;
  description?: string;
  icon?: string;
  color?: string;
  sort?: number;
}) {
  return requestClient.post<number>('/declare/project-type/create', data);
}

/**
 * 更新项目类型
 */
export function updateProjectType(data: {
  id: number;
  name: string;
  title: string;
  description?: string;
  icon?: string;
  color?: string;
  sort?: number;
  status?: number;
}) {
  return requestClient.put('/declare/project-type/update', data);
}

/**
 * 删除项目类型
 */
export function deleteProjectType(id: number) {
  return requestClient.delete(`/declare/project-type/delete?id=${id}`);
}

/**
 * 获取项目类型详情
 */
export function getProjectType(id: number) {
  return requestClient.get<ProjectType>(`/declare/project-type/get?id=${id}`);
}

/**
 * 获取项目类型分页列表
 */
export function getProjectTypePage(params: {
  pageNo?: number;
  pageSize?: number;
  typeCode?: string;
  name?: string;
  status?: number;
}) {
  return requestClient.get('/declare/project-type/page', { params });
}

/**
 * 获取项目类型列表（启用状态）
 */
export function getProjectTypeList() {
  return requestClient.get<ProjectType[]>('/declare/project-type/list');
}

/**
 * 获取项目类型简化列表（用于下拉选择）
 */
export function getProjectTypeSimpleList() {
  return requestClient.get<ProjectType[]>('/declare/project-type/simple-list');
}
