<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { onMounted, ref } from 'vue';

import { getDashboardSummaryApi } from '@/api/admin';

const loading = ref(false);
const summary = ref<Record<string, any>>({});

const cards = [
  { key: 'userCount', label: '用户总数', tone: 'blue' },
  { key: 'pendingVerifyCount', label: '待审核认证', tone: 'orange' },
  { key: 'sellingBookCount', label: '在售书籍', tone: 'green' },
  { key: 'orderCount', label: '订单总数', tone: 'slate' },
  { key: 'pendingReportCount', label: '待处理举报', tone: 'red' },
  { key: 'pendingFeedbackCount', label: '待处理反馈', tone: 'purple' },
  { key: 'todayUserCount', label: '今日新增用户', tone: 'blue' },
  { key: 'todayBookCount', label: '今日新增书籍', tone: 'green' },
  { key: 'todayOrderCount', label: '今日新增订单', tone: 'orange' },
  { key: 'todayPostCount', label: '今日新增帖子', tone: 'purple' },
];

async function loadData() {
  loading.value = true;
  try {
    summary.value = await getDashboardSummaryApi();
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div class="page-stack">
    <el-card class="hero-card" shadow="never">
      <div>
        <p class="eyebrow">Overview</p>
        <h1>数据概览</h1>
        <p>查看 BookFlow 当前运营状态和今日新增数据。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" type="primary" @click="loadData">刷新</el-button>
    </el-card>

    <div class="stat-grid" v-loading="loading">
      <el-card v-for="card in cards" :key="card.key" class="stat-card" shadow="never">
        <div class="stat-top">
          <span :class="['stat-dot', card.tone]" />
          <span>{{ card.label }}</span>
        </div>
        <strong>{{ summary[card.key] ?? 0 }}</strong>
      </el-card>
    </div>
  </div>
</template>
