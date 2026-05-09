<script setup lang="ts">
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type UploadRequestOptions, type UploadUserFile } from 'element-plus';
import { computed, onMounted, reactive, ref, watch } from 'vue';

import { getListApi, getPageApi, postActionApi, uploadAdminImageApi, uploadBookImageApi } from '@/api/admin';
import { getToken } from '@/api/request';
import { type ActionKey, type ColumnConfig, type FormFieldConfig, modules } from '@/config/modules';
import { assetUrl, formatDate, formatMoney, optionOf, shortText } from '@/utils/format';

const props = defineProps<{ moduleKey: string }>();

const moduleConfig = computed(() => modules[props.moduleKey]);
const loading = ref(false);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const query = reactive<Record<string, any>>({
  pageNo: 1,
  pageSize: 10,
});

const issueDialog = ref(false);
const issueRows = ref<Record<string, any>[]>([]);
const bannerDialog = ref(false);
const bannerForm = reactive<Record<string, any>>({
  id: undefined,
  imageUrl: '',
  link: '',
  sortOrder: 0,
  status: 1,
  title: '',
});
const bookImageDialog = ref(false);
const currentBook = ref<Record<string, any> | null>(null);
const bookImageFiles = ref<UploadUserFile[]>([]);
const savingBookImages = ref(false);
const editDialog = ref(false);
const editMode = ref<'create' | 'update'>('create');
const editForm = reactive<Record<string, any>>({});
const savingEdit = ref(false);

const editableModule = computed(() => Boolean(moduleConfig.value?.saveEndpoint && moduleConfig.value?.formFields?.length));

watch(
  () => props.moduleKey,
  () => {
    resetQuery();
    loadData();
  },
);

function resetQuery() {
  Object.keys(query).forEach((key) => {
    if (!['pageNo', 'pageSize'].includes(key)) delete query[key];
  });
  query.pageNo = 1;
  query.pageSize = 10;
}

