<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { ElMessage } from 'element-plus';

import { getAdminReportsApi, handleAdminReportApi } from '#/api/bookflow';

import { formatDate, labelOf, reportStatusText } from '../shared';

const loading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const current = ref<Record<string, any>>({});
const form = reactive({ hideTarget: false, status: 1 });
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  status: 0,
  targetType: '',
});

async function loadData() {
  loading.value = true;
  try {
    const data = await getAdminReportsApi(query);
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

function openHandle(row: Record<string, any>) {
  current.value = row;
  form.status = 1;
  form.hideTarget = false;
  dialogVisible.value = true;
}

async function submitHandle() {
  await handleAdminReportApi({
    hideTarget: form.hideTarget ? 1 : 0,
    reportId: current.value.id,
    status: form.status,
  });
  ElMessage.success('举报已处理');
  dialogVisible.value = false;
  loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="title-block">
          <h2>举报审核</h2>
          <p>统一处理帖子、评论、书籍、资源、学习路径和订单举报，可联动隐藏违规目标。</p>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="目标类型">
          <el-input v-model="query.targetType" clearable placeholder="post/book/order..." @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="待处理" :value="0" />
            <el-option label="已查看" :value="1" />
            <el-option label="已关闭" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="userName" label="举报人" width="120" />
        <el-table-column prop="targetType" label="目标类型" width="110" />
        <el-table-column prop="targetId" label="目标ID" width="100" />
        <el-table-column prop="targetTitle" label="目标标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="reasonType" label="原因类型" width="130" />
        <el-table-column prop="content" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : 'success'">
              {{ labelOf(reportStatusText, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="110">
          <template #default="{ row }">
            <el-button link type="primary" @click="openHandle(row)">处理</el-button>
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

    <el-dialog v-model="dialogVisible" title="处理举报" width="460px">
      <el-form label-width="110px">
        <el-form-item label="处理状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">已查看</el-radio>
            <el-radio :label="2">已关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="违规联动">
          <el-checkbox v-model="form.hideTarget">确认违规并隐藏/下架目标内容</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">提交</el-button>
      </template>
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

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
