import type { OptionItem } from '@/utils/format';

export type ColumnType = 'date' | 'image' | 'money' | 'status' | 'text' | 'textarea';
export type FormFieldType = 'image-upload' | 'input' | 'money' | 'number' | 'select' | 'textarea';
export type FilterType = 'input' | 'select';
export type ActionKey =
  | 'edit'
  | 'approveVerify'
  | 'bannerEdit'
  | 'bannerToggle'
  | 'bookDelete'
  | 'bookImages'
  | 'bookOff'
  | 'bookOn'
  | 'commentHide'
  | 'commentShow'
  | 'credit'
  | 'feedbackHandle'
  | 'issues'
  | 'pathDisable'
  | 'pathPublish'
  | 'postHide'
  | 'postShow'
  | 'rejectVerify'
  | 'reportClose'
  | 'reportConfirm'
  | 'resourceHide'
  | 'resourceShow'
  | 'userDisable'
  | 'userEnable';

export interface FilterConfig {
  label: string;
  prop: string;
  type: FilterType;
  options?: OptionItem[];
  placeholder?: string;
}

export interface ColumnConfig {
  label: string;
  prop: string;
  type?: ColumnType;
  minWidth?: number;
  options?: OptionItem[];
  width?: number;
}

export interface FormFieldConfig {
  defaultValue?: boolean | number | string;
  label: string;
  max?: number;
  min?: number;
  options?: OptionItem[];
  placeholder?: string;
  prop: string;
  required?: boolean;
  type?: FormFieldType;
  uploadMaxSizeMb?: number;
  uploadUrl?: string;
}

export interface ModuleConfig {
  actions?: ActionKey[];
  columns: ColumnConfig[];
  description: string;
  endpoint: string;
  filters?: FilterConfig[];
  formFields?: FormFieldConfig[];
  key: string;
  saveEndpoint?: string;
  title: string;
}

export const authStatusOptions: OptionItem[] = [
  { label: '未提交', value: 0, type: 'info' },
  { label: '待审核', value: 1, type: 'warning' },
  { label: '已通过', value: 2, type: 'success' },
  { label: '已驳回', value: 3, type: 'danger' },
];

export const enabledOptions: OptionItem[] = [
  { label: '正常', value: 1, type: 'success' },
  { label: '禁用', value: 0, type: 'danger' },
];

export const visibleOptions: OptionItem[] = [
  { label: '显示', value: true, type: 'success' },
  { label: '隐藏', value: false, type: 'danger' },
];

export const bookStatusOptions: OptionItem[] = [
  { label: '在售', value: 1, type: 'success' },
  { label: '交易中', value: 2, type: 'warning' },
  { label: '已售', value: 3, type: 'info' },
  { label: '下架', value: 4, type: 'danger' },
];

export const orderStatusOptions: OptionItem[] = [
  { label: '待付款', value: 0, type: 'warning' },
  { label: '待发货', value: 1, type: 'primary' },
  { label: '待收货', value: 2, type: 'primary' },
  { label: '已完成', value: 3, type: 'success' },
  { label: '已取消', value: 4, type: 'info' },
];

export const reportStatusOptions: OptionItem[] = [
  { label: '待处理', value: 0, type: 'warning' },
  { label: '已确认', value: 1, type: 'success' },
  { label: '已关闭', value: 2, type: 'info' },
];

export const pathStatusOptions: OptionItem[] = [
  { label: '草稿', value: 0, type: 'info' },
  { label: '已发布', value: 1, type: 'success' },
  { label: '审核中', value: 2, type: 'warning' },
  { label: '下架', value: 3, type: 'danger' },
];

export const pathCoverStatusOptions: OptionItem[] = [
  { label: '无需审核', value: 0, type: 'info' },
  { label: '待审核', value: 1, type: 'warning' },
  { label: '已通过', value: 2, type: 'success' },
  { label: '已驳回', value: 3, type: 'danger' },
];

export const binaryStatusOptions: OptionItem[] = [
  { label: '启用', value: 1, type: 'success' },
  { label: '停用', value: 0, type: 'danger' },
];

