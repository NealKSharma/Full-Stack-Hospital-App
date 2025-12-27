package backend.prescriptions;

import backend.logging.ErrorLogger;
import backend.authentication.JwtUtil;
import backend.patient.Patient;
import backend.patient.PatientRepository;
import backend.doctor.Doctor;
import backend.doctor.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// OpenAPI imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/doctor/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ErrorLogger errorLogger;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    // ---- CREATE ----
    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping
    @Operation(summary = "Create a prescription for a patient (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200",     description = "Prescription created successfully"),
            @ApiResponse(responseCode = "400",     description = "Invalid prescription request"),
            @ApiResponse(responseCode = "404",     description = "Doctor or patient not found"),
            @ApiResponse(responseCode = "500",     description = "Failed to create prescription")
    })
    public ResponseEntity<?> createPrescription(@RequestBody PrescriptionRequest request,
                                                HttpServletRequest servletRequest) {
        try {
            String token = jwtUtil.extractJwtFromRequest(servletRequest);
            Long doctorId = jwtUtil.extractUserId(token);

            if (doctorId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Doctor ID not found in token."));
            }

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));

            Patient patient = patientRepository.findByUserUsername(request.getPatientUsername())
                    .orElseThrow(() -> new RuntimeException("Patient not found with username: " + request.getPatientUsername()));

            Prescription prescription = prescriptionService.createPrescription(request, doctor, patient);

            Map<String, Object> response = new HashMap<>();
            response.put("prescriptionId", prescription.getId());
            response.put("patient", patient.getName());
            response.put("medication", prescription.getMedication());
            response.put("dosage", prescription.getDosage());
            response.put("refill", prescription.getRefill());
            response.put("notes", prescription.getNotes());
            response.put("createdAt", prescription.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to create prescription."));
        }
    }

    // ---- READ ALL FOR THIS DOCTOR ----
    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping
    @Operation(summary = "Get all prescriptions written by the authenticated doctor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to retrieve prescriptions")
    })
    public ResponseEntity<?> getDoctorPrescriptions(HttpServletRequest servletRequest) {
        try {
            String token = jwtUtil.extractJwtFromRequest(servletRequest);
            Long doctorId = jwtUtil.extractUserId(token);

            if (doctorId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid token: Doctor ID missing"));
            }

            List<Prescription> list = prescriptionService.getByDoctor(doctorId);
            return ResponseEntity.ok(list);

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve prescriptions."));
        }
    }

    // ---- READ ONE ----
    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/{id}")
    @Operation(summary = "Get a single prescription by ID (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prescription retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Prescription not found")
    })
    public ResponseEntity<?> getPrescription(@PathVariable Long id) {
        try {
            Prescription pres = prescriptionService.getById(id);
            return ResponseEntity.ok(pres);
        } catch (Exception e) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ---- DELETE ----
    @PreAuthorize("hasRole('DOCTOR')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prescription by ID (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prescription deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Prescription not found")
    })
    public ResponseEntity<String> deletePrescription(@PathVariable Long id) {
        String result = prescriptionService.delete(id);
        return ResponseEntity.ok(result);
    }
}
