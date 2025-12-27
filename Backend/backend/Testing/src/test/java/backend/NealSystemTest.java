import backend.doctor.DoctorSyncService;
import backend.messages.*;
import backend.authentication.*;
import backend.logging.ErrorLogger;
import backend.Application;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class NealSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepo;

    @MockitoBean
    private ChatMessageRepository msgRepo;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ErrorLogger errorLogger;

    @MockitoBean
    private DoctorSyncService doctorSyncService;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @BeforeEach
    public void setup() {
        when(jwtUtil.isTokenExpired(anyString())).thenReturn(false);
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);

        User alice = new User();
        alice.setUsername("alice");
        alice.setUserType("USER");
        when(userRepo.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepo.findByUsername("alice")).thenReturn(alice);
    }

    @Test
    public void testStartGroupChat() throws Exception {
        when(userRepo.findByUsername("bob")).thenReturn(new User());
        when(userRepo.findByUsername("charlie")).thenReturn(new User());

        mockMvc.perform(post("/api/chat/start")
                        .header("Authorization", "Bearer valid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipient\": \"charlie, bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.conversationId", is("group-alice-bob-charlie")));
    }

    @Test
    public void testAccessDeniedHistory() throws Exception {
        mockMvc.perform(get("/api/chat/history/bob-dave")
                        .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not part of this conversation")));
    }

    @Test
    public void testFileUpload() throws Exception {
        when(msgRepo.save(org.mockito.ArgumentMatchers.any(ChatMessage.class))).thenAnswer(i -> {
            ChatMessage m = i.getArgument(0);
            m.setId(100L);
            return m;
        });

        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "content".getBytes());

        mockMvc.perform(multipart("/api/chat/attachment/upload")
                        .file(file)
                        .param("conversationId", "alice-bob")
                        .param("content", "My Notes")
                        .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAttachment", is(true)))
                .andExpect(jsonPath("$.attachmentFilename", is("notes.txt")));
    }

    @Test
    public void testFileDownload() throws Exception {
        ChatMessage msg = new ChatMessage("alice-bob", "bob", "USER", "msg");
        msg.setId(50L);
        msg.setHasAttachment(true);
        msg.setAttachmentFilename("img.png");
        msg.setAttachmentContentType("image/png");
        msg.setAttachmentData(new byte[]{1, 2, 3});
        msg.setAttachmentSize(3L);

        when(msgRepo.findById(50L)).thenReturn(Optional.of(msg));

        mockMvc.perform(get("/api/chat/attachment/download/50")
                        .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", containsString("img.png")));
    }
}