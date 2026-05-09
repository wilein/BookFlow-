import { defineStore } from 'pinia';

import { getAdminInfoApi, loginApi, logoutApi } from '@/api/admin';
import { clearToken, getToken, setToken } from '@/api/request';

interface AuthState {
  token: string;
  userInfo: Record<string, any> | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getToken(),
    userInfo: null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    realName: (state) => state.userInfo?.realName || state.userInfo?.username || '管理员',
  },
  actions: {
    async login(username: string, password: string) {
      const data = await loginApi({ username, password });
      this.token = data.accessToken;
      setToken(data.accessToken);
      await this.fetchUserInfo();
    },
    async fetchUserInfo() {
      if (!this.token) return null;
      this.userInfo = await getAdminInfoApi();
      return this.userInfo;
    },
    async logout() {
      try {
        if (this.token) {
          await logoutApi();
        }
      } finally {
        this.token = '';
        this.userInfo = null;
        clearToken();
      }
    },
  },
});
