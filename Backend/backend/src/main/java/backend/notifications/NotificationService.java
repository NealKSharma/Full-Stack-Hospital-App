package backend.notifications;

import backend.authentication.User;
import backend.authentication.UserRepository;
import backend.logging.ErrorLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FcmNotificationService fcmNotificationService;


    /* ---------------------------------------------------------
     * 1. SIMPLE NOTIFY BY USER ID
     * --------------------------------------------------------- */
    public void saveNotificationByUserId(Long userId, String title, String content) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            Notification notif = new Notification();
            notif.setTitle(title);
            notif.setContent(content);
            notif.addUser(user);
            notif.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notif);

            boolean isUserActive = NotificationEndpoint.isUserActive(userId);

            if (isUserActive) {
                // Active session: use WebSocket only
                NotificationEndpoint.sendToUser(userId, "generic", title, content);
            } else {
                // Inactive: use FCM to wake the app
                fcmNotificationService.sendToUser(
                        userId,
                        title,
                        content,
                        Map.of(
                                "type", "generic",
                                "title", title,
                                "content", content
                        )
                );
            }

        } catch (Exception e) {
            System.err.println("Error saving notification for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    /* ---------------------------------------------------------
     * 2. BROADCAST TO ALL USERS (WS if active, FCM always)
     * --------------------------------------------------------- */
    public void saveBroadcastToActiveUsers(String title, String message) {
        try {
            List<User> users = userRepository.findAll();

            for (User user : users) {
                Long userId = user.getId();

                Notification notif = new Notification();
                notif.setTitle(title);
                notif.setContent(message);
                notif.addUser(user);
                notif.setCreatedAt(LocalDateTime.now());
                notificationRepository.save(notif);

                boolean isActive = NotificationEndpoint.isUserActive(userId);

                if (isActive) {
                    NotificationEndpoint.sendToUser(userId, "broadcast", title, message);
                } else {
                    fcmNotificationService.sendToUser(
                            userId,
                            title,
                            message,
                            Map.of(
                                    "type", "broadcast",
                                    "title", title,
                                    "content", message,
                                    "notificationId", String.valueOf(notif.getId())
                            )
                    );
                }
            }

        } catch (Exception e) {
            System.err.println("Error broadcasting: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /* ---------------------------------------------------------
     * 3. MAIN METHOD USED BY BACKEND SERVICES
     * --------------------------------------------------------- */
    public void saveNotificationToUser(
            User user,
            String title,
            String content,
            String type,
            String extraDataId
    ) {
        Notification notif = new Notification();
        notif.setTitle(title);
        notif.setContent(content);
        notif.addUser(user);
        notif.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notif);

        Long userId = user.getId();
        boolean isUserActive = NotificationEndpoint.isUserActive(userId);

        if (isUserActive) {
            NotificationEndpoint.sendToUser(userId, type, title, content);
        } else {
            fcmNotificationService.sendToUser(
                    userId,
                    title,
                    content,
                    Map.of(
                            "type", type,
                            "title", title,
                            "content", content,
                            "dataId", extraDataId != null ? extraDataId : ""
                    )
            );
        }
    }


    /* ---------------------------------------------------------
     * 4. GET LAST 20 NOTIFICATIONS
     * --------------------------------------------------------- */
    public List<Notification> getRecentNotifications(Long userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }


    /* ---------------------------------------------------------
     * 5. NOTIFY ADMINS OF SYSTEM ERRORS
     * --------------------------------------------------------- */
    public void notifyAdminsOfError(ErrorLog errorLog) {
        try {
            List<User> admins = userRepository.findByUserType("ADMIN");

            if (admins.isEmpty()) return;

            String title = String.format(
                    "System Error in %s.%s",
                    errorLog.getErrorFileName() != null ? errorLog.getErrorFileName() : "Unknown",
                    errorLog.getMethodName() != null ? errorLog.getMethodName() : "unknown"
            );

            String content = String.format(
                    "Error: %s\nLocation: %s:%d\nTime: %s",
                    errorLog.getErrorMessage() != null ? errorLog.getErrorMessage() : "No message",
                    errorLog.getErrorFileName() != null ? errorLog.getErrorFileName() : "Unknown",
                    errorLog.getLineNumber() != null ? errorLog.getLineNumber() : -1,
                    errorLog.getCreatedAt()
            );

            int MAX_LEN = 255;
            if (content.length() > MAX_LEN) {
                content = content.substring(0, MAX_LEN - 14) + "... [truncated]";
            }

            for (User admin : admins) {
                Notification notif = new Notification();
                notif.setTitle(title);
                notif.setContent(content);
                notif.addUser(admin);
                notif.setCreatedAt(LocalDateTime.now());
                notificationRepository.save(notif);

                Long userId = admin.getId();
                boolean isActive = NotificationEndpoint.isUserActive(userId);

                if (isActive) {
                    NotificationEndpoint.sendToUser(userId, "error", title, content);
                } else {
                    fcmNotificationService.sendToUser(
                            userId,
                            title,
                            content,
                            Map.of(
                                    "type", "error",
                                    "title", title,
                                    "content", content,
                                    "notificationId", String.valueOf(notif.getId())
                            )
                    );
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to notify admins: " + e.getMessage());
        }
    }
}
