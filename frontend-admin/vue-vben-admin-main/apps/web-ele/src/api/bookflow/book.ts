import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminBooksApi(params: PageQuery) {
  return requestClient.get<PageResult>('/books', { params: cleanParams(params) });
}

export function changeAdminBookStatusApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/books/change-status', data);
}

export function deleteAdminBookApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>('/books/delete', data);
}
