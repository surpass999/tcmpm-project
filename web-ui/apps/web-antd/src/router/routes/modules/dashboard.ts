import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    name: 'BusinessConsole',
    path: '/dashboard/business',
    component: () => import('#/views/dashboard/business/index.vue'),
    meta: {
      icon: 'lucide:layout-dashboard',
      order: -1,
      title: $t('page.dashboard.title'),
    },
  },
  {
    name: 'Profile',
    path: '/profile',
    component: () => import('#/views/_core/profile/index.vue'),
    meta: {
      icon: 'ant-design:profile-outlined',
      title: $t('ui.widgets.profile'),
      hideInMenu: true,
    },
  },
];

export default routes;
