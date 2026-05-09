"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_community = require("../../utils/api/community.js");
const utils_auth = require("../../utils/auth.js");
const common_assets = require("../../common/assets.js");
const TEXTS = {
  title: "帖子详情",
  defaultPostTitle: "帖子详情",
  emptyPostContent: "暂无正文内容",
  empty: "还没有回复",
  placeholder: "发表你的回复...",
  send: "发送",
  needContent: "请输入回复内容",
  defaultUser: "校园书友",
  recommend: "推荐",
  review: "书评",
  qa: "问答",
  path: "学习路径",
  like: "点赞",
  reply: "回复",
  replies: "全部回复",
  favorite: "收藏",
  sharedPath: "关联路径",
  nodeUnit: "个节点"
};
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      postId: "",
      postTitle: "",
      post: {},
      comments: [],
      draft: ""
    };
  },
  onLoad(options) {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    this.headerHeight = capsule ? capsule.top + capsule.height + 12 : this.statusBarHeight + 48;
    this.postId = options.postId || "";
    this.postTitle = decodeURIComponent(options.postTitle || TEXTS.defaultPostTitle);
    this.post = {
      title: this.postTitle
    };
    this.fetchPostDetail();
    this.fetchComments();
  },
  methods: {
    formatMeta(school, time) {
      return [school, time].filter(Boolean).join(" / ");
    },
    getPostTypeLabel(type) {
      const map = {
        recommend: TEXTS.recommend,
        review: TEXTS.review,
        qa: TEXTS.qa,
        path: TEXTS.path
      };
      return map[type] || TEXTS.recommend;
    },
    async fetchPostDetail() {
      if (!this.postId)
        return;
      try {
        const data = await utils_api_community.getCommunityPostDetail(this.postId);
        this.post = data || this.post;
        this.postTitle = this.post.title || this.postTitle;
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/comments.vue:147", "getCommunityPostDetail failed", error);
      }
    },
    async fetchComments() {
      if (!this.postId)
        return;
      try {
        this.comments = await utils_api_community.getPostComments(this.postId) || [];
        this.post = { ...this.post, comments: this.comments.length };
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/comments.vue:156", "getPostComments failed", error);
      }
    },
    async toggleLike() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      try {
        const data = await utils_api_community.toggleCommunityLike(this.postId);
        this.post = {
          ...this.post,
          liked: Boolean(data == null ? void 0 : data.liked),
          likes: Number((data == null ? void 0 : data.likeCount) || 0)
        };
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/comments.vue:169", "toggleCommunityLike failed", error);
      }
    },
    async toggleFavorite() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      try {
        const data = await utils_api_community.toggleCommunityFavorite(this.postId);
        this.post = {
          ...this.post,
          favorited: Boolean(data == null ? void 0 : data.favorited),
          favoriteCount: Number((data == null ? void 0 : data.favoriteCount) || 0)
        };
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/comments.vue:182", "toggleCommunityFavorite failed", error);
      }
    },
    openSharedPath(path) {
      if (!path || !path.id)
        return;
      common_vendor.index.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(path.id)}` });
    },
    async submitComment() {
      if (!this.draft.trim()) {
        common_vendor.index.showToast({ title: TEXTS.needContent, icon: "none" });
        return;
      }
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      try {
        const data = await utils_api_community.createPostComment(this.postId, this.draft);
        this.draft = "";
        if (data && data.commentCount !== void 0) {
          this.post = { ...this.post, comments: Number(data.commentCount || 0) };
        }
        this.fetchComments();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/comments.vue:203", "createPostComment failed", error);
      }
    },
    goBack() {
      common_vendor.index.navigateBack({ fail: () => common_vendor.index.switchTab({ url: "/pages/community/community" }) });
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
    g: $data.post.avatar || "/static/logo.png",
    h: common_vendor.t($data.post.author || $data.texts.defaultUser),
    i: common_vendor.t($options.formatMeta($data.post.school, $data.post.time)),
    j: common_vendor.t($options.getPostTypeLabel($data.post.type)),
    k: common_vendor.t($data.post.title || $data.postTitle),
    l: common_vendor.t($data.post.content || $data.texts.emptyPostContent),
    m: $data.post.sharedPath
  }, $data.post.sharedPath ? {
    n: common_vendor.t($data.texts.sharedPath),
    o: common_vendor.t($data.post.sharedPath.title),
    p: common_vendor.t($data.post.sharedPath.difficulty),
    q: common_vendor.t($data.post.sharedPath.totalDuration),
    r: common_vendor.t($data.post.sharedPath.nodeCount || 0),
    s: common_vendor.t($data.texts.nodeUnit),
    t: common_vendor.o(($event) => $options.openSharedPath($data.post.sharedPath))
  } : {}, {
    v: common_vendor.t($data.texts.like),
    w: common_vendor.t($data.post.likes || 0),
    x: $data.post.liked ? 1 : "",
    y: common_vendor.o((...args) => $options.toggleLike && $options.toggleLike(...args)),
    z: common_vendor.t($data.texts.reply),
    A: common_vendor.t($data.post.comments || $data.comments.length || 0),
    B: common_vendor.t($data.texts.favorite),
    C: common_vendor.t($data.post.favoriteCount || 0),
    D: $data.post.favorited ? 1 : "",
    E: common_vendor.o((...args) => $options.toggleFavorite && $options.toggleFavorite(...args)),
    F: common_vendor.t($data.texts.replies),
    G: common_vendor.t($data.comments.length),
    H: $data.comments.length
  }, $data.comments.length ? {
    I: common_vendor.f($data.comments, (item, k0, i0) => {
      return {
        a: item.avatar || "/static/logo.png",
        b: common_vendor.t(item.nickname || item.displayName || $data.texts.defaultUser),
        c: common_vendor.t(item.createTime),
        d: common_vendor.t(item.content),
        e: item.id
      };
    })
  } : {
    J: common_vendor.t($data.texts.empty)
  }, {
    K: $data.texts.placeholder,
    L: $data.draft,
    M: common_vendor.o(($event) => $data.draft = $event.detail.value),
    N: common_vendor.t($data.texts.send),
    O: common_vendor.o((...args) => $options.submitComment && $options.submitComment(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-466a9f85"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/community/comments.js.map
