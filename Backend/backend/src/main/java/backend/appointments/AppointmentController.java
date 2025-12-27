package backend.appointments;

import backend.authentication.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AppointmentController(AppointmentService appointmentService, JwtUtil jwtUtil) {
        this.appointmentService = appointmentService;
        this.jwtUtil = jwtUtil;
    }

    // Create new appointment (doctor or patient)
    @PreAuthorize("hasAnyRole('DOCTOR','PATIENT')")
    @PostMapping
    @Operation(summary = "Create a new appointment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid appointment data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Appointment createAppointment(@RequestBody AppointmentRequest request) {
        return appointmentService.createAppointment(request);
    }

    @PreAuthorize("hasAnyRole('DOCTOR','PATIENT')")
    @GetMapping("/my")
    @Operation(summary = "Get appointments for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User appointments returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "User role not permitted")
    })
    public ResponseEntity<List<Appointment>> getMyAppointments(HttpServletRequest http) {
        String token = jwtUtil.extractJwtFromRequest(http);
        Long userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        List<Appointment> appointments;
        if ("DOCTOR".equalsIgnoreCase(role)) {
            appointments = appointmentService.getByDoctorId(userId);
        } else if ("PATIENT".equalsIgnoreCase(role)) {
            appointments = appointmentService.getByPatientId(userId);
        } else {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(appointments);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    @Operation(summary = "Get all appointments (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All appointments returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAll());
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve an appointment (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment approved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public Appointment approveAppointment(@PathVariable Long id) {
        return appointmentService.updateStatus(id, AppointmentStatus.APPROVED);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/{id}/confirm")
    @Operation(summary = "Confirm an appointment (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment confirmed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public Appointment confirmAppointment(@PathVariable Long id) {
        return appointmentService.updateStatus(id, AppointmentStatus.CONFIRMED);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/{id}/complete")
    @Operation(summary = "Mark an appointment as completed (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment marked completed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public Appointment completeAppointment(@PathVariable Long id) {
        return appointmentService.updateStatus(id, AppointmentStatus.COMPLETED);
    }


    // Both doctor or patient can cancel
    @PreAuthorize("hasAnyRole('DOCTOR','PATIENT')")
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment canceled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public void cancelAppointment(@PathVariable Long id) {
        appointmentService.cancel(id);
    }
}
