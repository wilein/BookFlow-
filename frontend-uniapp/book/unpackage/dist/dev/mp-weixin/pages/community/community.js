"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api_chat = require("../../utils/api/chat.js");
const utils_api_community = require("../../utils/api/community.js");
const utils_api_path = require("../../utils/api/path.js");
const utils_auth = require("../../utils/auth.js");
const TEXTS = {
  communitySquare: "社区广场",
  headerSub: "校园书籍交易、交流与学习动态",
  community: "社区",
  activity: "动态",
  chat: "聊天",
  recommend: "推荐",
  review: "书评",
  qa: "问答",
  path: "学习路径",
  like: "点赞",
  comment: "回复",
  favorite: "收藏",
  report: "举报",
  sharedPath: "关联路径",
  nodeUnit: "个节点",
  learnersUnit: "人在学",
  emptyCommunity: "暂无社区内容",
  emptyPath: "暂无发布的学习路径",
  emptyPathDesc: "暂无路径说明",
  defaultDifficulty: "入门",
  recentActivity: "最近动态",
  postCount: "帖子",
  activityCount: "动态",
  sessionCount: "会话",
  emptyActivity: "暂无动态",
  emptyChat: "暂无聊天记录",
  aboutBook: "关于《",
  bookSuffix: "》",
  unknownBook: "未知书籍",
  defaultUser: "校园书友",
  defaultSeller: "书籍卖家",
  loading: "加载中...",
  retry: "重试",
  loadFailed: "加载失败",
  needLoginChat: "登录后查看聊天",
  goLogin: "去登录"
};
const _sfc_main = {
  data() {
    return {
      texts: TEXTS,
      statusBarHeight: 0,
      headerHeight: 0,
      headerRightPadding: 20,
      activeTopTab: "community",
      activeCommunityTab: "recommend",
      topTabs: [
        { label: TEXTS.community, value: "community" },
        { label: TEXTS.activity, value: "activity" },
        { label: TEXTS.chat, value: "chat" }
      ],
      communityTabs: [
        { label: TEXTS.recommend, value: "recommend" },
        { label: TEXTS.review, value: "review" },
        { label: TEXTS.qa, value: "qa" },
        { label: TEXTS.path, value: "path" }
      ],
      communityPosts: [],
      publicPaths: [],
      activityList: [],
      chatSessions: [],
      isLoggedIn: false,
      feedLoading: false,
      feedError: "",
      pathLoading: false,
      pathError: "",
      activityLoading: false,
      activityError: "",
      chatLoading: false,
      chatError: ""
    };
  },
  computed: {
    filteredCommunityPosts() {
      if (this.activeCommunityTab === "recommend")
        return this.communityPosts;
      return this.communityPosts.filter((item) => item.type === this.activeCommunityTab);
    }
  },
  onLoad() {
    const systemInfo = common_vendor.index.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    const capsule = typeof common_vendor.index.getMenuButtonBoundingClientRect === "function" ? common_vendor.index.getMenuButtonBoundingClientRect() : null;
    if (capsule) {
      const screenWidth = systemInfo.windowWidth || 375;
      this.headerRightPadding = Math.max(20, screenWidth - capsule.left + 10);
      this.headerHeight = capsule.top + capsule.height + 12;
    } else {
      this.headerRightPadding = 20;
      this.headerHeight = this.statusBarHeight + 48;
    }
    this.applyInitialTabIntent();
    this.isLoggedIn = utils_auth.hasValidSession();
    this.fetchCommunityFeeds();
    this.fetchActivities();
    if (this.isLoggedIn && this.activeTopTab === "chat") {
      this.fetchChats();
    }
  },
  onShow() {
    this.applyInitialTabIntent();
    this.isLoggedIn = utils_auth.hasValidSession();
    if (this.activeTopTab === "community") {
      if (this.activeCommunityTab === "path") {
        this.fetchPublicPaths();
      } else {
        this.fetchCommunityFeeds();
      }
    } else if (this.activeTopTab === "chat" && this.isLoggedIn) {
      this.fetchChats();
    }
  },
  methods: {
    formatMeta(school, time) {
      return [school, time].filter(Boolean).join(" / ");
    },
    switchTopTab(tab) {
      if (tab === "chat" && !utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      this.activeTopTab = tab;
      if (tab === "community") {
        if (this.activeCommunityTab === "path") {
          this.fetchPublicPaths();
        } else {
          this.fetchCommunityFeeds();
        }
      }
      if (tab === "activity")
        this.fetchActivities();
      if (tab === "chat")
        this.fetchChats();
    },
    switchCommunityTab(tab) {
      this.activeCommunityTab = tab;
      if (tab === "path") {
        this.fetchPublicPaths();
      } else {
        this.fetchCommunityFeeds();
      }
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
    applyInitialTabIntent() {
      const initialTab = common_vendor.index.getStorageSync("communityInitialTab");
      if (!initialTab)
        return;
      common_vendor.index.removeStorageSync("communityInitialTab");
      if (["community", "activity", "chat"].includes(initialTab)) {
        this.activeTopTab = initialTab;
      }
    },
    handlePublish() {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      common_vendor.index.navigateTo({ url: "/pages/community/create" });
    },
    openPost(post) {
      if (post && post.type === "path" && post.sharedPath && post.sharedPath.id) {
        this.openSharedPath(post.sharedPath);
        return;
      }
      this.openComments(post);
    },
    openComments(post) {
      common_vendor.index.navigateTo({
        url: `/pages/community/comments?postId=${encodeURIComponent(post.id || "")}&postTitle=${encodeURIComponent(post.title || "")}`
      });
    },
    openSharedPath(path) {
      if (!path || !path.id)
        return;
      common_vendor.index.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(path.id)}` });
    },
    openLearningPath(path) {
      const id = path.pathId || path.id;
      if (!id)
        return;
      common_vendor.index.navigateTo({ url: `/pages/path/detail?pathId=${encodeURIComponent(id)}` });
    },
    openChat(item) {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      const params = [
        `sessionId=${encodeURIComponent(item.sessionId || item.id || "")}`,
        `sellerId=${encodeURIComponent(item.sellerId || "")}`,
        `sellerName=${encodeURIComponent(item.name || "")}`,
        `bookId=${encodeURIComponent(item.bookId || "")}`,
        `bookTitle=${encodeURIComponent(item.bookTitle || "")}`
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/chat/chat?${params}` });
    },
    async toggleLike(post) {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      try {
        const data = await utils_api_community.toggleCommunityLike(post.id);
        post.liked = Boolean(data == null ? void 0 : data.liked);
        post.likes = Number((data == null ? void 0 : data.likeCount) || 0);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/community.vue:399", "toggleCommunityLike failed", error);
      }
    },
    async toggleFavorite(post) {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      try {
        const data = await utils_api_community.toggleCommunityFavorite(post.id);
        post.favorited = Boolean(data == null ? void 0 : data.favorited);
        post.favoriteCount = Number((data == null ? void 0 : data.favoriteCount) || 0);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/community.vue:409", "toggleCommunityFavorite failed", error);
      }
    },
    reportPost(post) {
      if (!utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl()))
        return;
      const query = [
        "mode=community-report",
        `postId=${encodeURIComponent(post.id || "")}`,
        `title=${encodeURIComponent(post.title || "")}`,
        `pagePath=${encodeURIComponent("/pages/community/community")}`
      ].join("&");
      common_vendor.index.navigateTo({ url: `/pages/placeholder/feedback?${query}` });
    },
    async fetchCommunityFeeds() {
      this.feedLoading = true;
      this.feedError = "";
      try {
        const type = this.activeCommunityTab === "recommend" ? "" : this.activeCommunityTab;
        this.communityPosts = await utils_api_community.getCommunityFeed(type) || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/community.vue:429", "fetchCommunityFeeds failed", error);
        this.feedError = (error == null ? void 0 : error.message) || TEXTS.loadFailed;
      } finally {
        this.feedLoading = false;
      }
    },
    async fetchPublicPaths() {
      this.pathLoading = true;
      this.pathError = "";
      try {
        this.publicPaths = await utils_api_path.getPathList({ category: "", keyword: "" }) || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/community.vue:441", "fetchPublicPaths failed", error);
        this.pathError = (error == null ? void 0 : error.message) || TEXTS.loadFailed;
      } finally {
        this.pathLoading = false;
      }
    },
    async fetchActivities() {
      this.activityLoading = true;
      this.activityError = "";
      try {
        this.activityList = await utils_api_community.getCommunityActivity() || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/community.vue:453", "fetchActivities failed", error);
        this.activityError = (error == null ? void 0 : error.message) || TEXTS.loadFailed;
      } finally {
        this.activityLoading = false;
      }
    },
    async fetchChats() {
      if (!utils_auth.hasValidSession()) {
        this.isLoggedIn = false;
        this.chatSessions = [];
        return;
      }
      this.isLoggedIn = true;
      this.chatLoading = true;
      this.chatError = "";
      try {
        this.chatSessions = await utils_api_chat.getChatSessions() || [];
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/community/community.vue:471", "fetchChats failed", error);
        this.chatError = (error == null ? void 0 : error.message) || TEXTS.loadFailed;
      } finally {
        this.chatLoading = false;
      }
    },
    goLoginForChat() {
      utils_auth.ensureLoggedIn(utils_auth.getCurrentPageUrl());
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.texts.communitySquare),
    b: common_vendor.t($data.texts.headerSub),
    c: common_vendor.o((...args) => $options.handlePublish && $options.handlePublish(...args)),
    d: $data.headerHeight + "px",
    e: $data.statusBarHeight + "px",
    f: $data.headerRightPadding + "px",
    g: $data.headerHeight + "px",
    h: common_vendor.f($data.topTabs, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.activeTopTab === item.value ? 1 : "",
        d: common_vendor.o(($event) => $options.switchTopTab(item.value), item.value)
      };
    }),
    i: $data.activeTopTab === "community"
  }, $data.activeTopTab === "community" ? {
    j: common_vendor.f($data.communityTabs, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: $data.activeCommunityTab === item.value ? 1 : "",
        d: common_vendor.o(($event) => $options.switchCommunityTab(item.value), item.value)
      };
    })
  } : {}, {
    k: $data.activeTopTab === "community"
  }, $data.activeTopTab === "community" ? common_vendor.e({
    l: $data.activeCommunityTab === "path"
  }, $data.activeCommunityTab === "path" ? common_vendor.e({
    m: $data.pathLoading
  }, $data.pathLoading ? {
    n: common_vendor.t($data.texts.loading)
  } : $data.pathError ? {
    p: common_vendor.t($data.pathError),
    q: common_vendor.t($data.texts.retry),
    r: common_vendor.o((...args) => $options.fetchPublicPaths && $options.fetchPublicPaths(...args))
  } : $data.publicPaths.length ? {
    t: common_vendor.f($data.publicPaths, (path, k0, i0) => {
      return common_vendor.e({
        a: path.coverImage || path.cover
      }, path.coverImage || path.cover ? {
        b: path.coverImage || path.cover
      } : {}, {
        c: common_vendor.t(path.title),
        d: common_vendor.t(path.category || $data.texts.path),
        e: common_vendor.t(path.description || $data.texts.emptyPathDesc),
        f: common_vendor.t(path.creator || $data.texts.defaultUser),
        g: common_vendor.t(path.difficulty || $data.texts.defaultDifficulty),
        h: common_vendor.t(path.nodeCount || 0),
        i: common_vendor.t(path.learnerCount || path.learners || 0),
        j: path.id,
        k: common_vendor.o(($event) => $options.openLearningPath(path), path.id)
      });
    }),
    v: common_vendor.t($data.texts.nodeUnit),
    w: common_vendor.t($data.texts.learnersUnit)
  } : {
    x: common_vendor.t($data.texts.emptyPath)
  }, {
    o: $data.pathError,
    s: $data.publicPaths.length
  }) : $data.feedLoading ? {
    z: common_vendor.t($data.texts.loading)
  } : $data.feedError ? {
    B: common_vendor.t($data.feedError),
    C: common_vendor.t($data.texts.retry),
    D: common_vendor.o((...args) => $options.fetchCommunityFeeds && $options.fetchCommunityFeeds(...args))
  } : $options.filteredCommunityPosts.length ? {
    F: common_vendor.f($options.filteredCommunityPosts, (post, k0, i0) => {
      return common_vendor.e({
        a: post.avatar || "/static/logo.png",
        b: common_vendor.t(post.author || $data.texts.defaultUser),
        c: common_vendor.t($options.formatMeta(post.school, post.time)),
        d: common_vendor.t(post.title),
        e: common_vendor.t($options.getPostTypeLabel(post.type)),
        f: common_vendor.t(post.content),
        g: post.sharedPath
      }, post.sharedPath ? common_vendor.e({
        h: post.sharedPath.coverImage || post.sharedPath.cover
      }, post.sharedPath.coverImage || post.sharedPath.cover ? {
        i: post.sharedPath.coverImage || post.sharedPath.cover
      } : {}, {
        j: common_vendor.t($data.texts.sharedPath),
        k: common_vendor.t(post.sharedPath.title),
        l: common_vendor.t(post.sharedPath.difficulty),
        m: common_vendor.t(post.sharedPath.totalDuration),
        n: common_vendor.t(post.sharedPath.nodeCount || 0),
        o: common_vendor.t($data.texts.nodeUnit),
        p: common_vendor.o(($event) => $options.openSharedPath(post.sharedPath), post.id)
      }) : {}, {
        q: (post.tags || []).length
      }, (post.tags || []).length ? {
        r: common_vendor.f(post.tags || [], (tag, k1, i1) => {
          return {
            a: common_vendor.t(tag),
            b: tag
          };
        })
      } : {}, {
        s: common_vendor.t(post.likes || 0),
        t: post.liked ? 1 : "",
        v: common_vendor.o(($event) => $options.toggleLike(post), post.id),
        w: common_vendor.t(post.comments || 0),
        x: common_vendor.o(($event) => $options.openComments(post), post.id),
        y: common_vendor.t(post.favoriteCount || 0),
        z: post.favorited ? 1 : "",
        A: common_vendor.o(($event) => $options.toggleFavorite(post), post.id),
        B: common_vendor.o(($event) => $options.reportPost(post), post.id),
        C: post.id,
        D: common_vendor.o(($event) => $options.openPost(post), post.id)
      });
    }),
    G: common_vendor.t($data.texts.like),
    H: common_vendor.t($data.texts.comment),
    I: common_vendor.t($data.texts.favorite),
    J: common_vendor.t($data.texts.report)
  } : {
    K: common_vendor.t($data.texts.emptyCommunity)
  }, {
    y: $data.feedLoading,
    A: $data.feedError,
    E: $options.filteredCommunityPosts.length
  }) : $data.activeTopTab === "activity" ? common_vendor.e({
    M: $data.activityLoading
  }, $data.activityLoading ? {
    N: common_vendor.t($data.texts.loading)
  } : $data.activityError ? {
    P: common_vendor.t($data.activityError),
    Q: common_vendor.t($data.texts.retry),
    R: common_vendor.o((...args) => $options.fetchActivities && $options.fetchActivities(...args))
  } : common_vendor.e({
    S: common_vendor.t($data.texts.recentActivity),
    T: common_vendor.t($data.communityPosts.length),
    U: common_vendor.t($data.texts.postCount),
    V: common_vendor.t($data.activityList.length),
    W: common_vendor.t($data.texts.activityCount),
    X: common_vendor.t($data.chatSessions.length),
    Y: common_vendor.t($data.texts.sessionCount),
    Z: $data.activityList.length
  }, $data.activityList.length ? {
    aa: common_vendor.f($data.activityList, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.title),
        b: common_vendor.t(item.desc),
        c: common_vendor.t(item.time),
        d: item.id
      };
    })
  } : {
    ab: common_vendor.t($data.texts.emptyActivity)
  }), {
    O: $data.activityError
  }) : common_vendor.e({
    ac: !$data.isLoggedIn
  }, !$data.isLoggedIn ? {
    ad: common_vendor.t($data.texts.needLoginChat),
    ae: common_vendor.t($data.texts.goLogin),
    af: common_vendor.o((...args) => $options.goLoginForChat && $options.goLoginForChat(...args))
  } : $data.chatLoading ? {
    ah: common_vendor.t($data.texts.loading)
  } : $data.chatError ? {
    aj: common_vendor.t($data.chatError),
    ak: common_vendor.t($data.texts.retry),
    al: common_vendor.o((...args) => $options.fetchChats && $options.fetchChats(...args))
  } : $data.chatSessions.length ? {
    an: common_vendor.f($data.chatSessions, (item, k0, i0) => {
      return common_vendor.e({
        a: item.avatar || "/static/logo.png",
        b: common_vendor.t(item.name || $data.texts.defaultSeller),
        c: common_vendor.t(item.time),
        d: common_vendor.t(item.bookTitle || $data.texts.unknownBook),
        e: common_vendor.t(item.preview),
        f: item.unread > 0
      }, item.unread > 0 ? {
        g: common_vendor.t(item.unread)
      } : {}, {
        h: item.id,
        i: common_vendor.o(($event) => $options.openChat(item), item.id)
      });
    }),
    ao: common_vendor.t($data.texts.aboutBook),
    ap: common_vendor.t($data.texts.bookSuffix)
  } : {
    aq: common_vendor.t($data.texts.emptyChat)
  }, {
    ag: $data.chatLoading,
    ai: $data.chatError,
    am: $data.chatSessions.length
  }), {
    L: $data.activeTopTab === "activity"
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-a6ef5318"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/community/community.js.map
