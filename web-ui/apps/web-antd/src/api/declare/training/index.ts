import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export interface Training {
  id?: number;
  name: string;
  type: number;
  typeLabel?: string;
  content?: string;
  organizer?: string;
  speaker?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  onlineLink?: string;
  targetScope?: number;
  targetScopeLabel?: string;
  targetProvinces?: string;
  registrationDeadline?: string;
  maxParticipants?: number;
  currentParticipants?: number;
  attachmentUrls?: string;
  meetingMaterials?: string;
  posterUrl?: string;
  status?: number;
  statusLabel?: string;
  publishTime?: string;
  publisherId?: number;
  publisherName?: string;
  remark?: string;
  createTime?: string;
}

export interface TrainingPageReqVO {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  type?: number;
  status?: number;
  targetScope?: number;
  startTimeStart?: string;
  startTimeEnd?: string;
}

export interface TrainingSaveReqVO {
  id?: number;
  name: string;
  type: number;
  content?: string;
  organizer?: string;
  speaker?: string;
  startTime: string;
  endTime: string;
  location?: string;
  onlineLink?: string;
  targetScope: number;
  targetProvinces?: string;
  registrationDeadline?: string;
  maxParticipants?: number;
  attachmentUrls?: string;
  meetingMaterials?: string;
  posterUrl?: string;
  remark?: string;
}

export interface Registration {
  id?: number;
  trainingId?: number;
  trainingName?: string;
  userId?: number;
  userName?: string;
  organization?: string;
  position?: string;
  phone?: string;
  email?: string;
  status?: number;
  statusLabel?: string;
  registerTime?: string;
  signInTime?: string;
  cancelTime?: string;
  feedback?: string;
  rating?: number;
  attendanceCertificate?: string;
  remark?: string;
}

export interface RegistrationPageReqVO {
  pageNo: number;
  pageSize: number;
  trainingId?: number;
  keyword?: string;
  status?: number;
}

export interface RegisterReqVO {
  trainingId: number;
  position?: string;
  phone?: string;
  email?: string;
  remark?: string;
}

export interface FeedbackReqVO {
  registrationId: number;
  feedback?: string;
  rating?: number;
}

export interface TrainingStatistics {
  totalRegistrations?: number;
  signedInCount?: number;
  cancelledCount?: number;
  absentCount?: number;
}

// ========== 培训活动 API ==========

export const trainingApi = {
  create: async (data: TrainingSaveReqVO) => {
    return await requestClient.post('/declare/training/create', data);
  },
  update: async (data: TrainingSaveReqVO) => {
    return await requestClient.put('/declare/training/update', data);
  },
  delete: async (id: number) => {
    return await requestClient.delete(`/declare/training/delete?id=${id}`);
  },
  get: async (id: number) => {
    return await requestClient.get<Training>(`/declare/training/get?id=${id}`);
  },
  getPage: async (params: TrainingPageReqVO) => {
    return await requestClient.get<{ list: Training[]; total: number }>(
      '/declare/training/page',
      { params },
    );
  },
  publish: async (id: number) => {
    return await requestClient.post('/declare/training/publish', null, { params: { id } });
  },
  unpublish: async (id: number) => {
    return await requestClient.post('/declare/training/unpublish', null, { params: { id } });
  },
  getStatistics: async (id: number) => {
    return await requestClient.get<TrainingStatistics>(
      `/declare/training/statistics?id=${id}`,
    );
  },
};

// ========== 报名管理 API ==========

export const registrationApi = {
  getPage: async (params: RegistrationPageReqVO) => {
    return await requestClient.get<{ list: Registration[]; total: number }>(
      '/declare/registration/page',
      { params },
    );
  },
  signIn: async (registrationId: number) => {
    return await requestClient.put('/declare/registration/sign-in', { registrationId });
  },
};

// ========== 兼容性别名 ==========

export const create = trainingApi.create;
export const update = trainingApi.update;
export const remove = trainingApi.delete;
export const get = trainingApi.get;
export const getPage = trainingApi.getPage;
export const publish = trainingApi.publish;
export const unpublish = trainingApi.unpublish;
export const getStatistics = trainingApi.getStatistics;

export const getRegistrationPage = registrationApi.getPage;
export const signIn = registrationApi.signIn;

// ========== 用户端 API（/client/training 前缀）==========

export const trainingClientApi = {
  getList: async () => {
    return await requestClient.get<Training[]>('/client/training/list');
  },
  get: async (id: number) => {
    return await requestClient.get<Training>(`/client/training/get?id=${id}`);
  },
  register: async (data: RegisterReqVO) => {
    return await requestClient.post('/client/training/register', data);
  },
  cancel: async (trainingId: number) => {
    return await requestClient.delete(`/client/training/cancel?trainingId=${trainingId}`);
  },
  getMy: async (params?: { pageNo?: number; pageSize?: number }) => {
    return await requestClient.get<{ list: Registration[]; total: number }>(
      '/client/training/my',
      { params },
    );
  },
  feedback: async (data: FeedbackReqVO) => {
    return await requestClient.put('/client/training/feedback', data);
  },
};

export const register = trainingClientApi.register;
export const cancel = trainingClientApi.cancel;
