import { requestClient } from '#/api/request';

export namespace DeclareIndicatorValueApi {
  /**
   * 指标值保存请求
   */
  export interface IndicatorValueSaveParams {
    businessType: number;
    businessId: number;
    values: IndicatorValueItem[];
  }

  /**
   * 单个指标值
   */
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

  /**
   * 指标值详情
   */
  export interface IndicatorValue {
    id: number;
    businessType: number;
    businessId: number;
    indicatorId: number;
    indicatorCode: string;
    valueType: number;
    valueNum: string;
    valueStr: string;
    valueBool: boolean;
    valueDate: string;
    valueDateStart: string;
    valueDateEnd: string;
    valueText: string;
    isValid: boolean;
    validationMsg: string;
    fillTime: string;
    fillerId: number;
  }
}

/** 保存指标值 */
export async function saveIndicatorValues(
  data: DeclareIndicatorValueApi.IndicatorValueSaveParams
) {
  return requestClient.post('/declare/indicator-value/save', data);
}

/** 获取指标值列表 */
export async function getIndicatorValues(
  businessType: number,
  businessId: number
): Promise<DeclareIndicatorValueApi.IndicatorValue[]> {
  return requestClient.get('/declare/indicator-value/list', {
    params: { businessType, businessId },
  });
}

/** 删除指标值 */
export async function deleteIndicatorValues(
  businessType: number,
  businessId: number
) {
  return requestClient.delete('/declare/indicator-value/delete', {
    params: { businessType, businessId },
  });
}
