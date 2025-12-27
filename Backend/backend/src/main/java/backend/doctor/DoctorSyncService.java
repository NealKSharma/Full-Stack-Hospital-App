package backend.doctor;

import backend.authentication.User;
import backend.authentication.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorSyncService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public DoctorSyncService(DoctorRepository doctorRepository,
                             UserRepository userRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void autoSyncDoctors() {
        System.out.println("[DoctorSync] Auto-syncing doctors with users table...");

        List<User> users = userRepository.findByUserType("DOCTOR");

        Map<Long, Doctor> doctorMap = doctorRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Doctor::getId, d -> d));

        Set<Long> validDoctorIds = new HashSet<>();

        // INSERT or UPDATE
        for (User user : users) {
            Doctor doctor = doctorMap.get(user.getId());

            if (doctor == null) {
                Doctor newDoctor = new Doctor();
                newDoctor.setId(user.getId());
                newDoctor.setUsername(user.getUsername());
                newDoctor.setEmail(user.getEmail());

                System.out.println("[DoctorSync] Added doctor: " + user.getUsername());
            } else {
                boolean changed = false;

                if (!Objects.equals(doctor.getUsername(), user.getUsername())) {
                    doctor.setUsername(user.getUsername());
                    changed = true;
                }
                if (!Objects.equals(doctor.getEmail(), user.getEmail())) {
                    doctor.setEmail(user.getEmail());
                    changed = true;
                }

                if (changed) {
                    doctorRepository.save(doctor);
                    System.out.println("[DoctorSync] Updated doctor: " + user.getUsername());
                }
            }

            validDoctorIds.add(user.getId());
        }

        // DELETE stale doctors (by ID)
        for (Doctor d : doctorMap.values()) {
            if (!validDoctorIds.contains(d.getId())) {
                doctorRepository.deleteById(d.getId());
                System.out.println("[DoctorSync] Removed doctor ID: " + d.getId());
            }
        }


        System.out.println("[DoctorSync] Doctor sync complete.");
    }
}
