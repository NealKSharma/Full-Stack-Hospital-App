package backend.appointments;

import backend.doctor.Doctor;
import backend.doctor.DoctorRepository;
import backend.notifications.NotificationEndpoint;
import backend.notifications.NotificationService;
import backend.patient.Patient;
import backend.patient.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private NotificationService notificationService;

    // PATIENT creates an appointment
    public Appointment createAppointment(AppointmentRequest request) {

        Doctor doctor = doctorRepository.findByUsername(request.getDoctorUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Patient patient = patientRepository.findByUserUsername(request.getPatientUsername())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Appointment appt = new Appointment();
        appt.setDoctor(doctor);
        appt.setPatient(patient);
        appt.setDate(request.getDate());
        appt.setTime(request.getTime());
        appt.setNotes(request.getNotes());
        appt.setPatientEmail(request.getPatientEmail());
        appt.setPatientPhone(request.getPatientPhone());
        appt.setStatus(AppointmentStatus.PENDING);

        // ----------- SEND NOTIFICATION TO DOCTOR -----------
        String message = "New appointment request from " + patient.getName()
                + " for " + appt.getDate() + " at " + appt.getTime();

        // Save in DB
        notificationService.saveNotificationByUserId(
                doctor.getId(),
                "New Appointment Request",
                message
        );

        // Real-time push
        NotificationEndpoint.sendToUser(
                doctor.getId(),
                "appointment",
                "New Appointment Request",
                message
        );

        return appointmentRepository.save(appt);
    }

    // DOCTOR: get appointments by doctorId
    public List<Appointment> getByDoctorId(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    // PATIENT: get appointments by patientId
    public List<Appointment> getByPatientId(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    // ADMIN: get all
    public List<Appointment> getAll() {
        return appointmentRepository.findAll();
    }

    // DOCTOR updates appointment status
    public Appointment updateStatus(Long id, AppointmentStatus status) {

        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appt.setStatus(status);
        appointmentRepository.save(appt);

        Long patientId = appt.getPatient().getId();

        // ----------- BUILD MESSAGE FOR PATIENT -----------
        String msg = switch (status) {
            case APPROVED -> "Your appointment has been approved for "
                    + appt.getDate() + " at " + appt.getTime();

            case CONFIRMED -> "Your appointment has been confirmed for "
                    + appt.getDate() + " at " + appt.getTime();

            case CANCELLED -> "Your appointment was cancelled by the doctor.";

            case PENDING -> "Your appointment status has been reset to pending.";

            case COMPLETED -> "Your appointment on "
                    + appt.getDate() + " at " + appt.getTime()
                    + " has been marked as completed by your doctor.";
        };

        // Save in DB
        notificationService.saveNotificationByUserId(
                patientId,
                "Appointment Update",
                msg
        );

        // Real-time push
        NotificationEndpoint.sendToUser(
                patientId,
                "appointment",
                "Appointment Update",
                msg
        );

        return appt;
    }

    // DOCTOR explicitly cancels
    public void cancel(Long id) {

        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appt.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt);

        Long patientId = appt.getPatient().getId();

        String msg = "Your appointment on "
                + appt.getDate() + " at " + appt.getTime()
                + " has been cancelled.";

        // Save in DB
        notificationService.saveNotificationByUserId(
                patientId,
                "Appointment Cancelled",
                msg
        );

        // Real-time push
        NotificationEndpoint.sendToUser(
                patientId,
                "appointment",
                "Appointment Cancelled",
                msg
        );
    }
}