export const genderOptions: OptionItem[] = [
  { label: '未知', value: 0, type: 'info' },
  { label: '男', value: 1, type: 'primary' },
  { label: '女', value: 2, type: 'danger' },
];

export const conditionOptions: OptionItem[] = [
  { label: '全新', value: 1, type: 'success' },
  { label: '9成新', value: 2, type: 'success' },
  { label: '8成新', value: 3, type: 'primary' },
  { label: '7成新', value: 4, type: 'warning' },
  { label: '6成新及以下', value: 5, type: 'info' },
];

export const postTypeOptions: OptionItem[] = [
  { label: '推荐', value: 0, type: 'primary' },
  { label: '书评', value: 1, type: 'success' },
  { label: '问答', value: 2, type: 'warning' },
  { label: '学习路径', value: 3, type: 'info' },
];

export const resourceTypeOptions: OptionItem[] = [
  { label: '课件', value: 1, type: 'primary' },
  { label: '习题', value: 2, type: 'success' },
  { label: '笔记', value: 3, type: 'warning' },
  { label: '拓展阅读', value: 4, type: 'info' },
  { label: '其他', value: 5, type: 'info' },
];

export const resourceVisibilityOptions: OptionItem[] = [
  { label: '公开', value: 1, type: 'success' },
  { label: '仅买家可见', value: 2, type: 'warning' },
  { label: '私密', value: 3, type: 'info' },
];

export const resourceBindTypeOptions: OptionItem[] = [
  { label: '未绑定', value: 'none', type: 'info' },
  { label: '关联书籍', value: 'book', type: 'success' },
  { label: '关联路径节点', value: 'pathNode', type: 'primary' },
];

