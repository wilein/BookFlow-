import { requestClient } from '#/api/request';

import { cleanParams, type PageQuery, type PageResult } from './types';

export function getAdminPostsApi(params: PageQuery) {
  return requestClient.get<PageResult>('/content/posts', {
    params: cleanParams(params),
  });
}

export function changeAdminPostStatusApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>(
    '/content/posts/change-status',
    data,
  );
}

export function getAdminCommentsApi(params: PageQuery) {
  return requestClient.get<PageResult>('/content/comments', {
    params: cleanParams(params),
  });
}

export function changeAdminCommentStatusApi(data: Record<string, any>) {
  return requestClient.post<Record<string, any>>(
    '/content/comments/change-status',
    data,
  );
}
