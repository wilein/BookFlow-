"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_annotation = require("../../utils/api/annotation.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
function normalizeType(type) {
  if (type === 1 || type === "1" || type === "highlight")
    return "highlight";
  if (type === 2 || type === "2" || type === "question")
    return "question";
  if (type === 3 || type === "3" || type === "insight")
    return "insight";
  return "highlight";
}
function normalizeAnnotationItem(item, index) {
  return {
    id: item.id || `annotation-${index + 1}`,
    page: Number(item.page || 1),
    type: normalizeType(item.type),
    content: item.content || "",
    positionText: item.positionText || "",
    imageUrl: item.imageUrl || "",
    nickname: item.nickname || item.creatorName || "书友",
    anonymous: Boolean(item.anonymous),
    createdAt: item.createdAt || item.createTime || "",
    likeCount: Number(item.likeCount || 0),
    commentCount: Number(item.commentCount || 0),
    liked: Boolean(item.liked)
  };
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      bookId: "",
      bookTitle: "",
      mineOnly: false,
      canAddAnnotation: false,
      selectedPage: 1,
      annotations: [],
      pageNavItems: [],
      textMap: {
        defaultTitle: "书籍批注",
        add: "添加批注",
        heroTitle: "批注笔记",
        heroSub: "条批注，按页查看",
        pagePrefix: "第 ",
        pageSuffix: " 页",
        countUnit: "条",
        empty: "当前页暂时无批注",
        positionPrefix: "位置：",
        anonymous: "匿名用户",
        viewImage: "点击查看图片",
        noImage: "此批注无图片展示"
      },
      typeMap: {
        highlight: { label: "重点", icon: "⭐" },
        question: { label: "疑问", icon: "❓" },
        insight: { label: "心得", icon: "💡" }
      }
    };
  },
  computed: {
    heroTitle() {
      return this.mineOnly ? "我的批注笔记" : this.textMap.heroTitle;
    },
    heroSubText() {
      return this.mineOnly ? "条我的批注，按页查看" : this.textMap.heroSub;
    },
    totalAnnotations() {
      return this.annotations.length;
    },
    currentPageAnnotations() {
      if (!this.selectedPage)
        return this.annotations;
      return this.annotations.filter((item) => item.page === this.selectedPage);
    }
  },
  async onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 54;
    }
    this.bookId = decodeURIComponent(options.bookId || "");
    this.bookTitle = decodeURIComponent(options.bookTitle || "");
    this.mineOnly = options.mineOnly === "1" || options.mineOnly === "true";
    await this.fetchAnnotations();
  },
  onShow() {
    this.fetchAnnotations();
  },
  methods: {
    async fetchAnnotations() {
      if (!this.bookId)
        return;
      try {
        const data = await utils_api_annotation.getAnnotationList(this.bookId, { mineOnly: this.mineOnly });
        this.bookTitle = data.bookTitle || this.bookTitle;
        this.canAddAnnotation = Boolean(data.canAdd);
        this.annotations = Array.isArray(data.annotations) ? data.annotations.map((item, index) => normalizeAnnotationItem(item, index)) : [];
        const pageItems = Array.isArray(data.pageNavItems) && data.pageNavItems.length ? data.pageNavItems : this.buildPageNavFromAnnotations();
        this.pageNavItems = [{ page: 0, count: this.annotations.length, all: true }, ...pageItems];
        this.selectedPage = 0;
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/annotations/list.vue:192", "fetchAnnotations failed", error);
      }
    },
    buildPageNavFromAnnotations() {
      const counter = {};
      this.annotations.forEach((item) => {
        counter[item.page] = (counter[item.page] || 0) + 1;
      });
      return Object.keys(counter).map((page) => ({ page: Number(page), count: counter[page] })).sort((a, b) => a.page - b.page);
    },
    goBack() {
      common_vendor.index.navigateBack({
        fail: () => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
        }
      });
    },
    goToCreate() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      common_vendor.index.navigateTo({
        url: `/pages/annotations/create?bookId=${encodeURIComponent(this.bookId)}&bookTitle=${encodeURIComponent(this.bookTitle)}&page=${encodeURIComponent(this.selectedPage || 1)}`
      });
    },
    async toggleLike(item) {
      try {
        const data = await utils_api_annotation.toggleAnnotationLike(item.id);
        item.liked = Boolean(data == null ? void 0 : data.liked);
        item.likeCount = Number((data == null ? void 0 : data.likeCount) || 0);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/annotations/list.vue:223", "toggleAnnotationLike failed", error);
      }
    },
    previewAnnotation(item) {
      if (!item.imageUrl) {
        common_vendor.index.showToast({ title: this.textMap.noImage, icon: "none" });
        return;
      }
      common_vendor.index.previewImage({
        urls: [item.imageUrl],
        current: item.imageUrl
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.bookTitle || $data.textMap.defaultTitle),
    d: $data.canAddAnnotation
  }, $data.canAddAnnotation ? {
    e: common_vendor.t($data.textMap.add),
    f: common_vendor.o((...args) => $options.goToCreate && $options.goToCreate(...args))
  } : {}, {
    g: $data.headerHeight + "px",
    h: $data.statusBarHeight + "px",
    i: $data.headerRightSafe + "px",
    j: $data.headerHeight + "px",
    k: common_vendor.t($options.heroTitle),
    l: common_vendor.t($options.totalAnnotations),
    m: common_vendor.t($options.heroSubText),
    n: common_vendor.f($data.pageNavItems, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.all ? "全部" : $data.textMap.pagePrefix + item.page + $data.textMap.pageSuffix),
        b: common_vendor.t(item.count),
        c: item.page,
        d: $data.selectedPage === item.page ? 1 : "",
        e: common_vendor.o(($event) => $data.selectedPage = item.page, item.page)
      };
    }),
    o: common_vendor.t($data.textMap.countUnit),
    p: $options.currentPageAnnotations.length === 0
  }, $options.currentPageAnnotations.length === 0 ? {
    q: common_vendor.t($data.textMap.empty)
  } : {
    r: common_vendor.f($options.currentPageAnnotations, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t($data.typeMap[item.type].icon),
        b: common_vendor.t($data.typeMap[item.type].label),
        c: common_vendor.n(item.type),
        d: common_vendor.t(item.createdAt),
        e: item.positionText
      }, item.positionText ? {
        f: common_vendor.t($data.textMap.positionPrefix),
        g: common_vendor.t(item.positionText)
      } : {}, {
        h: common_vendor.t(item.content),
        i: item.imageUrl
      }, item.imageUrl ? {
        j: item.imageUrl,
        k: common_vendor.o(($event) => $options.previewAnnotation(item), item.id)
      } : {}, {
        l: common_vendor.t(item.anonymous ? $data.textMap.anonymous : item.nickname),
        m: common_vendor.t(item.imageUrl ? $data.textMap.viewImage : $data.textMap.noImage),
        n: common_vendor.o(($event) => $options.previewAnnotation(item), item.id),
        o: common_vendor.t(item.liked ? "♥" : "♡"),
        p: common_vendor.t(item.likeCount),
        q: item.liked ? 1 : "",
        r: common_vendor.o(($event) => $options.toggleLike(item), item.id),
        s: item.id
      });
    })
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-30fceca5"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/annotations/list.js.map
