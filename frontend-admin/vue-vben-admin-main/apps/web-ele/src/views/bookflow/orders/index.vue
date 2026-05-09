<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { getAdminOrderIssuesApi, getAdminOrdersApi } from '#/api/bookflow';

import { formatDate, labelOf, money, orderStatusText } from '../shared';

const loading = ref(false);
const issueLoading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const issues = ref<Record<string, any>[]>([]);
const total = ref(0);
const issueVisible = ref(false);
const currentOrder = ref<Record<string, any>>({});
const query = reactive({
  keyword: '',
  pageNo: 1,
  pageSize: 10,
  status: '',
});

async function loadData() {
  loading.value = true;
  try {
    const data = await getAdminOrdersApi(query);
    rows.value = data.items;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNo = 1;
  loadData();
}

async function openIssues(row: Record<string, any>) {
  currentOrder.value = row;
  issueVisible.value = true;
  issueLoading.value = true;
  try {
    issues.value = await getAdminOrderIssuesApi(row.id);
  } finally {
    issueLoading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="title-block">
          <h2>订单管理</h2>
          <p>后台默认只读订单主流程，展示买家、卖家、书籍、金额、状态和售后问题。</p>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="订单号/收货人/手机号" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="待付款" :value="0" />
            <el-option label="待发货" :value="1" />
            <el-option label="待收货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="已关闭" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="书籍" min-width="220">
          <template #default="{ row }">
            <div class="book-cell">
              <el-image :src="row.bookCover" fit="cover" class="cover" />
              <span>{{ row.bookTitle || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="buyerName" label="买家" width="120" />
        <el-table-column prop="sellerName" label="卖家" width="120" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">￥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 3 ? 'success' : row.status >= 4 ? 'info' : 'warning'">
              {{ labelOf(orderStatusText, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiverName" label="收货人" width="110" />
        <el-table-column prop="receiverPhone" label="电话" width="130" />
        <el-table-column prop="receiverAddress" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openIssues(row)">问题记录</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @change="loadData"
      />
    </el-card>

    <el-dialog v-model="issueVisible" :title="`订单问题记录：${currentOrder.orderNo || ''}`" width="720px">
      <el-table v-loading="issueLoading" :data="issues" border>
        <el-table-column prop="userName" label="提交人" width="120" />
        <el-table-column prop="type" label="类型" width="90" />
        <el-table-column prop="content" label="内容" min-width="220" />
        <el-table-column prop="replyContent" label="回复" min-width="180" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  padding: 20px;
}

.title-block h2 {
  margin: 0;
}

.title-block p {
  color: #64748b;
}

.toolbar {
  margin-bottom: 12px;
}

.book-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.cover {
  width: 38px;
  height: 50px;
  border-radius: 6px;
  background: #f1f5f9;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
