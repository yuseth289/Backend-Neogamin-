package com.neogaming.address.repository;

import com.neogaming.address.domain.Address;
import com.neogaming.common.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad Address.
 *
 * Todos los métodos de búsqueda filtran por status = ACTIVE para ignorar
 * automáticamente las direcciones eliminadas (soft delete).
 */
public interface AddressRepository extends JpaRepository<Address, UUID> {

    /**
     * Lista todas las direcciones activas de un usuario, ordenadas con la
     * principal primero y luego por fecha de creación ascendente.
     *
     * @param userId UUID del usuario
     * @param status Estado a filtrar (normalmente ACTIVE)
     * @return Lista de direcciones ordenadas
     */
    List<Address> findByUserIdAndStatusOrderByPrimaryDescCreatedAtAsc(
            UUID userId, EstadoGenerico status);

    /**
     * Busca una dirección específica que pertenezca a un usuario y tenga un estado dado.
     * Se usa para validar que el usuario solo acceda a sus propias direcciones.
     *
     * @param id     UUID de la dirección
     * @param userId UUID del usuario dueño
     * @param status Estado a filtrar (normalmente ACTIVE)
     * @return Optional con la dirección si existe y pertenece al usuario
     */
    Optional<Address> findByIdAndUserIdAndStatus(UUID id, UUID userId, EstadoGenerico status);

    /**
     * Cuenta las direcciones activas de un usuario.
     * Se usa para determinar si la primera dirección creada debe ser la principal automáticamente.
     *
     * @param userId UUID del usuario
     * @param status Estado a filtrar
     * @return Cantidad de direcciones activas del usuario
     */
    long countByUserIdAndStatus(UUID userId, EstadoGenerico status);

    /**
     * Busca la dirección principal activa de un usuario.
     * Se usa para desmarcarla cuando se establece una nueva dirección principal.
     *
     * @param userId  UUID del usuario
     * @param status  Estado a filtrar (normalmente ACTIVE)
     * @param primary Debe ser true para buscar la principal
     * @return Optional con la dirección principal si existe
     */
    Optional<Address> findByUserIdAndStatusAndPrimary(
            UUID userId, EstadoGenerico status, boolean primary);

    /**
     * Busca la dirección activa más antigua del usuario (por fecha de creación).
     * Se usa para reasignar la dirección principal cuando se elimina la actual principal.
     *
     * @param userId UUID del usuario
     * @param status Estado a filtrar
     * @return Optional con la dirección activa más antigua
     */
    Optional<Address> findFirstByUserIdAndStatusOrderByCreatedAtAsc(
            UUID userId, EstadoGenerico status);
}
