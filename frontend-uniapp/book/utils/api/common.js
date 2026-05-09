import { get } from './request';

export function getBannerList() {
  return get('/common/banner/list');
}
