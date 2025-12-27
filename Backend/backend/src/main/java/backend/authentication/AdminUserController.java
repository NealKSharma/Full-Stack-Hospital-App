package backend.authentication;

import backend.logging.ErrorLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ErrorLogger errorLogger;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/user/{username}")
    @Operation(summary = "Get a user by username")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Error while fetching user")
    })
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("User not found"));
            }
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while fetching user"));
        }
    }

    @PostMapping("/user")
    @Operation(summary = "Create a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Username already exists"),
            @ApiResponse(responseCode = "500", description = "Failed to create user")
    })
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            String role = request.get("role");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(error("Username is required"));
            }

            User existingUser = userRepository.findByUsername(username);
            if (existingUser != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error("Username already exists"));
            }

            if (role == null || role.trim().isEmpty()) role = "PATIENT";
            role = role.toUpperCase();
            if (!role.equals("DOCTOR") && !role.equals("PATIENT") &&
                    !role.equals("ADMIN") && !role.equals("PHARMACIST")) {
                return ResponseEntity.badRequest().body(error("Invalid role"));
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setUserType(role);

            userRepository.save(user);

            return ResponseEntity.ok(success("User created successfully"));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to create user"));
        }
    }

    @PutMapping("/user/{id}")
    @Operation(summary = "Update an existing user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Failed to update user")
    })
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Optional<User> opt = userRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("User not found"));
            }
            User user = opt.get();

            if (request.containsKey("username")) {
                String newUsername = request.get("username");
                if (newUsername != null && !newUsername.trim().isEmpty()) {
                    user.setUsername(newUsername);
                }
            }

            if (request.containsKey("email")) {
                String newEmail = request.get("email");
                if (newEmail != null && !newEmail.trim().isEmpty()) {
                    user.setEmail(newEmail);
                }
            }

            if (request.containsKey("role")) {
                String newRole = request.get("role");
                if (newRole != null && !newRole.trim().isEmpty()) {
                    newRole = newRole.toUpperCase();
                    if (!newRole.equals("DOCTOR") && !newRole.equals("PATIENT") &&
                            !newRole.equals("ADMIN") && !newRole.equals("PHARMACIST")) {
                        return ResponseEntity.badRequest().body(error("Invalid role"));
                    }
                    user.setUserType(newRole);
                }
            }

            if (request.containsKey("password")) {
                String newPassword = request.get("password");
                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    user.setPassword(passwordEncoder.encode(newPassword));
                }
            }

            userRepository.save(user);
            return ResponseEntity.ok(success("User updated"));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to update user"));
        }
    }

    @DeleteMapping("/user/{id}")
    @Operation(summary = "Delete a user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "User cannot be deleted due to related records"),
            @ApiResponse(responseCode = "500", description = "Failed to delete user")
    })
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("User not found"));
            }
            userRepository.deleteById(id);
            return ResponseEntity.ok(success("User deleted"));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("Cannot delete user due to related records"));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to delete user"));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> err = new HashMap<>();
        err.put("message", message);
        err.put("status", "error");
        return err;
    }

    private Map<String, String> success(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("message", message);
        res.put("status", "success");
        return res;
    }
}
