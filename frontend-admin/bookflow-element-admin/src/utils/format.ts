export interface OptionItem {
  label: string;
  value: boolean | number | string;
  type?: 'danger' | 'info' | 'primary' | 'success' | 'warning';
}

export function formatDate(value: unknown) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

export function formatMoney(value: unknown) {
  const num = Number(value || 0);
  return `¥ ${num.toFixed(2)}`;
}

export function optionOf(options: OptionItem[] = [], value: unknown) {
  return options.find((item) => String(item.value) === String(value));
}

export function shortText(value: unknown, length = 42) {
  const text = String(value ?? '');
  if (text.length <= length) return text || '-';
  return `${text.slice(0, length)}...`;
}

export function assetUrl(value: unknown) {
  const url = String(value || '');
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  const origin = import.meta.env.VITE_BACKEND_ORIGIN || '';
  return `${origin}${url.startsWith('/') ? '' : '/'}${url}`;
}
