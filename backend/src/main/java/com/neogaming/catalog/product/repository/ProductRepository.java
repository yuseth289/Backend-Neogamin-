package com.neogaming.catalog.product.repository;

import com.neogaming.catalog.product.domain.Product;
import com.neogaming.common.enums.EstadoProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Product.
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Busca un producto por su slug.
     * Usado en el endpoint público GET /products/{slug}.
     *
     * @param slug Slug único del producto
     * @return El producto si existe
     */
    Optional<Product> findBySlug(String slug);

    /**
     * Verifica si ya existe un producto con el slug dado.
     * Previene duplicados al crear o actualizar productos.
     *
     * @param slug Slug a verificar
     * @return true si el slug ya está en uso
     */
    boolean existsBySlug(String slug);

    /**
     * Lista los productos de un vendedor filtrados por estado con paginación.
     * Usado en el panel del vendedor para gestionar su catálogo.
     *
     * @param sellerId UUID del vendedor
     * @param status   Estado a filtrar (DRAFT, ACTIVE, PAUSED, DELETED)
     * @param pageable Paginación y ordenamiento
     * @return Página de productos del vendedor
     */
    Page<Product> findBySellerIdAndStatus(UUID sellerId, EstadoProducto status, Pageable pageable);

    /**
     * Lista todos los productos activos del catálogo público con paginación.
     * Retorna productos de todos los vendedores activos.
     *
     * @param status   Estado (siempre ACTIVE para el catálogo público)
     * @param pageable Paginación y ordenamiento
     * @return Página de productos activos
     */
    Page<Product> findByStatus(EstadoProducto status, Pageable pageable);

    /**
     * Lista productos activos filtrados por categoría para el catálogo público.
     *
     * @param categoryId UUID de la categoría
     * @param status     Estado (ACTIVE)
     * @param pageable   Paginación
     * @return Página de productos de la categoría
     */
    Page<Product> findByCategoryIdAndStatus(UUID categoryId, EstadoProducto status, Pageable pageable);

    /**
     * Búsqueda de texto completo en nombre y descripción del producto.
     * Usa ILIKE de PostgreSQL para búsqueda insensible a mayúsculas.
     *
     * @param query    Texto a buscar
     * @param status   Estado (ACTIVE para catálogo público)
     * @param pageable Paginación
     * @return Productos que coinciden con el texto
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Product> buscarPorTextoYEstado(String query, EstadoProducto status, Pageable pageable);

    /**
     * Busca un producto verificando que pertenece al vendedor.
     * Centraliza la validación de propiedad para operaciones del vendedor.
     *
     * @param id       UUID del producto
     * @param sellerId UUID del vendedor propietario
     * @return El producto si existe y pertenece al vendedor
     */
    Optional<Product> findByIdAndSellerId(UUID id, UUID sellerId);

    Page<Product> findBySellerIdAndStatusNot(UUID sellerId, EstadoProducto status, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.sellerId = :sellerId
            AND p.status != com.neogaming.common.enums.EstadoProducto.DELETED
            AND LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Product> buscarPorSellerYNombre(UUID sellerId, String q, Pageable pageable);
}
