package backend.authentication;

import backend.logging.ErrorLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;

// OpenAPI imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/user")
public class ProfileUpdateController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ErrorLogger errorLogger;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or expired token"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch profile")
    })
    public ResponseEntity<?> me(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (jwtUtil.isTokenExpired(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token expired");
            }

            Long userId = jwtUtil.extractUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            Map<String, Object> res = new HashMap<>();
            res.put("id", user.getId());
            res.put("username", user.getUsername());
            res.put("email", user.getEmail());
            res.put("role", user.getUserType());
            return ResponseEntity.ok(res);

        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(error(ex.getReason()));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to fetch profile"));
        }
    }

    @PatchMapping("/me")
    @Transactional
    @Operation(summary = "Update the authenticated user's profile information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated and new tokens issued"),
            @ApiResponse(responseCode = "400", description = "Invalid profile data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or expired token"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Failed to update profile")
    })
    public ResponseEntity<?> updateUser(HttpServletRequest request,
                                        @RequestBody Map<String, String> updates) {
        try {
            String token = extractToken(request);
            if (jwtUtil.isTokenExpired(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token expired");
            }

            Long userId = jwtUtil.extractUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if (updates.containsKey("username")) {
                String newUsername = updates.get("username");
                if (newUsername != null && !newUsername.trim().isEmpty()) {
                    user.setUsername(newUsername);
                }
            }

            if (updates.containsKey("email")) {
                String newEmail = updates.get("email");
                if (newEmail != null && !newEmail.trim().isEmpty()) {
                    user.setEmail(newEmail);
                }
            }

            if (updates.containsKey("role")) {
                String newRole = updates.get("role");
                if (newRole != null && !newRole.trim().isEmpty()) {
                    newRole = newRole.toUpperCase();
                    if (!newRole.equals("DOCTOR") &&
                            !newRole.equals("PATIENT") &&
                            !newRole.equals("ADMIN")) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
                    }
                    user.setUserType(newRole);
                }
            }

            if (updates.containsKey("password")) {
                String newPassword = updates.get("password");
                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    user.setPassword(passwordEncoder.encode(newPassword));
                }
            }

            userRepository.save(user);

            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUserType());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());
            long expiresAt = jwtUtil.getExpirationTime(newAccessToken);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            response.put("expiresAt", expiresAt);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getUserType());

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(error(ex.getReason()));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to update profile"));
        }
    }

    @DeleteMapping("/me")
    @Transactional
    @Operation(summary = "Delete the authenticated user's account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or expired token"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "User cannot be deleted due to related records"),
            @ApiResponse(responseCode = "500", description = "Failed to delete account")
    })
    public ResponseEntity<?> deleteMe(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (jwtUtil.isTokenExpired(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token expired");
            }

            Long userId = jwtUtil.extractUserId(token);

            if (!userRepository.existsById(userId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }

            try {
                userRepository.deleteById(userId);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                errorLogger.logError(e);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot delete user due to related records");
            }

            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Account deleted");
            return ResponseEntity.ok(res);

        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(error(ex.getReason()));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to delete account"));
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }
        return authHeader.substring(7);
    }

    private Map<String, String> error(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("status", "error");
        res.put("message", message);
        return res;
    }
}
