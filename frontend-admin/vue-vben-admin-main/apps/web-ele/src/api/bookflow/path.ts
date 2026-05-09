import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminPathsApi(params: PageQuery) {
  return requestClient.get<PageResult>('/paths', { params: cleanParams(params) });
}

export function changeAdminPathStatusApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/paths/change-status', data);
}
