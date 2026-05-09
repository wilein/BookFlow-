import { get, post } from './request';

export function getCartItems() {
  return get('/cart/list');
}

export function addCartItem(bookId) {
  return post('/cart/add', { bookId });
}

export function removeCartItems(cartItemIds) {
  return post('/cart/remove', { cartItemIds });
}

export function clearInvalidCartItems() {
  return post('/cart/clear-invalid', {});
}
