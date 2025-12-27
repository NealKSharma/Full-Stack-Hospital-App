package backend.patient;

public class PatientRequest {
    private String name;
    private String mrn;
    private String dob;
    private String gender;
    private Long userId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    // Nested class for additional info request
    public static class PatientAdditionalRequest {
        private String symptoms;
        private String medications;

        public String getSymptoms() { return symptoms; }
        public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

        public String getMedications() { return medications; }
        public void setMedications(String medications) { this.medications = medications; }
    }
}
