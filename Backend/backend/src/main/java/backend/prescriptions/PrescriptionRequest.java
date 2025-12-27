package backend.prescriptions;

public class PrescriptionRequest {

    private String patientUsername;
    private String medication;
    private String dosage;
    private Integer refill;
    private String notes;

    public String getPatientUsername() {
        return patientUsername;
    }

    public void setPatientUsername(String patientUsername) {
        this.patientUsername = patientUsername;
    }

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public Integer getRefill() {
        return refill;
    }

    public void setRefill(Integer refill) {
        this.refill = refill;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
