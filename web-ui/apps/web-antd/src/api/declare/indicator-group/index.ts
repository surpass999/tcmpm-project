import type { PageParam, PageResult } from '@vben/request';
import { requestClient } from '#/api/request';

export namespace DeclareIndicatorGroupApi {
  /** 指标分组 */
  export interface IndicatorGroup {
    id?: number;
    parentId?: number;
    groupCode: string;
    groupName: string;
    groupLevel?: number;
    projectType?: number;
    projectTypeName?: string;
    groupPrefix?: string;
    description?: string;
    sort?: number;
    status?: number;
    createTime?: string;
    children?: IndicatorGroup[];
  }

  /** 指标分组保存请求 */
  export interface IndicatorGroupSaveParams {
    id?: number;
    parentId?: number;
    groupCode: string;
    groupName: string;
    groupLevel?: number;
    projectType?: number;
    groupPrefix?: string;
    description?: string;
    sort?: number;
    status?: number;
  }
}

/** 获取指标分组分页列表 */
export async function getIndicatorGroupPage(params: PageParam & {
  groupCode?: string;
  groupName?: string;
  groupLevel?: number;
  projectType?: number;
  status?: number;
}) {
  return requestClient.get<PageResult<DeclareIndicatorGroupApi.IndicatorGroup>>(
    '/declare/indicator-group/page',
    { params }
  );
}

/** 获取指标分组详情 */
export async function getIndicatorGroup(id: number) {
  return requestClient.get<DeclareIndicatorGroupApi.IndicatorGroup>(
    `/declare/indicator-group/get?id=${id}`
  );
}

/** 获取树形指标分组列表 */
export async function getIndicatorGroupTree() {
  return requestClient.get<DeclareIndicatorGroupApi.IndicatorGroup[]>(
    '/declare/indicator-group/tree'
  );
}

/** 获取所有启用的指标分组列表（扁平） */
export async function getIndicatorGroupList() {
  return requestClient.get<DeclareIndicatorGroupApi.IndicatorGroup[]>(
    '/declare/indicator-group/list'
  );
}

/** 获取一级指标分组列表 */
export async function getLevelOneIndicatorGroupList(projectType?: number) {
  return requestClient.get<DeclareIndicatorGroupApi.IndicatorGroup[]>(
    '/declare/indicator-group/list-level-one',
    projectType !== undefined ? { params: { projectType } } : {}
  );
}

/** 获取二级指标分组列表 */
export async function getLevelTwoIndicatorGroupList() {
  return requestClient.get<DeclareIndicatorGroupApi.IndicatorGroup[]>(
    '/declare/indicator-group/list-level-two'
  );
}

/** 根据一级分组ID获取二级分组 */
export async function getIndicatorGroupListByParentId(parentId: number) {
  return requestClient.get<DeclareIndicatorGroupApi.IndicatorGroup[]>(
    `/declare/indicator-group/list-by-parent-id?parentId=${parentId}`
  );
}

/** 根据项目类型获取一级分组 */
export async function getIndicatorGroupListByProjectType(projectType: number) {
  return requestClient.get<DeclareIndicatorGroupApi.IndicatorGroup[]>(
    `/declare/indicator-group/list-by-project-type?projectType=${projectType}`
  );
}

/** 根据项目类型获取树形分组列表 */
export async function getIndicatorGroupTreeByProjectType(projectType?: number) {
  const params = projectType !== undefined ? `?projectType=${projectType}` : '';
  return requestClient.get<DeclareIndicatorGroupApi.IndicatorGroup[]>(
    `/declare/indicator-group/tree-by-project-type${params}`
  );
}

/** 创建指标分组 */
export async function createIndicatorGroup(data: DeclareIndicatorGroupApi.IndicatorGroupSaveParams) {
  return requestClient.post('/declare/indicator-group/create', data);
}

/** 更新指标分组 */
export async function updateIndicatorGroup(data: DeclareIndicatorGroupApi.IndicatorGroupSaveParams) {
  return requestClient.put('/declare/indicator-group/update', data);
}

/** 删除指标分组 */
export async function deleteIndicatorGroup(id: number) {
  return requestClient.delete(`/declare/indicator-group/delete?id=${id}`);
}
