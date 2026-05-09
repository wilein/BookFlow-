import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminLogsApi(params: PageQuery) {
  return requestClient.get<PageResult>('/logs', { params: cleanParams(params) });
}
