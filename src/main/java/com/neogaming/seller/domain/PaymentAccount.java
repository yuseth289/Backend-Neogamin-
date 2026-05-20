package com.neogaming.seller.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.TipoCuentaPago;
import com.neogaming.common.enums.TipoDocumentoVendedor;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Cuenta bancaria del vendedor para recibir pagos en NeoGaming.
 *
 * Un vendedor puede registrar múltiples cuentas bancarias, pero solo
 * una puede estar activa (is_active = true) en un momento dado.
 * La cuenta activa es la que recibe los pagos del split de Mercado Pago.
 *
 * En producción, el campo accountNumber debe estar cifrado con AES-256
 * antes de persistirse en la base de datos.
 */
@Entity
@Table(name = "seller_payment_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAccount extends AuditableEntity {

    /** Identificador único de la cuenta bancaria */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Vendedor propietario de esta cuenta.
     * Referencia a Seller por UUID — no usamos @ManyToOne para evitar
     * carga accidental de la entidad completa en cada consulta.
     */
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    // ===== Datos bancarios colombianos =====

    /** Nombre del banco (ej: "Bancolombia", "Davivienda", "Nequi") */
    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    /**
     * Tipo de cuenta bancaria.
     * AHORROS o CORRIENTE — los dos tipos disponibles en Colombia.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private TipoCuentaPago accountType;

    /**
     * Número de cuenta bancaria.
     * ADVERTENCIA: En producción este campo debe estar cifrado con AES-256
     * antes de guardarse en la base de datos.
     */
    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    /** Nombre completo del titular de la cuenta */
    @Column(name = "account_holder", nullable = false, length = 200)
    private String accountHolder;

    /** Tipo de documento del titular (CC, NIT, CE) */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private TipoDocumentoVendedor documentType;

    /** Número de documento del titular */
    @Column(name = "document_number", nullable = false, length = 30)
    private String documentNumber;

    /**
     * Indica si esta es la cuenta activa para recibir pagos.
     * Solo una cuenta por vendedor puede tener is_active = true.
     * Al activar una cuenta, las demás se desactivan automáticamente.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = false;
}
