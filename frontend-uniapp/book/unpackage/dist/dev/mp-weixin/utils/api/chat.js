"use strict";
const utils_api_request = require("./request.js");
function getChatSessions() {
  return utils_api_request.get("/chat/session/list");
}
function openChatSession(data) {
  return utils_api_request.post("/chat/session/open", data);
}
function getChatMessages(sessionId) {
  return utils_api_request.get("/chat/message/list", { sessionId });
}
function pollChatMessages(sessionId, afterId = 0) {
  return utils_api_request.get("/chat/message/poll", { sessionId, afterId });
}
function markChatRead(sessionId) {
  return utils_api_request.post("/chat/message/read", { sessionId });
}
function sendChatMessage(data) {
  return utils_api_request.post("/chat/message/send", data);
}
exports.getChatMessages = getChatMessages;
exports.getChatSessions = getChatSessions;
exports.markChatRead = markChatRead;
exports.openChatSession = openChatSession;
exports.pollChatMessages = pollChatMessages;
exports.sendChatMessage = sendChatMessage;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/api/chat.js.map