export const modules: Record<string, ModuleConfig> = {
  users: {
    actions: ['edit', 'credit', 'userDisable', 'userEnable'],
    columns: [
      { label: '头像', prop: 'avatarUrl', type: 'image', width: 76 },
      { label: '昵称', prop: 'nickname', minWidth: 130 },
      { label: '手机号', prop: 'mobile', minWidth: 130 },
      { label: '学校', prop: 'school', minWidth: 150 },
      { label: '院系', prop: 'department', minWidth: 130 },
      { label: '学生证', prop: 'studentCardImageUrl', type: 'image', width: 86 },
      { label: '认证', prop: 'authStatus', type: 'status', options: authStatusOptions, width: 100 },
      { label: '信用分', prop: 'creditScore', width: 90 },
      { label: '状态', prop: 'enabled', type: 'status', options: visibleOptions, width: 90 },
      { label: '注册时间', prop: 'createTime', type: 'date', width: 170 },
    ],
    description: '查看微信用户资料、认证状态、信用分，支持禁用和恢复。',
    endpoint: '/users',
    filters: [
      { label: '关键词', prop: 'keyword', type: 'input', placeholder: '昵称/手机号/openid' },
      { label: '账号状态', prop: 'status', type: 'select', options: enabledOptions },
      { label: '认证状态', prop: 'authStatus', type: 'select', options: authStatusOptions },
    ],
    key: 'users',
    saveEndpoint: '/users/save',
    formFields: [
      { label: '昵称', prop: 'nickname', required: true },
      { label: 'OpenID', prop: 'openid', placeholder: '不填则后台自动生成' },
      { label: '手机号', prop: 'mobile' },
      { label: '头像地址', prop: 'avatarUrl' },
      { label: '性别', prop: 'gender', type: 'select', options: genderOptions, defaultValue: 0 },
      { label: '省份', prop: 'province' },
      { label: '城市', prop: 'city' },
      { label: '真实姓名', prop: 'realName' },
      { label: '学号', prop: 'studentId' },
      { label: '学校', prop: 'school' },
      { label: '院系', prop: 'department' },
      { label: '认证状态', prop: 'authStatus', type: 'select', options: authStatusOptions, defaultValue: 0 },
      { label: '信用分', prop: 'creditScore', type: 'number', min: 0, max: 100, defaultValue: 88 },
      { label: '账号状态', prop: 'enabled', type: 'select', options: visibleOptions, defaultValue: true },
      { label: '个人简介', prop: 'intro', type: 'textarea' },
      { label: '学生证图片', prop: 'studentCardImageUrl', type: 'image-upload', uploadUrl: '/uploads/student-card', uploadMaxSizeMb: 10 },
      { label: '认证方式', prop: 'verifyType', placeholder: 'student_card / edu_email' },
      { label: '审核备注', prop: 'auditRemark', type: 'textarea' },
    ],
    title: '用户管理',
  },
  verify: {
    actions: ['edit', 'approveVerify', 'rejectVerify'],
    columns: [
      { label: '头像', prop: 'avatarUrl', type: 'image', width: 76 },
      { label: '真实姓名', prop: 'realName', minWidth: 110 },
      { label: '学号', prop: 'studentId', minWidth: 130 },
      { label: '学校', prop: 'school', minWidth: 150 },
      { label: '院系', prop: 'department', minWidth: 130 },
      { label: '学生证', prop: 'studentCardImageUrl', type: 'image', width: 86 },
      { label: '状态', prop: 'authStatus', type: 'status', options: authStatusOptions, width: 100 },
      { label: '审核备注', prop: 'auditRemark', type: 'textarea', minWidth: 180 },
      { label: '提交时间', prop: 'verifySubmitTime', type: 'date', width: 170 },
    ],
    description: '处理学生身份认证，通过或驳回后会写回小程序个人资料。',
    endpoint: '/verify/list',
    filters: [
      { label: '关键词', prop: 'keyword', type: 'input', placeholder: '姓名/学号/学校/院系' },
      { label: '审核状态', prop: 'status', type: 'select', options: authStatusOptions },
    ],
    key: 'verify',
    saveEndpoint: '/verify/save',
    formFields: [
      { label: '用户ID', prop: 'userId', type: 'number', placeholder: '不填则创建后台用户' },
      { label: '昵称', prop: 'nickname', placeholder: '新增用户时使用' },
      { label: '手机号', prop: 'mobile' },
      { label: '真实姓名', prop: 'realName', required: true },
      { label: '学号', prop: 'studentId' },
      { label: '学校', prop: 'school' },
      { label: '院系', prop: 'department' },
      { label: '学生证图片', prop: 'studentCardImageUrl', type: 'image-upload', uploadUrl: '/uploads/student-card', uploadMaxSizeMb: 10 },
      { label: '认证方式', prop: 'verifyType', defaultValue: 'student_card' },
      { label: '认证状态', prop: 'authStatus', type: 'select', options: authStatusOptions, defaultValue: 1 },
      { label: '信用分', prop: 'creditScore', type: 'number', min: 0, max: 100, defaultValue: 88 },
      { label: '审核备注', prop: 'auditRemark', type: 'textarea' },
    ],
    title: '认证审核',
  },
  books: {
    actions: ['edit', 'bookImages', 'bookOn', 'bookOff', 'bookDelete'],
    columns: [
      { label: '封面', prop: 'cover', type: 'image', width: 82 },
      { label: '书名', prop: 'title', minWidth: 190 },
      { label: '作者', prop: 'author', minWidth: 120 },
      { label: '分类', prop: 'category', minWidth: 100 },
      { label: '卖家', prop: 'sellerName', minWidth: 110 },
      { label: '价格', prop: 'price', type: 'money', width: 110 },
      { label: '状态', prop: 'status', type: 'status', options: bookStatusOptions, width: 100 },
      { label: '发布时间', prop: 'createTime', type: 'date', width: 170 },
    ],
    description: '查看发布书籍，支持恢复在售、下架和软删除。',
    endpoint: '/books',
    filters: [
      { label: '关键词', prop: 'keyword', type: 'input', placeholder: '书名/作者/ISBN/分类' },
      { label: '书籍状态', prop: 'status', type: 'select', options: bookStatusOptions },
    ],
    key: 'books',
    saveEndpoint: '/books/save',
    formFields: [
      { label: '卖家用户ID', prop: 'userId', type: 'number', placeholder: '不填则创建后台卖家' },
      { label: '书名', prop: 'title', required: true },
      { label: '作者', prop: 'author' },
      { label: '出版社', prop: 'publisher' },
      { label: 'ISBN', prop: 'isbn' },
      { label: '分类', prop: 'category' },
      { label: '价格', prop: 'price', type: 'money', min: 0, defaultValue: 0 },
      { label: '原价', prop: 'originalPrice', type: 'money', min: 0 },
      { label: '新旧程度', prop: 'condition', type: 'select', options: conditionOptions, defaultValue: 3 },
      { label: '状态', prop: 'status', type: 'select', options: bookStatusOptions, defaultValue: 1 },
      { label: '描述', prop: 'description', type: 'textarea' },
      { label: '浏览数', prop: 'viewCount', type: 'number', min: 0, defaultValue: 0 },
      { label: '收藏数', prop: 'favoriteCount', type: 'number', min: 0, defaultValue: 0 },
    ],
    title: '书籍管理',
  },
  orders: {
    actions: ['edit', 'issues'],
    columns: [
      { label: '订单号', prop: 'orderNo', minWidth: 180 },
      { label: '书籍', prop: 'bookTitle', minWidth: 180 },
      { label: '买家', prop: 'buyerName', minWidth: 100 },
      { label: '卖家', prop: 'sellerName', minWidth: 100 },
      { label: '金额', prop: 'totalAmount', type: 'money', width: 110 },
      { label: '状态', prop: 'status', type: 'status', options: orderStatusOptions, width: 100 },
      { label: '收货人', prop: 'receiverName', minWidth: 110 },
      { label: '问题数', prop: 'issueCount', width: 90 },
      { label: '创建时间', prop: 'createTime', type: 'date', width: 170 },
    ],
    description: '查看订单全链路信息，后台默认只读，避免误改交易状态。',
    endpoint: '/orders',
    filters: [
      { label: '关键词', prop: 'keyword', type: 'input', placeholder: '订单号/收货人/手机号' },
      { label: '订单状态', prop: 'status', type: 'select', options: orderStatusOptions },
    ],
    key: 'orders',
    saveEndpoint: '/orders/save',
    formFields: [
      { label: '订单号', prop: 'orderNo', placeholder: '不填则后台自动生成' },
      { label: '书籍ID', prop: 'bookId', type: 'number' },
      { label: '买家ID', prop: 'buyerId', type: 'number', placeholder: '不填则创建后台买家' },
      { label: '卖家ID', prop: 'sellerId', type: 'number', placeholder: '不填则优先使用书籍卖家' },
      { label: '金额', prop: 'totalAmount', type: 'money', min: 0, defaultValue: 0 },
      { label: '订单状态', prop: 'status', type: 'select', options: orderStatusOptions, defaultValue: 0 },
      { label: '支付方式', prop: 'paymentMethod', type: 'number', min: 0, defaultValue: 0 },
      { label: '收货人', prop: 'receiverName' },
      { label: '收货手机号', prop: 'receiverPhone' },
      { label: '收货地址', prop: 'receiverAddress', type: 'textarea' },
      { label: '买家留言', prop: 'buyerMessage', type: 'textarea' },
    ],
    title: '订单管理',
  },
  posts: {
    actions: ['edit', 'postHide', 'postShow'],
    columns: [
      { label: '标题', prop: 'title', minWidth: 180 },
      { label: '作者', prop: 'authorName', minWidth: 110 },
      { label: '内容', prop: 'content', type: 'textarea', minWidth: 240 },
      { label: '浏览', prop: 'viewCount', width: 80 },
      { label: '点赞', prop: 'likeCount', width: 80 },
      { label: '评论', prop: 'commentCount', width: 80 },
      { label: '状态', prop: 'visible', type: 'status', options: visibleOptions, width: 90 },
      { label: '发布时间', prop: 'createTime', type: 'date', width: 170 },
    ],
    description: '管理社区帖子和学习路径分享内容，支持隐藏与恢复。',
    endpoint: '/content/posts',
    filters: [
      { label: '关键词', prop: 'keyword', type: 'input', placeholder: '标题/内容' },
      { label: '显示状态', prop: 'status', type: 'select', options: enabledOptions },
    ],
    key: 'posts',
    saveEndpoint: '/content/posts/save',
    formFields: [
      { label: '作者用户ID', prop: 'userId', type: 'number', placeholder: '不填则创建后台发帖用户' },
      { label: '标题', prop: 'title', required: true },
      { label: '内容', prop: 'content', type: 'textarea' },
      { label: '类型', prop: 'type', type: 'select', options: postTypeOptions, defaultValue: 0 },
      { label: '分享路径ID', prop: 'sharedPathId', type: 'number' },
      { label: '显示状态', prop: 'visible', type: 'select', options: visibleOptions, defaultValue: true },
      { label: '浏览数', prop: 'viewCount', type: 'number', min: 0, defaultValue: 0 },
      { label: '点赞数', prop: 'likeCount', type: 'number', min: 0, defaultValue: 0 },
      { label: '评论数', prop: 'commentCount', type: 'number', min: 0, defaultValue: 0 },
    ],
    title: '内容管理',
  },
  reports: {
    actions: ['edit', 'reportConfirm', 'reportClose'],
    columns: [
      { label: '举报人', prop: 'userName', minWidth: 110 },
      { label: '对象类型', prop: 'targetType', width: 110 },
      { label: '对象标题', prop: 'targetTitle', minWidth: 180 },
      { label: '原因', prop: 'reasonType', minWidth: 140 },
      { label: '说明', prop: 'content', type: 'textarea', minWidth: 240 },
      { label: '状态', prop: 'status', type: 'status', options: reportStatusOptions, width: 100 },
      { label: '举报时间', prop: 'createTime', type: 'date', width: 170 },
    ],
    description: '处理帖子、评论、书籍、资源、订单等举报，可联动隐藏违规内容。',
    endpoint: '/reports',
    filters: [
      { label: '对象类型', prop: 'targetType', type: 'input', placeholder: 'post/book/order...' },
      { label: '处理状态', prop: 'status', type: 'select', options: reportStatusOptions },
    ],
    key: 'reports',
    saveEndpoint: '/reports/save',
    formFields: [
      { label: '举报用户ID', prop: 'userId', type: 'number', placeholder: '不填则创建后台举报用户' },
      { label: '对象类型', prop: 'targetType', placeholder: 'post/book/order/resource/path' },
      { label: '对象ID', prop: 'targetId', type: 'number', defaultValue: 0 },
      { label: '原因类型', prop: 'reasonType', defaultValue: '后台录入' },
      { label: '说明内容', prop: 'content', type: 'textarea' },
      { label: '处理状态', prop: 'status', type: 'select', options: reportStatusOptions, defaultValue: 0 },
    ],
    title: '举报审核',
  },
  feedbacks: {
    actions: ['edit', 'feedbackHandle'],
    columns: [
      { label: '用户', prop: 'userName', minWidth: 110 },
      { label: '类型', prop: 'feedbackType', width: 110 },
      { label: '内容', prop: 'content', type: 'textarea', minWidth: 260 },
      { label: '联系方式', prop: 'contact', minWidth: 140 },
      { label: '页面路径', prop: 'pagePath', minWidth: 170 },
      { label: '状态', prop: 'status', type: 'status', options: reportStatusOptions.slice(0, 2), width: 100 },
      { label: '提交时间', prop: 'createTime', type: 'date', width: 170 },
    ],
    description: '处理用户反馈，保留页面路径和联系方式便于定位问题。',
    endpoint: '/feedbacks',
    filters: [{ label: '处理状态', prop: 'status', type: 'select', options: reportStatusOptions.slice(0, 2) }],
    key: 'feedbacks',
    saveEndpoint: '/feedbacks/save',
    formFields: [
      { label: '用户ID', prop: 'userId', type: 'number', placeholder: '不填则创建后台反馈用户' },
      { label: '反馈类型', prop: 'feedbackType', defaultValue: 'other' },
      { label: '反馈内容', prop: 'content', type: 'textarea' },
      { label: '联系方式', prop: 'contact' },
      { label: '页面路径', prop: 'pagePath' },
      { label: '处理状态', prop: 'status', type: 'select', options: reportStatusOptions.slice(0, 2), defaultValue: 0 },
    ],
    title: '反馈管理',
  },
  resources: {
    actions: ['edit', 'resourceHide', 'resourceShow'],
    columns: [
      { label: '标题', prop: 'title', minWidth: 180 },
      { label: '上传人', prop: 'ownerName', minWidth: 110 },
      { label: '绑定类型', prop: 'bindType', type: 'status', options: resourceBindTypeOptions, width: 120 },
      { label: '绑定ID', prop: 'bindId', width: 90 },
      { label: '绑定目标', prop: 'bindTargetTitle', minWidth: 180 },
      { label: '格式', prop: 'fileFormat', width: 90 },
      { label: '下载', prop: 'downloadCount', width: 80 },
      { label: '说明', prop: 'description', type: 'textarea', minWidth: 220 },
      { label: '状态', prop: 'visible', type: 'status', options: visibleOptions, width: 90 },
      { label: '上传时间', prop: 'createTime', type: 'date', width: 170 },
    ],
    description: '管理用户上传资源，支持下架和恢复。',
    endpoint: '/resources',
    filters: [
      { label: '关键词', prop: 'keyword', type: 'input', placeholder: '标题/说明/格式' },
      { label: '显示状态', prop: 'status', type: 'select', options: enabledOptions },
      { label: '绑定类型', prop: 'bindType', type: 'select', options: resourceBindTypeOptions },
    ],
    key: 'resources',
    saveEndpoint: '/resources/save',
    formFields: [
      { label: '上传用户ID', prop: 'userId', type: 'number', placeholder: '不填则创建后台资源用户' },
      { label: '关联书籍ID', prop: 'bookId', type: 'number', placeholder: '资源属于哪本书，可选' },
      { label: '绑定类型', prop: 'bindType', type: 'select', options: resourceBindTypeOptions, defaultValue: 'none' },
      { label: '绑定ID', prop: 'bindId', type: 'number', placeholder: '书籍ID或路径节点ID' },
      { label: '标题', prop: 'title', required: true },
      { label: '资源类型', prop: 'type', type: 'select', options: resourceTypeOptions, defaultValue: 5 },
      { label: '文件地址', prop: 'fileUrl' },
      { label: '文件大小', prop: 'fileSize', type: 'number', min: 0, defaultValue: 0 },
      { label: '文件格式', prop: 'fileFormat' },
      { label: '下载次数', prop: 'downloadCount', type: 'number', min: 0, defaultValue: 0 },
      { label: '可见性', prop: 'visibility', type: 'select', options: resourceVisibilityOptions, defaultValue: 1 },
      { label: '显示状态', prop: 'visible', type: 'select', options: visibleOptions, defaultValue: true },
      { label: '说明', prop: 'description', type: 'textarea' },
    ],
    title: '资源管理',
  },
  paths: {
    actions: ['edit', 'pathPublish', 'pathDisable'],
    columns: [
      { label: '封面', prop: 'coverImage', type: 'image', width: 96 },
      { label: '标题', prop: 'title', minWidth: 180 },
      { label: '作者', prop: 'authorName', minWidth: 110 },
      { label: '关联书籍', prop: 'bookTitle', minWidth: 160 },
      { label: '难度', prop: 'difficulty', width: 90 },
      { label: '节点数', prop: 'nodeCount', width: 90 },
      { label: '状态', prop: 'status', type: 'status', options: pathStatusOptions, width: 100 },
      { label: '封面审核', prop: 'coverImageStatus', type: 'status', options: pathCoverStatusOptions, width: 110 },
      { label: '创建时间', prop: 'createTime', type: 'date', width: 170 },
    ],
    description: '查看学习路径、节点数量、路径状态和封面审核状态，支持审核发布和下架。',
    endpoint: '/paths',
    filters: [
      { label: '关键词', prop: 'keyword', type: 'input', placeholder: '标题/说明' },
      { label: '路径状态', prop: 'status', type: 'select', options: pathStatusOptions },
      { label: '封面审核', prop: 'coverImageStatus', type: 'select', options: pathCoverStatusOptions },
    ],
    key: 'paths',
    saveEndpoint: '/paths/save',
    formFields: [
      { label: '作者用户ID', prop: 'userId', type: 'number', placeholder: '不填则创建后台路径用户' },
      { label: '关联书籍ID', prop: 'bookId', type: 'number' },
      { label: '来源路径ID', prop: 'sourcePathId', type: 'number' },
      { label: '标题', prop: 'title', required: true },
      { label: '封面图', prop: 'coverImage', type: 'image-upload', uploadUrl: '/uploads/path-cover', uploadMaxSizeMb: 10 },
      { label: '封面审核', prop: 'coverImageStatus', type: 'select', options: pathCoverStatusOptions, defaultValue: 0 },
      { label: '难度', prop: 'difficulty', type: 'number', min: 1, max: 5, defaultValue: 1 },
      { label: '预计小时', prop: 'estimatedHours', type: 'number', min: 0, defaultValue: 1 },
      { label: '状态', prop: 'status', type: 'select', options: pathStatusOptions, defaultValue: 0 },
      { label: '浏览数', prop: 'viewCount', type: 'number', min: 0, defaultValue: 0 },
      { label: '收藏数', prop: 'favoriteCount', type: 'number', min: 0, defaultValue: 0 },
      { label: '描述', prop: 'description', type: 'textarea' },
    ],
    title: '学习路径',
  },
  banners: {
    actions: ['edit', 'bannerToggle'],
    columns: [
      { label: '图片', prop: 'imageUrl', type: 'image', width: 100 },
      { label: '标题', prop: 'title', minWidth: 180 },
      { label: '链接', prop: 'link', minWidth: 220 },
      { label: '排序', prop: 'sortOrder', width: 90 },
      { label: '状态', prop: 'status', type: 'status', options: binaryStatusOptions, width: 90 },
      { label: '更新时间', prop: 'updateTime', type: 'date', width: 170 },
    ],
    description: '配置首页 Banner 展示图、跳转链接、排序和启停状态。',
    endpoint: '/banners',
    filters: [{ label: '状态', prop: 'status', type: 'select', options: binaryStatusOptions }],
    key: 'banners',
    saveEndpoint: '/banners/save',
    formFields: [
      { label: '标题', prop: 'title', required: true },
      { label: '图片地址', prop: 'imageUrl' },
      { label: '跳转链接', prop: 'link' },
      { label: '排序', prop: 'sortOrder', type: 'number', min: 0, defaultValue: 0 },
      { label: '状态', prop: 'status', type: 'select', options: binaryStatusOptions, defaultValue: 1 },
    ],
    title: 'Banner管理',
  },
  logs: {
    columns: [
      { label: '管理员', prop: 'adminName', minWidth: 120 },
      { label: '模块', prop: 'module', minWidth: 120 },
      { label: '动作', prop: 'action', minWidth: 140 },
      { label: '目标ID', prop: 'targetId', width: 100 },
      { label: '操作时间', prop: 'createTime', type: 'date', width: 170 },
      { label: '变更后数据', prop: 'afterData', type: 'textarea', minWidth: 260 },
    ],
    description: '查看后台审核、下架、禁用等操作留痕。',
    endpoint: '/logs',
    filters: [{ label: '模块', prop: 'module', type: 'input', placeholder: 'user/book/report...' }],
    key: 'logs',
    title: '系统日志',
  },
};
