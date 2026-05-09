<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { ElMessage, ElMessageBox } from 'element-plus';

import {
  changeAdminBookStatusApi,
  deleteAdminBookApi,
  getAdminBooksApi,
} from '#/api/bookflow';

import { bookStatusText, formatDate, labelOf, money } from '../shared';

const loading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const query = reactive({
  keyword: '',
  pageNo: 1,
  pageSize: 10,
  status: '',
});

async function loadData() {
  loading.value = true;
  try {
    const data = await getAdminBooksApi(query);
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

async function changeStatus(row: Record<string, any>, status: number) {
  await ElMessageBox.confirm(`确认将「${row.title}」改为「${labelOf(bookStatusText, status)}」？`, '书籍状态');
  await changeAdminBookStatusApi({ bookId: row.id, status });
  ElMessage.success('状态已更新');
  loadData();
}

async function removeBook(row: Record<string, any>) {
  await ElMessageBox.confirm(`确认软删除书籍「${row.title}」？`, '删除书籍', { type: 'warning' });
  await deleteAdminBookApi({ bookId: row.id });
  ElMessage.success('已删除');
  loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="title-block">
          <h2>书籍管理</h2>
          <p>查看发布人、价格、分类、状态和封面，支持下架、恢复和软删除。</p>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="书名/作者/ISBN/分类" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="在售" :value="1" />
            <el-option label="交易中" :value="2" />
            <el-option label="已售" :value="3" />
            <el-option label="下架" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column label="书籍" min-width="260">
          <template #default="{ row }">
            <div class="book-cell">
              <el-image :src="row.cover" fit="cover" class="cover" />
              <div>
                <div class="strong">{{ row.title }}</div>
                <div class="muted">{{ row.author }} / {{ row.publisher || '-' }}</div>
                <div class="muted">ISBN: {{ row.isbn || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sellerName" label="发布人" width="140" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">￥{{ money(row.price) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 4 ? 'danger' : 'warning'">
              {{ labelOf(bookStatusText, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数据" width="140">
          <template #default="{ row }">
            <div class="muted">浏览 {{ row.viewCount ?? 0 }}</div>
            <div class="muted">收藏 {{ row.favoriteCount ?? 0 }}</div>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="210">
          <template #default="{ row }">
            <el-dropdown>
              <el-button link type="primary">改状态</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="changeStatus(row, 1)">恢复在售</el-dropdown-item>
                  <el-dropdown-item @click="changeStatus(row, 4)">下架</el-dropdown-item>
                  <el-dropdown-item @click="changeStatus(row, 3)">标记已售</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button link type="danger" @click="removeBook(row)">删除</el-button>
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
  </div>
</template>

<style scoped>
.page {
  padding: 20px;
}

.title-block h2 {
  margin: 0;
}

.title-block p,
.muted {
  color: #64748b;
}

.toolbar {
  margin-bottom: 12px;
}

.book-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.cover {
  width: 48px;
  height: 64px;
  border-radius: 8px;
  background: #f1f5f9;
}

.strong {
  font-weight: 700;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
