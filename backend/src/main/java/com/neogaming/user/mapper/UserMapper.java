package com.neogaming.user.mapper;

import com.neogaming.user.domain.User;
import com.neogaming.user.dto.response.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper responsable de convertir entidades User a DTOs de respuesta.
 *
 * El mapeo es manual (sin MapStruct) para mantener las dependencias simples
 * y tener control explícito sobre qué campos se exponen al cliente.
 *
 * Regla principal: passwordHash nunca debe aparecer en ningún DTO de respuesta.
 */
@Component
public class UserMapper {

    /**
     * Convierte una entidad User en un UserResponse listo para enviar al cliente.
     *
     * @param user Entidad persistida en la base de datos
     * @return DTO con los campos públicos del usuario (sin passwordHash)
     */
    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
