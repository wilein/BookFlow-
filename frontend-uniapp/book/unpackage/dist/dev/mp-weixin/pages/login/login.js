"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  // computed: {  
  //           ...mapState(['userInfo'])
  //       },
  data() {
    return {
      code: ""
      //微信临时登录凭证
    };
  },
  onLoad: function() {
    common_vendor.index.login({
      success: (res) => {
        if (res.errMsg == "login:ok") {
          this.code = res.code;
          common_vendor.index.__f__("log", "at pages/login/login.vue:29", this.code);
        } else {
          common_vendor.index.showToast({
            title: "系统异常，请联系管理员!"
          });
        }
      }
    });
  },
  methods: {
    ...common_vendor.mapMutations(["login"]),
    //微信授权登录
    getUserInfo(e) {
      let that = this;
      var p = this.getSetting();
      p.then(function(isAuth) {
        common_vendor.index.__f__("log", "at pages/login/login.vue:45", "是否已经授权", isAuth);
        if (isAuth) {
          common_vendor.index.__f__("log", "at pages/login/login.vue:56", "用户信息，加密数据", e);
          JSON.parse(e.detail.rawData);
          common_vendor.index.request({
            header: {
              "content-type": "application/x-www-form-urlencoded"
            },
            url: "http://localhost:8080/user/auth/wechat",
            //你的接口地址
            method: "POST",
            //接口类型 
            data: { code: that.code },
            //接口需要的数据
            success: function(res) {
              common_vendor.index.__f__("log", "at pages/login/login.vue:68", res);
              if (res.data.Success) {
                that.login(res.data);
                common_vendor.index.__f__("log", "at pages/login/login.vue:71", res.data);
              } else {
                common_vendor.index.showToast({
                  title: "授权登录失败！",
                  mask: true,
                  icon: "none"
                });
              }
            }
          });
        } else {
          common_vendor.index.showToast({
            title: "授权失败，请确认授权已开启",
            mask: true,
            icon: "none"
          });
        }
      });
    },
    //获取用户的当前设置
    getSetting() {
      return new Promise(function(resolve, reject) {
        common_vendor.index.getSetting({
          success: function(res) {
            if (res.authSetting["scope.userInfo"]) {
              common_vendor.index.__f__("log", "at pages/login/login.vue:102", "存在");
              resolve(true);
            } else {
              common_vendor.index.__f__("log", "at pages/login/login.vue:105", "不存在");
              resolve(false);
            }
          }
        });
      }).catch((e) => {
        common_vendor.index.__f__("log", "at pages/login/login.vue:111", e);
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.o((...args) => $options.getUserInfo && $options.getUserInfo(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/login/login.js.map
