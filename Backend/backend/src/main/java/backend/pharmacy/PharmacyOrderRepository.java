package backend.pharmacy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PharmacyOrderRepository extends JpaRepository<PharmacyOrder, Long> {
    List<PharmacyOrder> findAllByOrderByCreatedAtDesc();
    List<PharmacyOrder> findByUserId(Long userId);
}
