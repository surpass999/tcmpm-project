import type { BusinessType } from '@/constants/bpm/business-type';
import type { ProcessDefinitionKey } from '@/constants/bpm/process-definition';
import {
  getProcessDefinitionKey,
} from '@/constants/bpm';
import {
  getAvailableActions,
  getAvailableActionsBatch,
} from '../action';
import { getTaskByBusiness } from '../task';

/**
 * BPM 核心 API 封装
 * 提供统一的业务操作接口
 */
export class BpmCoreApi {
  /**
   * 获取可用操作
   */
  static async getActions(businessType: BusinessType, businessId: number) {
    return getAvailableActions(businessType, businessId);
  }

  /**
   * 批量获取可用操作
   */
  static async getActionsBatch(
    businessType: BusinessType,
    businessIds: number[]
  ) {
    return getAvailableActionsBatch(businessType, businessIds);
  }

  /**
   * 获取业务的任务列表
   */
  static async getTasks(businessType: BusinessType, businessId: number) {
    return getTaskByBusiness({
      tableName: businessType,
      businessIds: [businessId],
    });
  }

  /**
   * 发起流程
   */
  static async startProcess(params: {
    businessId: number;
    businessType: BusinessType;
    processDefinitionKey?: ProcessDefinitionKey;
  }) {
    const processDefinitionKey =
      params.processDefinitionKey || getProcessDefinitionKey(params.businessType);

    if (!processDefinitionKey) {
      throw new Error(`未找到流程定义: businessType=${params.businessType}`);
    }

    // TODO: 调用实际的发流程 API
    console.warn('[BpmCoreApi] startProcess 需要接入实际的 API', params);
    return Promise.resolve(null);
  }
}

export { getAvailableActions, getAvailableActionsBatch } from '../action';
export { getTaskByBusiness } from '../task';
