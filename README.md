# 校园学术资源传承平台

> 基于微信小程序的二手书交易与学习经验传承系统 | 本科毕业设计

本平台旨在解决高校二手教材流转效率低、学习笔记难以传承的问题。**将二手书交易与批注传承、学习路径、资源共享相结合**，让学长学姐的学习心得能够随书籍一起传递给学弟学妹。

## ✨ 核心特色

- 📚 **二手书交易**：发布/搜索/购买教材，ISBN自动补全，订单状态全程跟踪
- ✍️ **批注传承**：按页码添加文字/图片批注，支持公开/仅购买者可见等权限控制
- 🗺️ **学习路径**：创建树形结构的学习路线（如“Java学习路线”），节点绑定资源，支持进度追踪
- 📎 **资源共享**：上传PDF/PPT/图片等资料，与书籍或路径节点关联
- 💬 **社区互动**：发帖、评论、点赞、收藏、举报，构建学术互助圈
- 💬 **私信沟通**：买卖双方围绕订单即时聊天，降低交易摩擦
- 🛡️ **学生认证**：学号/学生证审核，确保校园纯净环境
- 🖥️ **管理后台**：用户/书籍/订单/举报审核，轮播图配置，数据概览

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 4.0.x |
| 持久层 | MyBatis-Flex + MySQL 8.0 |
| 缓存与会话 | Redis 7.0 + JWT |
| 用户端 | UniApp (Vue 3) → 微信小程序 |
| 管理后台 | Vue 3 + Element Plus + Vite |
| 文件存储 | 本地文件上传（可扩展MinIO） |
| 开发工具 | IntelliJ IDEA, HBuilderX, VS Code |

## 📱 功能预览

> 以下为论文中的关键界面，实际运行截图请查看 `/screenshots` 目录

| 用户端 | 管理端 |
|--------|--------|
| 首页浏览、分类检索 | 数据概览仪表盘 |
| 微信登录 + 学生认证 | 用户/认证审核 |
| 书籍发布（ISBN自动补全） | 书籍/资源/路径管理 |
| 订单创建/支付/发货/收货 | 订单纠纷处理 |
| 批注列表 + 新增批注 | 社区帖子/评论审核 |
| 学习路径查看 + 进度标记 | 轮播图/举报/反馈处理 |
| 社区帖子 + 评论/点赞 | 操作日志记录 |
| 私信聊天（订单关联） | |

## 🚀 快速开始

### 1. 克隆项目
```bash
https://github.com/wilein/BookFlow-.git
```markdown
# 后端服务 (Spring Boot)

本目录包含校园学术资源传承平台的后端代码，提供 RESTful API，支持小程序和管理后台的数据交互。

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis 7.0

## 快速启动
#后端（spring）
### 1. 导入数据库

创建数据库（如 `academic_resource`），执行 SQL 脚本：

```bash
mysql -u root -p academic_resource < sql/init.sql
### 2. 修改配置文件application.yml
修改数据库url，username，password
修改redis配置
修改weixin开发者appid和secret
### 3. 编译与运行
# 进入 backend 目录
cd backend

# 编译打包
mvn clean package

# 运行
java -jar target/academic-resource-backend.jar --spring.profiles.active=dev
或者是直接运行主类BookflowApplication.java


# 微信小程序端（UniApp）

本目录为基于 UniApp 开发的微信小程序源码，提供书籍浏览、交易、批注、学习路径、社区等用户端功能。

## 环境要求

- HBuilderX（最新版）
- 微信开发者工具
- Node.js（用于安装依赖，UniApp 通常通过 HBuilderX 管理）

## 快速启动

### 1. 打开项目

使用 HBuilderX 打开 `frontend-miniprogram` 目录。

### 2. 配置小程序 AppID

修改 `manifest.json` 文件：

```json
{
  "mp-weixin": {
    "appid": "your_wechat_miniprogram_appid",
    "setting": {
      "urlCheck": false,   // 开发时可关闭域名校验
      "es6": true
    }
  }
}
### 3. 配置后端地址
修改utils/config
const ENV_CONFIG = {
  development: {
    API_BASE_URL: 'http://10.212.211.152:8080',
    IMAGE_BASE_URL: 'http://10.212.211.152:8080'
  }
}



# 管理后台 (Vue 3 + Element Plus)

本目录为平台的管理端 Web 应用，用于用户认证审核、内容管理、订单处理、数据统计等。

## 环境要求

- Node.js 16+
- npm 或 yarn

## 快速启动

### 1. 安装依赖

```bash
cd frontend-admin
npm install
### 2. 启动
pnpm run dev
