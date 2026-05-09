<script setup lang="ts">
import { Bell, Fold, Search, SwitchButton } from '@element-plus/icons-vue';
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { menus } from '@/config/menu';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const collapsed = ref(false);

const activePath = computed(() => route.path);
const adminInitial = computed(() => (auth.realName || 'A').slice(0, 1).toUpperCase());

async function logout() {
  await auth.logout();
  router.replace('/login');
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside :width="collapsed ? '72px' : '238px'" class="admin-aside">
      <div class="brand" :class="{ collapsed }">
        <div class="brand-mark">BF</div>
        <div v-if="!collapsed" class="brand-text">
          <strong>BookFlow</strong>
          <span>后台管理端</span>
        </div>
      </div>

      <div v-if="!collapsed" class="sidebar-section-label">Management</div>

      <el-menu
        router
        :collapse="collapsed"
        :default-active="activePath"
        class="admin-menu"
        background-color="transparent"
        text-color="#a6b0c3"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="admin-body">
      <el-header class="admin-header">
        <div class="header-left">
          <el-button class="icon-button" text :icon="Fold" @click="collapsed = !collapsed" />
          <div class="title-block">
            <span class="page-title">{{ route.meta.title || '后台管理' }}</span>
            <span class="page-subtitle">BookFlow campus operations</span>
          </div>
        </div>
        <div class="header-right">
          <div class="header-search">
            <el-icon><Search /></el-icon>
            <span>搜索用户、书籍、订单</span>
          </div>
          <el-button class="icon-button" text :icon="Bell" />
          <div class="admin-profile">
            <div class="admin-avatar">{{ adminInitial }}</div>
            <span class="admin-name">{{ auth.realName }}</span>
          </div>
          <el-button class="logout-button" plain :icon="SwitchButton" @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
