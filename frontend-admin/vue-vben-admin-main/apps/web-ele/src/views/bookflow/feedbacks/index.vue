<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { ElMessage, ElMessageBox } from 'element-plus';

import { getAdminFeedbacksApi, handleAdminFeedbackApi } from '#/api/bookflow';

import { feedbackStatusText, formatDate, labelOf } from '../shared';

const loading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  status: 0,
});

async function loadData() {
  loading.value = true;
  try {
    const data = await getAdminFeedbacksApi(query);
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

async function handle(row: Record<string, any>) {
  await ElMessageBox.confirm(`确认将反馈 #${row.id} 标记为已处理？`, '反馈处理');
  await handleAdminFeedbackApi({ feedbackId: row.id });
  ElMessage.success('处理成功');
  loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="title-block">
          <h2>反馈管理</h2>
          <p>查看用户反馈、联系方式和页面路径，支持标记已处理。</p>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="待处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="feedbackType" label="类型" width="130" />
        <el-table-column prop="content" label="反馈内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="contact" label="联系方式" width="160" />
        <el-table-column prop="pagePath" label="页面路径" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : 'success'">
              {{ labelOf(feedbackStatusText, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="110">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 1" @click="handle(row)">标记处理</el-button>
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
