import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminReportsApi(params: PageQuery) {
  return requestClient.get<PageResult>('/reports', {
    params: cleanParams(params),
  });
}

export function handleAdminReportApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/reports/handle', data);
}
