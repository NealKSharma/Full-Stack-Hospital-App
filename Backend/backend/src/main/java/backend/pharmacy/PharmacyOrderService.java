package backend.pharmacy;

import backend.pharmacy.dto.PlaceOrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PharmacyOrderService {
    private final PharmacyOrderRepository orderRepo;
    private final PharmacyRepository productRepo;

    public PharmacyOrderService(PharmacyOrderRepository orderRepo, PharmacyRepository productRepo) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
    }

    @Transactional
    public List<PharmacyOrder> placeFullOrder(Long userId, PlaceOrderRequest req) {
        return req.getItems().stream().map(item -> {
            PharmacyOrder order = new PharmacyOrder();
            order.setUserId(userId);
            order.setProductId(item.getMedicineId());
            order.setProductName(item.getName());
            order.setProductDosage(item.getDosage());
            order.setUnitPriceInCents(item.getPriceCents());
            order.setQuantity(item.getQuantity());
            order.setTotalInCents(item.getPriceCents() * item.getQuantity());
            order.setStatus(PharmacyOrderStatus.PENDING);
            order.setCreatedAt(Instant.now());
            return orderRepo.save(order);
        }).collect(Collectors.toList());
    }

    public List<PharmacyOrder> getAll() {
        return orderRepo.findAll();
    }

    public List<PharmacyOrder> getMine(Long userId) {
        return orderRepo.findByUserId(userId);
    }

    public PharmacyOrder updateStatus(Long id, PharmacyOrderStatus status) {
        PharmacyOrder order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        return orderRepo.save(order);
    }

    public PharmacyOrder getOne(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}
