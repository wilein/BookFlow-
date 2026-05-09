<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { ElMessage, ElMessageBox } from 'element-plus';

import { changeAdminPathStatusApi, getAdminPathsApi } from '#/api/bookflow';

import { formatDate, labelOf, pathStatusText } from '../shared';

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
    const data = await getAdminPathsApi(query);
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
  await ElMessageBox.confirm(`确认将学习路径「${row.title}」改为「${labelOf(pathStatusText, status)}」？`, '学习路径状态');
  await changeAdminPathStatusApi({ pathId: row.id, status });
  ElMessage.success('状态已更新');
  loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="title-block">
          <h2>学习路径管理</h2>
          <p>查看路径、作者、节点数量、难度和状态，支持发布/审核中/下架。</p>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="标题/描述" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="审核中" :value="2" />
            <el-option label="已下架" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="title" label="路径标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column prop="bookTitle" label="关联书籍" min-width="160" show-overflow-tooltip />
        <el-table-column prop="nodeCount" label="节点" width="90" />
        <el-table-column prop="difficulty" label="难度" width="90" />
        <el-table-column prop="estimatedHours" label="预计小时" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 3 ? 'danger' : 'warning'">
              {{ labelOf(pathStatusText, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="210">
          <template #default="{ row }">
            <el-button link type="success" @click="changeStatus(row, 1)">发布</el-button>
            <el-button link type="warning" @click="changeStatus(row, 2)">审核中</el-button>
            <el-button link type="danger" @click="changeStatus(row, 3)">下架</el-button>
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

.title-block p {
  color: #64748b;
}

.toolbar {
  margin-bottom: 12px;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
