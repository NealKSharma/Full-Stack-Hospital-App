package backend.pharmacist;

import backend.authentication.User;
import jakarta.persistence.*;

@Entity
@Table(name = "pharmacists")
public class Pharmacist {

    @Id
    private Long id; // same as user ID

    @Column(nullable = false)
    private String name;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    public Pharmacist() {}
    public Pharmacist(User user, String name) {
        this.user = user;
        this.name = name;
    }

    // getters/setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public User getUser() { return user; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setUser(User user) { this.user = user; }
}
