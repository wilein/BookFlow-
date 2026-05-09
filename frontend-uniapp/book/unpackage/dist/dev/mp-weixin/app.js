"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const utils_api_user = require("./utils/api/user.js");
const utils_auth = require("./utils/auth.js");
if (!Math) {
  "./pages/index/index.js";
  "./pages/login/login.js";
  "./pages/my/my.js";
  "./pages/my/edit.js";
  "./pages/my/bookshelf.js";
  "./pages/my/orders.js";
  "./pages/my/order-detail.js";
  "./pages/my/favorites.js";
  "./pages/my/annotations.js";
  "./pages/my/paths.js";
  "./pages/my/resources.js";
  "./pages/my/address.js";
  "./pages/my/address-edit.js";
  "./pages/my/notifications.js";
  "./pages/cart/cart.js";
  "./pages/cart/checkout.js";
  "./pages/community/community.js";
  "./pages/category/category.js";
  "./pages/search/search.js";
  "./pages/books/detail.js";
  "./pages/annotations/list.js";
  "./pages/annotations/create.js";
  "./pages/path/detail.js";
  "./pages/path/node-detail.js";
  "./pages/path/list.js";
  "./pages/path/create.js";
  "./pages/resources/list.js";
  "./pages/resources/detail.js";
  "./pages/resources/create.js";
  "./pages/chat/chat.js";
  "./pages/placeholder/history.js";
  "./pages/placeholder/feedback.js";
  "./pages/placeholder/verify.js";
  "./pages/publish/create.js";
  "./pages/community/create.js";
  "./pages/community/comments.js";
}
const _sfc_main = {
  onLaunch() {
    this.bootstrapAuth();
  },
  onShow() {
    this.bootstrapAuth();
  },
  methods: {
    async bootstrapAuth() {
      if (!utils_auth.hasValidSession()) {
        utils_auth.clearSession();
        return;
      }
      try {
        await utils_api_user.checkAuthSession();
      } catch (error) {
        common_vendor.index.__f__("error", "at App.vue:21", "bootstrapAuth failed", error);
      }
    }
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  return {
    app
  };
}
createApp().app.mount("#app");
exports.createApp = createApp;
//# sourceMappingURL=../.sourcemap/mp-weixin/app.js.map
