package com.neogaming.chat.repository;

import com.neogaming.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    void deleteByConversationId(UUID conversationId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readBySeller = true WHERE m.conversationId = :convId AND m.readBySeller = false")
    void markReadBySeller(UUID convId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readByBuyer = true WHERE m.conversationId = :convId AND m.readByBuyer = false")
    void markReadByBuyer(UUID convId);
}
