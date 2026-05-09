import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminFeedbacksApi(params: PageQuery) {
  return requestClient.get<PageResult>('/feedbacks', {
    params: cleanParams(params),
  });
}

export function handleAdminFeedbackApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/feedbacks/handle', data);
}
