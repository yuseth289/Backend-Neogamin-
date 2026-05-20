package com.neogaming.seller.repository;

import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.seller.domain.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Seller.
 *
 * Métodos de consulta usados por:
 *  - SellerService: gestión del perfil del vendedor
 *  - Admin panel: listado y aprobación de vendedores
 *  - Catálogo público: buscar tienda por slug
 */
public interface SellerRepository extends JpaRepository<Seller, UUID> {

    /**
     * Busca el perfil de vendedor asociado a un usuario específico.
     * Usado para verificar que un usuario ya tiene perfil antes de crear otro.
     *
     * @param userId UUID del usuario
     * @return El perfil del vendedor si existe
     */
    Optional<Seller> findByUserId(UUID userId);

    /**
     * Verifica si ya existe un perfil de vendedor para un usuario.
     * Previene la creación de perfiles duplicados.
     *
     * @param userId UUID del usuario
     * @return true si ya tiene perfil de vendedor
     */
    boolean existsByUserId(UUID userId);

    /**
     * Busca una tienda por su slug (URL amigable).
     * Usado en el endpoint público GET /sellers/{storeSlug}.
     *
     * @param storeSlug Slug de la tienda (ej: "gaming-shop-bogota")
     * @return La tienda si existe y sin filtrar por estado (el servicio valida)
     */
    Optional<Seller> findByStoreSlug(String storeSlug);

    /**
     * Verifica si ya existe una tienda con el slug dado.
     * Previene slugs duplicados al crear o actualizar un perfil.
     *
     * @param storeSlug Slug a verificar
     * @return true si el slug ya está en uso
     */
    boolean existsByStoreSlug(String storeSlug);

    /**
     * Lista vendedores filtrados por estado con paginación.
     * Usado en el panel de administración para revisar solicitudes pendientes.
     *
     * @param status   Estado a filtrar (PENDING, ACTIVE, SUSPENDED)
     * @param pageable Configuración de paginación y ordenamiento
     * @return Página de vendedores con el estado dado
     */
    Page<Seller> findByStatus(EstadoGenerico status, Pageable pageable);

    /**
     * Cuenta los vendedores con un estado dado.
     * Usado en el dashboard de administración para mostrar pendientes de aprobación.
     *
     * @param status Estado a contar
     * @return Número de vendedores con ese estado
     */
    long countByStatus(EstadoGenerico status);
}
