package com.neogaming.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Clase base para todas las entidades que requieren auditoría de fechas.
 *
 * Al extender esta clase, la entidad hereda automáticamente:
 *  - createdAt : fecha y hora de creación (se asigna una sola vez al persistir)
 *  - updatedAt : fecha y hora de última modificación (se actualiza en cada cambio)
 *
 * Requiere que @EnableJpaAuditing esté activo en la configuración (ver JpaConfig).
 * Las fechas se almacenan en TIMESTAMPTZ (con zona horaria) usando Instant de Java.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity {

    /** Fecha y hora de creación del registro. No se puede actualizar después de la inserción. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Fecha y hora de la última modificación del registro. Se actualiza automáticamente. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
