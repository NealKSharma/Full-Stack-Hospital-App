package backend.notifications;

import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.firebase.messaging.Notification;

import java.util.*;

@Service
public class FcmNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FcmNotificationService.class);

    @Autowired
    private FcmDeviceTokenRepository tokenRepository;
    /**
     * Safely trims values to avoid Firebase "payload too large" failures.
     */
    private Map<String, String> sanitizeData(Map<String, String> data) {
        if (data == null) return Collections.emptyMap();

        Map<String, String> sanitized = new HashMap<>();
        data.forEach((k, v) -> {
            if (v == null) v = "";
            if (v.length() > 500) { // prevent FCM 413 errors
                v = v.substring(0, 480) + "...[trimmed]";
            }
            sanitized.put(k, v);
        });
        return sanitized;
    }

    /**
     * Send a notification to a single FCM device token.
     */
    public void sendToToken(String token, String title, String body, Map<String, String> data) {

        logger.info("FCM Attempt: token={} title='{}' body='{}' data={}", token, title, body, data);

        if (token == null || token.isBlank()) {
            logger.warn("FCM skipped: empty token");
            return;
        }

        data = sanitizeData(data);

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(data)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("FCM Success -> token={} response={}", token, response);

        } catch (FirebaseMessagingException e) {

            logger.warn("FCM FAILED for token={} code={} msg={} internal={}",
                    token,
                    e.getMessagingErrorCode(),
                    e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "none"
            );

            MessagingErrorCode code = e.getMessagingErrorCode();

            // Token invalid → revoke it
            if (code == MessagingErrorCode.UNREGISTERED ||
                    code == MessagingErrorCode.INVALID_ARGUMENT ||
                    code == MessagingErrorCode.SENDER_ID_MISMATCH) {

                tokenRepository.findByToken(token).ifPresent(deviceToken -> {
                    deviceToken.setRevoked(true);
                    tokenRepository.save(deviceToken);
                    logger.info("Token revoked in DB: {}", token);
                });
            }

        } catch (Exception e) {
            logger.error("FCM Unexpected Failure token={} error={}", token, e.getMessage(), e);
        }
    }


    /**
     * Send to all active tokens for a given user.
     */
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {

        List<FcmDeviceToken> tokens = tokenRepository.findByUserIdAndRevokedFalse(userId);

        logger.info("FCM User {} → {} active tokens", userId, tokens.size());
        if (tokens.isEmpty()) {
            logger.info("FCM No active tokens for user {}", userId);
            return;
        }

        for (FcmDeviceToken deviceToken : tokens) {
            logger.info("→ Sending to token {} (user={})", deviceToken.getToken(), userId);
            sendToToken(deviceToken.getToken(), title, body, data);
        }
    }
}
