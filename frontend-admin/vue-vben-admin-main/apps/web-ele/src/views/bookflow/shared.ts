export const authStatusText: Record<number, string> = {
  0: '未提交',
  1: '待审核',
  2: '已通过',
  3: '已驳回',
};

export const bookStatusText: Record<number, string> = {
  1: '在售',
  2: '交易中',
  3: '已售',
  4: '下架',
};

export const orderStatusText: Record<number, string> = {
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消',
  5: '已关闭',
};

export const pathStatusText: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '审核中',
  3: '已下架',
};

export const reportStatusText: Record<number, string> = {
  0: '待处理',
  1: '已查看',
  2: '已关闭',
};

export const feedbackStatusText: Record<number, string> = {
  0: '待处理',
  1: '已处理',
};

export function formatDate(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

export function money(value?: number | string) {
  if (value === undefined || value === null || value === '') return '0.00';
  const numberValue = Number(value);
  return Number.isNaN(numberValue) ? String(value) : numberValue.toFixed(2);
}

export function labelOf(map: Record<number, string>, value?: number) {
  if (value === undefined || value === null) return '-';
  return map[value] ?? String(value);
}