async function loadData() {
  if (!moduleConfig.value) return;
  loading.value = true;
  try {
    const data = await getPageApi(moduleConfig.value.endpoint, query);
    rows.value = data.items || [];
    total.value = Number(data.total || 0);
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNo = 1;
  loadData();
}

function optionLabel(column: ColumnConfig, value: unknown) {
  return optionOf(column.options, value)?.label || String(value ?? '-');
}

function optionType(column: ColumnConfig, value: unknown) {
  return optionOf(column.options, value)?.type || 'info';
}

function actionVisible(action: ActionKey, row: Record<string, any>) {
  if (action === 'userDisable') return row.enabled;
  if (action === 'userEnable') return !row.enabled;
  if (action === 'postHide' || action === 'resourceHide' || action === 'commentHide') return row.visible;
  if (action === 'postShow' || action === 'resourceShow' || action === 'commentShow') return !row.visible;
  if (action === 'bookOn') return Number(row.status) === 4;
  if (action === 'bookOff') return Number(row.status) !== 4;
  if (action === 'pathPublish') return Number(row.status) !== 1;
  if (action === 'pathDisable') return Number(row.status) !== 3;
  if (action === 'bannerToggle') return true;
  return true;
}

function actionText(action: ActionKey, row: Record<string, any>) {
  const map: Record<ActionKey, string> = {
    edit: '编辑',
    approveVerify: '通过',
    bannerEdit: '编辑',
    bannerToggle: Number(row.status) === 1 ? '停用' : '启用',
    bookDelete: '删除',
    bookImages: '图片',
    bookOff: '下架',
    bookOn: '恢复',
    commentHide: '隐藏',
    commentShow: '恢复',
    credit: '信用分',
    feedbackHandle: '处理',
    issues: '问题记录',
    pathDisable: '下架',
    pathPublish: '发布',
    postHide: '隐藏',
    postShow: '恢复',
    rejectVerify: '驳回',
    reportClose: '关闭',
    reportConfirm: '确认违规',
    resourceHide: '下架',
    resourceShow: '恢复',
    userDisable: '禁用',
    userEnable: '恢复',
  };
  return map[action];
}

function actionType(action: ActionKey) {
  if (
    [
      'bookDelete',
      'bookOff',
      'commentHide',
      'pathDisable',
      'postHide',
      'rejectVerify',
      'reportClose',
      'resourceHide',
      'userDisable',
    ].includes(action)
  ) {
    return 'danger';
  }
  if (['approveVerify', 'bookOn', 'pathPublish', 'postShow', 'resourceShow', 'userEnable'].includes(action)) {
    return 'success';
  }
  return 'primary';
}

async function runAction(action: ActionKey, row: Record<string, any>) {
  switch (action) {
    case 'edit':
      return openEdit(row);
    case 'credit':
      return editCredit(row);
    case 'approveVerify':
      return auditVerify(row, 2);
    case 'rejectVerify':
      return auditVerify(row, 3);
    case 'bookImages':
      return openBookImages(row);
    case 'bookOn':
      return confirmPost('确认恢复该书籍为在售？', '/books/change-status', { bookId: row.id, status: 1 });
    case 'bookOff':
      return confirmPost('确认下架该书籍？', '/books/change-status', { bookId: row.id, status: 4 });
    case 'bookDelete':
      return confirmPost('确认软删除该书籍？', '/books/delete', { bookId: row.id });
    case 'userDisable':
      return confirmPost('确认禁用该用户？', '/users/change-status', { enabled: 0, userId: row.id });
    case 'userEnable':
      return confirmPost('确认恢复该用户？', '/users/change-status', { enabled: 1, userId: row.id });
    case 'postHide':
      return confirmPost('确认隐藏该帖子？', '/content/posts/change-status', { postId: row.id, visible: 0 });
    case 'postShow':
      return confirmPost('确认恢复该帖子？', '/content/posts/change-status', { postId: row.id, visible: 1 });
    case 'resourceHide':
      return confirmPost('确认下架该资源？', '/resources/change-status', { resourceId: row.id, visible: 0 });
    case 'resourceShow':
      return confirmPost('确认恢复该资源？', '/resources/change-status', { resourceId: row.id, visible: 1 });
    case 'pathPublish':
      return confirmPost('确认发布该学习路径？', '/paths/change-status', { pathId: row.id, status: 1 });
    case 'pathDisable':
      return confirmPost('确认下架该学习路径？', '/paths/change-status', { pathId: row.id, status: 3 });
    case 'reportConfirm':
      return confirmReport(row);
    case 'reportClose':
      return confirmPost('确认关闭该举报？', '/reports/handle', { hideTarget: 0, reportId: row.id, status: 2 });
    case 'feedbackHandle':
      return confirmPost('确认标记该反馈为已处理？', '/feedbacks/handle', { feedbackId: row.id });
    case 'issues':
      return openIssues(row);
    case 'bannerEdit':
      return openBanner(row);
    case 'bannerToggle':
      return confirmPost(Number(row.status) === 1 ? '确认停用该 Banner？' : '确认启用该 Banner？', '/banners/change-status', {
        bannerId: row.id,
        status: Number(row.status) === 1 ? 0 : 1,
      });
    default:
      return undefined;
  }
}

async function confirmPost(message: string, url: string, payload: Record<string, any>) {
  await ElMessageBox.confirm(message, '操作确认', { type: 'warning' });
  await postActionApi(url, payload);
  ElMessage.success('操作成功');
  await loadData();
}

async function editCredit(row: Record<string, any>) {
  const { value } = await ElMessageBox.prompt('请输入 0-100 的信用分', '调整信用分', {
    inputErrorMessage: '信用分必须是 0-100 的整数',
    inputPattern: /^(100|[1-9]?\d)$/,
    inputValue: String(row.creditScore ?? 88),
  });
  await postActionApi('/users/credit', { creditScore: Number(value), userId: row.id });
  ElMessage.success('信用分已更新');
  await loadData();
}

async function auditVerify(row: Record<string, any>, status: 2 | 3) {
  let auditRemark = status === 2 ? '认证资料真实有效' : '';
  if (status === 3) {
    const result = await ElMessageBox.prompt('请输入驳回原因', '驳回认证', {
      inputErrorMessage: '驳回原因不能为空',
      inputPattern: /.+/,
    });
    auditRemark = result.value;
  } else {
    await ElMessageBox.confirm('确认通过该学生身份认证？', '认证审核', { type: 'success' });
  }
  await postActionApi('/verify/audit', { auditRemark, status, userId: row.userId });
  ElMessage.success('审核成功');
  await loadData();
}

async function confirmReport(row: Record<string, any>) {
  await ElMessageBox.confirm('确认举报成立，并联动隐藏违规目标？', '举报处理', { type: 'warning' });
  await postActionApi('/reports/handle', { hideTarget: 1, reportId: row.id, status: 1 });
  ElMessage.success('举报已处理');
  await loadData();
}

async function openIssues(row: Record<string, any>) {
  issueRows.value = await getListApi('/orders/issues', { orderId: row.id });
  issueDialog.value = true;
}

function openBanner(row?: Record<string, any>) {
  Object.assign(bannerForm, {
    id: row?.id,
    imageUrl: row?.imageUrl || '',
    link: row?.link || '',
    sortOrder: row?.sortOrder ?? 0,
    status: row?.status ?? 1,
    title: row?.title || '',
  });
  bannerDialog.value = true;
}

async function saveBanner() {
  await postActionApi('/banners/save', { ...bannerForm });
  ElMessage.success('保存成功');
  bannerDialog.value = false;
  await loadData();
}

function parseBookImages(row: Record<string, any>) {
  const source = Array.isArray(row.imageList)
    ? row.imageList
    : String(row.coverImages || '')
        .replace(/\[|\]|"/g, '')
        .split(',');
  return source
    .map((item: unknown) => String(item || '').trim())
    .filter((item: string) => item && item !== '/static/logo.png');
}

function openBookImages(row: Record<string, any>) {
  currentBook.value = row;
  bookImageFiles.value = parseBookImages(row).map((url: string, index: number) => ({
    name: `书籍图片${index + 1}`,
    response: { data: { url } },
    uid: Date.now() + index,
    url: assetUrl(url),
  }));
  bookImageDialog.value = true;
}

async function uploadBookImage(options: UploadRequestOptions) {
  try {
    const result = await uploadBookImageApi(options.file);
    options.onSuccess({ data: result });
  } catch (error) {
    options.onError(error as any);
  }
}

function handleBookImageSuccess(response: unknown, uploadFile: UploadUserFile) {
  const url = extractBookImageUrl({ ...uploadFile, response });
  if (url) {
    uploadFile.url = assetUrl(url);
    uploadFile.response = { data: { url } };
  }
}

function extractBookImageUrl(file: UploadUserFile) {
  const response = file.response as { data?: { url?: string }; url?: string } | undefined;
  return String(response?.data?.url || response?.url || file.url || '').trim();
}

function adminApiBase() {
  return String(import.meta.env.VITE_API_BASE || '/api').replace(/\/+$/, '');
}

function isStudentCardProp(prop: string) {
  return prop === 'studentCardImageUrl';
}


function protectedStudentCardUrl(value: unknown) {
  const url = String(value || '').trim();
  if (!url) return '';
  const params = new URLSearchParams({ url });
  const token = getToken();
  if (token) {
    params.set('token', token);
  }
  return `${adminApiBase()}/uploads/student-card/view?${params.toString()}`;
}

function displayImageUrl(value: unknown, prop: string) {
  return isStudentCardProp(prop) ? protectedStudentCardUrl(value) : assetUrl(value);
}

async function saveBookImages() {
  if (!currentBook.value) return;
  const images = Array.from(new Set(bookImageFiles.value.map(extractBookImageUrl).filter(Boolean)));
  if (!images.length) {
    ElMessage.warning('请至少上传一张书籍图片');
    return;
  }
  savingBookImages.value = true;
  try {
    await postActionApi('/books/images', { bookId: currentBook.value.id, images });
    ElMessage.success('书籍图片已保存');
    bookImageDialog.value = false;
    await loadData();
  } finally {
    savingBookImages.value = false;
  }
}

function defaultFieldValue(field: FormFieldConfig) {
  if (field.defaultValue !== undefined) return field.defaultValue;
  if (field.type === 'number' || field.type === 'money') return undefined;
  return '';
}

function resetEditForm() {
  Object.keys(editForm).forEach((key) => delete editForm[key]);
}

function openEdit(row?: Record<string, any>) {
  if (!moduleConfig.value?.formFields?.length) return;
  resetEditForm();
  editMode.value = row ? 'update' : 'create';
  if (row?.id !== undefined) {
    editForm.id = row.id;
  }
  moduleConfig.value.formFields.forEach((field) => {
    editForm[field.prop] = row?.[field.prop] ?? defaultFieldValue(field);
  });
  editDialog.value = true;
}

function validateEditForm() {
  const fields = moduleConfig.value?.formFields || [];
  for (const field of fields) {
    const value = editForm[field.prop];
    if (field.required && (value === undefined || value === null || String(value).trim() === '')) {
      ElMessage.warning(`请填写${field.label}`);
      return false;
    }
  }
  return true;
}

async function uploadFormImage(options: UploadRequestOptions, field: FormFieldConfig) {
  const maxSizeMb = field.uploadMaxSizeMb || 10;
  const file = options.file;
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件');
    options.onError(new Error('Invalid image file') as any);
    return;
  }
  if (file.size > maxSizeMb * 1024 * 1024) {
    ElMessage.warning(`图片不能超过 ${maxSizeMb}MB`);
    options.onError(new Error('Image too large') as any);
    return;
  }
  try {
    const result = await uploadAdminImageApi(field.uploadUrl || '/uploads/student-card', file);
    const url = String(result.url || '');
    if (url) {
      editForm[field.prop] = url;
      if (field.prop === 'coverImage' && result.auditStatus !== undefined) {
        editForm.coverImageStatus = result.auditStatus;
      }
    }
    options.onSuccess({ data: result });
  } catch (error) {
    options.onError(error as any);
  }
}

function uploadFormImageRequest(field: FormFieldConfig) {
  return (options: UploadRequestOptions) => uploadFormImage(options, field);
}

function removeFormImage(field: FormFieldConfig) {
  editForm[field.prop] = '';
}

async function saveEdit() {
  if (!moduleConfig.value?.saveEndpoint || !validateEditForm()) return;
  savingEdit.value = true;
  try {
    await postActionApi(moduleConfig.value.saveEndpoint, { ...editForm });
    ElMessage.success(editMode.value === 'create' ? '新增成功' : '保存成功');
    editDialog.value = false;
    await loadData();
  } finally {
    savingEdit.value = false;
  }
}

onMounted(() => {
  resetQuery();
  loadData();
});
</script>

<template>
  <div v-if="moduleConfig" class="page-stack">
    <el-card class="module-card" shadow="never">
      <div class="module-header">
        <div>
          <p class="eyebrow">BookFlow Admin</p>
          <h1>{{ moduleConfig.title }}</h1>
          <p>{{ moduleConfig.description }}</p>
        </div>
        <div class="module-actions">
          <el-button v-if="editableModule" :icon="Plus" type="primary" @click="openEdit()">新增{{ moduleConfig.title }}</el-button>
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-form :inline="true" class="toolbar" @submit.prevent>
        <el-form-item v-for="filter in moduleConfig.filters || []" :key="filter.prop" :label="filter.label">
          <el-input
            v-if="filter.type === 'input'"
            v-model="query[filter.prop]"
            clearable
            :placeholder="filter.placeholder || filter.label"
            @keyup.enter="search"
          />
          <el-select v-else v-model="query[filter.prop]" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="item in filter.options || []" :key="String(item.value)" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :icon="Search" type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery(); loadData()">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column type="index" width="56" label="#" />
        <el-table-column
          v-for="column in moduleConfig.columns"
          :key="column.prop"
          :label="column.label"
          :min-width="column.minWidth"
          :prop="column.prop"
          :width="column.width"
        >
          <template #default="{ row }">
            <el-image
              v-if="column.type === 'image'"
              fit="cover"
              :preview-src-list="row[column.prop] ? [displayImageUrl(row[column.prop], column.prop)] : []"
              preview-teleported
              class="table-image"
              :src="displayImageUrl(row[column.prop], column.prop)"
            >
              <template #error><div class="image-fallback">无图</div></template>
            </el-image>
            <el-tag v-else-if="column.type === 'status'" :type="optionType(column, row[column.prop])">
              {{ optionLabel(column, row[column.prop]) }}
            </el-tag>
            <span v-else-if="column.type === 'date'">{{ formatDate(row[column.prop]) }}</span>
            <span v-else-if="column.type === 'money'">{{ formatMoney(row[column.prop]) }}</span>
            <el-tooltip v-else-if="column.type === 'textarea'" :content="String(row[column.prop] || '-')" placement="top">
              <span>{{ shortText(row[column.prop]) }}</span>
            </el-tooltip>
            <span v-else>{{ row[column.prop] ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="moduleConfig.actions?.length" fixed="right" label="操作" width="280">
          <template #default="{ row }">
            <el-button
              v-for="action in moduleConfig.actions"
              v-show="actionVisible(action, row)"
              :key="action"
              link
              :type="actionType(action)"
              @click="runAction(action, row)"
            >
              {{ actionText(action, row) }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        background
        class="pager"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @change="loadData"
      />
    </el-card>

    <el-dialog
      v-model="editDialog"
      :title="`${editMode === 'create' ? '新增' : '编辑'}${moduleConfig.title}`"
      width="720px"
    >
      <el-form class="edit-form" label-width="130px">
        <el-form-item v-for="field in moduleConfig.formFields || []" :key="field.prop" :label="field.label" :required="field.required">
          <el-select
            v-if="field.type === 'select'"
            v-model="editForm[field.prop]"
            clearable
            :placeholder="field.placeholder || `请选择${field.label}`"
            style="width: 100%"
          >
            <el-option
              v-for="item in field.options || []"
              :key="String(item.value)"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-input-number
            v-else-if="field.type === 'number' || field.type === 'money'"
            v-model="editForm[field.prop]"
            :max="field.max"
            :min="field.min"
            :precision="field.type === 'money' ? 2 : 0"
            :step="field.type === 'money' ? 1 : 1"
            style="width: 100%"
          />
          <div v-else-if="field.type === 'image-upload'" class="form-image-uploader">
            <el-image
              v-if="editForm[field.prop]"
              class="form-image-preview"
              fit="cover"
              :preview-src-list="[displayImageUrl(editForm[field.prop], field.prop)]"
              preview-teleported
              :src="displayImageUrl(editForm[field.prop], field.prop)"
            >
              <template #error><div class="image-fallback">无图</div></template>
            </el-image>
            <div class="form-image-actions">
              <el-upload
                accept="image/*"
                :http-request="uploadFormImageRequest(field)"
                :show-file-list="false"
              >
                <el-button type="primary">{{ editForm[field.prop] ? '替换图片' : '上传图片' }}</el-button>
              </el-upload>
              <el-button v-if="editForm[field.prop]" @click="removeFormImage(field)">删除图片</el-button>
            </div>
            <div class="form-image-tip">仅支持图片文件，大小不超过 {{ field.uploadMaxSizeMb || 10 }}MB。点击预览图可放大查看。</div>
          </div>
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="editForm[field.prop]"
            :placeholder="field.placeholder || `请输入${field.label}`"
            :rows="4"
            type="textarea"
          />
          <el-input
            v-else
            v-model="editForm[field.prop]"
            clearable
            :placeholder="field.placeholder || `请输入${field.label}`"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button :loading="savingEdit" type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueDialog" title="订单问题记录" width="760px">
      <el-table :data="issueRows" border>
        <el-table-column prop="userName" label="提交人" width="120" />
        <el-table-column prop="type" label="类型" width="90" />
        <el-table-column prop="content" label="问题内容" min-width="220" />
        <el-table-column prop="replyContent" label="回复内容" min-width="180" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="bookImageDialog" title="书籍图片管理" width="760px">
      <div class="upload-tip">
        当前书籍：{{ currentBook?.title || '-' }}。支持上传多张图片，第一张会作为列表封面展示。
      </div>
      <el-upload
        v-model:file-list="bookImageFiles"
        accept="image/*"
        :http-request="uploadBookImage"
        list-type="picture-card"
        multiple
        :on-success="handleBookImageSuccess"
      >
        <el-icon><Plus /></el-icon>
      </el-upload>
      <template #footer>
        <el-button @click="bookImageDialog = false">取消</el-button>
        <el-button :loading="savingBookImages" type="primary" @click="saveBookImages">保存图片</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bannerDialog" :title="bannerForm.id ? '编辑Banner' : '新增Banner'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="标题"><el-input v-model="bannerForm.title" /></el-form-item>
        <el-form-item label="图片地址"><el-input v-model="bannerForm.imageUrl" /></el-form-item>
        <el-form-item label="跳转链接"><el-input v-model="bannerForm.link" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="bannerForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="bannerForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bannerDialog = false">取消</el-button>
        <el-button type="primary" @click="saveBanner">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.upload-tip {
  margin-bottom: 16px;
  color: #5f6b7a;
  line-height: 1.6;
}

.edit-form {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 10px;
}

.table-image {
  cursor: zoom-in;
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.table-image:hover {
  box-shadow: 0 8px 22px rgb(31 45 61 / 18%);
  transform: scale(1.04);
}

.form-image-uploader {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.form-image-preview {
  width: 132px;
  height: 92px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  cursor: zoom-in;
  overflow: hidden;
}

.form-image-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.form-image-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}
</style>
