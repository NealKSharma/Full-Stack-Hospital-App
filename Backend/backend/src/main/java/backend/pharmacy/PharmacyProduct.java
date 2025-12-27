package backend.pharmacy;

import jakarta.persistence.*;

@Entity
@Table(name = "pharmacy_products")
public class PharmacyProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String genericName;

    @Column(nullable = false)
    private String dosage;

    @Column(nullable = false)
    private Integer priceInCents; // e.g., 1299 = $12.99

    @Column(nullable = true)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isProtected = false;

    public PharmacyProduct() {}

    public PharmacyProduct(String name, String genericName, String dosage, Integer priceInCents, String imageUrl) {
        this.name = name;
        this.genericName = genericName;
        this.dosage = dosage;
        this.priceInCents = priceInCents;
        this.imageUrl = imageUrl;
        this.isProtected = false;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public Integer getPriceInCents() { return priceInCents; }
    public void setPriceInCents(Integer priceInCents) { this.priceInCents = priceInCents; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getIsProtected() { return isProtected; }
    public void setIsProtected(Boolean isProtected) {
        this.isProtected = isProtected != null ? isProtected : false;
    }
}