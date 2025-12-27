package backend.messages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import backend.authentication.User;
import backend.authentication.UserRepository;
import backend.authentication.JwtUtil;
import backend.logging.ErrorLogger;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ErrorLogger errorLogger;

    @Autowired
    private JwtUtil jwtUtil;

    // --------------------------------------------------------
    // EXTRACT USER FROM JWT
    // --------------------------------------------------------
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

    // --------------------------------------------------------
    // START PRIVATE OR GROUP CHAT
    // --------------------------------------------------------
    @PostMapping("/start")
    @Operation(summary = "Start a new chat conversation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing recipient data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized request"),
            @ApiResponse(responseCode = "404", description = "Recipient not found")
    })
    public ResponseEntity<?> startConversation(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        try {
            String currentUsername = extractUsernameFromRequest(httpRequest);
            String recipientInput = request.get("recipient");

            if (recipientInput == null || recipientInput.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(error("Recipient username(s) is required"));
            }

            String[] recipientArray = recipientInput.split(",");
            List<String> recipients = new ArrayList<>();

            for (String recipient : recipientArray) {
                String trimmed = recipient.trim();
                if (!trimmed.isEmpty()) {
                    recipients.add(trimmed);
                }
            }

            // remove duplicates and self
            recipients = new ArrayList<>(new LinkedHashSet<>(recipients));
            recipients.removeIf(r -> r.equalsIgnoreCase(currentUsername));

            if (recipients.isEmpty()) {
                return ResponseEntity.badRequest().body(error("Cannot start a conversation with yourself"));
            }

            // Validate recipients exist
            for (String recipient : recipients) {
                if (userRepository.findByUsername(recipient) == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(error("User '" + recipient + "' not found"));
                }
            }

            String conversationId;
            String conversationType;

            if (recipients.size() == 1) {
                conversationId = createConversationId(currentUsername, recipients.get(0));
                conversationType = "direct";
            } else {
                conversationId = createGroupConversationId(currentUsername, recipients);
                conversationType = "group";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("conversationId", conversationId);
            response.put("recipients", String.join(", ", recipients));
            response.put("conversationType", conversationType);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("Unauthorized: " + e.getMessage()));
        }
    }

    // --------------------------------------------------------
    // GET USER'S CHAT LIST (INCLUDING AI CHAT)
    // --------------------------------------------------------
    @GetMapping("/conversations")
    @Operation(summary = "Get all conversations for the authenticated user")
    public ResponseEntity<?> getUserConversations(HttpServletRequest httpRequest) {
        try {
            String username = extractUsernameFromRequest(httpRequest);
            String aiConversationId = "assistant&" + username;

            // --------- AUTO-CREATE AI CHAT IF NEEDED ----------
            boolean aiExists = messageRepository.existsByConversationId(aiConversationId);

            if (!aiExists) {
                ChatMessage welcome = new ChatMessage(
                        aiConversationId,
                        "Assistant",
                        "AI",
                        "Hello! I am your personal health assistant. You can ask about symptoms, medications, " +
                                "appointments, and general health questions."
                );
                messageRepository.save(welcome);
            }

            // --------- GATHER ALL USER CONVERSATIONS ----------
            List<ChatMessage> allMessages = messageRepository.findAll();
            Set<String> allConversationIds = new HashSet<>();

            for (ChatMessage msg : allMessages) {
                allConversationIds.add(msg.getConversationId());
            }

            List<String> userConversations = new ArrayList<>();

            for (String convId : allConversationIds) {
                // skip AI conv here; we add it as a special entry below
                if (convId.equals(aiConversationId)) {
                    continue;
                }
                if (isUserInConversation(convId, username)) {
                    userConversations.add(convId);
                }
            }

            List<Map<String, Object>> conversations = new ArrayList<>();

            for (String conversationId : userConversations) {
                List<ChatMessage> messages =
                        messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);

                if (!messages.isEmpty()) {
                    ChatMessage lastMessage = messages.get(messages.size() - 1);

                    Map<String, Object> conv = new HashMap<>();
                    conv.put("conversationId", conversationId);
                    conv.put("isGroup", conversationId.startsWith("group-"));
                    conv.put("lastMessage", lastMessage.getContent());
                    conv.put("lastMessageSender", lastMessage.getSender());
                    conv.put("lastMessageTime", lastMessage.getTimestamp());

                    if (!conversationId.startsWith("group-") &&
                            !conversationId.startsWith("assistant&")) {
                        conv.put("otherUser", getOtherUsername(conversationId, username));
                    }

                    if (Boolean.TRUE.equals(lastMessage.getHasAttachment())) {
                        conv.put("lastMessageHasAttachment", true);
                        conv.put("lastMessageAttachmentFilename", lastMessage.getAttachmentFilename());
                    }

                    conversations.add(conv);
                }
            }

            // --------- ALWAYS INCLUDE AI CHAT EXACTLY ONCE ---------
            ChatMessage latestAI =
                    messageRepository.findTopByConversationIdOrderByTimestampDesc(aiConversationId);

            if (latestAI != null) {
                Map<String, Object> aiConv = new HashMap<>();
                aiConv.put("conversationId", aiConversationId);
                aiConv.put("isGroup", false);
                aiConv.put("otherUser", "Assistant");
                aiConv.put("lastMessage", latestAI.getContent());
                aiConv.put("lastMessageSender", latestAI.getSender());
                aiConv.put("lastMessageTime", latestAI.getTimestamp());

                conversations.add(aiConv);
            }

            // Sort by most recent messages (descending)
            conversations.sort((a, b) -> {
                var t1 = (java.time.LocalDateTime) a.get("lastMessageTime");
                var t2 = (java.time.LocalDateTime) b.get("lastMessageTime");
                return t2.compareTo(t1);
            });

            return ResponseEntity.ok(conversations);

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("Unauthorized: " + e.getMessage()));
        }
    }

    // --------------------------------------------------------
    // GET MESSAGE HIST
    // --------------------------------------------------------
    @GetMapping("/history/{conversationId}")
    @Operation(summary = "Get message history for a conversation")
    public ResponseEntity<?> getChatHistory(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {

        try {
            String username = extractUsernameFromRequest(httpRequest);

            if (!isUserInConversation(conversationId, username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(error("You are not part of this conversation"));
            }

            List<ChatMessage> messages =
                    messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);

            List<Map<String, Object>> dtoList = new ArrayList<>();

            for (ChatMessage msg : messages) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", msg.getId());
                dto.put("conversationId", msg.getConversationId());
                dto.put("sender", msg.getSender());
                dto.put("role", msg.getRole());
                dto.put("content", msg.getContent());
                dto.put("timestamp", msg.getTimestamp());
                dto.put("hasAttachment", msg.getHasAttachment());

                if (Boolean.TRUE.equals(msg.getHasAttachment())) {
                    dto.put("attachmentFilename", msg.getAttachmentFilename());
                    dto.put("attachmentContentType", msg.getAttachmentContentType());
                    dto.put("attachmentSize", msg.getAttachmentSize());
                }

                dtoList.add(dto);
            }

            return ResponseEntity.ok(dtoList);

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("Unauthorized: " + e.getMessage()));
        }
    }

    // --------------------------------------------------------
    // DELETE CHAT HISTORY
    // --------------------------------------------------------
    @DeleteMapping("/history/{conversationId}")
    public ResponseEntity<?> deleteChatHistory(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {

        try {
            String username = extractUsernameFromRequest(httpRequest);

            if (!isUserInConversation(conversationId, username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(error("You are not part of this conversation"));
            }

            messageRepository.deleteByConversationId(conversationId);
            return ResponseEntity.ok(success("Deleted"));

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("Unauthorized: " + e.getMessage()));
        }
    }

    // --------------------------------------------------------
    // UTILITIES
    // --------------------------------------------------------
    private String createGroupConversationId(String currentUser, List<String> recipients) {
        List<String> all = new ArrayList<>(recipients);
        all.add(currentUser);
        all.sort(String.CASE_INSENSITIVE_ORDER);
        return "group-" + String.join("-", all);
    }

    private String createConversationId(String u1, String u2) {
        return (u1.compareToIgnoreCase(u2) < 0)
                ? u1 + "-" + u2
                : u2 + "-" + u1;
    }

    private boolean isUserInConversation(String conversationId, String username) {

        // AI chat permission
        if (conversationId.equals("assistant&" + username)) {
            return true;
        }

        // normal conversation
        String id = conversationId.startsWith("group-")
                ? conversationId.substring(6)
                : conversationId;

        String[] parts = id.split("-");
        if (parts.length < 2) {
            return false;
        }

        for (String p : parts) {
            if (p.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    private String getOtherUsername(String conversationId, String currentUsername) {
        String[] parts = conversationId.split("-");
        if (parts.length != 2) {
            return "";
        }
        return parts[0].equalsIgnoreCase(currentUsername) ? parts[1] : parts[0];
    }

    private Map<String, String> error(String msg) {
        return Map.of("status", "error", "message", msg);
    }

    private Map<String, String> success(String msg) {
        return Map.of("status", "success", "message", msg);
    }
}
