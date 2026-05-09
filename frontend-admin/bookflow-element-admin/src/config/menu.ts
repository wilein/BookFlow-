import {
  Bell,
  Collection,
  DataAnalysis,
  Document,
  Files,
  Flag,
  Goods,
  House,
  List,
  MessageBox,
  Picture,
  Reading,
  User,
} from '@element-plus/icons-vue';

export const menus = [
  { icon: DataAnalysis, path: '/dashboard', title: '数据概览' },
  { icon: User, path: '/users', title: '用户管理' },
  { icon: Bell, path: '/verify', title: '认证审核' },
  { icon: Reading, path: '/books', title: '书籍管理' },
  { icon: Collection, path: '/orders', title: '订单管理' },
  { icon: MessageBox, path: '/content', title: '内容管理' },
  { icon: Flag, path: '/reports', title: '举报审核' },
  { icon: Document, path: '/feedbacks', title: '反馈管理' },
  { icon: Files, path: '/resources', title: '资源管理' },
  { icon: List, path: '/paths', title: '学习路径' },
  { icon: Picture, path: '/banners', title: 'Banner管理' },
  { icon: Goods, path: '/logs', title: '系统日志' },
];

export const brandMenu = { icon: House, path: '/dashboard', title: 'BookFlow后台' };
