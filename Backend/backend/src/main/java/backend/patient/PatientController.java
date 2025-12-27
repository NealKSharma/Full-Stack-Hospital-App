package backend.patient;

import backend.appointments.Appointment;
import backend.appointments.AppointmentRepository;
import backend.prescriptions.Prescription;
import backend.prescriptions.PrescriptionRepository;
import backend.authentication.JwtUtil;
import backend.logging.ErrorLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
public class PatientController {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ErrorLogger errorLogger;

    @GetMapping("/patients")
    @Operation(summary = "Get all patients (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patients retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or invalid token"),
            @ApiResponse(responseCode = "403", description = "Access denied: Doctors only"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAllPatients(HttpServletRequest request) {
        try {
            validateDoctorAccess(request);
            List<Patient> patients = patientRepository.findAll();
            return ResponseEntity.ok(patients);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(error(e.getReason() != null ? e.getReason() : "Access denied"));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while fetching patients"));
        }
    }

    @GetMapping("/patients/{id}/additional")
    @Operation(summary = "Get detailed patient history (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Patient not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getPatientDetails(@PathVariable Long id, HttpServletRequest request) {
        try {
            validateDoctorAccess(request);

            Patient patient = patientRepository.findById(id).orElse(null);
            if (patient == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Patient not found"));

            List<Appointment> appts = appointmentRepository.findByPatientId(id);
            List<Prescription> prescriptions = prescriptionRepository.findByPatientId(id);

            Map<String, Object> response = new HashMap<>();

            response.put("symptoms", patient.getSymptoms());
            response.put("medications", patient.getMedications());

            List<Map<String, String>> apptList = new ArrayList<>();
            for (Appointment a : appts) {
                Map<String, String> map = new HashMap<>();
                map.put("date", a.getDate() != null ? a.getDate().toString() : "");
                map.put("time", a.getTime() != null ? a.getTime().toString() : "");

                String drName = (a.getDoctor() != null && a.getDoctor().getUser() != null)
                        ? a.getDoctor().getUser().getUsername()
                        : "Unknown";
                map.put("doctor", drName);
                map.put("status", a.getStatus() != null ? a.getStatus().toString() : "");
                apptList.add(map);
            }
            response.put("appointments", apptList);

            List<Map<String, String>> rxList = new ArrayList<>();
            for (Prescription p : prescriptions) {
                Map<String, String> map = new HashMap<>();
                map.put("name", p.getMedication());
                map.put("dose", p.getDosage());
                map.put("freq", p.getRefill() + " refills");
                rxList.add(map);
            }
            response.put("prescriptions", rxList);

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(error(e.getReason() != null ? e.getReason() : "Access denied"));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while fetching patient details"));
        }
    }

    @PutMapping("/patients/{id}")
    @Operation(summary = "Update basic patient information (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or invalid token"),
            @ApiResponse(responseCode = "403", description = "Access denied: Doctors only"),
            @ApiResponse(responseCode = "404", description = "Patient not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updatePatient(@PathVariable Long id,
                                           @RequestBody PatientRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            validateDoctorAccess(httpRequest);

            Patient patient = patientRepository.findById(id).orElse(null);
            if (patient == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Patient not found"));

            if (request.getMrn() != null) patient.setMrn(request.getMrn());
            if (request.getDob() != null && !request.getDob().trim().isEmpty()) patient.setDob(request.getDob());
            if (request.getGender() != null && !request.getGender().trim().isEmpty()) patient.setGender(request.getGender());

            patientRepository.save(patient);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Patient updated successfully");
            response.put("patientId", patient.getId());
            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(error(e.getReason() != null ? e.getReason() : "Access denied"));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while updating patient"));
        }
    }

    @PutMapping("/patients/{id}/additional")
    @Operation(summary = "Update patient symptoms/medications (doctor only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient additional info updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or invalid token"),
            @ApiResponse(responseCode = "403", description = "Access denied: Doctors only"),
            @ApiResponse(responseCode = "404", description = "Patient not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updatePatientAdditional(@PathVariable Long id,
                                                     @RequestBody PatientRequest.PatientAdditionalRequest additionalRequest,
                                                     HttpServletRequest httpRequest) {
        try {
            validateDoctorAccess(httpRequest);

            Patient patient = patientRepository.findById(id).orElse(null);
            if (patient == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Patient not found"));

            if (additionalRequest.getSymptoms() != null)
                patient.setSymptoms(additionalRequest.getSymptoms());
            if (additionalRequest.getMedications() != null)
                patient.setMedications(additionalRequest.getMedications());

            patientRepository.save(patient);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Patient symptoms and medications updated successfully");
            response.put("patientId", patient.getId());
            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(error(e.getReason() != null ? e.getReason() : "Access denied"));
        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while updating patient information"));
        }
    }

    private void validateDoctorAccess(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");

        String token = authHeader.substring(7);
        try {
            if (jwtUtil.isTokenExpired(token))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token expired");

            String role = jwtUtil.extractRole(token);
            if (!"DOCTOR".equalsIgnoreCase(role))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Doctors only");

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token expired");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or malformed token");
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("status", "error");
        res.put("message", message);
        return res;
    }
}