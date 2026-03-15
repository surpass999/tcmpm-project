import type { PageParam, PageResult } from '@vben/request';
import type { Dayjs } from 'dayjs';

import { requestClient } from '#/api/request';

export namespace DeclareFilingApi {
  /** 项目备案核心信息信息 */
  export interface Filing {
    id: number; // 备案主键
    processInstanceId?: string; // 流程实例ID
    filingCode?: string; // 备案编号
    socialCreditCode?: string; // 社会信用代码
    medicalLicenseNo?: string; // 执业许可证号
    orgName?: string; // 机构名称
    projectType?: number; // 项目类型
    validStartTime?: number | string | Dayjs; // 有效期开始时间（时间戳或字符串）
    validEndTime?: number | string | Dayjs; // 有效期结束时间（时间戳或字符串）
    planEndTime?: number | string | Dayjs; // 项目计划完成时间（时间戳或字符串）
    constructionContent?: string; // 建设内容
    filingStatus?: string; // 备案状态： DRAFT=草稿，SUBMITTED=已提交
    provinceReviewOpinion: string; // 省审核意见
    provinceReviewTime: string | number | Dayjs; // 省审核时间
    provinceReviewerId: number; // 省级审核人
    expertReviewOpinion: string; // 专家论证意见
    expertReviewerIds: string; // 论证专家
    filingArchiveTime: string | number | Dayjs; // 归档时间
    /** 创建者（用户ID） */
    creator?: number;
    /** 指标值列表（用于提交时包含指标值） */
    indicatorValues?: IndicatorValueItem[];
  }

  /** 指标值项 */
  export interface IndicatorValueItem {
    indicatorId: number;
    indicatorCode: string;
    valueType: number;
    valueNum?: string;
    valueStr?: string;
    valueBool?: boolean;
    valueDate?: string;
    valueDateStart?: string;
    valueDateEnd?: string;
    valueText?: string;
  }
}

/** 查询项目备案核心信息分页 */
export function getFilingPage(params: PageParam) {
  return requestClient.get<PageResult<DeclareFilingApi.Filing>>(
    '/declare/filing/page',
    { params },
  );
}

/** 查询项目备案核心信息详情 */
export function getFiling(id: number) {
  return requestClient.get<DeclareFilingApi.Filing>(
    `/declare/filing/get?id=${id}`,
  );
}

/** 新增项目备案核心信息 */
export function createFiling(data: DeclareFilingApi.Filing) {
  return requestClient.post('/declare/filing/create', data);
}

/** 修改项目备案核心信息 */
export function updateFiling(data: DeclareFilingApi.Filing) {
  return requestClient.put('/declare/filing/update', data);
}

/** 删除项目备案核心信息 */
export function deleteFiling(id: number) {
  return requestClient.delete(`/declare/filing/delete?id=${id}`);
}

/** 批量删除项目备案核心信息 */
export function deleteFilingList(ids: number[]) {
  return requestClient.delete(
    `/declare/filing/delete-list?ids=${ids.join(',')}`,
  );
}

/** 导出项目备案核心信息 */
export function exportFiling(params: any) {
  return requestClient.download('/declare/filing/export-excel', { params });
}

/** 提交审核（发起流程） */
export function submitFiling(id: number, processDefinitionKey?: string) {
  return requestClient.post<string>(
    '/bpm/process/start',
    { 
      businessId: id, 
      businessType: 'filing:approval',
      processDefinitionKey: processDefinitionKey || undefined 
    },
  );
}