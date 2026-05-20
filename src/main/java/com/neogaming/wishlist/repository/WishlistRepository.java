package com.neogaming.wishlist.repository;

import com.neogaming.wishlist.domain.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Wishlist.
 */
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    /** Lista todas las listas de un usuario. */
    List<Wishlist> findByUserId(UUID userId);

    /** Busca una lista verificando que pertenezca al usuario (previene acceso cruzado). */
    Optional<Wishlist> findByIdAndUserId(UUID id, UUID userId);

    /** Busca una lista pública por ID (para compartir por enlace). */
    Optional<Wishlist> findByIdAndIsPublicTrue(UUID id);

    /** Cuenta las listas de un usuario — para limitar la cantidad máxima por usuario. */
    long countByUserId(UUID userId);
}
