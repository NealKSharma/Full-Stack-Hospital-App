package backend.prescriptions;

import backend.patient.Patient;
import backend.doctor.Doctor;
import backend.doctor.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    // ---- CREATE ----
    public Prescription createPrescription(PrescriptionRequest request, Doctor doctor, Patient patient) {
        Prescription prescription = new Prescription();
        prescription.setDoctor(doctor);
        prescription.setPatient(patient);
        prescription.setMedication(request.getMedication());
        prescription.setDosage(request.getDosage());
        prescription.setRefill(request.getRefill());
        prescription.setNotes(request.getNotes());
        // createdAt auto-handled by @PrePersist
        return prescriptionRepository.save(prescription);
    }

    // ---- GET PRESCRIPTIONS BY DOCTOR ----
    public List<Prescription> getByDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
        return prescriptionRepository.findByDoctor(doctor);
    }

    // ---- GET ONE ----
    public Prescription getById(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found with ID: " + id));
    }

    // ---- DELETE ----
    public String delete(Long id) {
        if (prescriptionRepository.existsById(id)) {
            prescriptionRepository.deleteById(id);
            return "Prescription deleted successfully.";
        } else {
            return "Prescription not found.";
        }
    }
}
