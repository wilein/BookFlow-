"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      headerPlaceholderHeight: 0,
      texts: {
        editProfile: "完善资料",
        verify: "学生认证",
        points: "积分",
        level: "学者等级",
        incompleteTitle: "个人信息待完善",
        incompleteDesc: "默认昵称为书友，点击补充学校、院系、简介和头像。",
        goComplete: "去完善",
        verifyStatus: "认证状态：",
        assetTitle: "资产管理",
        knowledgeTitle: "知识管理",
        helpTitle: "设置与帮助",
        notifications: "通知中心",
        address: "收货地址管理",
        service: "联系客服",
        setting: "设置",
        logout: "退出登录",
        iconNotification: "通",
        iconAddress: "地",
        iconService: "服",
        iconSetting: "设",
        confirmLogout: "退出登录",
        confirmLogoutText: "确认退出当前账号吗？",
        viewVerify: "查看认证信息",
        pendingVerify: "审核中",
        tapVerify: "点击去认证",
        guestTitle: "游客浏览中",
        guestDesc: "登录后可以发布书籍、收藏、聊天和查看订单。",
        goLogin: "去登录",
        goBrowse: "继续逛逛"
      },
      isLoggedIn: false,
      profile: {
        avatar: "/static/logo.png",
        nickname: "书友",
        signature: "个人信息待完善",
        creditBadge: "信誉良好",
        authStatus: 0,
        authLabel: "未认证",
        points: 88,
        level: "Lv.9",
        profileIncomplete: true
      },
      stats: {
        sellingBooks: 0,
        soldBooks: 0,
        pendingPay: 0,
        pendingShip: 0,
        pendingReceive: 0,
        favorites: 0,
        annotations: 0,
        paths: 0,
        resources: 0
      }
    };
  },
  computed: {
    verifyActionText() {
      if (this.profile.authStatus === 2)
        return this.texts.viewVerify;
      if (this.profile.authStatus === 1)
        return this.texts.pendingVerify;
      return this.texts.tapVerify;
    },
    assetItems() {
      return [
        { key: "bookshelf", icon: "书", title: "我的书架", sub: `在售 ${this.stats.sellingBooks} / 已售 ${this.stats.soldBooks}`, url: "/pages/my/bookshelf" },
        { key: "orders", icon: "单", title: "我的订单", sub: `待付 ${this.stats.pendingPay} / 待发 ${this.stats.pendingShip}`, url: "/pages/my/orders" },
        { key: "cart", icon: "车", title: "购物车", sub: "合并勾选后分单结算", url: "/pages/cart/cart" },
        { key: "favorites", icon: "藏", title: "我的收藏", sub: `${this.stats.favorites} 条收藏`, url: "/pages/my/favorites" },
        { key: "address", icon: "址", title: "收货地址", sub: "管理收货信息", url: "/pages/my/address" },
        { key: "notifications", icon: "通", title: "通知中心", sub: "查看交易和社区通知", url: "/pages/my/notifications" }
      ];
    },
    knowledgeItems() {
      return [
        { key: "annotations", icon: "注", title: "我的批注", url: "/pages/my/annotations" },
        { key: "paths", icon: "径", title: "我的路径", url: "/pages/my/paths" },
        { key: "resources", icon: "资", title: "我上传的资源", url: "/pages/my/resources" }
      ];
    }
  },
  onLoad() {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    const statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerPlaceholderHeight = capsule ? capsule.top + capsule.height + 12 : statusBarHeight + 48;
  },
  onShow() {
    this.isLoggedIn = utils_auth.hasValidSession();
    if (!this.isLoggedIn)
      return;
    this.fetchProfile();
    this.fetchStats();
  },
  methods: {
    async fetchProfile() {
      try {
        const data = await utils_api_user.getUserProfile();
        if (data) {
          this.profile = { ...this.profile, ...data };
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/my.vue:237", "fetchProfile failed", error);
      }
    },
    async fetchStats() {
      try {
        const data = await utils_api_user.getUserStats();
        if (data) {
          this.stats = { ...this.stats, ...data };
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/my.vue:247", "fetchStats failed", error);
      }
    },
    navigateTo(url) {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      common_vendor.index.navigateTo({ url });
    },
    goLogin() {
      utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl());
    },
    goBrowse() {
      common_vendor.index.switchTab({ url: "/pages/index/index" });
    },
    goEditProfile() {
      this.navigateTo("/pages/my/edit");
    },
    goVerify() {
      this.navigateTo("/pages/placeholder/verify");
    },
    goService() {
      common_vendor.index.navigateTo({ url: `/pages/placeholder/feedback?pagePath=${encodeURIComponent("/pages/my/my")}` });
    },
    onLogout() {
      common_vendor.index.showModal({
        title: this.texts.confirmLogout,
        content: this.texts.confirmLogoutText,
        success: async (res) => {
          if (!res.confirm)
            return;
          try {
            await utils_api_user.logoutAuth();
          } catch (error) {
            common_vendor.index.__f__("error", "at pages/my/my.vue:278", "logoutAuth failed", error);
          }
          utils_auth.clearSession();
          common_vendor.index.reLaunch({ url: "/pages/login/login" });
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.headerPlaceholderHeight + "px",
    b: !$data.isLoggedIn
  }, !$data.isLoggedIn ? {
    c: common_assets._imports_2,
    d: common_vendor.t($data.texts.guestTitle),
    e: common_vendor.t($data.texts.guestDesc),
    f: common_vendor.t($data.texts.goLogin),
    g: common_vendor.o((...args) => $options.goLogin && $options.goLogin(...args)),
    h: common_vendor.t($data.texts.goBrowse),
    i: common_vendor.o((...args) => $options.goBrowse && $options.goBrowse(...args))
  } : common_vendor.e({
    j: $data.profile.avatar || "/static/logo.png",
    k: common_vendor.t($data.profile.nickname),
    l: common_vendor.t($data.profile.creditBadge),
    m: common_vendor.t($data.profile.signature),
    n: common_vendor.t($data.texts.editProfile),
    o: common_vendor.o((...args) => $options.goEditProfile && $options.goEditProfile(...args)),
    p: common_vendor.t($data.texts.verify),
    q: common_vendor.o((...args) => $options.goVerify && $options.goVerify(...args)),
    r: common_vendor.t($data.profile.points),
    s: common_vendor.t($data.texts.points),
    t: common_vendor.t($data.profile.level),
    v: common_vendor.t($data.texts.level),
    w: $data.profile.profileIncomplete
  }, $data.profile.profileIncomplete ? {
    x: common_vendor.t($data.texts.incompleteTitle),
    y: common_vendor.t($data.texts.incompleteDesc),
    z: common_vendor.t($data.texts.goComplete),
    A: common_vendor.o((...args) => $options.goEditProfile && $options.goEditProfile(...args))
  } : {}, {
    B: common_vendor.t($data.texts.verifyStatus),
    C: common_vendor.t($data.profile.authLabel),
    D: common_vendor.t($options.verifyActionText),
    E: $data.profile.authStatus === 2 ? 1 : "",
    F: $data.profile.authStatus === 1 ? 1 : "",
    G: common_vendor.o((...args) => $options.goVerify && $options.goVerify(...args)),
    H: common_vendor.t($data.texts.assetTitle),
    I: common_vendor.f($options.assetItems, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.icon),
        b: common_vendor.t(item.title),
        c: common_vendor.t(item.sub),
        d: item.key,
        e: common_vendor.o(($event) => $options.navigateTo(item.url), item.key)
      };
    }),
    J: common_vendor.t($data.texts.knowledgeTitle),
    K: common_vendor.f($options.knowledgeItems, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.icon),
        b: common_vendor.t(item.title),
        c: item.key,
        d: common_vendor.o(($event) => $options.navigateTo(item.url), item.key)
      };
    }),
    L: common_vendor.t($data.texts.helpTitle),
    M: common_vendor.t($data.texts.iconNotification),
    N: common_vendor.t($data.texts.notifications),
    O: common_vendor.o(($event) => $options.navigateTo("/pages/my/notifications")),
    P: common_vendor.t($data.texts.iconAddress),
    Q: common_vendor.t($data.texts.address),
    R: common_vendor.o(($event) => $options.navigateTo("/pages/my/address")),
    S: common_vendor.t($data.texts.iconService),
    T: common_vendor.t($data.texts.service),
    U: common_vendor.o((...args) => $options.goService && $options.goService(...args)),
    V: common_vendor.t($data.texts.iconSetting),
    W: common_vendor.t($data.texts.setting),
    X: common_vendor.o((...args) => $options.goEditProfile && $options.goEditProfile(...args)),
    Y: common_vendor.t($data.texts.logout),
    Z: common_vendor.o((...args) => $options.onLogout && $options.onLogout(...args))
  }));
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-2f1ef635"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/my.js.map
