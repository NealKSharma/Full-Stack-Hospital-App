package backend.messages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import backend.authentication.User;
import backend.authentication.UserRepository;
import backend.authentication.JwtUtil;
import backend.logging.ErrorLogger;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

@RestController
@RequestMapping("/api/chat/attachment")
public class AttachmentController {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ErrorLogger errorLogger;

    @Autowired
    private JwtUtil jwtUtil;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    // Allowed file types
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "text/plain"
    ));

    /**
     * Extract username from JWT token
     */
    private String extractUsernameFromRequest(HttpServletRequest request) throws Exception {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new Exception("No valid authorization header");
        }

        String token = authHeader.substring(7);

        if (jwtUtil.isTokenExpired(token)) {
            throw new Exception("Token expired");
        }

        Long userId = jwtUtil.extractUserId(token);
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }

        return userOpt.get().getUsername();
    }

    /**
     * Upload attachment and create message
     * POST /api/chat/attachment/upload
     *
     * Form data:
     *   - file: MultipartFile
     *   - conversationId: String
     *   - content: String (optional message text)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") String conversationId,
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            HttpServletRequest httpRequest) {

        try {
            String currentUsername = extractUsernameFromRequest(httpRequest);

            // Validate conversation access
            if (!isUserInConversation(conversationId, currentUsername)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(error("You are not part of this conversation"));
            }

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(error("File is empty"));
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(error("File size exceeds maximum limit of 25MB"));
            }

            // Check content type
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest()
                        .body(error("File type not allowed. Allowed types: PDF, JPG, PNG, GIF, TXT"));
            }

            // Get user role
            String role = getUserRole(currentUsername);

            // Create message with attachment
            ChatMessage message = new ChatMessage(conversationId, currentUsername, role, content);
            message.setHasAttachment(true);
            message.setAttachmentFilename(file.getOriginalFilename());
            message.setAttachmentContentType(contentType);
            message.setAttachmentSize(file.getSize());
            message.setAttachmentData(file.getBytes());

            ChatMessage savedMessage = messageRepository.save(message);

            ChatRoomServer.broadcastAttachment(
                    savedMessage.getId(),
                    conversationId,
                    currentUsername,
                    content,
                    savedMessage.getAttachmentFilename()
            );

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("messageId", savedMessage.getId());
            response.put("conversationId", conversationId);
            response.put("sender", currentUsername);
            response.put("content", content);
            response.put("timestamp", savedMessage.getTimestamp());
            response.put("hasAttachment", true);
            response.put("attachmentFilename", savedMessage.getAttachmentFilename());
            response.put("attachmentContentType", savedMessage.getAttachmentContentType());
            response.put("attachmentSize", savedMessage.getAttachmentSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Error uploading attachment: " + e.getMessage()));
        }
    }

    /**
     * Download attachment
     * GET /api/chat/attachment/download/{messageId}
     */
    @GetMapping("/download/{messageId}")
    public ResponseEntity<?> downloadAttachment(
            @PathVariable Long messageId,
            HttpServletRequest httpRequest) {

        try {
            String currentUsername = extractUsernameFromRequest(httpRequest);

            Optional<ChatMessage> messageOpt = messageRepository.findById(messageId);
            if (!messageOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("Message not found"));
            }

            ChatMessage message = messageOpt.get();

            // Validate conversation access
            if (!isUserInConversation(message.getConversationId(), currentUsername)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(error("You are not authorized to access this attachment"));
            }

            // Check if message has attachment
            if (!Boolean.TRUE.equals(message.getHasAttachment()) || message.getAttachmentData() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("No attachment found for this message"));
            }

            // Prepare file download response
            ByteArrayResource resource = new ByteArrayResource(message.getAttachmentData());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + message.getAttachmentFilename() + "\"")
                    .contentType(MediaType.parseMediaType(message.getAttachmentContentType()))
                    .contentLength(message.getAttachmentSize())
                    .body(resource);

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Error downloading attachment: " + e.getMessage()));
        }
    }



    /**
     * Helper: Check if user is part of conversation
     */
    private boolean isUserInConversation(String conversationId, String username) {
        String[] participants = conversationId.split("-");
        for (String participant : participants) {
            if (participant.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper: Get user role
     */
    private String getUserRole(String username) {
        User user = userRepository.findByUsername(username);
        return (user != null && user.getUserType() != null) ? user.getUserType() : "USER";
    }

    /**
     * Helper: Create error response
     */
    private Map<String, String> error(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("status", "error");
        res.put("message", message);
        return res;
    }
}