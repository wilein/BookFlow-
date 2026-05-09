import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminOrdersApi(params: PageQuery) {
  return requestClient.get<PageResult>('/orders', {
    params: cleanParams(params),
  });
}

export function getAdminOrderIssuesApi(orderId: number | string) {
  return requestClient.get<Record<string, any>[]>('/orders/issues', {
    params: { orderId },
  });
}
