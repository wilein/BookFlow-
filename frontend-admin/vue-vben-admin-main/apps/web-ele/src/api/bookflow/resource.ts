import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminResourcesApi(params: PageQuery) {
  return requestClient.get<PageResult>('/resources', {
    params: cleanParams(params),
  });
}

export function changeAdminResourceStatusApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/resources/change-status', data);
}
