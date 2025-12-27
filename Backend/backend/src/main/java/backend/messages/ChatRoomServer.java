package backend.messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import backend.config.SpringContext;
import backend.geminiAI.AIRateLimiter;
import backend.geminiAI.GeminiService;
import backend.notifications.FcmNotificationService;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import backend.authentication.JwtUtil;
import backend.authentication.UserRepository;
import backend.authentication.User;
import backend.logging.ErrorLogger;
import backend.notifications.NotificationService;

/**
 * WebSocket server for direct messaging with JWT authentication and message persistence.
 */
@ServerEndpoint(value = "/chat", configurator = CustomConfigurator.class)
@Component
@EnableScheduling
public class ChatRoomServer {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomServer.class);

    private static JwtUtil jwtUtil;
    private static ChatMessageRepository msgRepo;
    private static UserRepository userRepo;
    private static ErrorLogger errorLogger;
    private static FcmNotificationService fcmService;
    private static NotificationService notificationService;

    // Map of conversationId -> Set of sessions in that conversation
    private static final Map<String, Set<Session>> conversationSessions = new ConcurrentHashMap<>();

    // Map of session -> conversationId
    private static final Map<Session, String> sessionConversations = new ConcurrentHashMap<>();

    // Map of session -> userId (extracted from JWT)
    private static final Map<Session, Long> sessionUserIds = new ConcurrentHashMap<>();

    // Map of session -> username (looked up from User table)
    private static final Map<Session, String> sessionUsernames = new ConcurrentHashMap<>();

    // Map of session -> role (extracted from JWT)
    private static final Map<Session, String> sessionRoles = new ConcurrentHashMap<>();

    // Track all active websocket sessions
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @Autowired
    public void setNotificationService(NotificationService ns) {
        ChatRoomServer.notificationService = ns;
    }

    @Autowired
    public void setFcmService(FcmNotificationService svc) {
        ChatRoomServer.fcmService = svc;
    }

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        ChatRoomServer.jwtUtil = jwtUtil;
    }

    @Autowired
    public void setMessageRepository(ChatMessageRepository repo) {
        ChatRoomServer.msgRepo = repo;
    }

    @Autowired
    public void setUserRepository(UserRepository repo) {
        ChatRoomServer.userRepo = repo;
    }

    @Autowired
    public void setErrorLogger(ErrorLogger errorLogger) {
        ChatRoomServer.errorLogger = errorLogger;
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("[onOpen] Session opened: " + session.getId());

        try {
            String token = extractTokenFromHeaders(session);

            if (token == null || token.isEmpty()) {
                logger.warn("[onOpen] No token provided");
                sendError(session, "Authentication required. Send Authorization header with Bearer token");
                session.close();
                return;
            }

            try {
                if (jwtUtil.isTokenExpired(token)) {
                    logger.warn("[onOpen] Token expired");
                    sendError(session, "Token expired");
                    session.close();
                    return;
                }

                Long userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);

                String username = null;
                if (userRepo != null) {
                    User user = userRepo.findById(userId).orElse(null);
                    if (user != null) {
                        username = user.getUsername();
                    }
                }

                if (username == null) {
                    logger.warn("[onOpen] User not found for userId: " + userId);
                    sendError(session, "User not found");
                    session.close();
                    return;
                }

                sessionUserIds.put(session, userId);
                sessionUsernames.put(session, username);
                sessionRoles.put(session, role);
                sessions.add(session);

                logger.info("[onOpen] Authenticated user: " + username + " (id: " + userId + ", role: " + role + ")");

            } catch (Exception e) {
                logger.error("[onOpen] Invalid token: " + e.getMessage());
                if (errorLogger != null) {
                    errorLogger.logError(e);
                }
                sendError(session, "Invalid or expired token");
                session.close();
            }

        } catch (Exception e) {
            logger.error("[onOpen] Error during authentication: " + e.getMessage());
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
            try {
                session.close();
            } catch (IOException ex) {
                if (errorLogger != null) {
                    errorLogger.logError(ex);
                }
                logger.error("[onOpen] Error closing session: " + ex.getMessage());
            }
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        Long userId = sessionUserIds.get(session);
        String username = sessionUsernames.get(session);
        logger.info("[onMessage] User " + username + " (" + userId + ") sent: " + message);

        if (userId == null || username == null) {
            sendError(session, "Not authenticated");
            return;
        }

        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type", "");

            switch (type) {
                case "CHAT_JOIN":
                    handleJoin(session, json);
                    break;
                case "CHAT_SEND":
                    handleSend(session, json);
                    break;

                case "CALL_OFFER":
                    handleCallOffer(session, json);
                    break;
                case "CALL_ANSWER":
                    handleCallAnswer(session, json);
                    break;
                case "CALL_ICE":
                    handleCallIce(session, json);
                    break;
                case "CALL_END":
                    handleCallEnd(session, json);
                    break;
                case "CALL_READY":
                    handleCallReady(session, json);
                    break;

                default:
                    logger.warn("[onMessage] Unknown message type: " + type);
                    sendError(session, "Unknown message type: " + type);
            }
        } catch (Exception e) {
            logger.error("[onMessage] Error processing message: " + e.getMessage());
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
            sendError(session, "Invalid message format");
        }
    }
    private void handleCallOffer(Session session, JSONObject json) {
        relayCallSignaling(session, json, "CALL_OFFER");

        // When caller sends CALL_OFFER → trigger FCM to wake up callee
        sendIncomingCallFcm(session, json);
    }

    private void handleCallAnswer(Session session, JSONObject json) {
        relayCallSignaling(session, json, "CALL_ANSWER");
    }

    private void handleCallIce(Session session, JSONObject json) {
        relayCallSignaling(session, json, "CALL_ICE");
    }

    private void handleCallEnd(Session session, JSONObject json) {
        relayCallSignaling(session, json, "CALL_END");
    }

    private void handleCallReady(Session session, JSONObject json) {
        relayCallSignaling(session, json, "CALL_READY");
    }

    private void relayCallSignaling(Session session, JSONObject json, String eventName) {
        String conversationId = json.optString("roomId", "");
        String from = json.optString("from", "");
        String username = sessionUsernames.get(session);

        if (conversationId.isEmpty()) {
            sendError(session, "roomId required for " + eventName);
            return;
        }

        if (username == null) {
            sendError(session, "Not authenticated");
            return;
        }

        if (!isUserInConversation(conversationId, username)) {
            logger.warn("[{}] Unauthorized user {} in conversation {}",
                    eventName, username, conversationId);
            sendError(session, "Not authorized");
            return;
        }

        logger.info("[{}] Forwarding from={} room={}", eventName, from, conversationId);

        // Relay original payload to everyone in the room, including sender
        broadcastToConversation(conversationId, json.toString(), null);
    }
    private List<String> getConversationParticipants(String conversationId) {
        if (conversationId == null) return List.of();

        // assistant chats don't allow calls
        if (conversationId.startsWith("assistant&")) return List.of();

        String id = conversationId.startsWith("group-")
                ? conversationId.substring("group-".length())
                : conversationId;

        String[] parts = id.split("-");
        return java.util.Arrays.stream(parts)
                .filter(p -> !p.isBlank())
                .toList();
    }
    private final Map<String, Long> lastFcmCall = new ConcurrentHashMap<>();

    private void sendIncomingCallFcm(Session session, JSONObject json) {
        if (fcmService == null) {
            logger.warn("FCM service not wired");
            return;
        }

        String conversationId = json.optString("roomId");
        String fromUsername = json.optString("from");

        if (conversationId == null || fromUsername == null) return;

        // ===== Prevent FCM spam (10-second cooldown) =====
        long now = System.currentTimeMillis();
        long last = lastFcmCall.getOrDefault(conversationId, 0L);

        if (now - last < 10_000) {
            logger.info("Skipping FCM (cooldown) for conversation {}", conversationId);
            return;
        }

        lastFcmCall.put(conversationId, now);

        List<String> participants = getConversationParticipants(conversationId);

        for (String p : participants) {

            if (p.equalsIgnoreCase(fromUsername)) continue; // skip caller

            // ===== Check if callee already connected via WebSocket =====
            boolean calleeConnected = false;
            Set<Session> sessions = conversationSessions.get(conversationId);

            if (sessions != null) {
                for (Session s : sessions) {
                    String user = sessionUsernames.get(s);
                    if (p.equalsIgnoreCase(user)) {
                        calleeConnected = true;
                        break;
                    }
                }
            }

            if (calleeConnected) {
                logger.info("Skipping FCM: {} already connected via WebSocket", p);
                continue;
            }

            // ===== Lookup callee user =====
            User u = userRepo.findByUsername(p);
            if (u == null) {
                logger.warn("No such user {}", p);
                continue;
            }

            Long calleeId = u.getId();

            Map<String, String> data = new HashMap<>();
            data.put("type", "CALL");
            data.put("conversationId", conversationId);
            data.put("title", "Incoming video call");
            data.put("content", fromUsername + " is calling you");

            logger.info("Sending CALL FCM to user {}", p);
            fcmService.sendToUser(calleeId, "Incoming video call", fromUsername + " is calling you", data);
        }
    }



    @OnClose
    public void onClose(Session session) {
        try {
            logger.info("[onClose] Session closed: " + session.getId());

            cleanupSession(session);
        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("[onError] Session: " + session.getId() + ", Error: " + throwable.getMessage());
        cleanupSession(session);
    }

    private void handleJoin(Session session, JSONObject json) {
        try {
            String conversationId = json.optString("roomId", "");
            String username = sessionUsernames.get(session);
            String role = sessionRoles.get(session);

            if (conversationId.isEmpty()) {
                sendError(session, "conversationId is required");
                return;
            }

            if (!isUserInConversation(conversationId, username)) {
                sendError(session, "You are not authorized to join this conversation");
                logger.warn("[handleJoin] User " + username + " attempted to join unauthorized conversation: " + conversationId);
                return;
            }

            String previousConversation = sessionConversations.get(session);
            if (previousConversation != null) {
                Set<Session> previousSessions = conversationSessions.get(previousConversation);
                if (previousSessions != null) {
                    previousSessions.remove(session);
                }
            }

            sessionConversations.put(session, conversationId);
            Set<Session> sessions = conversationSessions.get(conversationId);
            if (sessions == null) {
                sessions = new CopyOnWriteArraySet<>();
                conversationSessions.put(conversationId, sessions);
            }
            sessions.add(session);

            logger.info("[handleJoin] User " + username + " (role: " + role + ") joined conversation: " + conversationId);

            JSONObject joinConfirm = new JSONObject();
            joinConfirm.put("type", "CHAT_JOINED");
            joinConfirm.put("conversationId", conversationId);
            joinConfirm.put("message", "Successfully joined conversation");
            session.getBasicRemote().sendText(joinConfirm.toString());

        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
            sendError(session, "Error joining conversation");
        }
    }

    private void handleSend(Session session, JSONObject json) {
        try {
            String conversationId = json.optString("roomId", "");
            String content = json.optString("content", "");
            Long userId = sessionUserIds.get(session);
            String username = sessionUsernames.get(session);
            String role = sessionRoles.get(session);

            if (conversationId.isEmpty()) {
                sendError(session, "conversationId is required");
                return;
            }

            if (content.isEmpty()) {
                sendError(session, "content is required");
                return;
            }

            // sanitize user content for DB (remove emoji and other non-BMP)
            content = sanitizeContent(content);

            String currentConversation = sessionConversations.get(session);
            if (!conversationId.equals(currentConversation)) {
                sendError(session, "You are not in conversation: " + conversationId);
                return;
            }

            if (!isUserInConversation(conversationId, username)) {
                sendError(session, "You are not authorized to send messages in this conversation");
                return;
            }

            logger.info("[handleSend] User " + username + " in conversation " + conversationId + ": " + content);

            // Save user message
            JSONObject broadcast = new JSONObject();
            broadcast.put("type", "CHAT_MESSAGE");
            broadcast.put("content", content);
            broadcast.put("userId", userId);
            broadcast.put("sender", username);
            broadcast.put("role", role);

            if (msgRepo != null) {
                try {
                    ChatMessage chatMessage = new ChatMessage(conversationId, username, role, content);
                    msgRepo.save(chatMessage);
                    broadcast.put("timestamp", chatMessage.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e) {
                    logger.error("[handleSend] Error saving message to database: " + e.getMessage());
                    if (errorLogger != null) {
                        errorLogger.logError(e);
                    }
                }
            }

            // Send user's message out
            broadcastToConversation(conversationId, broadcast.toString(), session);

            notifyChatRecipients(conversationId, username, content);

            // ====== AI HANDLING (per-user assistant) ======
            if (conversationId.equals("assistant&" + username)) {

                AIRateLimiter limiter = SpringContext.getBean(AIRateLimiter.class);
                if (!limiter.allow(username)) {
                    sendAIMessage(conversationId,
                            "You are sending messages too quickly. Please wait about a minute before asking again.");
                    return;
                }

                GeminiService ai = SpringContext.getBean(GeminiService.class);
                String aiResponse = ai.askGemini(content, username);

                // sanitize AI response before saving to DB
                aiResponse = sanitizeContent(aiResponse);

                sendAIMessage(conversationId, aiResponse);
            }

        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
            sendError(session, "Error sending message");
        }
    }

    private void sendAIMessage(String conversationId, String content) {
        try {
            if (msgRepo == null) {
                return;
            }

            ChatMessage aiMessage = new ChatMessage(conversationId, "Assistant", "AI", content);
            msgRepo.save(aiMessage);

            JSONObject aiJson = new JSONObject();
            aiJson.put("type", "CHAT_MESSAGE");
            aiJson.put("content", content);
            aiJson.put("sender", "Assistant");
            aiJson.put("role", "AI");
            aiJson.put("timestamp", aiMessage.getTimestamp().toString());

            broadcastToConversation(conversationId, aiJson.toString(), null);
        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
        }
    }

    private void sendChatHistory(Session session, String conversationId) {
        try {
            if (msgRepo == null) {
                logger.warn("[sendChatHistory] Repository not available, skipping chat history");
                return;
            }

            List<ChatMessage> messages = msgRepo.findByConversationIdOrderByTimestampAsc(conversationId);

            if (messages == null || messages.isEmpty()) {
                logger.info("[sendChatHistory] No messages found for conversation: " + conversationId);
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            for (ChatMessage msg : messages) {
                JSONObject historyMsg = new JSONObject();
                historyMsg.put("type", "CHAT_HISTORY");
                historyMsg.put("text", msg.getContent());
                historyMsg.put("sender", msg.getSender());
                historyMsg.put("role", msg.getRole());
                historyMsg.put("timestamp", msg.getTimestamp().format(formatter));

                session.getBasicRemote().sendText(historyMsg.toString());
            }

            logger.info("[sendChatHistory] Sent " + messages.size() + " messages to user in conversation: " + conversationId);

        } catch (Exception e) {
            logger.error("[sendChatHistory] Error sending chat history: " + e.getMessage());
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
        }
    }

    private void broadcastToConversation(String conversationId, String message, Session excludeSession) {
        try {
            Set<Session> sessions = conversationSessions.get(conversationId);
            if (sessions == null) {
                return;
            }

            for (Session s : new java.util.HashSet<>(sessions)) {
                if (s == excludeSession) continue;
                if (!s.isOpen()) {
                    sessions.remove(s);
                    continue;
                }
                try {
                    s.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    logger.error("[broadcastToConversation] Error sending to session: " + e.getMessage());
                    if (errorLogger != null) {
                        errorLogger.logError(e);
                    }
                    cleanupSession(s);
                }
            }
        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        logger.info("[HEARTBEAT] Checking {} sessions", sessions.size());

        for (Session session : new java.util.HashSet<>(sessions)) {
            if (session.isOpen()) {
                try {
                    session.getAsyncRemote().sendPing(ByteBuffer.wrap(new byte[]{1}));
                } catch (Exception e) {
                    logger.warn("Ping failed for session {}: {}", session.getId(), e.getMessage());
                }
            } else {
                cleanupSession(session);
            }
        }
    }


    public static void broadcastAttachment(Long messageId, String conversationId, String sender, String content, String filename) {
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("type", "CHAT_MESSAGE");
            json.put("id", messageId);
            json.put("sender", sender);
            json.put("content", content);
            json.put("hasAttachment", true);
            json.put("attachmentFilename", filename);

            json.put("timestamp", java.time.LocalDateTime.now().toString());

            Set<Session> sessions = conversationSessions.get(conversationId);
            if (sessions != null) {
                for (Session s : sessions) {
                    if (s.isOpen()) {
                        s.getBasicRemote().sendText(json.toString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to broadcast attachment", e);
        }
    }


    /**
     * Check if a user is part of a conversation.
     * Handles:
     *  - direct messages (user1-user2)
     *  - groups (group-user1-user2-user3)
     *  - AI assistant chats (assistant&username)
     */
    private boolean isUserInConversation(String conversationId, String username) {
        if (conversationId == null || username == null) {
            return false;
        }

        // Special case: personal AI assistant room
        if (conversationId.equals("assistant&" + username)) {
            return true;
        }

        String participantsPart;
        if (conversationId.startsWith("group-")) {
            participantsPart = conversationId.substring("group-".length());
        } else {
            participantsPart = conversationId;
        }

        String[] participants = participantsPart.split("-");

        if (participants.length < 2) {
            return false;
        }

        for (String participant : participants) {
            if (participant.equalsIgnoreCase(username)) {
                return true;
            }
        }

        return false;
    }

    private void sendError(Session session, String errorMessage) {
        try {
            JSONObject error = new JSONObject();
            error.put("type", "ERROR");
            error.put("text", errorMessage);
            session.getBasicRemote().sendText(error.toString());
        } catch (Exception e) {
            logger.error("[sendError] Error sending error message: " + e.getMessage());
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
        }
    }

    private String extractTokenFromHeaders(Session session) {
        try {
            Object tokenObj = session.getUserProperties().get("token");
            if (tokenObj != null) {
                String token = tokenObj.toString().trim();
                if (!token.isEmpty()) {
                    return token;
                }
            }
            return null;
        } catch (Exception e) {
            if (errorLogger != null) {
                errorLogger.logError(e);
            }
            return null;
        }
    }

    /**
     * Remove all non-BMP characters (such as emoji) from text
     * so MySQL with utf8 (3-byte) encoding does not throw errors.
     */
    private String sanitizeContent(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        input.codePoints().forEach(cp -> {
            if (cp <= 0xFFFF) {
                sb.append((char) cp);
            }
        });
        return sb.toString();
    }

    private void cleanupSession(Session session) {
        if (session == null) return;

        sessions.remove(session);
        sessionUserIds.remove(session);
        sessionUsernames.remove(session);
        sessionRoles.remove(session);

        String conversationId = sessionConversations.remove(session);
        if (conversationId != null) {
            Set<Session> convSessions = conversationSessions.get(conversationId);
            if (convSessions != null) {
                convSessions.remove(session);
                if (convSessions.isEmpty()) {
                    conversationSessions.remove(conversationId);
                    logger.info("[cleanupSession] Conversation removed (empty): {}", conversationId);
                }
            }
        }
    }

    private void notifyChatRecipients(String conversationId, String senderUsername, String messageContent) {
        if (notificationService == null || userRepo == null) {
            logger.warn("Cannot send notification: Services not injected");
            return;
        }
        List<String> participants = getConversationParticipants(conversationId);
        Set<Session> activeSessionsInRoom = conversationSessions.getOrDefault(conversationId, Set.of());

        for (String participantUsername : participants) {
            if (participantUsername.equalsIgnoreCase(senderUsername)) continue;

            boolean isLookingAtChat = activeSessionsInRoom.stream()
                    .anyMatch(s -> participantUsername.equalsIgnoreCase(sessionUsernames.get(s)));

            if (isLookingAtChat) {
                continue;
            }

            User recipient = userRepo.findByUsername(participantUsername);
            if (recipient == null) continue;

            String preview = messageContent.length() > 50
                    ? messageContent.substring(0, 47) + "..."
                    : messageContent;

            notificationService.saveNotificationToUser(
                    recipient,
                    "New message from " + senderUsername,
                    preview,
                    "CHAT_MESSAGE",
                    conversationId
            );
        }
    }
}
