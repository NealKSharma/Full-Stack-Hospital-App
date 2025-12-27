package backend.authentication;

public class SignupRequest {
    private String username;
    private String password;
    private String email;
    private String userType;

    public SignupRequest() {}

    public SignupRequest(String username, String password, String email, String userType) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.userType = userType;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getUserType() { return userType; }

    public void setUserType(String userType) { this.userType = userType; }
}