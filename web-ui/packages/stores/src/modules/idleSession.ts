import { defineStore } from 'pinia';

interface FormDraftData {
  id?: number;
  reportYear?: number;
  reportBatch?: number;
  reportStatus: string;
  values: Array<{
    indicatorId: number;
    indicatorCode: string;
    valueType: number;
    value: any;
    valueDateStart?: string;
    valueDateEnd?: string;
  }>;
}

interface IdleSessionState {
  /** 最后操作时间（毫秒时间戳） */
  lastActiveTime: number;
  /** 登录时间（毫秒时间戳） */
  loginTime: number;
  /** 填报表单是否打开 */
  isFormOpened: boolean;
  /** 当前表单草稿数据 */
  formDraftData: FormDraftData | null;
  /** 超时保存草稿的回调函数（由应用层注入） */
  onAutoSaveDraft: (() => Promise<void>) | null;
  /** 超时登出的回调函数（由应用层注入） */
  onLogout: (() => Promise<void>) | null;
}

export const useIdleSessionStore = defineStore('idle-session', {
  state: (): IdleSessionState => ({
    lastActiveTime: Date.now(),
    loginTime: Date.now(),
    isFormOpened: false,
    formDraftData: null,
    onAutoSaveDraft: null,
    onLogout: null,
  }),

  actions: {
    /** 重置最后操作时间 */
    resetActiveTime() {
      this.lastActiveTime = Date.now();
    },

    /** 记录登录时间 */
    setLoginTime() {
      this.loginTime = Date.now();
      this.lastActiveTime = Date.now();
    },

    /** 标记表单打开/关闭状态 */
    setFormOpened(opened: boolean) {
      this.isFormOpened = opened;
      // 关闭时清空草稿数据
      if (!opened) {
        this.formDraftData = null;
      }
    },

    /** 同步表单数据到 Store */
    syncFormData(data: FormDraftData | null) {
      this.formDraftData = data;
    },

    /** 注入超时保存回调 */
    setAutoSaveDraftCallback(fn: () => Promise<void>) {
      this.onAutoSaveDraft = fn;
    },

    /** 注入超时登出回调 */
    setLogoutCallback(fn: () => Promise<void>) {
      this.onLogout = fn;
    },
  },
});
