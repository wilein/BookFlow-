<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { ElMessage, ElMessageBox } from 'element-plus';

import { changeAdminResourceStatusApi, getAdminResourcesApi } from '#/api/bookflow';

import { formatDate } from '../shared';

const loading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const query = reactive({
  keyword: '',
  pageNo: 1,
  pageSize: 10,
  status: '',
  visibility: '',
});

async function loadData() {
  loading.value = true;
  try {
    const data = await getAdminResourcesApi(query);
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

async function toggle(row: Record<string, any>) {
  const visible = row.visible ? 0 : 1;
  await ElMessageBox.confirm(`确认${visible ? '恢复' : '下架'}资源「${row.title}」？`, '资源状态');
  await changeAdminResourceStatusApi({ resourceId: row.id, visible });
  ElMessage.success('处理成功');
  loadData();
}

function formatSize(value?: number) {
  if (!value) return '-';
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

onMounted(loadData);
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="title-block">
          <h2>资源管理</h2>
          <p>管理上传资源，查看绑定书籍/路径、可见性、文件地址和下载次数。</p>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="标题/描述/格式" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px">
            <el-option label="可见" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="query.visibility" clearable placeholder="全部" style="width: 140px">
            <el-option label="公开" :value="1" />
            <el-option label="仅买家" :value="2" />
            <el-option label="私密" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="title" label="资源标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ownerName" label="上传人" width="120" />
        <el-table-column prop="bookTitle" label="绑定书籍" min-width="160" show-overflow-tooltip />
        <el-table-column prop="bindType" label="绑定类型" width="100" />
        <el-table-column prop="fileFormat" label="格式" width="90" />
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.visible ? 'success' : 'danger'">{{ row.visible ? '可见' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="110">
          <template #default="{ row }">
            <el-button link :type="row.visible ? 'danger' : 'success'" @click="toggle(row)">
              {{ row.visible ? '下架' : '恢复' }}
            </el-button>
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
