package backend.notifications;

import backend.authentication.JwtUtil;
import backend.logging.ErrorLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
@ServerEndpoint(value = "/notif", configurator = NotificationConfigurator.class)
@Component
public class NotificationEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEndpoint.class);

    private static final Map<Long, Set<Session>> userSessions = new ConcurrentHashMap<>();
    private static final Map<Session, Long> sessionUserMap = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static JwtUtil jwtUtil;

    @Autowired
    public void setJwtUtil(JwtUtil util) {
        NotificationEndpoint.jwtUtil = util;
    }

    // ===================== WebSocket Lifecycle =====================

    @OnOpen
    public void onOpen(Session session) {
        try {
            session.setMaxIdleTimeout(0); // never timeout
            Long userId = extractUserId(session);
            if (userId != null) {
                userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
                sessionUserMap.put(session, userId);
                logger.info("Notifications WS connected: userId={} session={}", userId, session.getId());
            } else {
                logger.warn("Notifications WS closing session {} due to invalid/missing token", session.getId());
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token"));
            }
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
        }
    }

    @OnMessage
    public void onMessage(Session session, String msg) {
        try {
            Map<String, Object> ack = Map.of(
                    "type", "ack",
                    "timestamp", System.currentTimeMillis()
            );
            session.getAsyncRemote().sendText(mapper.writeValueAsString(ack));
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        try {
            Long userId = sessionUserMap.remove(session);
            if (userId != null) {
                Set<Session> sessions = userSessions.get(userId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        userSessions.remove(userId);
                    }
                }
            }
            logger.info("Notifications WS closed: userId={} session={}", userId, session.getId());
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);

        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        try {
            String msg = (throwable != null && throwable.getMessage() != null)
                    ? throwable.getMessage()
                    : (throwable != null ? throwable.getClass().getSimpleName() : "Unknown error");
            logger.error("Notifications WS error for session {}: {}", session != null ? session.getId() : "n/a", msg, throwable);
            cleanupSession(session);
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
        }
    }

    // ===================== Utility Methods =====================

    private Long extractUserId(Session session) {
        try {
            if (jwtUtil == null) {
                logger.error("Notifications WS JwtUtil not injected; rejecting connection");
                return null;
            }

            Object tokenObj = session.getUserProperties().get("token");
            if (tokenObj instanceof String token) {
                token = token.trim();
                if (!token.isEmpty() && !jwtUtil.isTokenExpired(token)) {
                    return jwtUtil.extractUserId(token);
                } else {
                    logger.warn("Notifications WS rejected session {} due to missing/expired token", session.getId());
                }
            }
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
        }
        return null;
    }

    // ===================== Notification Dispatch =====================

    public static void sendToUser(Long userId, String type, String title, String content) {
        try {
            Set<Session> sessions = userSessions.get(userId);
            if (!hasOpenSession(userId, sessions)) {
                return;
            }
            send(sessions, type, title, content);
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
        }
    }

    public static void broadcast(String type, String title, String content) {
        try {
            userSessions.values().forEach(sessions -> send(sessions, type, title, content));
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
        }
    }

    private static void send(Set<Session> sessions, String type, String title, String content) {
        if (sessions == null) return;
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "type", type,
                    "title", title,
                    "content", content
            ));

            for (Session s : new HashSet<>(sessions)) { // avoid concurrent modification
                if (s.isOpen()) {
                    s.getAsyncRemote().sendText(json, result -> {
                        if (!result.isOK()) {
                            ErrorLogger.logErrorStatic(new Exception(
                                    "Failed to send message: " + result.getException()
                            ));
                        }
                    });
                } else {
                    sessions.remove(s); // cleanup closed sessions
                }
            }
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
        }
    }

    public static Set<Long> getActiveUsers() {
        return userSessions.keySet();
    }

    /**
     * Returns true when the user has at least one open session; cleans up closed sessions on the fly.
     */
    public static boolean isUserActive(Long userId) {
        if (userId == null) return false;
        Set<Session> sessions = userSessions.get(userId);
        return hasOpenSession(userId, sessions);
    }

    private static boolean hasOpenSession(Long userId, Set<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }

        boolean anyOpen = false;
        for (Session s : new HashSet<>(sessions)) {
            if (s.isOpen()) {
                anyOpen = true;
            } else {
                sessions.remove(s);
            }
        }

        if (!anyOpen && userId != null) {
            userSessions.remove(userId);
        }
        return anyOpen;
    }

    private void cleanupSession(Session session) {
        if (session == null) return;
        try {
            Long userId = sessionUserMap.remove(session);
            if (userId != null) {
                Set<Session> sessions = userSessions.get(userId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        userSessions.remove(userId);
                    }
                }
            }
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
        }
    }
}
