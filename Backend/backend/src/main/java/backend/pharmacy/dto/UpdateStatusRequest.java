package backend.pharmacy.dto;

import backend.pharmacy.PharmacyOrderStatus;

public class UpdateStatusRequest {
    private PharmacyOrderStatus status;
    private String note;

    public PharmacyOrderStatus getStatus() { return status; }
    public String getNote() { return note; }

    public void setStatus(PharmacyOrderStatus status) { this.status = status; }
    public void setNote(String note) { this.note = note; }
}
