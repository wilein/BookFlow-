# BookFlow Element Admin

这是为 BookFlow 单独重建的轻量后台管理端，技术栈采用 `vue-element-plus-admin` 同类组合：

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios

## 启动

```bash
pnpm install
pnpm dev
```

开发服务默认端口：`5788`

后端代理：

```text
/api -> http://127.0.0.1:8080/admin
```

默认后台账号由后端初始化脚本创建：

```text
admin / 123456
```

## 目录说明

```text
src/api        请求封装和后台接口
src/config     菜单和模块表格配置
src/layout     后台主布局
src/router     静态路由和登录守卫
src/stores     登录态和管理员信息
src/views      页面
```
