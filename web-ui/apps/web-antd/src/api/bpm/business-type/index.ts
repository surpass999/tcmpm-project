import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace BpmBusinessTypeApi {
  /** 业务类型 */
  export interface BusinessType {
    id?: number;
    businessType: string;
    businessName: string;
    processDefinitionKey: string;
    processCategory: string;
    description: string;
    enabled: number;
    sort: number;
    createTime?: string;
    updateTime?: string;
  }

  /** 业务类型保存请求 */
  export interface BusinessTypeSaveParams {
    id?: number;
    businessType: string;
    businessName: string;
    processDefinitionKey: string;
    processCategory: string;
    description: string;
    enabled: number;
    sort: number;
  }
}

/** 获取业务类型分页列表 */
export async function getBusinessTypePage(params: PageParam & {
  businessType?: string;
  businessName?: string;
  processDefinitionKey?: string;
  processCategory?: string;
  enabled?: number;
}) {
  return requestClient.get<PageResult<BpmBusinessTypeApi.BusinessType>>(
    '/bpm/business-type/page',
    { params }
  );
}

/** 获取业务类型详情 */
export async function getBusinessType(id: number) {
  return requestClient.get<BpmBusinessTypeApi.BusinessType>(
    `/bpm/business-type/get?id=${id}`
  );
}

/** 根据业务类型标识获取业务类型 */
export async function getBusinessTypeByType(businessType: string) {
  return requestClient.get<BpmBusinessTypeApi.BusinessType>(
    `/bpm/business-type/get-by-type?businessType=${businessType}`
  );
}

/** 根据流程定义Key获取业务类型列表 */
export async function getBusinessTypeListByProcessKey(processDefinitionKey: string) {
  return requestClient.get<BpmBusinessTypeApi.BusinessType[]>(
    `/bpm/business-type/list-by-process?processDefinitionKey=${processDefinitionKey}`
  );
}

/** 根据分类获取业务类型列表 */
export async function getBusinessTypeListByCategory(category: string) {
  return requestClient.get<BpmBusinessTypeApi.BusinessType[]>(
    `/bpm/business-type/list-by-category?category=${category}`
  );
}

/** 获取所有已启用的业务类型列表 */
export async function getAllEnabledBusinessTypeList() {
  return requestClient.get<BpmBusinessTypeApi.BusinessType[]>(
    '/bpm/business-type/list-all-enabled'
  );
}

/** 根据业务类型标识获取流程定义Key */
export async function getProcessDefinitionKey(businessType: string) {
  return requestClient.get<string>(
    `/bpm/business-type/get-process-key?businessType=${businessType}`
  );
}

/** 创建业务类型 */
export async function createBusinessType(data: BpmBusinessTypeApi.BusinessTypeSaveParams) {
  return requestClient.post('/bpm/business-type/create', data);
}

/** 更新业务类型 */
export async function updateBusinessType(data: BpmBusinessTypeApi.BusinessTypeSaveParams) {
  return requestClient.put('/bpm/business-type/update', data);
}

/** 删除业务类型 */
export async function deleteBusinessType(id: number) {
  return requestClient.delete(`/bpm/business-type/delete?id=${id}`);
}
