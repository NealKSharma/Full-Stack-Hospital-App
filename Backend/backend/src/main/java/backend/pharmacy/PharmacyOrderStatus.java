package backend.pharmacy;

public enum PharmacyOrderStatus {
    PENDING("Filling"),
    READY_FOR_PICKUP("Ready for pickup"),
    FULFILLED("Fulfilled"),
    REJECTED("Rejected");

    private final String displayName;

    PharmacyOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
