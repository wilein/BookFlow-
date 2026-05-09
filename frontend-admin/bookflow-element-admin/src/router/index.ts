import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

import AdminLayout from '@/layout/AdminLayout.vue';
import { useAuthStore } from '@/stores/auth';

const modulePage = () => import('@/views/ModulePage.vue');

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览' },
      },
      { path: 'users', name: 'Users', component: modulePage, props: { moduleKey: 'users' }, meta: { title: '用户管理' } },
      { path: 'verify', name: 'Verify', component: modulePage, props: { moduleKey: 'verify' }, meta: { title: '认证审核' } },
      { path: 'books', name: 'Books', component: modulePage, props: { moduleKey: 'books' }, meta: { title: '书籍管理' } },
      { path: 'orders', name: 'Orders', component: modulePage, props: { moduleKey: 'orders' }, meta: { title: '订单管理' } },
      { path: 'content', name: 'Content', component: modulePage, props: { moduleKey: 'posts' }, meta: { title: '内容管理' } },
      { path: 'reports', name: 'Reports', component: modulePage, props: { moduleKey: 'reports' }, meta: { title: '举报审核' } },
      { path: 'feedbacks', name: 'Feedbacks', component: modulePage, props: { moduleKey: 'feedbacks' }, meta: { title: '反馈管理' } },
      { path: 'resources', name: 'Resources', component: modulePage, props: { moduleKey: 'resources' }, meta: { title: '资源管理' } },
      { path: 'paths', name: 'Paths', component: modulePage, props: { moduleKey: 'paths' }, meta: { title: '学习路径' } },
      { path: 'banners', name: 'Banners', component: modulePage, props: { moduleKey: 'banners' }, meta: { title: 'Banner管理' } },
      { path: 'logs', name: 'Logs', component: modulePage, props: { moduleKey: 'logs' }, meta: { title: '系统日志' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (to.meta.public) return true;
  if (!auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (!auth.userInfo) {
    try {
      await auth.fetchUserInfo();
    } catch {
      await auth.logout();
      return { path: '/login', query: { redirect: to.fullPath } };
    }
  }
  return true;
});

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '后台')} - BookFlow Admin`;
});

export default router;
