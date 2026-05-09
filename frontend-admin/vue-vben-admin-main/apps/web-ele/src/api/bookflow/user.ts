import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminUsersApi(params: PageQuery) {
  return requestClient.get<PageResult>('/users', { params: cleanParams(params) });
}

export function changeAdminUserStatusApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/users/change-status', data);
}

export function updateAdminUserCreditApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/users/credit', data);
}
