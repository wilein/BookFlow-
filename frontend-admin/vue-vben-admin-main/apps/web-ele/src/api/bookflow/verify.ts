import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getVerifyListApi(params: PageQuery) {
  return requestClient.get<PageResult>('/verify/list', {
    params: cleanParams(params),
  });
}

export function auditVerifyApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/verify/audit', data);
}
