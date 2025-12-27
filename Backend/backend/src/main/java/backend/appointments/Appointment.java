package backend.appointments;

import backend.patient.Patient;
import backend.doctor.Doctor;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "appointments")
public class Appointment {
    //fixed the yaml

    // Getters & Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(nullable = false)
    private String patientEmail;

    @Column(nullable = false)
    private String patientPhone;

    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public void setPatient(Patient patient) { this.patient = patient; }

    public void setDate(LocalDate date) { this.date = date; }

    public void setTime(LocalTime time) { this.time = time; }

    public void setNotes(String notes) { this.notes = notes; }

    public void setStatus(AppointmentStatus status) { this.status = status; }

    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
}
