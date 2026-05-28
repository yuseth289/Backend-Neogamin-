package com.neogaming.user.repository;

import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.RolUsuario;
import com.neogaming.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad User.
 *
 * Extiende JpaRepository para obtener automáticamente las operaciones
 * CRUD estándar (save, findById, findAll, delete, etc.).
 *
 * Los métodos adicionales se derivan automáticamente por Spring Data JPA
 * a partir del nombre del método (no requieren implementación manual).
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Busca un usuario por su email.
     * Se usa en el login y en UserDetailsServiceImpl.
     *
     * @param email Email del usuario a buscar
     * @return Optional con el usuario si existe, vacío si no
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica si ya existe un usuario registrado con el email dado.
     * Se usa en el registro para evitar duplicados.
     *
     * @param email Email a verificar
     * @return true si el email ya está en uso
     */
    boolean existsByEmail(String email);

    Page<User> findByRoleNot(RolUsuario role, Pageable pageable);

    Page<User> findByRoleNotAndStatus(RolUsuario role, EstadoGenerico status, Pageable pageable);
}
