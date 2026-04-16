import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DeclareIndicatorJointRuleApi {
  /** 指标上期对比规则 */
  export interface JointRule {
    id?: number;
    ruleName: string;
    projectType?: number;
    triggerTiming: string;
    processNode: string;
    ruleConfig: string;
    status: number;
    createTime?: string;
  }

  /** 指标上期对比规则保存请求 */
  export interface JointRuleSaveParams {
    id?: number;
    ruleName: string;
    projectType?: number;
    triggerTiming: string;
    processNode: string;
    ruleConfig: string;
    status: number;
  }
}

/** 获取上期对比规则分页列表 */
export async function getJointRulePage(params: PageParam & {
  ruleName?: string;
  projectType?: number;
  businessType?: string;
  status?: number;
}) {
  return requestClient.get<PageResult<DeclareIndicatorJointRuleApi.JointRule>>(
    '/declare/indicator-joint-rule/page',
    { params }
  );
}

/** 获取上期对比规则详情 */
export async function getJointRule(id: number) {
  return requestClient.get<DeclareIndicatorJointRuleApi.JointRule>(
    `/declare/indicator-joint-rule/get?id=${id}`
  );
}

/** 创建上期对比规则 */
export async function createJointRule(data: DeclareIndicatorJointRuleApi.JointRuleSaveParams) {
  return requestClient.post('/declare/indicator-joint-rule/create', data);
}

/** 更新上期对比规则 */
export async function updateJointRule(data: DeclareIndicatorJointRuleApi.JointRuleSaveParams) {
  return requestClient.put('/declare/indicator-joint-rule/update', data);
}

/** 删除上期对比规则 */
export async function deleteJointRule(id: number) {
  return requestClient.delete(`/declare/indicator-joint-rule/delete?id=${id}`);
}

/** 获取上期对比规则列表 */
export async function getJointRuleList() {
  return requestClient.get<DeclareIndicatorJointRuleApi.JointRule[]>(
    '/declare/indicator-joint-rule/list'
  );
}

/** 获取启用的上期对比规则列表 */
export async function getEnabledJointRules(params?: {
  projectType?: number;
  processNode?: string;
  triggerTiming?: string;
}) {
  return requestClient.get<DeclareIndicatorJointRuleApi.JointRule[]>(
    '/declare/indicator-joint-rule/enabled-list',
    { params }
  );
}
