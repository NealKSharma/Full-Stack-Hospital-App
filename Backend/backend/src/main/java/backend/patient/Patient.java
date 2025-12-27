package backend.patient;

import backend.authentication.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "patients")
public class Patient {
    @Id
    private Long id; // Same as User id

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String mrn;

    private String dob;
    private String gender;

    @Column(length = 1000)
    private String symptoms;

    @Column(length = 1000)
    private String medications;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    @JsonIgnore
    private User user;

    public Patient() {}

    public Patient(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public Patient(String name, String mrn, String dob) {
        this.name = name;
        this.mrn = mrn;
        this.dob = dob;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
