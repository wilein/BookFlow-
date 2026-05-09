import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminBannersApi(params: PageQuery) {
  return requestClient.get<PageResult>('/banners', {
    params: cleanParams(params),
  });
}

export function saveAdminBannerApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/banners/save', data);
}

export function changeAdminBannerStatusApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/banners/change-status', data);
}
