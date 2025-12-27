package backend.authentication;

import backend.patient.Patient;
import backend.patient.PatientRepository;
import backend.logging.ErrorLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
public class SignupController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ErrorLogger errorLogger;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/api/signup")
    @Operation(summary = "Create a new user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signup successful"),
            @ApiResponse(responseCode = "400", description = "Invalid signup request"),
            @ApiResponse(responseCode = "409", description = "Username already exists"),
            @ApiResponse(responseCode = "500", description = "Internal error during signup")
    })
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().trim().isEmpty())
                return ResponseEntity.badRequest().body(error("Username cannot be empty"));
            if (request.getPassword() == null || request.getPassword().trim().isEmpty())
                return ResponseEntity.badRequest().body(error("Password cannot be empty"));
            if (userRepository.existsByUsername(request.getUsername()))
                return ResponseEntity.status(409).body(error("Username already exists: " + request.getUsername()));

            String userType = request.getUserType();
            if (userType == null || userType.trim().isEmpty())
                userType = "PATIENT";
            userType = userType.toUpperCase();
            if (!userType.equals("DOCTOR") && !userType.equals("PATIENT") && !userType.equals("ADMIN"))
                return ResponseEntity.badRequest().body(error("Invalid user type. Must be DOCTOR, PATIENT, or ADMIN"));

            String hashedPassword = passwordEncoder.encode(request.getPassword());
            User newUser = new User(request.getUsername(), hashedPassword, request.getEmail(), userType);
            userRepository.save(newUser);

            if ("PATIENT".equals(userType)) {
                Patient patient = new Patient(newUser, request.getUsername());
                patientRepository.save(patient);
            }

            return ResponseEntity.ok(success("Signup successful for user: " + request.getUsername()));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred during signup: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    @Operation(summary = "Get a list of all users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned successfully"),
            @ApiResponse(responseCode = "500", description = "Error fetching user list")
    })
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while fetching users"));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("status", "error");
        res.put("message", message);
        return res;
    }

    private Map<String, String> success(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("status", "success");
        res.put("message", message);
        return res;
    }
}
