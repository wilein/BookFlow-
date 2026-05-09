<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const form = reactive({
  password: '123456',
  username: 'admin',
});

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码');
    return;
  }
  loading.value = true;
  try {
    await auth.login(form.username, form.password);
    ElMessage.success('登录成功');
    router.replace(String(route.query.redirect || '/dashboard'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-hero">
      <div class="hero-badge">BookFlow Admin</div>
      <h1>校园二手书与学习资源平台后台</h1>
      <p>集中处理学生认证、书籍运营、交易订单、社区内容、举报反馈和系统日志。</p>
    </section>

    <el-card class="login-card" shadow="never">
      <h2>管理员登录</h2>
      <p class="login-tip">默认账号由后端启动脚本初始化。</p>
      <el-form label-position="top" @keyup.enter="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" :prefix-icon="User" placeholder="admin" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            placeholder="123456"
            show-password
            size="large"
            type="password"
          />
        </el-form-item>
        <el-button :loading="loading" class="login-button" size="large" type="primary" @click="submit">
          登录后台
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>
