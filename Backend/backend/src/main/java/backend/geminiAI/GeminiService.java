package backend.geminiAI;

import backend.appointments.Appointment;
import backend.appointments.AppointmentRepository;
import backend.authentication.User;
import backend.authentication.UserRepository;
import backend.patient.Patient;
import backend.patient.PatientRepository;
import backend.prescriptions.Prescription;
import backend.prescriptions.PrescriptionRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GeminiService {

    // Hard-coded key for now. Replace with environment variable before deployment.
    private final String apiKey = "AIzaSyB-M039VDrt4-aLAtlGANlztrb-pbW1rxE";

    private final RestTemplate client = new RestTemplate();
    private static final String MODEL = "gemini-2.5-flash";

    @Autowired private UserRepository userRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private AppointmentRepository appointmentRepo;
    @Autowired private PrescriptionRepository prescriptionRepo;


    /**
     * Called every time a user sends a message to assistant&<username>
     */
    public String askGemini(String userPrompt, String username) {

        String context = buildPatientContext(username);

        // Gemini decides what counts as a valid question
        String prompt =
                "You are a hospital health assistant designed to support patients.\n" +
                        "Your rules:\n" +
                        "1. You only answer questions related to health, symptoms, medications, prescriptions, treatments,\n" +
                        "   refills, appointments, or general medical guidance.\n" +
                        "2. If the user's question is not related to health, respond politely:\n" +
                        "   \"I can help with health-related questions such as symptoms, medications, treatments, or appointments.\"\n" +
                        "3. You never diagnose conditions. Provide safe and general guidance only.\n" +
                        "4. You must use ONLY the patient data provided below.\n" +
                        "5. Never mention databases, servers, backend systems, or internal architecture.\n\n" +
                        "=== PATIENT DATA ===\n" + context + "\n\n" +
                        "=== USER QUESTION ===\n" + userPrompt + "\n\n" +
                        "Respond clearly, professionally, and safely.";

        try {
            if (apiKey == null || apiKey.isEmpty()) {
                return "The AI assistant is not configured. Please contact support.";
            }

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent?key=" + apiKey;

            JSONObject contentObj = new JSONObject()
                    .put("parts", new JSONArray()
                            .put(new JSONObject().put("text", prompt)));

            JSONObject body = new JSONObject()
                    .put("contents", new JSONArray().put(contentObj));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response =
                    client.exchange(url, HttpMethod.POST, entity, String.class);

            JSONObject responseJson = new JSONObject(response.getBody());

            return responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (Exception e) {
            return "The AI assistant is currently unavailable. Please try again later.";
        }
    }


    /**
     * Build a human-readable medical profile for Gemini to use.
     */
    private String buildPatientContext(String username) {

        User user = userRepo.findByUsername(username);
        if (user == null) {
            return "No user found.";
        }

        Long userId = user.getId();
        StringBuilder sb = new StringBuilder();

        // ------------------ PATIENT PROFILE ------------------
        Patient patient = patientRepo.findById(userId).orElse(null);

        if (patient != null) {
            sb.append("Name: ").append(patient.getName()).append("\n");
            sb.append("DOB: ").append(patient.getDob()).append("\n");
            sb.append("Gender: ").append(patient.getGender()).append("\n");
            sb.append("Symptoms: ").append(patient.getSymptoms()).append("\n");
            sb.append("Medications: ").append(patient.getMedications()).append("\n\n");
        } else {
            sb.append("No patient medical profile found.\n\n");
        }

        // ------------------ APPOINTMENTS ------------------
        List<Appointment> appts = appointmentRepo.findByPatientId(userId);

        sb.append("Appointments:\n");

        if (appts == null || appts.isEmpty()) {
            sb.append("- No appointments recorded.\n\n");
        } else {
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

            for (Appointment a : appts) {
                sb.append("- ")
                        .append(a.getDate().format(dateFmt))
                        .append(" at ")
                        .append(a.getTime().format(timeFmt))
                        .append(" with doctorId=")
                        .append(a.getDoctor().getId())
                        .append(" | Status: ")
                        .append(a.getStatus())
                        .append("\n");
            }
            sb.append("\n");
        }

        // ------------------ PRESCRIPTIONS ------------------
        List<Prescription> prescriptions = prescriptionRepo.findByPatient(patient);

        sb.append("Prescriptions:\n");
        DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (prescriptions == null || prescriptions.isEmpty()) {
            sb.append("- No prescriptions recorded.\n\n");
        } else {
            for (Prescription rx : prescriptions) {
                sb.append("- Medication: ").append(rx.getMedication()).append("\n")
                        .append("  Dosage: ").append(rx.getDosage()).append("\n")
                        .append("  Refills Left: ").append(rx.getRefill()).append("\n")
                        .append("  Notes: ").append(rx.getNotes()).append("\n")
                        .append("  Created At: ").append(rx.getCreatedAt().format(dateOnly)).append("\n\n");
            }
        }

        return sb.toString();
    }
}
