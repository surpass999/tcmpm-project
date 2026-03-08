import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DeclareExpertApi {
  /** 专家信息 */
  export interface Expert {
    id?: number; // 主键
    userId?: number; // 关联系统用户ID
    userName?: string; // 关联用户名称（显示用）
    expertName?: string; // 专家姓名
    idCard?: string; // 身份证号
    gender?: number; // 性别：1=男，2=女
    birthDate?: string; // 出生日期
    phone?: string; // 联系电话
    email?: string; // 电子邮箱
    workUnit?: string; // 工作单位
    jobTitle?: string; // 职务
    professionalTitle?: string; // 职称
    department?: string; // 所在部门/科室
    expertType?: number; // 专家类型
    specialties?: string; // 专业领域
    expertiseAreas?: string; // 擅长方向
    educationBackground?: string; // 学历
    degree?: string; // 学位
    graduatedFrom?: string; // 毕业院校
    qualificationCert?: string; // 资格证书编号
    certAttachId?: number; // 资质证书附件ID
    reviewCount?: number; // 累计评审次数
    lastReviewTime?: string; // 上次评审时间
    reviewScore?: number; // 平均评审评分
    status?: number; // 状态
    statusRemark?: string; // 状态说明
    remark?: string; // 备注
    createTime?: string; // 创建时间
  }

  /** 专家列表请求 */
  export interface ExpertListReq {
    expertName?: string;
    expertType?: number;
    status?: number;
    workUnit?: string;
  }
}

/** 查询专家分页 */
export function getExpertPage(params: PageParam & DeclareExpertApi.ExpertListReq) {
  return requestClient.get<PageResult<DeclareExpertApi.Expert>>(
    '/declare/expert/page',
    { params },
  );
}

/** 查询专家详情 */
export function getExpert(id: number) {
  return requestClient.get<DeclareExpertApi.Expert>(
    `/declare/expert/get?id=${id}`,
  );
}

/** 根据用户ID获取专家 */
export function getExpertByUserId(userId: number) {
  return requestClient.get<DeclareExpertApi.Expert>(
    `/declare/expert/get-by-user?userId=${userId}`,
  );
}

/** 新增专家 */
export function createExpert(data: DeclareExpertApi.Expert) {
  return requestClient.post('/declare/expert/create', data);
}

/** 修改专家 */
export function updateExpert(data: DeclareExpertApi.Expert) {
  return requestClient.put('/declare/expert/update', data);
}

/** 删除专家 */
export function deleteExpert(id: number) {
  return requestClient.delete(`/declare/expert/delete?id=${id}`);
}

/** 获取专家列表 */
export function getExpertList(params: DeclareExpertApi.ExpertListReq) {
  return requestClient.get<DeclareExpertApi.Expert[]>(
    '/declare/expert/list',
    { params },
  );
}

/** 获取专家简单列表（下拉选择用） */
export function getExpertSimpleList() {
  return requestClient.get<DeclareExpertApi.Expert[]>('/declare/expert/simple-list');
}

/** 修改专家状态 */
export function updateExpertStatus(id: number, status: number) {
  return requestClient.put(`/declare/expert/update-status?id=${id}&status=${status}`);
}

/** 获取绑定用户的专家userId列表 */
export function getExpertUserIds() {
  return requestClient.get<number[]>('/declare/expert/user-ids');
}
