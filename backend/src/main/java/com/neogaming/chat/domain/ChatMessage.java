package com.neogaming.chat.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.RolMensaje;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    /** User.id del que envía */
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 10)
    private RolMensaje senderRole;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "read_by_buyer", nullable = false)
    @Builder.Default
    private boolean readByBuyer = false;

    @Column(name = "read_by_seller", nullable = false)
    @Builder.Default
    private boolean readBySeller = false;
}
