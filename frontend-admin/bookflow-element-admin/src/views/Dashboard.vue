<script setup lang="ts">
import {
  ChatDotRound,
  Collection,
  DataLine,
  Goods,
  Reading,
  Refresh,
  Tickets,
  User,
  Warning,
} from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';

import { getDashboardSummaryApi } from '@/api/admin';

const loading = ref(false);
const summary = ref<Record<string, any>>({});

const statCards = [
  { key: 'userCount', label: '用户总数', desc: '平台注册用户', icon: User, tone: 'orange' },
  { key: 'sellingBookCount', label: '在售书籍', desc: '可交易书籍', icon: Reading, tone: 'teal' },
  { key: 'orderCount', label: '订单总数', desc: '交易链路', icon: Tickets, tone: 'blue' },
  { key: 'pendingVerifyCount', label: '待审认证', desc: '学生身份审核', icon: Warning, tone: 'rose' },
];

const todayCards = [
  { key: 'todayUserCount', label: '今日新增用户', icon: User },
  { key: 'todayBookCount', label: '今日新增书籍', icon: Goods },
  { key: 'todayOrderCount', label: '今日新增订单', icon: Collection },
  { key: 'todayPostCount', label: '今日新增帖子', icon: ChatDotRound },
];

const riskCards = [
  { key: 'pendingVerifyCount', label: '认证审核', hint: '优先处理学生认证材料' },
  { key: 'pendingReportCount', label: '举报审核', hint: '维护社区与交易秩序' },
  { key: 'pendingFeedbackCount', label: '用户反馈', hint: '收集异常和体验问题' },
];

const maxTodayValue = computed(() => {
  const values = todayCards.map((item) => numberOf(item.key));
  return Math.max(...values, 1);
});

function numberOf(key: string) {
  return Number(summary.value[key] ?? 0);
}

function barHeight(key: string) {
  const value = numberOf(key);
  return `${Math.max(18, Math.round((value / maxTodayValue.value) * 112))}px`;
}

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
  <div class="dashboard-page" v-loading="loading">
    <section class="dashboard-hero">
      <div class="hero-copy">
        <p class="eyebrow">BookFlow Operations</p>
        <h1>运营数据看板</h1>
        <p>集中查看学生认证、书籍交易、资源内容与社区治理状态。</p>
      </div>
      <div class="hero-side">
        <div class="hero-status">
          <span class="status-dot" />
          <span>数据已连接</span>
        </div>
        <el-button :icon="Refresh" :loading="loading" type="primary" @click="loadData">刷新数据</el-button>
      </div>
    </section>

    <section class="stat-grid dashboard-stat-grid">
      <el-card v-for="card in statCards" :key="card.key" class="stat-card dashboard-stat-card" shadow="never">
        <div class="stat-icon" :class="card.tone">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-label">{{ card.label }}</span>
          <strong>{{ numberOf(card.key) }}</strong>
          <span class="stat-desc">{{ card.desc }}</span>
        </div>
      </el-card>
    </section>

    <section class="dashboard-panels">
      <el-card class="panel-card trend-card" shadow="never">
        <div class="panel-title-row">
          <div>
            <p class="eyebrow">Today</p>
            <h2>今日新增趋势</h2>
          </div>
          <div class="panel-icon">
            <el-icon><DataLine /></el-icon>
          </div>
        </div>
        <div class="mini-chart">
          <div v-for="item in todayCards" :key="item.key" class="chart-column">
            <div class="chart-track">
              <span class="chart-bar" :style="{ height: barHeight(item.key) }" />
            </div>
            <div class="chart-label">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label.replace('今日新增', '') }}</span>
            </div>
            <strong>{{ numberOf(item.key) }}</strong>
          </div>
        </div>
      </el-card>

      <el-card class="panel-card risk-card" shadow="never">
        <div class="panel-title-row">
          <div>
            <p class="eyebrow">Queue</p>
            <h2>待处理事项</h2>
          </div>
        </div>
        <div class="risk-list">
          <div v-for="item in riskCards" :key="item.key" class="risk-item">
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.hint }}</span>
            </div>
            <em>{{ numberOf(item.key) }}</em>
          </div>
        </div>
      </el-card>
    </section>

    <section class="quick-grid">
      <el-card v-for="item in todayCards" :key="item.key" class="quick-card" shadow="never">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
        <strong>{{ numberOf(item.key) }}</strong>
      </el-card>
    </section>
  </div>
</template>
