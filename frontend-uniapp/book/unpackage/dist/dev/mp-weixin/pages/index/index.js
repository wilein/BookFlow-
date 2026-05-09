"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_common = require("../../utils/api/common.js");
const utils_api_book = require("../../utils/api/book.js");
const utils_api_path = require("../../utils/api/path.js");
const utils_auth = require("../../utils/auth.js");
const utils_bookDetail = require("../../utils/book-detail.js");
const common_assets = require("../../common/assets.js");
function toNumber(value, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}
function createKey(book, index) {
  if (book && book.id !== void 0 && book.id !== null && book.id !== "") {
    return String(book.id);
  }
  return `${(book == null ? void 0 : book.title) || "book"}-${(book == null ? void 0 : book.author) || "author"}-${index}`;
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      capsulePaddingRight: 0,
      fixedHeaderHeight: 0,
      bannerList: [],
      hotCategoryTabs: ["全部"],
      selectedHotCategory: "全部",
      hotBookMap: {
        "全部": []
      },
      currentPath: null,
      textMap: {
        appName: "薪传",
        slogan: "批注传承 · 学习路径 · 知识分享",
        hotBooks: "热门书籍",
        recommendPaths: "推荐学习路径",
        communityFeed: "社区动态",
        more: "更多 >",
        unknownCategory: "未分类",
        noHotBooks: "暂无热门书籍",
        noPaths: "暂无推荐路径",
        nodeUnit: "个节点",
        learnersUnit: "人在学"
      },
      navItems: [
        { name: "分类浏览", icon: "书", action: "category" },
        { name: "学习路径", icon: "路", action: "path" },
        { name: "发布书籍", icon: "发", action: "publish" },
        { name: "社区动态", icon: "动", action: "community" }
      ],
      studyPaths: [],
      dynamics: [
        { username: "王同学", time: "2小时前", content: "分享了一本带有详细批注的设计模式教材。" },
        { username: "赵同学", time: "5小时前", content: "刚完成数据结构学习路径，收获很大。" }
      ]
    };
  },
  computed: {
    displayHotBooks() {
      return this.hotBookMap[this.selectedHotCategory] || [];
    }
  },
  onLoad() {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const screenWidth = systemInfo.windowWidth || 375;
      this.capsulePaddingRight = screenWidth - capsule.left + 10;
      this.fixedHeaderHeight = capsule.top + capsule.height + 58;
    } else {
      this.capsulePaddingRight = 100;
      this.fixedHeaderHeight = this.statusBarHeight + 92;
    }
    this.fetchData();
  },
  onShow() {
    this.fetchCurrentPath();
  },
  methods: {
    async fetchData() {
      const [banners, grouped, books] = await Promise.all([
        utils_api_common.getBannerList().catch((error) => {
          common_vendor.index.__f__("error", "at pages/index/index.vue:215", "fetchBannerList failed", error);
          return [];
        }),
        utils_api_book.getBooksByCategory().catch((error) => {
          common_vendor.index.__f__("error", "at pages/index/index.vue:219", "fetchBooksByCategory failed", error);
          return {};
        }),
        utils_api_book.getBookList().catch((error) => {
          common_vendor.index.__f__("error", "at pages/index/index.vue:223", "fetchBookList failed", error);
          return [];
        })
      ]);
      this.bannerList = Array.isArray(banners) ? banners : [];
      const groupedBooks = grouped || {};
      const fallbackBooks = Array.isArray(books) ? books.map((book, index) => this.normalizeBook(book, index)) : [];
      this.buildHotBooks(groupedBooks, fallbackBooks);
      this.fetchPathData();
      this.fetchCurrentPath();
    },
    async fetchPathData() {
      try {
        const paths = await utils_api_path.getPathList({ category: "", keyword: "" });
        this.studyPaths = Array.isArray(paths) ? paths.slice(0, 4) : [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/index/index.vue:239", "fetchPathData failed", error);
      }
    },
    async fetchCurrentPath() {
      if (!utils_auth.hasValidSession()) {
        this.currentPath = null;
        return;
      }
      try {
        this.currentPath = await utils_api_path.getCurrentLearningPath() || null;
      } catch (error) {
        this.currentPath = null;
      }
    },
    normalizeBook(book, index, categoryName = "") {
      const key = createKey(book, index);
      return {
        ...book,
        id: book.id !== void 0 && book.id !== null && book.id !== "" ? String(book.id) : key,
        _uniqueKey: `${key}-${index}`,
        price: toNumber(book.price, 0),
        annotationCount: toNumber(book.annotationCount ?? book.annotations, 0),
        categoryName: book.categoryName || categoryName || book.category || this.textMap.unknownCategory
      };
    },
    buildHotBooks(grouped, fallbackBooks) {
      const groupedEntries = Object.keys(grouped || {}).map((name, categoryIndex) => {
        const books = Array.isArray(grouped[name]) ? grouped[name] : [];
        return {
          name,
          books: books.map((book, index) => this.normalizeBook(book, categoryIndex * 20 + index, name))
        };
      });
      const sortedEntries = groupedEntries.sort((a, b) => b.books.length - a.books.length);
      const topEntries = sortedEntries.slice(0, 3);
      const seen = /* @__PURE__ */ new Set();
      const allBooks = [];
      groupedEntries.forEach((entry) => {
        entry.books.forEach((book) => {
          const key = createKey(book, allBooks.length);
          if (seen.has(key))
            return;
          seen.add(key);
          allBooks.push(book);
        });
      });
      if (!allBooks.length && Array.isArray(fallbackBooks)) {
        fallbackBooks.forEach((book, index) => {
          const key = createKey(book, index);
          if (seen.has(key))
            return;
          seen.add(key);
          allBooks.push(book);
        });
      }
      const tabs = ["全部", ...topEntries.map((item) => item.name)];
      const bookMap = {
        "全部": allBooks.slice(0, 5)
      };
      topEntries.forEach((entry) => {
        bookMap[entry.name] = entry.books.slice(0, 5);
      });
      if (!bookMap["全部"].length && Array.isArray(fallbackBooks)) {
        bookMap["全部"] = fallbackBooks.slice(0, 5);
      }
      this.hotCategoryTabs = tabs.length ? tabs : ["全部"];
      this.hotBookMap = bookMap;
      this.selectedHotCategory = this.hotCategoryTabs.includes(this.selectedHotCategory) ? this.selectedHotCategory : "全部";
    },
    formatPrice(price) {
      return "￥" + utils_bookDetail.toPriceText(price);
    },
    getAnnotationText(book) {
      return `${book.annotationCount || 0}条批注`;
    },
    goToSearch() {
      common_vendor.index.navigateTo({ url: "/pages/search/search" });
    },
    goToCommunity() {
      common_vendor.index.switchTab({ url: "/pages/community/community" });
    },
    handleNav(item) {
      if (item.action === "category") {
        common_vendor.index.switchTab({ url: "/pages/category/category" });
        return;
      }
      if (item.action === "publish") {
        common_vendor.index.switchTab({ url: "/pages/publish/create" });
        return;
      }
      if (item.action === "community") {
        this.goToCommunity();
        return;
      }
      this.goToPathList();
    },
    goToBookDetail(book) {
      const query = utils_bookDetail.buildBookQueryFromListItem(book);
      common_vendor.index.navigateTo({ url: `/pages/books/detail?${query}` });
    },
    goToPathDetail(path) {
      const id = path.pathId || path.id;
      if (id) {
        common_vendor.index.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(id)}` });
        return;
      }
      common_vendor.index.navigateTo({
        url: `/pages/path/detail?title=${encodeURIComponent(path.name)}&creator=${encodeURIComponent(path.creator)}`
      });
    },
    goToPathList() {
      common_vendor.index.navigateTo({ url: "/pages/path/list" });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_2,
    b: common_vendor.t($data.textMap.appName),
    c: common_assets._imports_1,
    d: common_vendor.o((...args) => $options.goToSearch && $options.goToSearch(...args)),
    e: common_vendor.t($data.textMap.slogan),
    f: $data.statusBarHeight + "px",
    g: $data.capsulePaddingRight + "px",
    h: $data.fixedHeaderHeight + "px",
    i: $data.currentPath
  }, $data.currentPath ? {
    j: common_vendor.t($data.currentPath.progressPercent || 0),
    k: common_vendor.t($data.currentPath.title),
    l: common_vendor.t($data.currentPath.completedCount || 0),
    m: common_vendor.t($data.currentPath.nodeCount || 0),
    n: common_vendor.t($data.currentPath.lastLearnTime || "刚刚学习"),
    o: ($data.currentPath.progressPercent || 0) + "%",
    p: common_vendor.o(($event) => $options.goToPathDetail($data.currentPath))
  } : {}, {
    q: $data.bannerList.length > 0
  }, $data.bannerList.length > 0 ? {
    r: common_vendor.f($data.bannerList, (item, index, i0) => {
      return {
        a: item.image,
        b: index
      };
    })
  } : {}, {
    s: common_vendor.f($data.navItems, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.icon),
        b: common_vendor.t(item.name),
        c: item.name,
        d: common_vendor.o(($event) => $options.handleNav(item), item.name)
      };
    }),
    t: common_vendor.t($data.textMap.hotBooks),
    v: common_vendor.t($data.textMap.more),
    w: common_vendor.o((...args) => $options.goToSearch && $options.goToSearch(...args)),
    x: common_vendor.f($data.hotCategoryTabs, (tab, k0, i0) => {
      return {
        a: common_vendor.t(tab),
        b: tab,
        c: $data.selectedHotCategory === tab ? 1 : "",
        d: common_vendor.o(($event) => $data.selectedHotCategory = tab, tab)
      };
    }),
    y: $options.displayHotBooks.length > 0
  }, $options.displayHotBooks.length > 0 ? {
    z: common_vendor.f($options.displayHotBooks, (book, k0, i0) => {
      return {
        a: book.cover || "/static/cover_placeholder.png",
        b: common_vendor.t(book.title),
        c: common_vendor.t(book.author),
        d: common_vendor.t($options.formatPrice(book.price)),
        e: common_vendor.t($options.getAnnotationText(book)),
        f: common_vendor.t(book.categoryName || book.category || $data.textMap.unknownCategory),
        g: book._uniqueKey,
        h: common_vendor.o(($event) => $options.goToBookDetail(book), book._uniqueKey)
      };
    })
  } : {
    A: common_vendor.t($data.textMap.noHotBooks)
  }, {
    B: common_vendor.t($data.textMap.recommendPaths),
    C: common_vendor.t($data.textMap.more),
    D: common_vendor.o((...args) => $options.goToPathList && $options.goToPathList(...args)),
    E: $data.studyPaths.length
  }, $data.studyPaths.length ? {
    F: common_vendor.f($data.studyPaths, (path, k0, i0) => {
      return {
        a: common_vendor.t(path.title || path.name),
        b: common_vendor.t(path.creator),
        c: common_vendor.t(path.category || $data.textMap.unknownCategory),
        d: common_vendor.t(path.nodeCount || path.bookCount || 0),
        e: common_vendor.t(path.learnerCount || path.learners || 0),
        f: path.id || path.name,
        g: common_vendor.o(($event) => $options.goToPathDetail(path), path.id || path.name)
      };
    }),
    G: common_vendor.t($data.textMap.nodeUnit),
    H: common_vendor.t($data.textMap.learnersUnit)
  } : {
    I: common_vendor.t($data.textMap.noPaths)
  }, {
    J: common_vendor.t($data.textMap.communityFeed),
    K: common_vendor.t($data.textMap.more),
    L: common_vendor.o((...args) => $options.goToCommunity && $options.goToCommunity(...args)),
    M: common_vendor.f($data.dynamics, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.username),
        b: common_vendor.t(item.time),
        c: common_vendor.t(item.content),
        d: item.username + item.time,
        e: common_vendor.o((...args) => $options.goToCommunity && $options.goToCommunity(...args), item.username + item.time)
      };
    }),
    N: common_assets._imports_2
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-1cf27b2a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map
