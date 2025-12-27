package backend.prescriptions;

import backend.doctor.Doctor;
import backend.patient.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByDoctor(Doctor doctor);

    List<Prescription> findByPatientId(Long patientId);

    boolean existsByPatientIdAndMedication(Long patientId, String medication);

    List<Prescription> findByPatient(Patient patient);
}
