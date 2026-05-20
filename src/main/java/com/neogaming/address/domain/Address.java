package com.neogaming.address.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoGenerico;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que representa una dirección física de un usuario en NeoGaming.
 *
 * Un usuario puede tener múltiples direcciones (casa, trabajo, etc.) pero solo
 * una puede ser la principal (isPrimary = true). La dirección principal se sugiere
 * automáticamente en el checkout como dirección de envío.
 *
 * Las direcciones no se eliminan físicamente de la base de datos. Cuando el usuario
 * las "elimina", el estado cambia a DELETED (soft delete). Esto preserva el historial
 * de pedidos que referenciaron esa dirección.
 *
 * Formato de dirección colombiana:
 *  - street  : nombre de la vía  (ej: "Cra 7", "Calle 32", "Av. El Dorado")
 *  - number  : número de la vía  (ej: "# 32-15")
 *  - floor   : piso              (ej: "3", "PH")
 *  - apartment: apartamento      (ej: "301", "A")
 *  - department: departamento    (ej: "Cundinamarca", "Antioquia")
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** UUID del usuario dueño de esta dirección */
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    /** Etiqueta descriptiva para identificar la dirección (ej: "Casa", "Oficina", "Mamá") */
    @Column(nullable = false, length = 50)
    private String label;

    /** Nombre de la vía principal (ej: "Cra 7", "Calle 32", "Av. El Dorado") */
    @Column(nullable = false, length = 200)
    private String street;

    /** Número de la dirección (ej: "# 32-15") */
    @Column(nullable = false, length = 20)
    private String number;

    /** Piso del edificio. Opcional. */
    @Column(length = 10)
    private String floor;

    /** Número o letra del apartamento. Opcional. */
    @Column(length = 10)
    private String apartment;

    /** Ciudad o municipio (ej: "Bogotá", "Medellín", "Cali") */
    @Column(nullable = false, length = 100)
    private String city;

    /** Departamento colombiano (ej: "Cundinamarca", "Antioquia", "Valle del Cauca") */
    @Column(nullable = false, length = 100)
    private String department;

    /** País. Por defecto Colombia para NeoGaming. */
    @Column(nullable = false, length = 100)
    @Builder.Default
    private String country = "Colombia";

    /** Código postal colombiano. Opcional (poco usado en Colombia). */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * Indica si esta es la dirección principal del usuario.
     * Solo puede haber una dirección principal por usuario a la vez.
     * Se establece en el servicio con lógica de exclusividad.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    /**
     * Estado de la dirección.
     * ACTIVE  → visible y usable por el usuario
     * DELETED → eliminada por el usuario (soft delete, no se borra de la BD)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoGenerico status = EstadoGenerico.ACTIVE;
}
