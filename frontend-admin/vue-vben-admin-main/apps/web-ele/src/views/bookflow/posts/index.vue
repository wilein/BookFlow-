<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { ElMessage, ElMessageBox } from 'element-plus';

import {
  changeAdminCommentStatusApi,
  changeAdminPostStatusApi,
  getAdminCommentsApi,
  getAdminPostsApi,
} from '#/api/bookflow';

import { formatDate } from '../shared';

const activeTab = ref('posts');
const loading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const query = reactive({
  keyword: '',
  pageNo: 1,
  pageSize: 10,
  status: '',
  type: '',
});

async function loadData() {
  loading.value = true;
  try {
    const data =
      activeTab.value === 'posts'
        ? await getAdminPostsApi(query)
        : await getAdminCommentsApi(query);
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

function switchTab() {
  query.pageNo = 1;
  rows.value = [];
  loadData();
}

async function togglePost(row: Record<string, any>) {
  const visible = row.visible ? 0 : 1;
  await ElMessageBox.confirm(`确认${visible ? '恢复' : '隐藏'}帖子「${row.title || row.id}」？`, '内容状态');
  await changeAdminPostStatusApi({ postId: row.id, visible });
  ElMessage.success('处理成功');
  loadData();
}

async function toggleComment(row: Record<string, any>) {
  const visible = row.visible ? 0 : 1;
  await ElMessageBox.confirm(`确认${visible ? '恢复' : '隐藏'}该评论？`, '评论状态');
  await changeAdminCommentStatusApi({ commentId: row.id, visible });
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
          <h2>社区内容管理</h2>
          <p>管理帖子、学习路径分享内容和评论，违规内容采用隐藏/恢复。</p>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="switchTab">
        <el-tab-pane label="帖子" name="posts" />
        <el-tab-pane label="评论" name="comments" />
      </el-tabs>

      <el-form :inline="true" :model="query" class="toolbar">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="标题/内容" @keyup.enter="search" />
        </el-form-item>
        <el-form-item v-if="activeTab === 'posts'" label="类型">
          <el-select v-model="query.type" clearable placeholder="全部" style="width: 140px">
            <el-option label="推荐" :value="0" />
            <el-option label="书评" :value="1" />
            <el-option label="问答" :value="2" />
            <el-option label="学习路径" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px">
            <el-option label="可见" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table v-if="activeTab === 'posts'" v-loading="loading" :data="rows" border>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column prop="type" label="类型" width="90" />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="互动" width="130">
          <template #default="{ row }">
            <div class="muted">赞 {{ row.likeCount ?? 0 }}</div>
            <div class="muted">评 {{ row.commentCount ?? 0 }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.visible ? 'success' : 'danger'">{{ row.visible ? '可见' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="110">
          <template #default="{ row }">
            <el-button link :type="row.visible ? 'danger' : 'success'" @click="togglePost(row)">
              {{ row.visible ? '隐藏' : '恢复' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else v-loading="loading" :data="rows" border>
        <el-table-column prop="postId" label="帖子ID" width="100" />
        <el-table-column prop="authorName" label="评论人" width="120" />
        <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="likeCount" label="点赞" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.visible ? 'success' : 'danger'">{{ row.visible ? '可见' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="110">
          <template #default="{ row }">
            <el-button link :type="row.visible ? 'danger' : 'success'" @click="toggleComment(row)">
              {{ row.visible ? '隐藏' : '恢复' }}
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
