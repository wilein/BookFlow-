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
      uploading: false,
      submitting: false,
      studentCardPreviewSrc: "",
      verifyMode: "student_card",
      profile: {
        authStatus: 0
      },
      form: {
        realName: "",
        studentId: "",
        school: "",
        department: "",
        studentCardImageUrl: ""
      },
      texts: {
        title: "学生认证",
        heroTitle: "身份认证",
        heroSub: "完成认证后可发布书籍、提升信誉展示，审核结果暂以状态字段表示。",
        realName: "真实姓名",
        realNamePlaceholder: "请输入真实姓名",
        studentId: "学号",
        studentIdPlaceholder: "请输入学号",
        school: "学校",
        schoolPlaceholder: "请输入学校名称",
        department: "院系",
        departmentPlaceholder: "请输入院系名称",
        studentCard: "学生证照片",
        required: "必填",
        uploading: "上传中...",
        uploadText: "上传学生证正面照片",
        reupload: "重新上传",
        remove: "移除",
        emailComingTitle: "教育邮箱验证即将开放",
        emailComingDesc: "本次先完成学生证认证主流程，教育邮箱验证码链路后续补充。",
        tipsTitle: "说明",
        tip1: "1. 学生证图片仅用于审核，不会在前台公开展示。",
        tip2: "2. 提交后状态会变为“待审核”，审核后台本次先不实现。",
        tip3: "3. 如需修改认证资料，可在未审核通过前重新提交。",
        submit: "提交审核",
        submitting: "提交中...",
        uploadLoading: "上传中",
        uploaded: "图片已上传",
        modeStudent: "学生证认证",
        modeStudentDesc: "上传学生证照片并提交审核",
        modeEmail: "教育邮箱验证",
        modeEmailDesc: "入口预留，后续补充验证码流程",
        waitAudit: "待审核",
        verified: "已认证",
        rejected: "已驳回",
        unverified: "未认证",
        emailSoon: "教育邮箱验证即将开放",
        currentVerified: "当前已通过认证",
        alreadyPending: "已提交审核，请耐心等待",
        needRealName: "请填写真实姓名",
        needStudentId: "请填写学号",
        needCard: "请上传学生证照片",
        previewLoading: "图片加载中...",
        submitSuccess: "已提交审核"
      }
    };
  },
  computed: {
    verifyModes() {
      return [
        { key: "student_card", title: this.texts.modeStudent, desc: this.texts.modeStudentDesc },
        { key: "edu_email", title: this.texts.modeEmail, desc: this.texts.modeEmailDesc }
      ];
    },
    statusText() {
      const status = Number(this.profile.authStatus || 0);
      if (status === 1)
        return this.texts.waitAudit;
      if (status === 2)
        return this.texts.verified;
      if (status === 3)
        return this.texts.rejected;
      return this.texts.unverified;
    },
    statusClass() {
      const status = Number(this.profile.authStatus || 0);
      if (status === 1)
        return "pending";
      if (status === 2)
        return "approved";
      if (status === 3)
        return "rejected";
      return "unverified";
    }
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
        this.profile = data;
        this.form.realName = data.realName || "";
        this.form.studentId = data.studentId || "";
        this.form.school = data.school || "";
        this.form.department = data.department || "";
        this.form.studentCardImageUrl = data.studentCardImageUrl || "";
        this.loadStudentCardPreview();
        if (data.verifyType === "edu_email") {
          this.verifyMode = "edu_email";
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/placeholder/verify.vue:219", "fetchProfile failed", error);
      }
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/my/my" }) });
    },
    switchMode(mode) {
      this.verifyMode = mode;
      if (mode === "edu_email") {
        common_vendor.index.showToast({ title: this.texts.emailSoon, icon: "none" });
      }
    },
    async loadStudentCardPreview(keepCurrent = false) {
      const url = this.form.studentCardImageUrl;
      if (!keepCurrent) {
        this.studentCardPreviewSrc = "";
      }
      if (!url)
        return;
      if (/^(wxfile:\/\/|blob:|data:)/i.test(url)) {
        this.studentCardPreviewSrc = url;
        return;
      }
      try {
        this.studentCardPreviewSrc = await utils_api_user.downloadStudentCardImage(url);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/placeholder/verify.vue:244", "download student card failed", error);
      }
    },
    chooseStudentCard() {
      common_vendor.index.chooseImage({
        count: 1,
        sourceType: ["album", "camera"],
        success: async (res) => {
          const filePath = (res.tempFilePaths || [])[0];
          if (!filePath)
            return;
          this.uploading = true;
          common_vendor.index.showLoading({ title: this.texts.uploadLoading, mask: true });
          try {
            const result = await utils_api_user.uploadProfileImage(filePath, "studentCard");
            this.form.studentCardImageUrl = result.url || "";
            this.studentCardPreviewSrc = filePath;
            this.loadStudentCardPreview(true);
            common_vendor.index.showToast({ title: this.texts.uploaded, icon: "success" });
          } catch (error) {
            common_vendor.index.__f__("error", "at pages/placeholder/verify.vue:263", "upload student card failed", error);
          } finally {
            this.uploading = false;
            common_vendor.index.hideLoading();
          }
        }
      });
    },
    removeStudentCard() {
      this.form.studentCardImageUrl = "";
      this.studentCardPreviewSrc = "";
    },
    previewStudentCard() {
      if (!this.studentCardPreviewSrc)
        return;
      common_vendor.index.previewImage({
        urls: [this.studentCardPreviewSrc],
        current: this.studentCardPreviewSrc
      });
    },
    async submitVerify() {
      if (this.submitting || this.verifyMode !== "student_card")
        return;
      if (Number(this.profile.authStatus) === 2) {
        common_vendor.index.showToast({ title: this.texts.currentVerified, icon: "none" });
        return;
      }
      if (Number(this.profile.authStatus) === 1) {
        common_vendor.index.showToast({ title: this.texts.alreadyPending, icon: "none" });
        return;
      }
      if (!this.form.realName.trim()) {
        common_vendor.index.showToast({ title: this.texts.needRealName, icon: "none" });
        return;
      }
      if (!this.form.studentId.trim()) {
        common_vendor.index.showToast({ title: this.texts.needStudentId, icon: "none" });
        return;
      }
      if (!this.form.studentCardImageUrl) {
        common_vendor.index.showToast({ title: this.texts.needCard, icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        const data = await utils_api_user.verifyStudent({
          realName: this.form.realName,
          studentId: this.form.studentId,
          school: this.form.school,
          department: this.form.department,
          studentCardImageUrl: this.form.studentCardImageUrl,
          verifyType: "student_card"
        });
        this.profile = { ...this.profile, ...data || {} };
        common_vendor.index.showToast({ title: this.texts.submitSuccess, icon: "success" });
        setTimeout(() => {
          common_vendor.index.switchTab({ url: "/pages/my/my" });
        }, 500);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/placeholder/verify.vue:320", "verifyStudent failed", error);
      } finally {
        this.submitting = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($data.texts.title),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerHeight + "px",
    g: common_vendor.t($data.texts.heroTitle),
    h: common_vendor.t($data.texts.heroSub),
    i: common_vendor.t($options.statusText),
    j: common_vendor.n($options.statusClass),
    k: common_vendor.f($options.verifyModes, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.title),
        b: common_vendor.t(item.desc),
        c: item.key,
        d: $data.verifyMode === item.key ? 1 : "",
        e: common_vendor.o(($event) => $options.switchMode(item.key), item.key)
      };
    }),
    l: $data.verifyMode === "student_card"
  }, $data.verifyMode === "student_card" ? common_vendor.e({
    m: common_vendor.t($data.texts.realName),
    n: $data.texts.realNamePlaceholder,
    o: $data.form.realName,
    p: common_vendor.o(($event) => $data.form.realName = $event.detail.value),
    q: common_vendor.t($data.texts.studentId),
    r: $data.texts.studentIdPlaceholder,
    s: $data.form.studentId,
    t: common_vendor.o(($event) => $data.form.studentId = $event.detail.value),
    v: common_vendor.t($data.texts.school),
    w: $data.texts.schoolPlaceholder,
    x: $data.form.school,
    y: common_vendor.o(($event) => $data.form.school = $event.detail.value),
    z: common_vendor.t($data.texts.department),
    A: $data.texts.departmentPlaceholder,
    B: $data.form.department,
    C: common_vendor.o(($event) => $data.form.department = $event.detail.value),
    D: common_vendor.t($data.texts.studentCard),
    E: common_vendor.t($data.texts.required),
    F: !$data.form.studentCardImageUrl
  }, !$data.form.studentCardImageUrl ? {
    G: common_vendor.t($data.uploading ? $data.texts.uploading : $data.texts.uploadText),
    H: common_vendor.o((...args) => $options.chooseStudentCard && $options.chooseStudentCard(...args))
  } : common_vendor.e({
    I: $data.studentCardPreviewSrc
  }, $data.studentCardPreviewSrc ? {
    J: $data.studentCardPreviewSrc,
    K: common_vendor.o((...args) => $options.previewStudentCard && $options.previewStudentCard(...args))
  } : {
    L: common_vendor.t($data.texts.previewLoading)
  }, {
    M: common_vendor.t($data.texts.reupload),
    N: common_vendor.o((...args) => $options.chooseStudentCard && $options.chooseStudentCard(...args)),
    O: common_vendor.t($data.texts.remove),
    P: common_vendor.o((...args) => $options.removeStudentCard && $options.removeStudentCard(...args))
  })) : {
    Q: common_vendor.t($data.texts.emailComingTitle),
    R: common_vendor.t($data.texts.emailComingDesc)
  }, {
    S: common_vendor.t($data.texts.tipsTitle),
    T: common_vendor.t($data.texts.tip1),
    U: common_vendor.t($data.texts.tip2),
    V: common_vendor.t($data.texts.tip3),
    W: $data.verifyMode === "student_card"
  }, $data.verifyMode === "student_card" ? {
    X: common_vendor.t($data.submitting ? $data.texts.submitting : $data.texts.submit),
    Y: common_vendor.o((...args) => $options.submitVerify && $options.submitVerify(...args))
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-0e40733d"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/placeholder/verify.js.map
