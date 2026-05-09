"use strict";
const utils_api_request = require("./request.js");
function getCartItems() {
  return utils_api_request.get("/cart/list");
}
function addCartItem(bookId) {
  return utils_api_request.post("/cart/add", { bookId });
}
function removeCartItems(cartItemIds) {
  return utils_api_request.post("/cart/remove", { cartItemIds });
}
exports.addCartItem = addCartItem;
exports.getCartItems = getCartItems;
exports.removeCartItems = removeCartItems;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/cart.js.map
