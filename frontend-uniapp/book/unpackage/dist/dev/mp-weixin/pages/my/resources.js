"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_resource = require("../../utils/api/resource.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const TEXTS = {
  title: "我的资源",
  summaryTitle: "按绑定类型整理",
  grouped: "资源视图",
  resourceUnit: "个资源",
  searchPlaceholder: "搜索资源名称、绑定对象或说明",
  resource: "资源",
  emptyDesc: "暂无资源描述",
  file: "文件",
  unknownSize: "未知大小",
  public: "公开",
  empty: "暂无上传资源",
  emptySub: "你上传的课件、PDF、视频或图片会按绑定对象自动整理到这里",
  noResult: "没有找到相关资源",
  noResultSub: "换一个关键词或分类试试",
  add: "添加"
};
function normalizeBindType(value) {
  const text = String(value || "none");
  return ["book", "pathNode", "none"].includes(text) ? text : "none";
}
function bindTypeLabel(value) {
  const type = normalizeBindType(value);
  if (type === "book")
    return "关联书籍";
  if (type === "pathNode")
    return "关联路径节点";
  return "未绑定";
}
function normalizeItem(item) {
  const bindType = normalizeBindType(item.bindType);
  return {
    ...item,
    bindType,
    bindTypeLabel: item.bindTypeLabel || bindTypeLabel(bindType),
    title: item.title || item.name || "未命名资源",
    bindingSummary: item.bindingSummary || buildBindingSummary(item),
    previewType: item.previewType || inferPreviewType(item)
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
function buildBindingSummary(item) {
  const label = item.bindTypeLabel || bindTypeLabel(item.bindType);
  const id = item.bindId || (item.bindType === "book" ? item.bookId : "");
  const target = item.bindTargetTitle || item.bookTitle || "";
  const idText = id ? ` #${id}` : "";
  return target ? `${label}${idText} · ${target}` : `${label}${idText}`;
}
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      keyword: "",
      activeTab: "all",
      items: []
    };
  },
  computed: {
    tabs() {
      return [
        { key: "all", label: "全部", count: this.items.length },
        { key: "book", label: "关联书籍", count: this.countByBindType("book") },
        { key: "pathNode", label: "路径节点", count: this.countByBindType("pathNode") },
        { key: "none", label: "未绑定", count: this.countByBindType("none") }
      ];
    },
    currentTabLabel() {
      var _a;
      return ((_a = this.tabs.find((item) => item.key === this.activeTab)) == null ? void 0 : _a.label) || "全部";
    },
    filteredItems() {
      const keyword = this.keyword.trim().toLowerCase();
      return this.items.filter((item) => {
        if (this.activeTab !== "all" && item.bindType !== this.activeTab)
          return false;
        if (!keyword)
          return true;
        const target = [
          item.title,
          item.description,
          item.typeLabel,
          item.bindTypeLabel,
          item.bindingSummary,
          item.bindTargetTitle,
          item.fileFormat
        ].join(" ").toLowerCase();
        return target.includes(keyword);
      });
    },
    emptyTitle() {
      return this.items.length ? TEXTS.noResult : TEXTS.empty;
    },
    emptySub() {
      return this.items.length ? TEXTS.noResultSub : TEXTS.emptySub;
    }
  },
  onLoad() {
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
  },
  onShow() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        const data = await utils_api_resource.getMyResources();
        this.items = Array.isArray(data) ? data.map(normalizeItem) : [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/resources.vue:206", "getMyResources failed", error);
      }
    },
    countByBindType(type) {
      return this.items.filter((item) => item.bindType === type).length;
    },
    previewIcon(item) {
      if (item.previewType === "image")
        return "图";
      if (item.previewType === "video")
        return "视";
      if (item.previewType === "document")
        return "文";
      return item.typeLabel ? item.typeLabel.slice(0, 1) : "资";
    },
    buildBindingSummary,
    openResource(item) {
      common_vendor.index.navigateTo({ url: `/pages/resources/detail?id=${encodeURIComponent(item.id)}` });
    },
    goCreate() {
      common_vendor.index.navigateTo({ url: "/pages/resources/create" });
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/my/my" }) });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.texts.title),
    d: common_vendor.t($data.texts.add),
    e: common_vendor.o((...args) => $options.goCreate && $options.goCreate(...args)),
    f: $data.headerHeight + "px",
    g: $data.statusBarHeight + "px",
    h: $data.headerRightSafe + "px",
    i: $data.headerHeight + "px",
    j: common_vendor.t($data.texts.summaryTitle),
    k: common_vendor.t($data.items.length),
    l: common_vendor.t($data.texts.resourceUnit),
    m: common_vendor.t($options.currentTabLabel),
    n: common_vendor.t($data.texts.grouped),
    o: $data.texts.searchPlaceholder,
    p: $data.keyword,
    q: common_vendor.o(($event) => $data.keyword = $event.detail.value),
    r: $data.keyword
  }, $data.keyword ? {
    s: common_vendor.o(($event) => $data.keyword = "")
  } : {}, {
    t: common_vendor.f($options.tabs, (tab, k0, i0) => {
      return {
        a: common_vendor.t(tab.label),
        b: common_vendor.t(tab.count),
        c: tab.key,
        d: $data.activeTab === tab.key ? 1 : "",
        e: common_vendor.o(($event) => $data.activeTab = tab.key, tab.key)
      };
    }),
    v: $options.filteredItems.length
  }, $options.filteredItems.length ? {
    w: common_vendor.f($options.filteredItems, (item, k0, i0) => {
      return {
        a: common_vendor.t($options.previewIcon(item)),
        b: common_vendor.n(item.bindType),
        c: common_vendor.t(item.title || item.name),
        d: common_vendor.t(item.typeLabel || $data.texts.resource),
        e: common_vendor.t(item.bindingSummary || $options.buildBindingSummary(item)),
        f: common_vendor.t(item.description || $data.texts.emptyDesc),
        g: common_vendor.t(item.fileFormat || item.previewType || $data.texts.file),
        h: common_vendor.t(item.fileSizeLabel || $data.texts.unknownSize),
        i: common_vendor.t(item.visibilityLabel || $data.texts.public),
        j: item.id,
        k: common_vendor.o(($event) => $options.openResource(item), item.id)
      };
    })
  } : {
    x: common_vendor.t($options.emptyTitle),
    y: common_vendor.t($options.emptySub)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-d33b89aa"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/resources.js.map
