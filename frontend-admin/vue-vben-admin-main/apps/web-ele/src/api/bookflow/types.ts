export interface PageResult<T = Record<string, any>> {
  items: T[];
  total: number;
}

export type PageQuery = {
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
  status?: number | string;
  [key: string]: any;
};

export function cleanParams(params: PageQuery) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => {
      return value !== '' && value !== undefined && value !== null;
    }),
  );
}
