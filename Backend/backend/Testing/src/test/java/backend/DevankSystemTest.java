import backend.authentication.*;
import backend.patient.Patient;
import backend.patient.PatientRepository;
import backend.logging.ErrorLogger;
import backend.doctor.DoctorSyncService;
import backend.Application;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class DevankSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepo;

    @MockitoBean
    private PatientRepository patientRepo;

    @MockitoBean
    private ErrorLogger errorLogger;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private DoctorSyncService doctorSyncService;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @BeforeEach
    public void setup() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
    }

    @Test
    public void testSignupSuccess() throws Exception {
        when(userRepo.existsByUsername("alice")).thenReturn(false);
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(patientRepo.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"alice\", " +
                                "\"password\": \"password123\", " +
                                "\"email\": \"alice@test.com\", " +
                                "\"userType\": \"PATIENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", containsString("Signup successful")));
    }

    @Test
    public void testSignupUsernameExists() throws Exception {
        when(userRepo.existsByUsername("alice")).thenReturn(true);

        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"alice\", " +
                                "\"password\": \"password123\", " +
                                "\"email\": \"alice@test.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("Username already exists")));
    }

    @Test
    public void testSignupEmptyUsername() throws Exception {
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"\", " +
                                "\"password\": \"password123\", " +
                                "\"email\": \"test@test.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("Username cannot be empty")));
    }

    @Test
    public void testGetAllUsers() throws Exception {
        User user1 = new User();
        user1.setUsername("alice");
        user1.setEmail("alice@test.com");
        user1.setUserType("PATIENT");

        User user2 = new User();
        user2.setUsername("bob");
        user2.setEmail("bob@test.com");
        user2.setUserType("DOCTOR");

        List<User> users = Arrays.asList(user1, user2);
        when(userRepo.findAll()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("alice")))
                .andExpect(jsonPath("$[1].username", is("bob")));
    }
}