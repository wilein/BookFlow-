import { requestClient } from '#/api/request';

export function getDashboardSummaryApi() {
  return requestClient.get<Record<string, any>>('/dashboard/summary');
}
