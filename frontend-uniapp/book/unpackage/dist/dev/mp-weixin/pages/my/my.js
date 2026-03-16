"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  data() {
    return {
      defaultAvatar: "/static/avatar_placeholder.png",
      user: {
        userId: null,
        nickname: "",
        avatarUrl: "",
        creditLevelText: "新用户",
        isVerified: false,
        stats: {
          selling: 0,
          sold: 0,
          collections: 0
        }
      }
    };
  },
  onLoad() {
    this.loadUserInfo();
  },
  methods: {
    // 从后端 / 登录结果中获取用户数据（参考 /user/auth/wechat 返回的 userInfo）
    loadUserInfo() {
      try {
        const cached = common_vendor.index.getStorageSync("userInfo");
        if (cached) {
          this.user = {
            ...this.user,
            ...cached
          };
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/my/my.vue:186", "loadUserInfo error", e);
      }
    },
    goToVerify() {
      common_vendor.index.navigateTo({
        url: "/pages/auth/auth"
        // 预留认证页面路由
      });
    },
    goToEditProfile() {
      common_vendor.index.navigateTo({
        url: "/pages/my/edit"
        // 路由到编辑个人资料页
      });
    },
    goToShelf(type) {
      common_vendor.index.navigateTo({
        url: `/pages/shelf/shelf?type=${type}`
      });
    },
    goToOrder(status) {
      common_vendor.index.navigateTo({
        url: `/pages/order/list?status=${status}`
      });
    },
    goToCollections() {
      common_vendor.index.navigateTo({
        url: "/pages/collection/collection"
      });
    },
    goToAnnotations() {
      common_vendor.index.navigateTo({
        url: "/pages/annotation/annotation"
      });
    },
    goToPaths() {
      common_vendor.index.navigateTo({
        url: "/pages/paths/my"
      });
    },
    goToResources() {
      common_vendor.index.navigateTo({
        url: "/pages/resources/my"
      });
    },
    goToAddress() {
      common_vendor.index.navigateTo({
        url: "/pages/address/list"
      });
    },
    goToSettings() {
      common_vendor.index.navigateTo({
        url: "/pages/settings/settings"
      });
    },
    contactService() {
      common_vendor.index.showModal({
        title: "联系客服",
        content: "请通过公众号或客服微信联系管理员。",
        showCancel: false
      });
    },
    logout() {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定要退出登录吗？",
        success: (res) => {
          if (res.confirm) {
            common_vendor.index.removeStorageSync("userInfo");
            common_vendor.index.removeStorageSync("token");
            this.user = {
              ...this.user,
              userId: null,
              nickname: "",
              avatarUrl: "",
              creditLevelText: "新用户",
              isVerified: false
            };
            common_vendor.index.showToast({ title: "已退出登录", icon: "none" });
          }
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.user.avatarUrl || $data.defaultAvatar,
    b: common_vendor.t($data.user.nickname || "未登录用户"),
    c: $data.user.creditLevelText
  }, $data.user.creditLevelText ? {
    d: common_vendor.t($data.user.creditLevelText)
  } : {}, {
    e: $data.user.userId
  }, $data.user.userId ? {
    f: common_vendor.t($data.user.userId)
  } : {}, {
    g: common_vendor.t($data.user.stats.selling),
    h: common_vendor.t($data.user.stats.sold),
    i: common_vendor.t($data.user.stats.collections),
    j: common_vendor.t($data.user.isVerified ? "已完成实名认证" : "未认证，点击去认证"),
    k: $data.user.isVerified ? 1 : "",
    l: common_vendor.t($data.user.isVerified ? "账户更安全，交易更放心" : "认证后可提升信誉等级，增加交易额度"),
    m: common_vendor.o((...args) => $options.goToVerify && $options.goToVerify(...args)),
    n: common_vendor.o(($event) => $options.goToShelf("selling")),
    o: common_vendor.o(($event) => $options.goToShelf("sold")),
    p: common_vendor.o(($event) => $options.goToShelf("draft")),
    q: common_vendor.o(($event) => $options.goToOrder("unpaid")),
    r: common_vendor.o(($event) => $options.goToOrder("undelivered")),
    s: common_vendor.o(($event) => $options.goToOrder("unreceived")),
    t: common_vendor.o(($event) => $options.goToOrder("finished")),
    v: common_vendor.o((...args) => $options.goToCollections && $options.goToCollections(...args)),
    w: common_vendor.o((...args) => $options.goToAnnotations && $options.goToAnnotations(...args)),
    x: common_vendor.o((...args) => $options.goToPaths && $options.goToPaths(...args)),
    y: common_vendor.o((...args) => $options.goToResources && $options.goToResources(...args)),
    z: common_vendor.o((...args) => $options.goToAddress && $options.goToAddress(...args)),
    A: common_vendor.o((...args) => $options.goToSettings && $options.goToSettings(...args)),
    B: common_vendor.o((...args) => $options.goToEditProfile && $options.goToEditProfile(...args)),
    C: common_vendor.o((...args) => $options.contactService && $options.contactService(...args)),
    D: common_vendor.o((...args) => $options.logout && $options.logout(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-2f1ef635"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/my.js.map
