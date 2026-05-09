<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { getDashboardSummaryApi } from '#/api/bookflow';

const loading = ref(false);
const summary = ref<Record<string, any>>({});

const cards = [
  { key: 'userCount', label: '用户总数', hint: '微信小程序用户' },
  { key: 'pendingVerifyCount', label: '待审核认证', hint: '学生身份待处理' },
  { key: 'sellingBookCount', label: '在售书籍', hint: '当前可购买书籍' },
  { key: 'orderCount', label: '订单总数', hint: '交易订单' },
  { key: 'pendingReportCount', label: '待处理举报', hint: '内容/订单举报' },
  { key: 'pendingFeedbackCount', label: '待处理反馈', hint: '用户反馈' },
  { key: 'todayUserCount', label: '今日新增用户', hint: '当天注册' },
  { key: 'todayBookCount', label: '今日新增书籍', hint: '当天发布' },
  { key: 'todayOrderCount', label: '今日新增订单', hint: '当天交易' },
  { key: 'todayPostCount', label: '今日新增帖子', hint: '当天社区内容' },
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
  <div class="bookflow-page" v-loading="loading">
    <div class="hero">
      <div>
        <p class="eyebrow">BookFlow Admin</p>
        <h1>数据概览</h1>
        <p class="subtitle">
          汇总小程序运营、交易、审核和反馈数据，作为后台管理入口。
        </p>
      </div>
      <el-button type="primary" @click="loadData">刷新数据</el-button>
    </div>

    <div class="summary-grid">
      <el-card v-for="item in cards" :key="item.key" shadow="never" class="metric-card">
        <div class="metric-value">{{ summary[item.key] ?? 0 }}</div>
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-hint">{{ item.hint }}</div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.bookflow-page {
  padding: 24px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px;
  margin-bottom: 18px;
  color: #172033;
  background:
    radial-gradient(circle at 12% 20%, rgba(66, 153, 225, 0.18), transparent 26%),
    linear-gradient(135deg, #f8fbff 0%, #eef5ff 48%, #fff7ed 100%);
  border: 1px solid #e5edf8;
  border-radius: 18px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 800;
}

.subtitle {
  margin: 10px 0 0;
  color: #64748b;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 16px;
}

.metric-card {
  border: 1px solid #eef2f7;
  border-radius: 16px;
}

.metric-value {
  font-size: 34px;
  font-weight: 800;
  color: #0f172a;
}

.metric-label {
  margin-top: 8px;
  font-weight: 700;
  color: #334155;
}

.metric-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
