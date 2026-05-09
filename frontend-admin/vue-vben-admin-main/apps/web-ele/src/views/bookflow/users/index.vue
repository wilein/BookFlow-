<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { ElMessage, ElMessageBox } from 'element-plus';

import {
  changeAdminUserStatusApi,
  getAdminUsersApi,
  updateAdminUserCreditApi,
} from '#/api/bookflow';

import { authStatusText, formatDate, labelOf } from '../shared';

const loading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const query = reactive({
  authStatus: '',
  keyword: '',
  pageNo: 1,
  pageSize: 10,
  status: '',
});

async function loadData() {
  loading.value = true;
  try {
    const data = await getAdminUsersApi(query);
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

async function toggleUser(row: Record<string, any>) {
  const enabled = row.enabled ? 0 : 1;
  await ElMessageBox.confirm(
    `确认${enabled ? '恢复' : '禁用'}用户「${row.nickname || row.id}」？`,
    '用户状态变更',
  );
  await changeAdminUserStatusApi({ enabled, userId: row.id });
  ElMessage.success('处理成功');
  loadData();
}

async function editCredit(row: Record<string, any>) {
  const { value } = await ElMessageBox.prompt('请输入 0-100 的信用分', '调整信用分', {
    inputPattern: /^(100|[1-9]?\d)$/,
    inputValue: String(row.creditScore ?? 88),
    inputErrorMessage: '信用分必须是 0-100 的整数',
  });
  await updateAdminUserCreditApi({ creditScore: Number(value), userId: row.id });
  ElMessage.success('信用分已更新');
  loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <div>
            <h2>用户管理</h2>
            <p>查看微信用户资料、认证状态，支持禁用/恢复和信用分调整。</p>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="昵称/手机号/openid" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="正常" :value="1" />
            <el-option label="已禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证状态">
          <el-select v-model="query.authStatus" clearable placeholder="全部" style="width: 150px">
            <el-option label="未提交" :value="0" />
            <el-option label="待审核" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="row.avatarUrl" :size="36" />
              <div>
                <div class="strong">{{ row.nickname || '书友' }}</div>
                <div class="muted">ID: {{ row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="手机号" width="140" />
        <el-table-column prop="school" label="学校" min-width="160" />
        <el-table-column prop="department" label="院系" min-width="140" />
        <el-table-column label="认证" width="110">
          <template #default="{ row }">
            <el-tag :type="row.authStatus === 2 ? 'success' : row.authStatus === 1 ? 'warning' : 'info'">
              {{ labelOf(authStatusText, row.authStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信用分" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="190">
          <template #default="{ row }">
            <el-button link type="primary" @click="editCredit(row)">信用分</el-button>
            <el-button link :type="row.enabled ? 'danger' : 'success'" @click="toggleUser(row)">
              {{ row.enabled ? '禁用' : '恢复' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]"
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

.header h2 {
  margin: 0;
}

.header p {
  margin: 6px 0 0;
  color: #64748b;
}

.toolbar {
  margin-bottom: 12px;
}

.user-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.strong {
  font-weight: 700;
}

.muted {
  font-size: 12px;
  color: #94a3b8;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
