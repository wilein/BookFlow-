"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const DEFAULT_COVER = "/static/logo.png";
function normalizeText(value) {
  return String(value || "").trim();
}
function normalizePageNums(value, fallback) {
  const source = Array.isArray(value) ? value : fallback ? [fallback] : [];
  return Array.from(new Set(source.map((item) => Number(item)).filter((item) => item > 0))).sort((a, b) => a - b);
}
function toBookGroup(item, index) {
  const pageNums = normalizePageNums(item.pageNums, item.latestPageNum || item.pageNum);
  const visiblePages = pageNums.slice(0, 3);
  return {
    id: item.id || `book-${item.bookId || index}`,
    bookId: item.bookId || "",
    bookTitle: normalizeText(item.bookTitle) || "书籍已下架",
    cover: item.bookCover || item.cover || DEFAULT_COVER,
    category: normalizeText(item.category) || "未分类",
    annotationCount: Number(item.annotationCount || item.count || 0),
    pageCount: Number(item.pageCount || pageNums.length || 0),
    pageNums,
    visiblePages,
    morePageCount: Math.max(0, pageNums.length - visiblePages.length),
    latestContent: normalizeText(item.latestContent || item.content),
    latestTypeLabel: normalizeText(item.latestTypeLabel || item.typeLabel) || "批注",
    latestTime: normalizeText(item.latestTime || item.createTime)
  };
}
function groupFlatAnnotations(items) {
  const grouped = /* @__PURE__ */ new Map();
  items.forEach((item, index) => {
    const key = item.bookId || `unknown-${index}`;
    if (!grouped.has(key)) {
      grouped.set(key, {
        id: `book-${key}`,
        bookId: item.bookId || "",
        bookTitle: item.bookTitle || "书籍已下架",
        cover: item.bookCover || item.cover || DEFAULT_COVER,
        category: item.category || "未分类",
        annotationCount: 0,
        pageCount: 0,
        pageNums: [],
        latestContent: item.content || "",
        latestTypeLabel: item.typeLabel || "批注",
        latestTime: item.createTime || ""
      });
    }
    const group = grouped.get(key);
    group.annotationCount += 1;
    const page = Number(item.pageNum || item.latestPageNum || 0);
    if (page > 0 && !group.pageNums.includes(page)) {
      group.pageNums.push(page);
      group.pageCount = group.pageNums.length;
    }
  });
  return Array.from(grouped.values()).map(toBookGroup);
}
function normalizeGroups(items) {
  if (!Array.isArray(items) || !items.length)
    return [];
  const groupedShape = items.some((item) => item.annotationCount !== void 0 || item.latestContent !== void 0 || item.bookCover);
  return groupedShape ? items.map(toBookGroup) : groupFlatAnnotations(items);
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      searchKeyword: "",
      bookGroups: [],
      texts: {
        title: "我的批注",
        summaryTitle: "按书整理的批注",
        byBook: "书籍视图",
        annotationUnit: "条批注",
        bookUnit: "本书",
        pageUnit: "个页码",
        searchPlaceholder: "搜索书名、分类或批注内容",
        pagePrefix: "第",
        pageSuffix: "页",
        noContent: "暂无批注内容",
        open: "查看",
        empty: "暂无批注",
        emptySub: "在书籍详情里添加批注后，会按书籍自动归类到这里",
        noResult: "没有找到相关书籍",
        noResultSub: "换一个关键词试试"
      }
    };
  },
  computed: {
    totalAnnotations() {
      return this.bookGroups.reduce((sum, item) => sum + Number(item.annotationCount || 0), 0);
    },
    filteredBookGroups() {
      const keyword = this.searchKeyword.trim().toLowerCase();
      if (!keyword)
        return this.bookGroups;
      return this.bookGroups.filter((book) => {
        const target = [
          book.bookTitle,
          book.category,
          book.latestContent,
          book.pageNums.join(",")
        ].join(" ").toLowerCase();
        return target.includes(keyword);
      });
    },
    emptyTitle() {
      return this.bookGroups.length ? this.texts.noResult : this.texts.empty;
    },
    emptySub() {
      return this.bookGroups.length ? this.texts.noResultSub : this.texts.emptySub;
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
        const data = await utils_api_user.getMyAnnotations();
        this.bookGroups = normalizeGroups(data);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/annotations.vue:230", "getMyAnnotations failed", error);
      }
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/my/my" }) });
    },
    handleCoverError(book) {
      book.cover = DEFAULT_COVER;
    },
    goFindBook() {
      common_vendor.index.navigateTo({ url: "/pages/my/bookshelf?select=annotation" });
    },
    openBookAnnotations(book) {
      if (!book.bookId) {
        common_vendor.index.showToast({ title: "书籍不存在，无法查看批注", icon: "none" });
        return;
      }
      const params = [
        `bookId=${encodeURIComponent(book.bookId)}`,
        `bookTitle=${encodeURIComponent(book.bookTitle)}`,
        "mineOnly=1"
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/annotations/list?${params}` });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.texts.title),
    d: common_vendor.o((...args) => $options.goFindBook && $options.goFindBook(...args)),
    e: $data.headerHeight + "px",
    f: $data.statusBarHeight + "px",
    g: $data.headerRightSafe + "px",
    h: $data.headerHeight + "px",
    i: common_vendor.t($data.texts.summaryTitle),
    j: common_vendor.t($options.totalAnnotations),
    k: common_vendor.t($data.texts.annotationUnit),
    l: common_vendor.t($data.bookGroups.length),
    m: common_vendor.t($data.texts.bookUnit),
    n: common_vendor.t($data.texts.byBook),
    o: $data.texts.searchPlaceholder,
    p: $data.searchKeyword,
    q: common_vendor.o(($event) => $data.searchKeyword = $event.detail.value),
    r: $data.searchKeyword
  }, $data.searchKeyword ? {
    s: common_vendor.o(($event) => $data.searchKeyword = "")
  } : {}, {
    t: $options.filteredBookGroups.length
  }, $options.filteredBookGroups.length ? {
    v: common_vendor.f($options.filteredBookGroups, (book, k0, i0) => {
      return common_vendor.e({
        a: book.cover,
        b: common_vendor.o(($event) => $options.handleCoverError(book), book.id),
        c: common_vendor.t(book.bookTitle),
        d: common_vendor.t(book.category),
        e: common_vendor.t(book.annotationCount),
        f: common_vendor.t(book.pageCount),
        g: book.latestTime
      }, book.latestTime ? {
        h: common_vendor.t(book.latestTime)
      } : {}, {
        i: common_vendor.t(book.latestTypeLabel),
        j: common_vendor.t(book.latestContent || $data.texts.noContent),
        k: common_vendor.f(book.visiblePages, (page, k1, i1) => {
          return {
            a: common_vendor.t(page),
            b: page
          };
        }),
        l: book.morePageCount > 0
      }, book.morePageCount > 0 ? {
        m: common_vendor.t(book.morePageCount)
      } : {}, {
        n: book.id,
        o: common_vendor.o(($event) => $options.openBookAnnotations(book), book.id)
      });
    }),
    w: common_vendor.t($data.texts.annotationUnit),
    x: common_vendor.t($data.texts.pageUnit),
    y: common_vendor.t($data.texts.pagePrefix),
    z: common_vendor.t($data.texts.pageSuffix),
    A: common_vendor.t($data.texts.open)
  } : {
    B: common_vendor.t($options.emptyTitle),
    C: common_vendor.t($options.emptySub)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-d7b4f9ec"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/annotations.js.map
