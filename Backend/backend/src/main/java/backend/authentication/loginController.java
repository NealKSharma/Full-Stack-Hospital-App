package backend.authentication;

import backend.logging.ErrorLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api")
public class loginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ErrorLogger errorLogger;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and generate JWT tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid login request"),
            @ApiResponse(responseCode = "401", description = "Invalid username, password, or role")
    })
    public ResponseEntity<?> login(@RequestBody loginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().trim().isEmpty())
                return ResponseEntity.badRequest().body(error("Username cannot be empty"));
            if (request.getPassword() == null || request.getPassword().trim().isEmpty())
                return ResponseEntity.badRequest().body(error("Password cannot be empty"));

            User user = userRepository.findByUsername(request.getUsername());
            if (user == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("User not found"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Invalid password"));

            if (request.getRole() != null && !request.getRole().isEmpty()) {
                if (!user.getUserType().equalsIgnoreCase(request.getRole()))
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Invalid role"));
            }

            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUserType());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());
            long expiresAt = jwtUtil.getExpirationTime(accessToken);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("expiresAt", expiresAt);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("role", user.getUserType());
            response.put("email", user.getEmail());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.badRequest().body(error("An Error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT tokens using a valid refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New tokens issued"),
            @ApiResponse(responseCode = "400", description = "Refresh token missing"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty())
            return ResponseEntity.badRequest().body(error("Refresh token is required"));
        try {
            Long userId = jwtUtil.extractUserId(refreshToken);
            if (jwtUtil.isTokenExpired(refreshToken))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Refresh token expired"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUserType());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());
            long expiresAt = jwtUtil.getExpirationTime(newAccessToken);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            response.put("expiresAt", expiresAt);
            response.put("userId", user.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("Invalid compact JWT") || e.getClass().getName().contains("jsonwebtoken"))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Invalid refresh token"));
            }

            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Invalid refresh token"));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> err = new HashMap<>();
        err.put("message", message);
        return err;
    }
}
