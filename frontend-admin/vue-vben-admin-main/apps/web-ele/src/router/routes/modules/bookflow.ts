import type { RouteRecordRaw } from 'vue-router';

import Banners from '#/views/bookflow/banners/index.vue';
import Books from '#/views/bookflow/books/index.vue';
import Dashboard from '#/views/bookflow/dashboard/index.vue';
import Feedbacks from '#/views/bookflow/feedbacks/index.vue';
import Logs from '#/views/bookflow/logs/index.vue';
import Orders from '#/views/bookflow/orders/index.vue';
import Paths from '#/views/bookflow/paths/index.vue';
import Posts from '#/views/bookflow/posts/index.vue';
import Reports from '#/views/bookflow/reports/index.vue';
import Resources from '#/views/bookflow/resources/index.vue';
import Users from '#/views/bookflow/users/index.vue';
import Verify from '#/views/bookflow/verify/index.vue';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:book-open',
      order: -10,
      title: 'BookFlow后台',
    },
    name: 'BookFlowAdmin',
    path: '/bookflow',
    redirect: '/bookflow/dashboard',
    children: [
      {
        name: 'BookFlowDashboard',
        path: '/bookflow/dashboard',
        component: Dashboard,
        meta: {
          affixTab: true,
          icon: 'lucide:layout-dashboard',
          title: '数据概览',
        },
      },
      {
        name: 'BookFlowUsers',
        path: '/bookflow/users',
        component: Users,
        meta: { icon: 'lucide:users', title: '用户管理' },
      },
      {
        name: 'BookFlowVerify',
        path: '/bookflow/verify',
        component: Verify,
        meta: { icon: 'lucide:badge-check', title: '认证审核' },
      },
      {
        name: 'BookFlowBooks',
        path: '/bookflow/books',
        component: Books,
        meta: { icon: 'lucide:book-marked', title: '书籍管理' },
      },
      {
        name: 'BookFlowOrders',
        path: '/bookflow/orders',
        component: Orders,
        meta: { icon: 'lucide:receipt-text', title: '订单管理' },
      },
      {
        name: 'BookFlowPosts',
        path: '/bookflow/posts',
        component: Posts,
        meta: { icon: 'lucide:message-square-text', title: '内容管理' },
      },
      {
        name: 'BookFlowReports',
        path: '/bookflow/reports',
        component: Reports,
        meta: { icon: 'lucide:shield-alert', title: '举报审核' },
      },
      {
        name: 'BookFlowFeedbacks',
        path: '/bookflow/feedbacks',
        component: Feedbacks,
        meta: { icon: 'lucide:inbox', title: '反馈管理' },
      },
      {
        name: 'BookFlowResources',
        path: '/bookflow/resources',
        component: Resources,
        meta: { icon: 'lucide:file-stack', title: '资源管理' },
      },
      {
        name: 'BookFlowPaths',
        path: '/bookflow/paths',
        component: Paths,
        meta: { icon: 'lucide:route', title: '学习路径' },
      },
      {
        name: 'BookFlowBanners',
        path: '/bookflow/banners',
        component: Banners,
        meta: { icon: 'lucide:images', title: 'Banner管理' },
      },
      {
        name: 'BookFlowLogs',
        path: '/bookflow/logs',
        component: Logs,
        meta: { icon: 'lucide:scroll-text', title: '系统日志' },
      },
    ],
  },
];

export default routes;
