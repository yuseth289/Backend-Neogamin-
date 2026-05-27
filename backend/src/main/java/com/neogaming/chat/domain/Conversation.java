package com.neogaming.chat.domain;

import com.neogaming.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"buyer_id", "seller_id", "product_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** User.id del comprador */
    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    /** User.id del vendedor */
    @Column(name = "seller_user_id", nullable = false)
    private UUID sellerUserId;

    /** Seller.id (para mostrar nombre/logo de tienda) */
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    /** Producto que originó la conversación (opcional) */
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "unread_buyer", nullable = false)
    @Builder.Default
    private int unreadBuyer = 0;

    @Column(name = "unread_seller", nullable = false)
    @Builder.Default
    private int unreadSeller = 0;
}
