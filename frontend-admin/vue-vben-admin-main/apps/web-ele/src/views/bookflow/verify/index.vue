<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { ElMessage } from 'element-plus';

import { auditVerifyApi, getVerifyListApi } from '#/api/bookflow';

import { authStatusText, formatDate, labelOf } from '../shared';

const loading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const current = ref<Record<string, any>>({});
const auditForm = reactive({ auditRemark: '', status: 2 });
const query = reactive({
  keyword: '',
  pageNo: 1,
  pageSize: 10,
  status: 1,
});

async function loadData() {
  loading.value = true;
  try {
    const data = await getVerifyListApi(query);
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

function openAudit(row: Record<string, any>, status: number) {
  current.value = row;
  auditForm.status = status;
  auditForm.auditRemark = status === 2 ? '审核通过' : '';
  dialogVisible.value = true;
}

async function submitAudit() {
  await auditVerifyApi({
    auditRemark: auditForm.auditRemark,
    status: auditForm.status,
    userId: current.value.userId,
  });
  ElMessage.success('审核已提交');
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
          <h2>学生身份审核</h2>
          <p>审核小程序提交的学生证资料，处理结果会回写用户认证状态并发送通知。</p>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="姓名/学号/学校/院系" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 150px">
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
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="studentId" label="学号" width="150" />
        <el-table-column prop="school" label="学校" min-width="160" />
        <el-table-column prop="department" label="院系" min-width="140" />
        <el-table-column prop="verifyType" label="方式" width="120" />
        <el-table-column label="学生证" width="120">
          <template #default="{ row }">
            <el-image
              v-if="row.studentCardImageUrl"
              :preview-src-list="[row.studentCardImageUrl]"
              :src="row.studentCardImageUrl"
              fit="cover"
              preview-teleported
              style="width: 64px; height: 42px; border-radius: 6px"
            />
            <span v-else class="muted">未上传</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.authStatus === 2 ? 'success' : row.authStatus === 1 ? 'warning' : 'info'">
              {{ labelOf(authStatusText, row.authStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDate(row.verifySubmitTime) }}</template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="审核备注" min-width="160" />
        <el-table-column fixed="right" label="操作" width="170">
          <template #default="{ row }">
            <el-button link type="success" @click="openAudit(row, 2)">通过</el-button>
            <el-button link type="danger" @click="openAudit(row, 3)">驳回</el-button>
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

    <el-dialog v-model="dialogVisible" title="认证审核" width="460px">
      <el-form label-width="90px">
        <el-form-item label="审核结果">
          <el-tag :type="auditForm.status === 2 ? 'success' : 'danger'">
            {{ auditForm.status === 2 ? '通过' : '驳回' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.auditRemark"
            :rows="4"
            placeholder="驳回时请填写原因"
            type="textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">提交</el-button>
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

.title-block p,
.muted {
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
