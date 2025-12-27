package backend.notifications;

public class FcmTokenRegisterRequest {

    private String token;
    private String platform; // optional, default "android"
    // optional flag: when true we actually revoke (use on explicit logout only)
    private Boolean logout;

    public FcmTokenRegisterRequest() {}

    public FcmTokenRegisterRequest(String token, String platform) {
        this.token = token;
        this.platform = platform;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Boolean getLogout() {
        return logout;
    }

    public void setLogout(Boolean logout) {
        this.logout = logout;
    }
}
