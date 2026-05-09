"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const TEXTS = {
  appName: "薪传",
  subtitle: "教材流转、批注传承、学习路径共享",
  cardTitle: "登录后继续使用",
  cardDesc: "首次进入需要完成登录，登录状态有效期 15 天，活跃访问会自动续期。",
  h5Login: "开发环境一键登录",
  wechatLogin: "微信一键登录",
  loggingIn: "登录中...",
  tip: "当前不支持匿名进入业务页",
  loginSuccess: "登录成功",
  loginFail: "登录失败",
  codeFail: "获取登录凭证失败"
};
function getRuntimePlatform() {
  let platform = "unknown";
  platform = "mp-weixin";
  return platform;
}
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      loading: false,
      platform: getRuntimePlatform()
    };
  },
  computed: {
    loginButtonText() {
      return this.platform === "h5" ? TEXTS.h5Login : TEXTS.wechatLogin;
    }
  },
  onLoad() {
    if (utils_auth.hasValidSession()) {
      utils_auth.navigateAfterLogin();
    }
  },
  methods: {
    async handleLogin() {
      if (this.loading)
        return;
      this.loading = true;
      try {
        if (this.platform === "h5") {
          await utils_api_user.loginWithDev();
        } else {
          const code = await this.fetchWechatCode();
          await utils_api_user.loginWithWechat(code);
        }
        common_vendor.index.showToast({ title: TEXTS.loginSuccess, icon: "success" });
        setTimeout(() => {
          utils_auth.navigateAfterLogin();
        }, 300);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/login/login.vue:83", "handleLogin failed", error);
        common_vendor.index.showToast({ title: (error == null ? void 0 : error.message) || TEXTS.loginFail, icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    fetchWechatCode() {
      return new Promise((resolve, reject) => {
        common_vendor.index.login({
          provider: "weixin",
          success: (res) => {
            if (res.code) {
              resolve(res.code);
              return;
            }
            reject(new Error("missing wechat code"));
          },
          fail: (error) => {
            common_vendor.index.showToast({ title: TEXTS.codeFail, icon: "none" });
            reject(error);
          }
        });
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_assets._imports_2,
    b: common_vendor.t($data.texts.appName),
    c: common_vendor.t($data.texts.subtitle),
    d: common_vendor.t($data.texts.cardTitle),
    e: common_vendor.t($data.texts.cardDesc),
    f: common_vendor.t($data.loading ? $data.texts.loggingIn : $options.loginButtonText),
    g: $data.loading ? 1 : "",
    h: common_vendor.o((...args) => $options.handleLogin && $options.handleLogin(...args)),
    i: common_vendor.t($data.texts.tip)
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-e4e4508d"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/login/login.js.map
