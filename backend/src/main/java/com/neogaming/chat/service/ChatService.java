package com.neogaming.chat.service;

import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.chat.domain.ChatMessage;
import com.neogaming.chat.domain.Conversation;
import com.neogaming.chat.dto.request.SendMessageRequest;
import com.neogaming.chat.dto.request.StartConversationRequest;
import com.neogaming.chat.dto.response.ConversationResponse;
import com.neogaming.chat.dto.response.MessageResponse;
import com.neogaming.chat.repository.ChatMessageRepository;
import com.neogaming.chat.repository.ConversationRepository;
import com.neogaming.common.enums.RolMensaje;
import com.neogaming.common.exception.ForbiddenException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.seller.domain.Seller;
import com.neogaming.seller.repository.SellerRepository;
import com.neogaming.user.domain.User;
import com.neogaming.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final SellerRepository sellerRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    public List<ConversationResponse> listConversations(UUID currentUserId) {
        Seller mySeller = sellerRepo.findByUserId(currentUserId).orElse(null);

        List<Conversation> convs;
        if (mySeller != null) {
            convs = conversationRepo.findBySellerIdOrderByLastMessageAtDesc(mySeller.getId());
        } else {
            List<Conversation> buyerConvs = conversationRepo.findByBuyerIdOrderByLastMessageAtDesc(currentUserId);
            List<Conversation> directConvs = conversationRepo.findByDirectUserIdOrderByLastMessageAtDesc(currentUserId);
            convs = new java.util.ArrayList<>(buyerConvs);
            convs.addAll(directConvs);
            convs.sort(java.util.Comparator.comparing(
                    Conversation::getLastMessageAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        }

        return convs.stream().map(c -> toConversationResponse(c, currentUserId, mySeller)).toList();
    }

    public ConversationResponse adminStartConversation(UUID targetUserId, String firstMessage, UUID adminId) {
        userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", targetUserId.toString()));

        Conversation conv = conversationRepo
                .findByBuyerIdAndDirectUserId(adminId, targetUserId)
                .orElseGet(() -> conversationRepo.save(Conversation.builder()
                        .buyerId(adminId)
                        .directUserId(targetUserId)
                        .lastMessageAt(Instant.now())
                        .build()));

        sendMessage(conv.getId(), new SendMessageRequest(firstMessage), adminId);
        conv = conversationRepo.findById(conv.getId()).orElseThrow();
        return toConversationResponse(conv, adminId, null);
    }

    public ConversationResponse startConversation(StartConversationRequest req, UUID buyerId) {
        Seller seller = sellerRepo.findById(req.sellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Tienda", req.sellerId().toString()));

        Conversation conv;
        if (req.productId() != null) {
            conv = conversationRepo
                    .findByBuyerIdAndSellerIdAndProductId(buyerId, seller.getId(), req.productId())
                    .orElseGet(() -> conversationRepo.save(Conversation.builder()
                            .buyerId(buyerId)
                            .sellerUserId(seller.getUserId())
                            .sellerId(seller.getId())
                            .productId(req.productId())
                            .lastMessageAt(Instant.now())
                            .build()));
        } else {
            conv = conversationRepo
                    .findByBuyerIdAndSellerIdAndProductIdIsNull(buyerId, seller.getId())
                    .orElseGet(() -> conversationRepo.save(Conversation.builder()
                            .buyerId(buyerId)
                            .sellerUserId(seller.getUserId())
                            .sellerId(seller.getId())
                            .lastMessageAt(Instant.now())
                            .build()));
        }

        sendMessage(conv.getId(), new SendMessageRequest(req.firstMessage()), buyerId);
        conv = conversationRepo.findById(conv.getId()).orElseThrow();
        return toConversationResponse(conv, buyerId, null);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID convId, UUID currentUserId) {
        Conversation conv = getAndValidate(convId, currentUserId);
        Seller mySeller = sellerRepo.findByUserId(currentUserId).orElse(null);
        return toConversationResponse(conv, currentUserId, mySeller);
    }

    public List<MessageResponse> getMessages(UUID convId, UUID currentUserId) {
        Conversation conv = getAndValidate(convId, currentUserId);
        markRead(conv, currentUserId);
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(convId)
                .stream().map(this::toMessageResponse).toList();
    }

    public MessageResponse sendMessage(UUID convId, SendMessageRequest req, UUID senderId) {
        Conversation conv = getAndValidate(convId, senderId);
        Seller mySeller = sellerRepo.findByUserId(senderId).orElse(null);
        boolean isDirectRecipient = conv.getDirectUserId() != null && conv.getDirectUserId().equals(senderId);
        RolMensaje role = (isDirectRecipient
                || (mySeller != null && conv.getSellerId() != null && mySeller.getId().equals(conv.getSellerId())))
                ? RolMensaje.SELLER : RolMensaje.BUYER;

        ChatMessage msg = messageRepo.save(ChatMessage.builder()
                .conversationId(convId)
                .senderId(senderId)
                .senderRole(role)
                .content(req.content())
                .readByBuyer(role == RolMensaje.BUYER)
                .readBySeller(role == RolMensaje.SELLER)
                .build());

        conv.setLastMessage(req.content().length() > 80
                ? req.content().substring(0, 80) + "…" : req.content());
        conv.setLastMessageAt(Instant.now());
        if (role == RolMensaje.BUYER) conv.setUnreadSeller(conv.getUnreadSeller() + 1);
        else conv.setUnreadBuyer(conv.getUnreadBuyer() + 1);
        conversationRepo.save(conv);

        return toMessageResponse(msg);
    }

    public void deleteConversation(UUID convId, UUID currentUserId) {
        Conversation conv = getAndValidate(convId, currentUserId);
        messageRepo.deleteByConversationId(conv.getId());
        conversationRepo.delete(conv);
    }

    public void markRead(UUID convId, UUID currentUserId) {
        Conversation conv = getAndValidate(convId, currentUserId);
        markRead(conv, currentUserId);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void markRead(Conversation conv, UUID userId) {
        Seller mySeller = sellerRepo.findByUserId(userId).orElse(null);
        boolean isDirectRecipient = conv.getDirectUserId() != null && conv.getDirectUserId().equals(userId);
        boolean isSeller = isDirectRecipient
                || (mySeller != null && conv.getSellerId() != null && mySeller.getId().equals(conv.getSellerId()));
        if (isSeller) {
            messageRepo.markReadBySeller(conv.getId());
            conv.setUnreadSeller(0);
        } else {
            messageRepo.markReadByBuyer(conv.getId());
            conv.setUnreadBuyer(0);
        }
        conversationRepo.save(conv);
    }

    private Conversation getAndValidate(UUID convId, UUID userId) {
        Conversation conv = conversationRepo.findById(convId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación", convId.toString()));
        Seller mySeller = sellerRepo.findByUserId(userId).orElse(null);
        boolean isBuyer = conv.getBuyerId().equals(userId);
        boolean isSeller = mySeller != null && conv.getSellerId() != null && mySeller.getId().equals(conv.getSellerId());
        boolean isDirectRecipient = conv.getDirectUserId() != null && conv.getDirectUserId().equals(userId);
        if (!isBuyer && !isSeller && !isDirectRecipient)
            throw new ForbiddenException("No tienes acceso a esta conversación");
        return conv;
    }

    private ConversationResponse toConversationResponse(Conversation c, UUID currentUserId, Seller mySeller) {
        User buyer = userRepo.findById(c.getBuyerId()).orElse(null);
        Seller seller = (c.getSellerId() != null) ? sellerRepo.findById(c.getSellerId()).orElse(null) : null;

        boolean isDirectRecipient = c.getDirectUserId() != null && c.getDirectUserId().equals(currentUserId);
        boolean isSellerSide = isDirectRecipient
                || (mySeller != null && c.getSellerId() != null && mySeller.getId().equals(c.getSellerId()));
        int unread = isSellerSide ? c.getUnreadSeller() : c.getUnreadBuyer();

        String displayName;
        String storeSlug = null;
        String storeLogoUrl = null;
        if (seller != null) {
            displayName  = seller.getStoreName();
            storeSlug    = seller.getStoreSlug();
            storeLogoUrl = seller.getStoreLogoUrl();
        } else if (c.getDirectUserId() != null) {
            if (isDirectRecipient) {
                displayName = "Soporte NeoGaming";
            } else {
                User target = userRepo.findById(c.getDirectUserId()).orElse(null);
                displayName = target != null ? target.getFirstName() + " " + target.getLastName() : "Usuario";
            }
        } else {
            displayName = "Tienda";
        }

        String productName = null;
        if (c.getProductId() != null) {
            productName = productRepo.findById(c.getProductId()).map(Product::getName).orElse(null);
        }

        return new ConversationResponse(
                c.getId(),
                c.getBuyerId(),
                buyer != null ? buyer.getFirstName() + " " + buyer.getLastName() : "Usuario",
                c.getSellerId(),
                displayName,
                storeSlug,
                storeLogoUrl,
                c.getProductId(),
                productName,
                c.getLastMessage(),
                c.getLastMessageAt(),
                unread,
                c.getCreatedAt()
        );
    }

    private MessageResponse toMessageResponse(ChatMessage m) {
        User sender = userRepo.findById(m.getSenderId()).orElse(null);
        String name = sender != null ? sender.getFirstName() + " " + sender.getLastName() : "Usuario";
        String avatar = sender != null ? sender.getAvatarUrl() : null;
        return new MessageResponse(m.getId(), m.getConversationId(), m.getSenderId(),
                m.getSenderRole(), name, avatar, m.getContent(),
                m.isReadByBuyer(), m.isReadBySeller(), m.getCreatedAt());
    }
}
