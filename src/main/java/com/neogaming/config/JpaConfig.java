package com.neogaming.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuración de JPA para NeoGaming.
 *
 * La anotación @EnableJpaAuditing activa el sistema de auditoría automática de Spring Data JPA.
 * Esto hace que los campos anotados con @CreatedDate y @LastModifiedDate en AuditableEntity
 * se llenen automáticamente al crear o actualizar cualquier entidad que extienda esa clase.
 *
 * Sin esta configuración, los campos createdAt y updatedAt quedarían en null.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // No requiere beans adicionales.
    // Spring Data JPA configura automáticamente el AuditingEntityListener
    // al detectar @EnableJpaAuditing en el contexto.
}
