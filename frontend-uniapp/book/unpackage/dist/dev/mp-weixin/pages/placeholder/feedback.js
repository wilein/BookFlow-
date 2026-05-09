"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_community = require("../../utils/api/community.js");
const utils_api_order = require("../../utils/api/order.js");
const utils_api_user = require("../../utils/api/user.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightSafe: 20,
      submitting: false,
      mode: "generic",
      orderId: "",
      issueId: "",
      postId: "",
      form: {
        feedbackType: "bug",
        issueType: "question",
        reasonType: "other",
        content: "",
        contact: "",
        pagePath: ""
      },
      displayTitle: "",
      textMap: {
        title: "反馈",
        submit: "提交",
        target: "反馈对象",
        type: "反馈类型",
        issueType: "问题类型",
        reasonType: "举报原因",
        content: "内容说明",
        issueReply: "回复内容",
        problemPlaceholder: "请描述具体情况，便于我们处理。",
        replyPlaceholder: "请输入你的回复内容。",
        contact: "联系方式（可选）",
        contactPlaceholder: "微信号、QQ 或手机号",
        page: "当前页面"
      },
      typeOptions: [
        { value: "bug", label: "功能异常" },
        { value: "ux", label: "体验问题" },
        { value: "suggestion", label: "产品建议" }
      ],
      issueTypeOptions: [
        { value: "question", label: "订单疑问" },
        { value: "after_sale", label: "售后处理" }
      ],
      reasonOptions: [
        { value: "fraud", label: "疑似欺诈" },
        { value: "abuse", label: "不当内容" },
        { value: "other", label: "其他原因" }
      ]
    };
  },
  computed: {
    pageTitle() {
      const titleMap = {
        generic: "反馈",
        "order-issue-create": "订单问题",
        "order-issue-reply": "问题回复",
        "order-report": "举报订单",
        "community-report": "举报帖子"
      };
      return titleMap[this.mode] || this.textMap.title;
    },
    submitText() {
      return this.submitting ? "提交中" : this.textMap.submit;
    },
    contentLabel() {
      return this.mode === "order-issue-reply" ? this.textMap.issueReply : this.textMap.content;
    },
    contentPlaceholder() {
      return this.mode === "order-issue-reply" ? this.textMap.replyPlaceholder : this.textMap.problemPlaceholder;
    }
  },
  onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const windowWidth = systemInfo.windowWidth || 375;
      this.headerRightSafe = windowWidth - capsule.left + 10;
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightSafe = 20;
      this.headerHeight = this.statusBarHeight + 54;
    }
    this.mode = options.mode || "generic";
    this.orderId = options.orderId || "";
    this.issueId = options.issueId || "";
    this.postId = options.postId || "";
    this.displayTitle = decodeURIComponent(options.title || "");
    this.form.pagePath = decodeURIComponent(options.pagePath || this.getCurrentPagePath());
    if (options.issueType) {
      this.form.issueType = options.issueType;
    }
  },
  methods: {
    getCurrentPagePath() {
      const pages = getCurrentPages();
      const current = pages[pages.length - 1];
      return (current == null ? void 0 : current.route) ? `/${current.route}` : "/pages/placeholder/feedback";
    },
    goBack() {
      common_vendor.index.navigateBack({
        fail: () => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
        }
      });
    },
    async submit() {
      if (this.submitting)
        return;
      if (!this.form.content.trim()) {
        common_vendor.index.showToast({ title: "请填写内容", icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        if (this.mode === "order-issue-create") {
          await utils_api_order.createOrderIssue({
            orderId: this.orderId,
            type: this.form.issueType,
            content: this.form.content.trim()
          });
        } else if (this.mode === "order-issue-reply") {
          await utils_api_order.replyOrderIssue({
            issueId: this.issueId,
            replyContent: this.form.content.trim()
          });
        } else if (this.mode === "order-report") {
          await utils_api_order.reportOrder({
            orderId: this.orderId,
            reasonType: this.form.reasonType,
            content: this.form.content.trim()
          });
        } else if (this.mode === "community-report") {
          await utils_api_community.reportCommunityPost({
            postId: this.postId,
            reasonType: this.form.reasonType,
            content: this.form.content.trim()
          });
        } else {
          if (!this.form.feedbackType) {
            common_vendor.index.showToast({ title: "请选择反馈类型", icon: "none" });
            return;
          }
          await utils_api_user.submitFeedback({
            feedbackType: this.form.feedbackType,
            content: this.form.content.trim(),
            contact: this.form.contact.trim(),
            pagePath: this.form.pagePath
          });
        }
        common_vendor.index.showToast({ title: "提交成功", icon: "none" });
        setTimeout(() => this.goBack(), 500);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/placeholder/feedback.vue:249", "submit feedback failed", error);
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
    c: common_vendor.t($options.pageTitle),
    d: common_vendor.t($options.submitText),
    e: common_vendor.o((...args) => $options.submit && $options.submit(...args)),
    f: $data.headerHeight + "px",
    g: $data.statusBarHeight + "px",
    h: $data.headerRightSafe + "px",
    i: $data.headerHeight + "px",
    j: $data.displayTitle
  }, $data.displayTitle ? {
    k: common_vendor.t($data.textMap.target)
  } : {}, {
    l: $data.displayTitle
  }, $data.displayTitle ? {
    m: $data.displayTitle
  } : {}, {
    n: $data.mode === "generic"
  }, $data.mode === "generic" ? {
    o: common_vendor.t($data.textMap.type),
    p: common_vendor.f($data.typeOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.form.feedbackType === item.value ? 1 : "",
        d: common_vendor.o(($event) => $data.form.feedbackType = item.value, item.value)
      };
    }),
    q: common_vendor.t($data.textMap.contact),
    r: $data.textMap.contactPlaceholder,
    s: $data.form.contact,
    t: common_vendor.o(($event) => $data.form.contact = $event.detail.value)
  } : {}, {
    v: $data.mode === "order-report" || $data.mode === "community-report"
  }, $data.mode === "order-report" || $data.mode === "community-report" ? {
    w: common_vendor.t($data.textMap.reasonType)
  } : {}, {
    x: $data.mode === "order-report" || $data.mode === "community-report"
  }, $data.mode === "order-report" || $data.mode === "community-report" ? {
    y: common_vendor.f($data.reasonOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.form.reasonType === item.value ? 1 : "",
        d: common_vendor.o(($event) => $data.form.reasonType = item.value, item.value)
      };
    })
  } : {}, {
    z: $data.mode === "order-issue-create"
  }, $data.mode === "order-issue-create" ? {
    A: common_vendor.t($data.textMap.issueType)
  } : {}, {
    B: $data.mode === "order-issue-create"
  }, $data.mode === "order-issue-create" ? {
    C: common_vendor.f($data.issueTypeOptions, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.form.issueType === item.value ? 1 : "",
        d: common_vendor.o(($event) => $data.form.issueType = item.value, item.value)
      };
    })
  } : {}, {
    D: common_vendor.t($options.contentLabel),
    E: $options.contentPlaceholder,
    F: $data.form.content,
    G: common_vendor.o(($event) => $data.form.content = $event.detail.value),
    H: common_vendor.t($data.textMap.page),
    I: $data.form.pagePath,
    J: common_vendor.t($options.submitText),
    K: common_vendor.o((...args) => $options.submit && $options.submit(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-cd8410a3"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/placeholder/feedback.js.map
