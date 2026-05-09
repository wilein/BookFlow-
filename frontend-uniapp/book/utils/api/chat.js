import { get, post } from './request';

export function getChatSessions() {
  return get('/chat/session/list');
}

export function openChatSession(data) {
  return post('/chat/session/open', data);
}

export function getChatMessages(sessionId) {
  return get('/chat/message/list', { sessionId });
}

export function pollChatMessages(sessionId, afterId = 0) {
  return get('/chat/message/poll', { sessionId, afterId });
}

export function markChatRead(sessionId) {
  return post('/chat/message/read', { sessionId });
}

export function sendChatMessage(data) {
  return post('/chat/message/send', data);
}
