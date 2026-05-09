"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_user = require("../../utils/api/user.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      avatarUploading: false,
      submitting: false,
      texts: {
        title: "编辑资料",
        heroTitle: "完善个人信息",
        heroSub: "昵称、学校和简介会同步展示在个人主页，头像上传后可直接预览。",
        avatar: "头像",
        changeAvatar: "点击更换头像",
        avatarDesc: "支持从相册或拍照选择",
        uploading: "上传中...",
        nickname: "昵称",
        nicknamePlaceholder: "请输入昵称",
        mobile: "手机号",
        mobilePlaceholder: "请输入手机号",
        city: "城市",
        cityPlaceholder: "请输入所在城市",
        school: "学校",
        schoolPlaceholder: "请输入学校名称",
        department: "院系",
        departmentPlaceholder: "请输入院系名称",
        intro: "个人简介",
        introPlaceholder: "介绍一下你的研究方向、感兴趣领域或一句签名",
        save: "保存修改",
        saving: "保存中...",
        uploaded: "头像已更新",
        saved: "已保存",
        uploadLoading: "上传中",
        needNickname: "请输入昵称"
      },
      form: {
        nickname: "",
        avatarUrl: "",
        mobile: "",
        city: "",
        school: "",
        department: "",
        intro: ""
      }
    };
  },
  async onLoad() {
    if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
      return;
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    await this.fetchProfile();
  },
  methods: {
    async fetchProfile() {
      try {
        const data = await utils_api_user.getUserProfile();
        if (!data)
          return;
        this.form = {
          nickname: data.nickname || "",
          avatarUrl: data.avatarUrl || data.avatar || "",
          mobile: data.mobile || "",
          city: data.city || "",
          school: data.school || "",
          department: data.department || "",
          intro: data.intro || ""
        };
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/edit.vue:140", "fetchProfile failed", error);
      }
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/my/my" }) });
    },
    changeAvatar() {
      common_vendor.index.chooseImage({
        count: 1,
        sourceType: ["album", "camera"],
        success: async (res) => {
          const filePath = (res.tempFilePaths || [])[0];
          if (!filePath)
            return;
          this.avatarUploading = true;
          common_vendor.index.showLoading({ title: this.texts.uploadLoading, mask: true });
          try {
            const result = await utils_api_user.uploadProfileImage(filePath, "avatar");
            this.form.avatarUrl = result.url || "";
            common_vendor.index.showToast({ title: this.texts.uploaded, icon: "success" });
          } catch (error) {
            common_vendor.index.__f__("error", "at pages/my/edit.vue:160", "upload avatar failed", error);
          } finally {
            this.avatarUploading = false;
            common_vendor.index.hideLoading();
          }
        }
      });
    },
    async submit() {
      if (this.submitting)
        return;
      if (!this.form.nickname.trim()) {
        common_vendor.index.showToast({ title: this.texts.needNickname, icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        await utils_api_user.updateUserProfile({
          nickname: this.form.nickname,
          avatarUrl: this.form.avatarUrl,
          mobile: this.form.mobile,
          city: this.form.city,
          school: this.form.school,
          department: this.form.department,
          intro: this.form.intro
        });
        common_vendor.index.showToast({ title: this.texts.saved, icon: "success" });
        setTimeout(() => this.goBack(), 500);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/my/edit.vue:188", "updateUserProfile failed", error);
      } finally {
        this.submitting = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.texts.title),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerHeight + "px",
    g: common_vendor.t($data.texts.heroTitle),
    h: common_vendor.t($data.texts.heroSub),
    i: common_vendor.t($data.texts.avatar),
    j: $data.form.avatarUrl || "/static/logo.png",
    k: common_vendor.t($data.texts.changeAvatar),
    l: common_vendor.t($data.avatarUploading ? $data.texts.uploading : $data.texts.avatarDesc),
    m: common_vendor.o((...args) => $options.changeAvatar && $options.changeAvatar(...args)),
    n: common_vendor.t($data.texts.nickname),
    o: $data.texts.nicknamePlaceholder,
    p: $data.form.nickname,
    q: common_vendor.o(($event) => $data.form.nickname = $event.detail.value),
    r: common_vendor.t($data.texts.mobile),
    s: $data.texts.mobilePlaceholder,
    t: $data.form.mobile,
    v: common_vendor.o(($event) => $data.form.mobile = $event.detail.value),
    w: common_vendor.t($data.texts.city),
    x: $data.texts.cityPlaceholder,
    y: $data.form.city,
    z: common_vendor.o(($event) => $data.form.city = $event.detail.value),
    A: common_vendor.t($data.texts.school),
    B: $data.texts.schoolPlaceholder,
    C: $data.form.school,
    D: common_vendor.o(($event) => $data.form.school = $event.detail.value),
    E: common_vendor.t($data.texts.department),
    F: $data.texts.departmentPlaceholder,
    G: $data.form.department,
    H: common_vendor.o(($event) => $data.form.department = $event.detail.value),
    I: common_vendor.t($data.texts.intro),
    J: $data.texts.introPlaceholder,
    K: $data.form.intro,
    L: common_vendor.o(($event) => $data.form.intro = $event.detail.value),
    M: common_vendor.t($data.submitting ? $data.texts.saving : $data.texts.save),
    N: common_vendor.o((...args) => $options.submit && $options.submit(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-d4d01944"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/edit.js.map
