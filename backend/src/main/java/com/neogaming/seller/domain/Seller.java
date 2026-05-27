package com.neogaming.seller.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.TipoDocumentoVendedor;
import com.neogaming.common.enums.TipoRegimenFiscal;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que representa el perfil de un vendedor en NeoGaming.
 *
 * Un usuario con rol SELLER tiene exactamente un registro en esta tabla.
 * El perfil se crea cuando el usuario solicita convertirse en vendedor,
 * y debe ser aprobado por un ADMIN antes de poder publicar productos.
 *
 * Flujo de estados:
 *   PENDING  → [admin aprueba] → ACTIVE
 *   ACTIVE   → [admin suspende] → SUSPENDED
 *   SUSPENDED → [admin reactiva] → ACTIVE
 */
@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller extends AuditableEntity {

    /** Identificador único del perfil de vendedor */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Referencia al usuario propietario de este perfil.
     * Relación 1:1 — un usuario solo puede tener un perfil de vendedor.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    // ===== Datos de la tienda =====

    /** Nombre público de la tienda visible para los compradores */
    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    /**
     * Slug URL-amigable de la tienda (generado desde store_name).
     * Ejemplo: "tech-store-bogota", "gaming-shop"
     * Usado en la URL pública: /sellers/{storeSlug}
     */
    @Column(name = "store_slug", nullable = false, unique = true, length = 120)
    private String storeSlug;

    /** Descripción de la tienda que ven los compradores */
    @Column(name = "store_description", columnDefinition = "TEXT")
    private String storeDescription;

    /** URL o base64 del logo de la tienda. */
    @Column(name = "store_logo_url", columnDefinition = "TEXT")
    private String storeLogoUrl;

    /** URL o base64 del banner/portada de la tienda. */
    @Column(name = "store_banner_url", columnDefinition = "TEXT")
    private String storeBannerUrl;

    // ===== Datos fiscales colombianos =====

    /**
     * Tipo de documento de identidad del vendedor.
     * CC=Cédula de Ciudadanía, NIT=Empresa, CE=Cédula Extranjería, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumentoVendedor tipoDocumento;

    /** Número de identificación (cédula, NIT, etc.) */
    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    /**
     * Razón social de la empresa.
     * Obligatorio cuando tipoDocumento = NIT, opcional en los demás casos.
     */
    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    /**
     * Régimen fiscal del vendedor según DIAN Colombia.
     * Determina si el vendedor aplica IVA en sus ventas.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_regimen", nullable = false, length = 30)
    private TipoRegimenFiscal tipoRegimen;

    // ===== Datos de contacto =====

    /** Teléfono de contacto del vendedor */
    @Column(name = "phone", length = 20)
    private String phone;

    /** Dirección física del vendedor/empresa */
    @Column(name = "address", length = 300)
    private String address;

    /** Ciudad donde opera el vendedor */
    @Column(name = "city", length = 100)
    private String city;

    /** Departamento donde opera el vendedor (contexto colombiano) */
    @Column(name = "department", length = 100)
    private String department;

    // ===== Integración Mercado Pago Colombia =====

    /**
     * Access token de Mercado Pago obtenido tras el flujo OAuth.
     * Permite hacer split de pagos a la cuenta del vendedor.
     * ADVERTENCIA: Este campo debe estar cifrado en producción.
     */
    @Column(name = "mp_access_token", length = 500)
    private String mpAccessToken;

    /** ID de usuario en Mercado Pago (para identificar la cuenta destino del split) */
    @Column(name = "mp_user_id", length = 50)
    private String mpUserId;

    // ===== Estado y control =====

    /**
     * Estado del vendedor en la plataforma.
     * PENDING: esperando aprobación de un administrador.
     * ACTIVE: aprobado, puede publicar productos.
     * SUSPENDED: suspendido por incumplimiento.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EstadoGenerico status = EstadoGenerico.PENDING;
}
