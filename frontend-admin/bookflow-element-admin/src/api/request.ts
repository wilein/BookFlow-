import axios, { type AxiosError, type AxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';

const TOKEN_KEY = 'bookflow-element-admin-token';

export interface ApiResult<T = any> {
  code: number | string;
  data: T;
  message?: string;
  msg?: string;
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20_000,
});

request.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResult;
    if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
      if (payload.code === 0 || payload.code === '0') {
        return payload.data;
      }
      const message = payload.message || payload.msg || '请求失败';
      if (payload.code === 401 || payload.code === '401') {
        clearToken();
        if (location.pathname !== '/login') {
          location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`;
        }
      }
      ElMessage.error(message);
      return Promise.reject(new Error(message));
    }
    return payload;
  },
  (error: AxiosError<any>) => {
    const message = error.response?.data?.message || error.message || '网络异常';
    ElMessage.error(message);
    return Promise.reject(error);
  },
);

export function get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.get(url, config) as Promise<T>;
}

export function post<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig,
): Promise<T> {
  return request.post(url, data, config) as Promise<T>;
}
