package backend.notifications;

import backend.authentication.JwtUtil;
import backend.authentication.User;
import backend.authentication.UserRepository;
import backend.notifications.FcmTokenRegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/push")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FcmTokenController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FcmDeviceTokenRepository tokenRepository;


    // ------------------------------
    // Helper: Extract JWT safely
    // ------------------------------
    private Long extractUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7); // remove "Bearer "
        return jwtUtil.extractUserId(token);
    }


    // ------------------------------
    // Register token endpoint
    // ------------------------------
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','PHARMACIST','ADMIN')")
    public ResponseEntity<?> registerToken(@RequestBody FcmTokenRegisterRequest req,
                                           HttpServletRequest request) {

        Long userId = extractUserIdFromRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for id " + userId));

        String fcmToken = req.getToken();
        String platform = (req.getPlatform() == null || req.getPlatform().isBlank())
                ? "android" : req.getPlatform();


        tokenRepository.findByToken(fcmToken).ifPresentOrElse(existing -> {
            existing.setUser(user);
            existing.setRevoked(false);
            existing.setPlatform(platform);
            existing.setLastSeenAt(LocalDateTime.now());
            tokenRepository.save(existing);
        }, () -> {
            FcmDeviceToken token = new FcmDeviceToken();
            token.setUser(user);
            token.setToken(fcmToken);
            token.setPlatform(platform);
            token.setCreatedAt(LocalDateTime.now());
            token.setLastSeenAt(LocalDateTime.now());
            tokenRepository.save(token);
        });

        return ResponseEntity.ok(Map.of("status", "ok"));
    }


    // ------------------------------
    // Revoke token endpoint
    // ------------------------------
    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','PHARMACIST','ADMIN')")
    public ResponseEntity<?> revokeToken(@RequestBody FcmTokenRegisterRequest req,
                                         HttpServletRequest request) {

        Long userId = extractUserIdFromRequest(request);
        boolean isLogout = Boolean.TRUE.equals(req.getLogout()); // only hard-revoke when user explicitly logs out

        tokenRepository.findByToken(req.getToken()).ifPresent(deviceToken -> {
            if (deviceToken.getUser().getId().equals(userId)) {
                if (isLogout) {
                    deviceToken.setRevoked(true);
                } else {
                    // App closed/backgrounded: keep token active so push works while app is closed
                    deviceToken.setRevoked(false);
                }
                deviceToken.setLastSeenAt(LocalDateTime.now());
                tokenRepository.save(deviceToken);
            }
        });

        return ResponseEntity.ok(Map.of("status", isLogout ? "revoked" : "kept-active"));
    }
}
