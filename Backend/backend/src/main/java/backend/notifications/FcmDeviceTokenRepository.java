package backend.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmDeviceTokenRepository extends JpaRepository<FcmDeviceToken, Long> {

    List<FcmDeviceToken> findByUserIdAndRevokedFalse(Long userId);

    Optional<FcmDeviceToken> findByToken(String token);
}
