import { requestClient } from '#/api/request';

/**
 * 填报时间窗口
 */
export interface ReportWindow {
  id: number;
  reportYear: number;
  reportBatch: number;
  windowStart: string;
  windowEnd: string;
  remark?: string;
  status: number;
}

/**
 * 创建时间窗口
 */
export function createReportWindow(data: {
  reportYear: number;
  reportBatch: number;
  windowStart: string;
  windowEnd: string;
  remark?: string;
}) {
  return requestClient.post<number>('/declare/report-window/create', data);
}

/**
 * 更新时间窗口
 */
export function updateReportWindow(
  id: number,
  data: {
    reportYear: number;
    reportBatch: number;
    windowStart: string;
    windowEnd: string;
    remark?: string;
  },
) {
  return requestClient.put(`/declare/report-window/update?id=${id}`, data);
}

/**
 * 删除时间窗口
 */
export function deleteReportWindow(id: number) {
  return requestClient.delete(`/declare/report-window/delete?id=${id}`);
}

/**
 * 获取时间窗口列表
 */
export function getReportWindowList(reportYear?: number) {
  return requestClient.get<ReportWindow[]>(
    `/declare/report-window/list${reportYear ? `?reportYear=${reportYear}` : ''}`,
  );
}

/**
 * 检查是否有开放的时间窗口
 */
export function checkReportWindowOpen() {
  return requestClient.get<boolean>('/declare/report-window/check-open');
}
