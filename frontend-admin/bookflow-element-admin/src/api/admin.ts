import { get, post } from './request';

export interface PageResult<T = any> {
  items: T[];
  total: number;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface LoginResult {
  accessToken: string;
}

export function loginApi(data: LoginPayload) {
  return post<LoginResult>('/auth/login', data);
}

export function getAdminInfoApi() {
  return get<Record<string, any>>('/auth/info');
}

export function logoutApi() {
  return post<Record<string, any>>('/auth/logout');
}

export function getDashboardSummaryApi() {
  return get<Record<string, any>>('/dashboard/summary');
}

export function getPageApi<T = any>(url: string, params: Record<string, any>) {
  return get<PageResult<T>>(url, { params: cleanParams(params) });
}

export function getListApi<T = any>(url: string, params: Record<string, any>) {
  return get<T[]>(url, { params: cleanParams(params) });
}

export function postActionApi<T = any>(url: string, data: Record<string, any>) {
  return post<T>(url, data);
}

export function uploadBookImageApi(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return post<Record<string, any>>('/books/upload-image', formData);
}

export function uploadAdminImageApi(url: string, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return post<Record<string, any>>(url, formData);
}

function cleanParams(params: Record<string, any>) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined),
  );
}
