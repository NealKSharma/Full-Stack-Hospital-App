package backend.messages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByTimestampAsc(String conversationId);

    List<ChatMessage> findTop100ByConversationIdOrderByTimestampDesc(String conversationId);

    // Get **latest** message for a conversation (newest one)
    ChatMessage findTopByConversationIdOrderByTimestampDesc(String conversationId);

    boolean existsByConversationId(String conversationId);

    @Transactional
    void deleteByConversationId(String conversationId);
}