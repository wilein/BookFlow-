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
  { label: "不绑定", value: "none" },
  { label: "绑定书籍", value: "book" },
  { label: "绑定路径节点", value: "pathNode" }
];
const TEXTS = {
  title: "添加资源",
  basic: "基本信息",
  name: "资源名称",
  namePlaceholder: "例如：第三章复习课件",
  description: "资源说明",
  descPlaceholder: "说明资源适合哪本书或哪个路径节点",
  resourceType: "资源类型",
  visibility: "可见性",
  file: "资源文件",
  chooseFile: "选择文件",
  fileReady: "已选择文件",
  fileHint: "支持文档、图片、视频，也可在下方直接填写链接",
  fileUrl: "文件链接",
  fileUrlPlaceholder: "上传后自动填写，也可粘贴外部链接",
  binding: "绑定对象",
  bindType: "绑定类型",
  bindTarget: "绑定到",
  selectBook: "选择一本我的书籍",
  selectNode: "选择一个路径节点",
  noBook: "你的书架暂无可绑定书籍",
  noNode: "你的路径暂无可绑定节点",
  fillName: "请填写资源名称",
  fillFile: "请选择文件或填写文件链接",
  fillTarget: "请选择绑定对象",
  submit: "保存资源",
  submitting: "保存中...",
  saved: "资源已保存",
  uploading: "上传中...",
  uploadUnsupported: "当前环境不支持选择文件"
};
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
function extractExtension(name = "") {
  const clean = String(name).split("?")[0].split("#")[0];
  const index = clean.lastIndexOf(".");
  return index >= 0 ? clean.slice(index + 1).toUpperCase() : "";
}
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
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      typeOptions: TYPE_OPTIONS,
      visibilityOptions: VISIBILITY_OPTIONS,
      bindOptions: BIND_OPTIONS,
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      submitting: false,
      books: [],
      pathNodes: [],
      form: {
        title: "",
        description: "",
        type: 1,
        visibility: 1,
        bindType: "none",
        bindId: "",
        bookId: "",
        fileUrl: "",
        fileFormat: "",
        fileSize: 0
      }
    };
  },
  computed: {
    typeIndex() {
      return Math.max(0, TYPE_OPTIONS.findIndex((item) => Number(item.value) === Number(this.form.type)));
    },
    selectedTypeLabel() {
      var _a;
      return ((_a = TYPE_OPTIONS[this.typeIndex]) == null ? void 0 : _a.label) || TYPE_OPTIONS[0].label;
    },
    visibilityIndex() {
      return Math.max(0, VISIBILITY_OPTIONS.findIndex((item) => Number(item.value) === Number(this.form.visibility)));
    },
    selectedVisibilityLabel() {
      var _a;
      return ((_a = VISIBILITY_OPTIONS[this.visibilityIndex]) == null ? void 0 : _a.label) || VISIBILITY_OPTIONS[0].label;
    },
    bindIndex() {
      return Math.max(0, BIND_OPTIONS.findIndex((item) => item.value === this.form.bindType));
    },
    selectedBindLabel() {
      var _a;
      return ((_a = BIND_OPTIONS[this.bindIndex]) == null ? void 0 : _a.label) || BIND_OPTIONS[0].label;
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
    if (options.bookId) {
      this.form.bindType = "book";
      this.form.bindId = normalizeId(options.bookId);
      this.form.bookId = normalizeId(options.bookId);
    }
    this.loadBindTargets();
  },
  methods: {
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
        common_vendor.index.__f__("error", "at pages/resources/create.vue:305", "load bind targets failed", error);
      }
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
      if (typeof common_vendor.index.chooseImage === "function") {
        common_vendor.index.chooseImage({
          count: 1,
          success: (res) => handleFile({ tempFilePath: (res.tempFilePaths || [])[0], size: 0, name: "image" })
        });
        return;
      }
      common_vendor.index.showToast({ title: TEXTS.uploadUnsupported, icon: "none" });
    },
    async submitResource() {
      if (this.submitting)
        return;
      if (!this.form.title.trim()) {
        common_vendor.index.showToast({ title: TEXTS.fillName, icon: "none" });
        return;
      }
      if (!this.form.fileUrl.trim()) {
        common_vendor.index.showToast({ title: TEXTS.fillFile, icon: "none" });
        return;
      }
      if (this.form.bindType !== "none" && !this.form.bindId) {
        common_vendor.index.showToast({ title: TEXTS.fillTarget, icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        await utils_api_resource.createResource({
          ...this.form,
          title: this.form.title.trim(),
          description: this.form.description.trim(),
          fileUrl: normalizeFileUrl(this.form.fileUrl),
          fileFormat: this.form.fileFormat || extractExtension(this.form.fileUrl),
          bindId: this.form.bindType === "none" ? null : this.form.bindId,
          bookId: this.form.bookId || null
        });
        common_vendor.index.showToast({ title: TEXTS.saved, icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack({ fail: () => common_vendor.index.navigateTo({ url: "/pages/my/resources" }) }), 500);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/resources/create.vue:392", "createResource failed", error);
      } finally {
        this.submitting = false;
      }
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
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerRightSafe + "px",
    g: $data.headerHeight + "px",
    h: common_vendor.t($data.texts.basic),
    i: common_vendor.t($data.texts.name),
    j: $data.texts.namePlaceholder,
    k: $data.form.title,
    l: common_vendor.o(($event) => $data.form.title = $event.detail.value),
    m: common_vendor.t($data.texts.description),
    n: $data.texts.descPlaceholder,
    o: $data.form.description,
    p: common_vendor.o(($event) => $data.form.description = $event.detail.value),
    q: common_vendor.t($data.texts.resourceType),
    r: common_vendor.t($options.selectedTypeLabel),
    s: $data.typeOptions,
    t: $options.typeIndex,
    v: common_vendor.o((...args) => $options.onTypeChange && $options.onTypeChange(...args)),
    w: common_vendor.t($data.texts.visibility),
    x: common_vendor.t($options.selectedVisibilityLabel),
    y: $data.visibilityOptions,
    z: $options.visibilityIndex,
    A: common_vendor.o((...args) => $options.onVisibilityChange && $options.onVisibilityChange(...args)),
    B: common_vendor.t($data.texts.file),
    C: common_vendor.t($data.form.fileUrl ? $data.texts.fileReady : $data.texts.chooseFile),
    D: common_vendor.t($data.form.fileUrl || $data.texts.fileHint),
    E: common_vendor.o((...args) => $options.chooseFile && $options.chooseFile(...args)),
    F: common_vendor.t($data.texts.fileUrl),
    G: $data.texts.fileUrlPlaceholder,
    H: $data.form.fileUrl,
    I: common_vendor.o(($event) => $data.form.fileUrl = $event.detail.value),
    J: common_vendor.t($data.texts.binding),
    K: common_vendor.t($data.texts.bindType),
    L: common_vendor.t($options.selectedBindLabel),
    M: $data.bindOptions,
    N: $options.bindIndex,
    O: common_vendor.o((...args) => $options.onBindTypeChange && $options.onBindTypeChange(...args)),
    P: $data.form.bindType !== "none"
  }, $data.form.bindType !== "none" ? {
    Q: common_vendor.t($data.texts.bindTarget),
    R: common_vendor.t($options.selectedTargetLabel),
    S: !$data.form.bindId ? 1 : "",
    T: $options.targetOptions,
    U: $options.targetIndex,
    V: !$options.targetOptions.length,
    W: common_vendor.o((...args) => $options.onTargetChange && $options.onTargetChange(...args)),
    X: common_vendor.t($options.targetTip)
  } : {}, {
    Y: common_vendor.t($data.submitting ? $data.texts.submitting : $data.texts.submit),
    Z: $data.submitting ? 1 : "",
    aa: common_vendor.o((...args) => $options.submitResource && $options.submitResource(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-06b7583b"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/resources/create.js.map
