<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { ElMessage, ElMessageBox } from 'element-plus';

import {
  changeAdminBannerStatusApi,
  getAdminBannersApi,
  saveAdminBannerApi,
} from '#/api/bookflow';

import { formatDate } from '../shared';

const loading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  status: '',
});
const form = reactive<Record<string, any>>({
  id: undefined,
  imageUrl: '',
  link: '',
  sortOrder: 0,
  status: 1,
  title: '',
});

async function loadData() {
  loading.value = true;
  try {
    const data = await getAdminBannersApi(query);
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

function openEdit(row?: Record<string, any>) {
  Object.assign(form, {
    id: row?.id,
    imageUrl: row?.imageUrl ?? '',
    link: row?.link ?? '',
    sortOrder: row?.sortOrder ?? 0,
    status: row?.status ?? 1,
    title: row?.title ?? '',
  });
  dialogVisible.value = true;
}

async function save() {
  await saveAdminBannerApi(form);
  ElMessage.success('保存成功');
  dialogVisible.value = false;
  loadData();
}

async function changeStatus(row: Record<string, any>, status: number) {
  await ElMessageBox.confirm(`确认${status === 1 ? '启用' : '禁用'} Banner「${row.title}」？`, 'Banner 状态');
  await changeAdminBannerStatusApi({ bannerId: row.id, status });
  ElMessage.success('处理成功');
  loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <div class="title-block">
            <h2>Banner 管理</h2>
            <p>维护小程序首页轮播图标题、图片、跳转和启停状态。</p>
          </div>
          <el-button type="primary" @click="openEdit()">新增 Banner</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column label="图片" width="120">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" fit="cover" style="width: 88px; height: 44px; border-radius: 6px" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="link" label="跳转" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="170">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="changeStatus(row, row.status === 1 ? 0 : 1)">
              {{ row.status === 1 ? '禁用' : '启用' }}
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

    <el-dialog v-model="dialogVisible" title="Banner 配置" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="form.imageUrl" placeholder="图片 URL" />
        </el-form-item>
        <el-form-item label="跳转地址">
          <el-input v-model="form.link" placeholder="小程序页面路径或外链" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
