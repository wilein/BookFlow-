"use strict";
function decodeSafe(value) {
  if (value === void 0 || value === null)
    return "";
  try {
    return decodeURIComponent(String(value));
  } catch (error) {
    return String(value);
  }
}
function toNumber(value, fallback = 0) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : fallback;
}
function parseBool(value, fallback = false) {
  if (typeof value === "boolean")
    return value;
  if (value === "true" || value === "1" || value === 1)
    return true;
  if (value === "false" || value === "0" || value === 0)
    return false;
  return fallback;
}
function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}
function parseImages(rawImages, cover) {
  const imageList = [];
  const text = decodeSafe(rawImages).trim();
  if (text) {
    if (text.startsWith("[") && text.endsWith("]")) {
      try {
        const parsed = JSON.parse(text);
        if (Array.isArray(parsed)) {
          parsed.forEach((item) => item && imageList.push(item));
        }
      } catch (error) {
        text.replace(/^\[|\]$/g, "").split(",").map((item) => item.replace(/"/g, "").trim()).filter(Boolean).forEach((item) => imageList.push(item));
      }
    } else {
      text.split(",").map((item) => item.trim()).filter(Boolean).forEach((item) => imageList.push(item));
    }
  }
  const decodedCover = decodeSafe(cover);
  if (decodedCover)
    imageList.unshift(decodedCover);
  if (imageList.length === 0)
    imageList.push("/static/cover_placeholder.png");
  while (imageList.length < 3)
    imageList.push(imageList[imageList.length - 1]);
  return Array.from(new Set(imageList));
}
function buildLearningPaths(book) {
  return [];
}
function normalizeLearningPaths(value, fallbackBook) {
  if (Array.isArray(value) && value.length) {
    return value.map((item, index) => ({
      id: item.id || item.pathId || "",
      pathId: item.pathId || item.id || "",
      name: decodeSafe(item.name || item.title) || `学习路径 ${index + 1}`,
      title: decodeSafe(item.title || item.name) || `学习路径 ${index + 1}`,
      intro: decodeSafe(item.intro || item.description) || "",
      description: decodeSafe(item.description || item.intro) || "",
      learners: toNumber(item.learners || item.learnerCount, 0),
      nodeCount: toNumber(item.nodeCount || item.bookCount, 0)
    }));
  }
  return buildLearningPaths();
}
function buildConditionLabel(options) {
  const fromText = decodeSafe(options.conditionLabel);
  if (fromText)
    return fromText;
  const condition = toNumber(options.condition, 0);
  if (condition <= 0)
    return "8成新";
  if (condition === 1)
    return "全新";
  if (condition === 2)
    return "9成新";
  if (condition === 3)
    return "8成新";
  if (condition === 4)
    return "7成新";
  return "6成新及以下";
}
function buildBookDetail(options = {}) {
  const status = toNumber(options.status, 1);
  const soldByStatus = status === 3 || status === 4;
  const isSold = parseBool(options.isSold, soldByStatus);
  const title = decodeSafe(options.title) || "未命名书籍";
  const author = decodeSafe(options.author) || "未知作者";
  const category = decodeSafe(options.category || options.categoryName) || "教材";
  const price = toNumber(options.price, 39.9);
  const annotationCount = toNumber(options.annotationCount || options.annotations, 0);
  const resourceCount = toNumber(options.resourceCount, 0);
  const sellerName = decodeSafe(options.sellerName) || "校园书友";
  const sellerScore = clamp(toNumber(options.sellerScore, 4.8), 1, 5);
  return {
    id: decodeSafe(options.id),
    title,
    author,
    publisher: decodeSafe(options.publisher) || `${category}出版社`,
    isbn: decodeSafe(options.isbn) || "978-7-0000-0000-0",
    conditionLabel: buildConditionLabel(options),
    price,
    annotationCount,
    resourceCount,
    status,
    isSold,
    cover: decodeSafe(options.cover) || "/static/cover_placeholder.png",
    images: parseImages(options.images || options.coverImages, options.cover),
    description: decodeSafe(options.description) || `《${title}》适合用于${category}相关课程的日常学习与考试复习。`,
    remark: decodeSafe(options.remark) || "支持校内当面交易。",
    seller: {
      id: decodeSafe(options.sellerId) || "seller-local",
      name: sellerName,
      avatar: decodeSafe(options.sellerAvatar) || "/static/logo.png",
      score: sellerScore
    },
    learningPaths: normalizeLearningPaths(options.learningPaths)
  };
}
function buildBookQueryFromListItem(book = {}) {
  const id = book.id ?? book.bookId ?? book.targetId ?? "";
  const params = [
    `id=${encodeURIComponent(id)}`,
    `title=${encodeURIComponent(book.title || "")}`,
    `author=${encodeURIComponent(book.author || "")}`,
    `publisher=${encodeURIComponent(book.publisher || "")}`,
    `isbn=${encodeURIComponent(book.isbn || "")}`,
    `price=${encodeURIComponent(book.price ?? "")}`,
    `condition=${encodeURIComponent(book.condition ?? "")}`,
    `conditionLabel=${encodeURIComponent(book.conditionLabel || "")}`,
    `annotationCount=${encodeURIComponent(book.annotationCount ?? book.annotations ?? "")}`,
    `resourceCount=${encodeURIComponent(book.resourceCount ?? "")}`,
    `status=${encodeURIComponent(book.status ?? "")}`,
    `isSold=${encodeURIComponent(book.isSold ?? "")}`,
    `category=${encodeURIComponent(book.category || book.categoryName || "")}`,
    `cover=${encodeURIComponent(book.cover || "")}`,
    `images=${encodeURIComponent(book.images || book.coverImages || "")}`,
    `sellerId=${encodeURIComponent(book.sellerId || "")}`,
    `sellerName=${encodeURIComponent(book.sellerName || "")}`,
    `sellerAvatar=${encodeURIComponent(book.sellerAvatar || "")}`,
    `sellerScore=${encodeURIComponent(book.sellerScore ?? "")}`,
    `description=${encodeURIComponent(book.description || "")}`,
    `remark=${encodeURIComponent(book.remark || "")}`
  ];
  return params.join("&");
}
function toPriceText(price) {
  const numberPrice = toNumber(price, 0);
  return numberPrice % 1 === 0 ? String(numberPrice) : numberPrice.toFixed(2);
}
exports.buildBookDetail = buildBookDetail;
exports.buildBookQueryFromListItem = buildBookQueryFromListItem;
exports.toPriceText = toPriceText;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/book-detail.js.map
