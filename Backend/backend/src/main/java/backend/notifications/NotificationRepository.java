package backend.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n JOIN n.users u WHERE u.id = :userId ORDER BY n.createdAt DESC LIMIT 20")
    List<Notification> findTop20ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Delete all but the latest 20 - updated for join table
    @Query(value = """
        SELECT n.id FROM notifications n
        INNER JOIN notification_users nu ON n.id = nu.notification_id
        WHERE nu.user_id = :userId
        ORDER BY n.created_at DESC
        LIMIT 18446744073709551615 OFFSET 20
    """, nativeQuery = true)
    List<Long> findIdsToTrim(@Param("userId") Long userId);
}