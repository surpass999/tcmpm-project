import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

/** 政策类型选项 */
export const POLICY_TYPE_OPTIONS = getDictOptions(DICT_TYPE.DECLARE_POLICY_TYPE, 'number');

/** 获取政策类型标签 */
export function getPolicyTypeLabel(type: number) {
  const item = POLICY_TYPE_OPTIONS.find((opt) => opt.value === type);
  return item?.label || '-';
}

/** 获取政策类型颜色 */
export function getPolicyTypeColor(type: number) {
  return type === 1 ? '#4A8F72' : '#A07852';
}

/** 格式化时间 */
export function formatTime(time: string | number) {
  if (!time) return '';
  const date = new Date(time);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
