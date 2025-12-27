package backend.pharmacy;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyRepository extends JpaRepository<PharmacyProduct, Long> {
    PharmacyProduct findByName(String name);
}
