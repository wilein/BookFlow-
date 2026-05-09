"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_resource = require("../../utils/api/resource.js");
const utils_auth = require("../../utils/auth.js");
const utils_config = require("../../utils/config.js");
const utils_api_path = require("../../utils/api/path.js");
const utils_api_user = require("../../utils/api/user.js");
const common_assets = require("../../common/assets.js");
const TYPE_OPTIONS = [
  { label: "课件", value: 1 },
  { label: "习题", value: 2 },
  { label: "笔记", value: 3 },
  { label: "拓展阅读", value: 4 },
  { label: "其他", value: 5 }
];
const VISIBILITY_OPTIONS = [
  { label: "公开", value: 1 },
  { label: "仅买家可见", value: 2 },
  { label: "私密", value: 3 }
];
const BIND_OPTIONS = [
  { label: "未绑定", value: "none" },
  { label: "关联书籍", value: "book" },
  { label: "关联路径节点", value: "pathNode" }
];
const TEXTS = {
  title: "资源详情",
  edit: "编辑",
  cancel: "取消",
  unnamed: "未命名资源",
  resource: "资源",
  unbound: "未绑定",
  preview: "内容预览",
  unknownSize: "未知大小",
  open: "打开",
  openDocument: "打开文档",
  openVideo: "播放视频",
  openImage: "查看图片",
  copy: "复制链接",
  info: "资源信息",
  description: "说明",
  visibility: "可见性",
  bindType: "绑定类型",
  bindId: "绑定ID",
  bindTarget: "绑定对象",
  selectBook: "选择一本我的书籍",
  selectNode: "选择一个路径节点",
  noBook: "你的书架暂无可绑定书籍",
  noNode: "你的路径暂无可绑定节点",
  public: "公开",
  emptyDesc: "暂无资源说明",
  editResource: "编辑资源",
  name: "名称",
  namePlaceholder: "请输入资源名称",
  descPlaceholder: "说明这个资源适合怎么使用",
  resourceType: "资源类型",
  bookId: "书籍ID",
  bindIdPlaceholder: "书籍ID或节点ID",
  bookIdPlaceholder: "资源所属书籍ID",
  fileUrl: "文件链接",
  fileUrlPlaceholder: "上传后自动填写，也可手动粘贴",
  replaceFile: "替换文件",
  save: "保存修改",
  filePending: "资源文件待补充",
  copied: "资源链接已复制",
  saved: "已保存",
  uploadUnsupported: "当前环境不支持选择文件",
  uploading: "上传中",
  opening: "打开中",
  openFailed: "打开失败，链接已复制",
  imageDesc: "图片资源可以直接预览，点击图片可放大查看。",
  videoDesc: "视频资源可以直接播放，也可以复制链接到浏览器打开。",
  documentDesc: "PDF、Word、PPT、Excel 等文档会下载后打开。",
  linkDesc: "该资源是外部链接或普通文件，可复制链接后查看。"
};
function normalizeId(value) {
  const text = String(value == null ? "" : value).trim();
  return /^\d+$/.test(text) ? text : "";
}
function dedupeBooks(list) {
  const seen = /* @__PURE__ */ new Set();
  return (Array.isArray(list) ? list : []).filter((book) => {
    const id = normalizeId(book.id || book.bookId);
    if (!id || seen.has(id))
      return false;
    seen.add(id);
    return true;
  });
}
function normalizeFileUrl(value) {
  const text = String(value || "").trim();
  if (!text)
    return "";
  const lower = text.toLowerCase();
  if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("wxfile://") || lower.startsWith("cloud://") || lower.startsWith("data:")) {
    return text;
  }
  const baseUrl = String(utils_config.API_BASE_URL || "").replace(/\/+$/, "");
  if (!baseUrl)
    return text;
  return text.startsWith("/") ? `${baseUrl}${text}` : `${baseUrl}/${text}`;
}
function normalizeResource(item = {}) {
  const fileUrl = normalizeFileUrl(item.fileUrl || item.rawFileUrl || "");
  return {
    id: item.id || "",
    title: item.title || item.name || "",
    description: item.description || "",
    type: Number(item.type || 5),
    typeLabel: item.typeLabel || "",
    fileUrl,
    fileFormat: item.fileFormat || extractExtension(fileUrl),
    fileSize: Number(item.fileSize || 0),
    fileSizeLabel: item.fileSizeLabel || "",
    previewType: item.previewType || inferPreviewType(item),
    visibility: Number(item.visibility || 1),
    visibilityLabel: item.visibilityLabel || "",
    bindType: item.bindType || "none",
    bindTypeLabel: item.bindTypeLabel || "",
    bindId: item.bindId || "",
    bookId: item.bookId || "",
    bindTargetTitle: item.bindTargetTitle || "",
    bindingSummary: item.bindingSummary || "",
    canEdit: Boolean(item.canEdit)
  };
}
function inferPreviewType(item) {
  const text = `${item.fileFormat || ""} ${item.fileUrl || ""}`.toLowerCase();
  if (/(jpg|jpeg|png|gif|webp|bmp)/.test(text))
    return "image";
  if (/(mp4|mov|m4v|webm|avi)/.test(text))
    return "video";
  if (/(pdf|doc|docx|ppt|pptx|xls|xlsx)/.test(text))
    return "document";
  if (/^https?:\/\//.test(item.fileUrl || ""))
    return "link";
  return "file";
}
function extractExtension(name = "") {
  const clean = String(name).split("?")[0].split("#")[0];
  const index = clean.lastIndexOf(".");
  return index >= 0 ? clean.slice(index + 1).toUpperCase() : "";
}
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      resourceId: "",
      resource: normalizeResource(),
      editMode: false,
      form: {},
      books: [],
      pathNodes: [],
      typeOptions: TYPE_OPTIONS,
      visibilityOptions: VISIBILITY_OPTIONS,
      bindOptions: BIND_OPTIONS
    };
  },
  computed: {
    previewIcon() {
      if (this.resource.previewType === "image")
        return "图";
      if (this.resource.previewType === "video")
        return "视";
      if (this.resource.previewType === "document")
        return "文";
      return "链";
    },
    previewLabel() {
      if (this.resource.previewType === "image")
        return "图片";
      if (this.resource.previewType === "video")
        return "视频";
      if (this.resource.previewType === "document")
        return "文档";
      if (this.resource.previewType === "link")
        return "链接";
      return "文件";
    },
    previewDescription() {
      if (this.resource.previewType === "image")
        return TEXTS.imageDesc;
      if (this.resource.previewType === "video")
        return TEXTS.videoDesc;
      if (this.resource.previewType === "document")
        return TEXTS.documentDesc;
      return TEXTS.linkDesc;
    },
    primaryActionText() {
      if (this.resource.previewType === "image")
        return TEXTS.openImage;
      if (this.resource.previewType === "video")
        return TEXTS.openVideo;
      if (this.resource.previewType === "document")
        return TEXTS.openDocument;
      return TEXTS.open;
    },
    typeIndex() {
      return Math.max(0, TYPE_OPTIONS.findIndex((item) => Number(item.value) === Number(this.form.type)));
    },
    selectedTypeLabel() {
      var _a;
      return ((_a = TYPE_OPTIONS[this.typeIndex]) == null ? void 0 : _a.label) || "其他";
    },
    visibilityIndex() {
      return Math.max(0, VISIBILITY_OPTIONS.findIndex((item) => Number(item.value) === Number(this.form.visibility)));
    },
    selectedVisibilityLabel() {
      var _a;
      return ((_a = VISIBILITY_OPTIONS[this.visibilityIndex]) == null ? void 0 : _a.label) || "公开";
    },
    bindIndex() {
      return Math.max(0, BIND_OPTIONS.findIndex((item) => item.value === this.form.bindType));
    },
    selectedBindLabel() {
      var _a;
      return ((_a = BIND_OPTIONS[this.bindIndex]) == null ? void 0 : _a.label) || "未绑定";
    },
    targetOptions() {
      if (this.form.bindType === "book") {
        return this.books.map((book) => ({
          label: book.title || book.name || `Book #${book.id}`,
          value: normalizeId(book.id || book.bookId),
          bookId: normalizeId(book.id || book.bookId)
        }));
      }
      if (this.form.bindType === "pathNode") {
        return this.pathNodes;
      }
      return [];
    },
    targetIndex() {
      const index = this.targetOptions.findIndex((item) => String(item.value) === String(this.form.bindId));
      return index >= 0 ? index : 0;
    },
    selectedTargetLabel() {
      const selected = this.targetOptions[this.targetIndex];
      if (this.form.bindId && selected)
        return selected.label;
      if (this.form.bindType === "book")
        return TEXTS.selectBook;
      if (this.form.bindType === "pathNode")
        return TEXTS.selectNode;
      return "";
    },
    targetTip() {
      if (this.form.bindType === "book" && !this.targetOptions.length)
        return TEXTS.noBook;
      if (this.form.bindType === "pathNode" && !this.targetOptions.length)
        return TEXTS.noNode;
      return "";
    }
  },
  onLoad(options = {}) {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 48;
    }
    this.resourceId = normalizeId(options.id);
    this.loadBindTargets();
    this.fetchDetail();
  },
  methods: {
    async fetchDetail() {
      if (!this.resourceId)
        return;
      try {
        const data = await utils_api_resource.getResourceDetail(this.resourceId);
        this.resource = normalizeResource(data);
        this.resetForm();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/resources/detail.vue:429", "getResourceDetail failed", error);
      }
    },
    async loadBindTargets() {
      try {
        const [selling, sold, paths] = await Promise.all([
          utils_api_user.getMyBookshelf("selling").catch(() => []),
          utils_api_user.getMyBookshelf("sold").catch(() => []),
          utils_api_user.getMyPaths().catch(() => [])
        ]);
        this.books = dedupeBooks([...selling || [], ...sold || []]);
        const pathList = Array.isArray(paths) ? paths.slice(0, 50) : [];
        const details = await Promise.all(
          pathList.map((path) => utils_api_path.getPathDetail(path.id).catch(() => ({ ...path, nodes: [] })))
        );
        this.pathNodes = details.flatMap((path, pathIndex) => {
          var _a;
          const pathTitle = path.title || path.name || ((_a = pathList[pathIndex]) == null ? void 0 : _a.title) || TEXTS.bindTarget;
          return (Array.isArray(path.nodes) ? path.nodes : []).filter((node) => normalizeId(node.id)).map((node) => {
            var _a2, _b;
            return {
              label: `${pathTitle} / ${node.title || `Node #${node.id}`}`,
              value: normalizeId(node.id),
              pathId: normalizeId(path.id || ((_a2 = pathList[pathIndex]) == null ? void 0 : _a2.id)),
              bookId: normalizeId(path.bookId || ((_b = pathList[pathIndex]) == null ? void 0 : _b.bookId))
            };
          });
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/resources/detail.vue:457", "load bind targets failed", error);
      }
    },
    resetForm() {
      this.form = {
        id: this.resource.id,
        title: this.resource.title,
        description: this.resource.description,
        type: this.resource.type,
        visibility: this.resource.visibility,
        bindType: this.resource.bindType || "none",
        bindId: this.resource.bindId || "",
        bookId: this.resource.bookId || "",
        fileUrl: this.resource.fileUrl,
        fileFormat: this.resource.fileFormat,
        fileSize: this.resource.fileSize
      };
    },
    toggleEdit() {
      this.editMode = !this.editMode;
      if (this.editMode)
        this.resetForm();
    },
    onTypeChange(event) {
      var _a;
      this.form.type = ((_a = TYPE_OPTIONS[Number(event.detail.value)]) == null ? void 0 : _a.value) || 5;
    },
    onVisibilityChange(event) {
      var _a;
      this.form.visibility = ((_a = VISIBILITY_OPTIONS[Number(event.detail.value)]) == null ? void 0 : _a.value) || 1;
    },
    onBindTypeChange(event) {
      var _a;
      this.form.bindType = ((_a = BIND_OPTIONS[Number(event.detail.value)]) == null ? void 0 : _a.value) || "none";
      this.form.bindId = "";
      this.form.bookId = "";
    },
    onTargetChange(event) {
      const option = this.targetOptions[Number(event.detail.value)];
      if (!option)
        return;
      this.form.bindId = option.value;
      this.form.bookId = this.form.bindType === "book" ? option.bookId : option.bookId || "";
    },
    chooseFile() {
      const handleFile = async (file) => {
        const path = file.path || file.tempFilePath;
        if (!path)
          return;
        common_vendor.index.showLoading({ title: TEXTS.uploading });
        try {
          const data = await utils_api_resource.uploadResourceFile(path);
          this.form.fileUrl = normalizeFileUrl(data.url || this.form.fileUrl);
          this.form.fileFormat = extractExtension(file.name || data.fileName || this.form.fileUrl);
          this.form.fileSize = Number(file.size || this.form.fileSize || 0);
        } finally {
          common_vendor.index.hideLoading();
        }
      };
      if (typeof common_vendor.index.chooseMessageFile === "function") {
        common_vendor.index.chooseMessageFile({
          count: 1,
          type: "all",
          success: (res) => handleFile((res.tempFiles || [])[0] || {})
        });
        return;
      }
      if (typeof common_vendor.index.chooseFile === "function") {
        common_vendor.index.chooseFile({
          count: 1,
          success: (res) => handleFile((res.tempFiles || [])[0] || {})
        });
        return;
      }
      common_vendor.index.showToast({ title: TEXTS.uploadUnsupported, icon: "none" });
    },
    async saveResource() {
      if (!this.form.title || !this.form.title.trim()) {
        common_vendor.index.showToast({ title: "请填写资源名称", icon: "none" });
        return;
      }
      if (this.form.bindType !== "none" && !this.form.bindId) {
        common_vendor.index.showToast({ title: "请选择绑定对象", icon: "none" });
        return;
      }
      try {
        const bindType = this.form.bindType || "none";
        await utils_api_resource.updateResource({
          ...this.form,
          bindType,
          fileUrl: normalizeFileUrl(this.form.fileUrl),
          fileFormat: this.form.fileFormat || extractExtension(this.form.fileUrl),
          bindId: this.form.bindId || (bindType === "book" ? this.form.bookId : null) || null,
          bookId: this.form.bookId || null
        });
        common_vendor.index.showToast({ title: TEXTS.saved, icon: "none" });
        this.editMode = false;
        await this.fetchDetail();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/resources/detail.vue:551", "updateResource failed", error);
      }
    },
    previewImage() {
      if (!this.resource.fileUrl)
        return;
      common_vendor.index.previewImage({ urls: [this.resource.fileUrl], current: this.resource.fileUrl });
    },
    openResource() {
      const url = this.resource.fileUrl;
      if (!url) {
        common_vendor.index.showToast({ title: TEXTS.filePending, icon: "none" });
        return;
      }
      if (this.resource.previewType === "image") {
        this.previewImage();
        return;
      }
      if (this.resource.previewType === "video") {
        const videoContext = common_vendor.index.createVideoContext("resourceVideo", this);
        if (videoContext && typeof videoContext.play === "function") {
          videoContext.play();
        }
        return;
      }
      if (this.resource.previewType !== "document") {
        this.copyUrl();
        return;
      }
      common_vendor.index.showLoading({ title: TEXTS.opening });
      common_vendor.index.downloadFile({
        url,
        success: (res) => {
          if (res.statusCode !== 200 || !res.tempFilePath) {
            this.copyUrl(TEXTS.openFailed);
            return;
          }
          common_vendor.index.openDocument({
            filePath: res.tempFilePath,
            showMenu: true,
            fail: () => this.copyUrl(TEXTS.openFailed)
          });
        },
        fail: () => this.copyUrl(TEXTS.openFailed),
        complete: () => common_vendor.index.hideLoading()
      });
    },
    copyUrl(title = TEXTS.copied) {
      if (!this.resource.fileUrl) {
        common_vendor.index.showToast({ title: TEXTS.filePending, icon: "none" });
        return;
      }
      common_vendor.index.setClipboardData({
        data: this.resource.fileUrl,
        success: () => common_vendor.index.showToast({ title, icon: "none" })
      });
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.navigateTo({ url: "/pages/my/resources" }) });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.texts.title),
    d: $data.resource.canEdit
  }, $data.resource.canEdit ? {
    e: common_vendor.t($data.editMode ? $data.texts.cancel : $data.texts.edit),
    f: common_vendor.o((...args) => $options.toggleEdit && $options.toggleEdit(...args))
  } : {}, {
    g: $data.headerHeight + "px",
    h: $data.statusBarHeight + "px",
    i: $data.headerRightSafe + "px",
    j: $data.headerHeight + "px",
    k: common_vendor.t($options.previewIcon),
    l: common_vendor.n($data.resource.bindType),
    m: common_vendor.t($data.resource.title || $data.texts.unnamed),
    n: common_vendor.t($data.resource.typeLabel || $data.texts.resource),
    o: common_vendor.t($data.resource.fileFormat || $options.previewLabel),
    p: common_vendor.t($data.resource.bindingSummary || $data.texts.unbound),
    q: common_vendor.t($data.texts.preview),
    r: common_vendor.t($data.resource.fileSizeLabel || $data.texts.unknownSize),
    s: $data.resource.previewType === "image" && $data.resource.fileUrl
  }, $data.resource.previewType === "image" && $data.resource.fileUrl ? {
    t: $data.resource.fileUrl,
    v: common_vendor.o((...args) => $options.previewImage && $options.previewImage(...args))
  } : $data.resource.previewType === "video" && $data.resource.fileUrl ? {
    x: $data.resource.fileUrl
  } : {
    y: common_vendor.t($options.previewIcon),
    z: common_vendor.t($data.resource.fileFormat || $options.previewLabel),
    A: common_vendor.t($options.previewDescription)
  }, {
    w: $data.resource.previewType === "video" && $data.resource.fileUrl,
    B: common_vendor.t($options.primaryActionText),
    C: common_vendor.o((...args) => $options.openResource && $options.openResource(...args)),
    D: common_vendor.t($data.texts.copy),
    E: common_vendor.o((...args) => $options.copyUrl && $options.copyUrl(...args)),
    F: common_vendor.t($data.texts.info),
    G: common_vendor.t($data.texts.description),
    H: common_vendor.t($data.resource.description || $data.texts.emptyDesc),
    I: common_vendor.t($data.texts.visibility),
    J: common_vendor.t($data.resource.visibilityLabel || $data.texts.public),
    K: common_vendor.t($data.texts.bindType),
    L: common_vendor.t($data.resource.bindTypeLabel || $data.texts.unbound),
    M: common_vendor.t($data.texts.bindId),
    N: common_vendor.t($data.resource.bindId || $data.resource.bookId || "-"),
    O: common_vendor.t($data.texts.bindTarget),
    P: common_vendor.t($data.resource.bindTargetTitle || "-"),
    Q: $data.editMode
  }, $data.editMode ? common_vendor.e({
    R: common_vendor.t($data.texts.editResource),
    S: common_vendor.t($data.texts.name),
    T: $data.texts.namePlaceholder,
    U: $data.form.title,
    V: common_vendor.o(($event) => $data.form.title = $event.detail.value),
    W: common_vendor.t($data.texts.description),
    X: $data.texts.descPlaceholder,
    Y: $data.form.description,
    Z: common_vendor.o(($event) => $data.form.description = $event.detail.value),
    aa: common_vendor.t($data.texts.resourceType),
    ab: common_vendor.t($options.selectedTypeLabel),
    ac: $data.typeOptions,
    ad: $options.typeIndex,
    ae: common_vendor.o((...args) => $options.onTypeChange && $options.onTypeChange(...args)),
    af: common_vendor.t($data.texts.visibility),
    ag: common_vendor.t($options.selectedVisibilityLabel),
    ah: $data.visibilityOptions,
    ai: $options.visibilityIndex,
    aj: common_vendor.o((...args) => $options.onVisibilityChange && $options.onVisibilityChange(...args)),
    ak: common_vendor.t($data.texts.bindType),
    al: common_vendor.t($options.selectedBindLabel),
    am: $data.bindOptions,
    an: $options.bindIndex,
    ao: common_vendor.o((...args) => $options.onBindTypeChange && $options.onBindTypeChange(...args)),
    ap: $data.form.bindType !== "none"
  }, $data.form.bindType !== "none" ? {
    aq: common_vendor.t($data.texts.bindTarget),
    ar: common_vendor.t($options.selectedTargetLabel),
    as: !$data.form.bindId ? 1 : "",
    at: $options.targetOptions,
    av: $options.targetIndex,
    aw: !$options.targetOptions.length,
    ax: common_vendor.o((...args) => $options.onTargetChange && $options.onTargetChange(...args)),
    ay: common_vendor.t($options.targetTip)
  } : {}, {
    az: $data.form.bindType !== "none" && !$options.targetOptions.length
  }, $data.form.bindType !== "none" && !$options.targetOptions.length ? {
    aA: common_vendor.t($data.texts.bindId),
    aB: $data.texts.bindIdPlaceholder,
    aC: $data.form.bindId,
    aD: common_vendor.o(($event) => $data.form.bindId = $event.detail.value),
    aE: common_vendor.t($data.texts.bookId),
    aF: $data.texts.bookIdPlaceholder,
    aG: $data.form.bookId,
    aH: common_vendor.o(($event) => $data.form.bookId = $event.detail.value)
  } : {}, {
    aI: common_vendor.t($data.texts.fileUrl),
    aJ: $data.texts.fileUrlPlaceholder,
    aK: $data.form.fileUrl,
    aL: common_vendor.o(($event) => $data.form.fileUrl = $event.detail.value),
    aM: common_vendor.t($data.texts.replaceFile),
    aN: common_vendor.o((...args) => $options.chooseFile && $options.chooseFile(...args)),
    aO: common_vendor.t($data.texts.save),
    aP: common_vendor.o((...args) => $options.saveResource && $options.saveResource(...args))
  }) : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-3d7100db"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/resources/detail.js.map
