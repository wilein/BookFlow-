"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_bookDetail = require("../../utils/book-detail.js");
const utils_api_book = require("../../utils/api/book.js");
const utils_api_annotation = require("../../utils/api/annotation.js");
const utils_api_favorite = require("../../utils/api/favorite.js");
const utils_api_cart = require("../../utils/api/cart.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
function normalizeId(value) {
  const text = String(value == null ? "" : value).trim();
  return /^\d+$/.test(text) ? text : "";
}
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerRightSafe: 24,
      book: utils_bookDetail.buildBookDetail(),
      isFavorite: false,
      texts: {
        share: "分享",
        favorite: "收藏",
        favorited: "已收藏",
        favoritedShort: "已藏",
        publisher: "出版社",
        onSale: "在售",
        sold: "已售",
        currency: "¥",
        starFull: "★",
        starEmpty: "☆",
        contactSeller: "联系卖家",
        featureTitle: "特色功能",
        annotations: "查看批注",
        resources: "配套资源",
        annotationUnit: "条批注",
        resourceUnit: "份资源",
        learningPaths: "学习路径",
        learners: "人在学",
        description: "书籍描述",
        remark: "备注",
        buyNow: "立即购买",
        addCart: "加购",
        addCartSuccess: "已加入购物车",
        soldToast: "该书已售",
        shareDev: "分享功能开发中"
      }
    };
  },
  computed: {
    priceText() {
      return utils_bookDetail.toPriceText(this.book.price);
    },
    stars() {
      return [1, 2, 3, 4, 5].map((index) => this.book.seller.score >= index);
    }
  },
  onShow() {
    if (this.book.id) {
      this.fetchBookDetail();
      this.fetchFavoriteState();
    }
  },
  onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
    } else {
      this.headerRightSafe = 24;
    }
    this.book = utils_bookDetail.buildBookDetail(options);
    this.book.id = normalizeId(this.book.id);
    this.fetchBookDetail(options);
    this.fetchFavoriteState();
  },
  methods: {
    buildRouteUrl(options = {}) {
      const id = encodeURIComponent(this.book.id || options.id || "");
      return id ? `/pages/books/detail?id=${id}` : "/pages/books/detail";
    },
    async recordHistory(options) {
      if (!this.book.id || !utils_auth.hasValidSession())
        return;
      try {
        await utils_api_user.recordBrowseHistory({
          targetType: "book",
          targetId: this.book.id,
          title: this.book.title,
          subTitle: this.book.author,
          coverUrl: this.book.cover,
          routeUrl: this.buildRouteUrl(options)
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/books/detail.vue:206", "record book history failed", error);
      }
    },
    async fetchBookDetail(options = {}) {
      if (!this.book.id) {
        await this.recordHistory(options);
        return;
      }
      try {
        const data = await utils_api_book.getBookDetail(this.book.id);
        if (data && Object.keys(data).length) {
          this.book = utils_bookDetail.buildBookDetail(data);
          this.book.id = normalizeId(this.book.id);
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/books/detail.vue:221", "getBookDetail failed", error);
        await this.recordHistory(options);
        return;
      }
      try {
        const annotationData = await utils_api_annotation.getAnnotationList(this.book.id, { showError: false });
        this.book.annotationCount = Number((annotationData == null ? void 0 : annotationData.total) || 0);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/books/detail.vue:229", "getAnnotationList failed", error);
      }
      await this.recordHistory(options);
    },
    async fetchFavoriteState() {
      if (!utils_auth.hasValidSession() || !this.book.id)
        return;
      try {
        const data = await utils_api_favorite.getFavoriteStatus("book", this.book.id);
        this.isFavorite = Boolean(data == null ? void 0 : data.favorited);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/books/detail.vue:239", "getFavoriteStatus failed", error);
      }
    },
    goBack() {
      common_vendor.index.navigateBack({
        fail: () => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
        }
      });
    },
    handleShare() {
      common_vendor.index.showToast({ title: this.texts.shareDev, icon: "none" });
    },
    async toggleFavoriteAction() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      try {
        const data = await utils_api_favorite.toggleFavorite("book", this.book.id);
        this.isFavorite = Boolean(data == null ? void 0 : data.favorited);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/books/detail.vue:258", "toggleFavorite failed", error);
      }
    },
    contactSeller() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      const params = [
        `sellerId=${encodeURIComponent(this.book.seller.id || "")}`,
        `sellerName=${encodeURIComponent(this.book.seller.name || "")}`,
        `bookId=${encodeURIComponent(this.book.id || "")}`,
        `bookTitle=${encodeURIComponent(this.book.title || "")}`
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/chat/chat?${params}` });
    },
    async handleAddCart() {
      if (this.book.isSold) {
        common_vendor.index.showToast({ title: this.texts.soldToast, icon: "none" });
        return;
      }
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      try {
        await utils_api_cart.addCartItem(this.book.id);
        common_vendor.index.showToast({ title: this.texts.addCartSuccess, icon: "success" });
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/books/detail.vue:281", "addCartItem failed", error);
      }
    },
    goToAnnotations() {
      const params = [
        `bookId=${encodeURIComponent(this.book.id || "")}`,
        `bookTitle=${encodeURIComponent(this.book.title || "")}`
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/annotations/list?${params}` });
    },
    goToResources() {
      common_vendor.index.navigateTo({
        url: `/pages/resources/list?bookId=${encodeURIComponent(this.book.id || "")}&title=${encodeURIComponent(this.book.title + this.texts.resources)}`
      });
    },
    goToPath(path) {
      const params = [
        `pathId=${encodeURIComponent(path.id || "")}`,
        `title=${encodeURIComponent(path.name || "")}`,
        `description=${encodeURIComponent(path.intro || "")}`,
        `creator=${encodeURIComponent(this.book.seller && this.book.seller.name || "路径创建者")}`,
        "isCreator=0"
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/path/detail?${params}` });
    },
    handleBuy() {
      if (this.book.isSold) {
        common_vendor.index.showToast({ title: this.texts.soldToast, icon: "none" });
        return;
      }
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      common_vendor.index.navigateTo({ url: `/pages/cart/checkout?bookId=${encodeURIComponent(this.book.id || "")}` });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.f($data.book.images, (image, index, i0) => {
      return {
        a: image,
        b: index
      };
    }),
    b: common_assets._imports_0,
    c: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    d: common_vendor.t($data.texts.share),
    e: common_vendor.o((...args) => $options.handleShare && $options.handleShare(...args)),
    f: common_vendor.t($data.isFavorite ? $data.texts.favoritedShort : $data.texts.favorite),
    g: common_vendor.o((...args) => $options.toggleFavoriteAction && $options.toggleFavoriteAction(...args)),
    h: $data.statusBarHeight + "px",
    i: $data.headerRightSafe + "px",
    j: common_vendor.t($data.book.title),
    k: common_vendor.t($data.book.author),
    l: common_vendor.t($data.texts.publisher),
    m: common_vendor.t($data.book.publisher),
    n: common_vendor.t($data.book.isbn),
    o: common_vendor.t($data.book.conditionLabel),
    p: common_vendor.t($data.book.isSold ? $data.texts.sold : $data.texts.onSale),
    q: common_vendor.t($data.book.category),
    r: common_vendor.t($data.texts.currency),
    s: common_vendor.t($options.priceText),
    t: $data.book.seller.avatar,
    v: common_vendor.t($data.book.seller.name),
    w: common_vendor.f($options.stars, (star, index, i0) => {
      return {
        a: common_vendor.t(star ? $data.texts.starFull : $data.texts.starEmpty),
        b: index
      };
    }),
    x: common_vendor.t($data.book.seller.score.toFixed(1)),
    y: common_vendor.t($data.texts.contactSeller),
    z: common_vendor.o((...args) => $options.contactSeller && $options.contactSeller(...args)),
    A: common_vendor.t($data.texts.featureTitle),
    B: common_vendor.t($data.texts.annotations),
    C: common_vendor.t($data.book.annotationCount),
    D: common_vendor.t($data.texts.annotationUnit),
    E: common_vendor.o((...args) => $options.goToAnnotations && $options.goToAnnotations(...args)),
    F: common_vendor.t($data.texts.resources),
    G: common_vendor.t($data.book.resourceCount),
    H: common_vendor.t($data.texts.resourceUnit),
    I: common_vendor.o((...args) => $options.goToResources && $options.goToResources(...args)),
    J: $data.book.learningPaths.length
  }, $data.book.learningPaths.length ? {
    K: common_vendor.t($data.texts.learningPaths),
    L: common_vendor.f($data.book.learningPaths, (path, k0, i0) => {
      return {
        a: common_vendor.t(path.name),
        b: common_vendor.t(path.intro),
        c: common_vendor.t(path.learners),
        d: path.id,
        e: common_vendor.o(($event) => $options.goToPath(path), path.id)
      };
    }),
    M: common_vendor.t($data.texts.learners)
  } : {}, {
    N: common_vendor.t($data.texts.description),
    O: common_vendor.t($data.book.description),
    P: common_vendor.t($data.texts.remark),
    Q: common_vendor.t($data.book.remark),
    R: common_vendor.t($data.isFavorite ? $data.texts.favorited : $data.texts.favorite),
    S: common_vendor.o((...args) => $options.toggleFavoriteAction && $options.toggleFavoriteAction(...args)),
    T: common_vendor.t($data.texts.addCart),
    U: $data.book.isSold ? 1 : "",
    V: common_vendor.o((...args) => $options.handleAddCart && $options.handleAddCart(...args)),
    W: common_vendor.t($data.book.isSold ? $data.texts.sold : $data.texts.buyNow),
    X: $data.book.isSold ? 1 : "",
    Y: common_vendor.o((...args) => $options.handleBuy && $options.handleBuy(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-92687f9f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/books/detail.js.map
