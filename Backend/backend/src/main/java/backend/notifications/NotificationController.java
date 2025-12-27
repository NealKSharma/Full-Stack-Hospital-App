package backend.notifications;

import backend.authentication.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtil jwtUtil;

    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @GetMapping
    @Operation(summary = "Get recent notifications for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized request")
    })
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(notificationService.getRecentNotifications(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/user/{userId}")
    @Operation(summary = "Send a notification to a specific user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification sent successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized request")
    })
    public ResponseEntity<?> sendToUser(@PathVariable Long userId,
                                        @RequestBody Map<String, String> req) {

        notificationService.saveNotificationByUserId(userId, req.get("title"), req.get("content"));
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/broadcast")
    @Operation(summary = "Broadcast a notification to all active users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Broadcast sent successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized request")
    })
    public ResponseEntity<?> broadcast(@RequestBody Map<String, String> req) {

        notificationService.saveBroadcastToActiveUsers(req.get("title"), req.get("content"));
        return ResponseEntity.ok(Map.of("status", "broadcasted"));
    }
}
