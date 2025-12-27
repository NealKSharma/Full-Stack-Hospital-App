package backend.pharmacy;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pharmacy_orders")
public class PharmacyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // who placed the order (patient userId from JWT)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    // snapshot of product fields at time of order
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_dosage")
    private String productDosage;

    @Column(name = "unit_price_in_cents", nullable = false)
    private Integer unitPriceInCents;

    @Column(name = "total_in_cents", nullable = false)
    private Integer totalInCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PharmacyOrderStatus status = PharmacyOrderStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public String getProductName() { return productName; }
    public String getProductDosage() { return productDosage; }
    public Integer getUnitPriceInCents() { return unitPriceInCents; }
    public Integer getTotalInCents() { return totalInCents; }
    public PharmacyOrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setProductDosage(String productDosage) { this.productDosage = productDosage; }
    public void setUnitPriceInCents(Integer unitPriceInCents) { this.unitPriceInCents = unitPriceInCents; }
    public void setTotalInCents(Integer totalInCents) { this.totalInCents = totalInCents; }
    public void setStatus(PharmacyOrderStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
