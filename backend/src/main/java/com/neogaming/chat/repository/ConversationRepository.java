package com.neogaming.chat.repository;

import com.neogaming.chat.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByBuyerIdOrderByLastMessageAtDesc(UUID buyerId);

    List<Conversation> findBySellerIdOrderByLastMessageAtDesc(UUID sellerId);

    Optional<Conversation> findByBuyerIdAndSellerIdAndProductId(UUID buyerId, UUID sellerId, UUID productId);

    Optional<Conversation> findByBuyerIdAndSellerIdAndProductIdIsNull(UUID buyerId, UUID sellerId);

    List<Conversation> findByDirectUserIdOrderByLastMessageAtDesc(UUID directUserId);

    Optional<Conversation> findByBuyerIdAndDirectUserId(UUID buyerId, UUID directUserId);
}
