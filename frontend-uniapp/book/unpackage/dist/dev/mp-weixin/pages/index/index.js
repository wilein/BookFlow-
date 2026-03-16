"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      capsulePaddingRight: 0,
      fixedHeaderHeight: 0,
      navItems: [
        { name: "分类浏览", icon: "📚" },
        { name: "学习路径", icon: "🗺️" },
        { name: "发布书籍", icon: "📤" },
        { name: "社区动态", icon: "💬" }
      ],
      // 轮播图数据（初始为空，由 API 填充）
      bannerList: [],
      // 热门书籍数据（初始为空，由 API 填充）
      hotBooks: [],
      // 以下为静态数据，可根据需要改为 API 获取
      studyPaths: [
        { name: "Java后端开发路线", creator: "张三学长创建", bookCount: 8, learners: 234 },
        { name: "前端工程师进阶", creator: "李四学姐创建", bookCount: 12, learners: 456 }
      ],
      dynamics: [
        { username: "王五", time: "2小时前", content: "分享一本宝藏书籍《设计模式》，学长的批注超级详细！" },
        { username: "赵六", time: "5小时前", content: "刚完成了数据结构学习路径，感谢学长推荐！" }
      ]
    };
  },
  onLoad() {
    common_vendor.index.getSystemInfo({
      success: (res) => {
        this.statusBarHeight = res.statusBarHeight;
      }
    });
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const screenWidth = common_vendor.index.getSystemInfoSync().windowWidth;
      this.capsulePaddingRight = screenWidth - capsule.left + 10;
    } else {
      this.capsulePaddingRight = 100;
    }
    this.fetchBannerData();
    this.fetchHotBooks();
  },
  onReady() {
    common_vendor.index.createSelectorQuery().in(this).select(".fixed-header").boundingClientRect((rect) => {
      if (rect) {
        this.fixedHeaderHeight = rect.height;
      }
    }).exec();
  },
  methods: {
    // 获取轮播图数据
    fetchBannerData() {
      common_vendor.index.showLoading({ title: "加载中..." });
      common_vendor.index.request({
        url: "http://localhost:8080/common/banner/list",
        // 替换为实际接口地址
        method: "GET",
        success: (res) => {
          if (res.data.code === "200") {
            this.bannerList = res.data.data;
          } else {
            common_vendor.index.showToast({ title: "轮播图加载失败", icon: "none" });
          }
        },
        fail: (err) => {
          common_vendor.index.__f__("error", "at pages/index/index.vue:219", "banner api error", err);
          common_vendor.index.showToast({ title: "网络错误", icon: "none" });
        },
        complete: () => {
          common_vendor.index.hideLoading();
        }
      });
    },
    // 获取热门书籍数据
    fetchHotBooks() {
      common_vendor.index.request({
        url: "http://localhost:8080/book/list",
        // 替换为实际接口地址
        method: "GET",
        success: (res) => {
          if (res.data.code === "200") {
            common_vendor.index.__f__("log", "at pages/index/index.vue:234", res.data.data);
            common_vendor.index.__f__("log", "at pages/index/index.vue:235", res.data.data);
            this.hotBooks = res.data.data;
          } else {
            common_vendor.index.showToast({ title: "书籍加载失败", icon: "none" });
          }
        },
        fail: (err) => {
          common_vendor.index.__f__("error", "at pages/index/index.vue:242", "hotBooks api error", err);
          common_vendor.index.showToast({ title: "网络错误", icon: "none" });
        }
      });
    },
    goToSearch() {
      common_vendor.index.navigateTo({ url: "/pages/search/search" });
    },
    goToBookDetail(book) {
      common_vendor.index.navigateTo({
        url: `/pages/books/detail?title=${encodeURIComponent(book.title)}`
      });
    },
    goToPathDetail(path) {
      common_vendor.index.navigateTo({
        url: `/pages/paths/detail?name=${encodeURIComponent(path.name)}`
      });
    },
    goToDynamicDetail(dynamic) {
      common_vendor.index.navigateTo({
        url: `/pages/community/detail?username=${encodeURIComponent(dynamic.username)}`
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_assets._imports_1,
    c: common_vendor.o((...args) => $options.goToSearch && $options.goToSearch(...args)),
    d: $data.statusBarHeight + "px",
    e: $data.capsulePaddingRight + "px",
    f: $data.fixedHeaderHeight + "px",
    g: $data.bannerList.length > 0
  }, $data.bannerList.length > 0 ? {
    h: common_vendor.f($data.bannerList, (item, index, i0) => {
      return {
        a: item.image,
        b: index
      };
    })
  } : {}, {
    i: common_vendor.f($data.navItems, (item, index, i0) => {
      return {
        a: common_vendor.t(item.icon),
        b: common_vendor.t(item.name),
        c: index
      };
    }),
    j: $data.hotBooks.length > 0
  }, $data.hotBooks.length > 0 ? {
    k: common_vendor.f($data.hotBooks, (book, index, i0) => {
      return {
        a: book.cover || "/static/cover_placeholder.png",
        b: common_vendor.t(book.title),
        c: common_vendor.t(book.author),
        d: common_vendor.t(book.price),
        e: common_vendor.t(book.annotations),
        f: common_vendor.t(book.category),
        g: index,
        h: common_vendor.o(($event) => $options.goToBookDetail(book), index)
      };
    })
  } : {}, {
    l: common_vendor.f($data.studyPaths, (path, index, i0) => {
      return {
        a: common_vendor.t(path.name),
        b: common_vendor.t(path.creator),
        c: common_vendor.t(path.bookCount),
        d: common_vendor.t(path.learners),
        e: index,
        f: common_vendor.o(($event) => $options.goToPathDetail(path), index)
      };
    }),
    m: common_vendor.f($data.dynamics, (item, index, i0) => {
      return {
        a: common_vendor.t(item.username),
        b: common_vendor.t(item.time),
        c: common_vendor.t(item.content),
        d: index,
        e: common_vendor.o(($event) => $options.goToDynamicDetail(item), index)
      };
    }),
    n: common_assets._imports_2
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-1cf27b2a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map
