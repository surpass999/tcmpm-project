/**
 * useLinkageState - 联动状态的共享状态（独立文件避免循环依赖）
 */
import { ref } from 'vue';
import type { LinkageEvaluationResult } from '../types';

export const linkageResults = ref<Map<string, LinkageEvaluationResult>>(new Map());
