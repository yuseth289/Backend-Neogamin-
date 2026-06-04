package com.neogaming.catalog.product.repository;

import com.neogaming.catalog.product.domain.Product;
import com.neogaming.common.enums.EstadoProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySellerIdAndSkuAndStatusNot(UUID sellerId, String sku, EstadoProducto status);

    Page<Product> findBySellerIdAndStatus(UUID sellerId, EstadoProducto status, Pageable pageable);

    Page<Product> findByStatus(EstadoProducto status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(UUID categoryId, EstadoProducto status, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Product> buscarPorTextoYEstado(String query, EstadoProducto status, Pageable pageable);

    /**
     * Catálogo público con todos los filtros opcionales: sellerId, brand (LIKE), rango de precio.
     * Pasa null en cualquier parámetro para omitir ese filtro.
     */
    /**
     * Catálogo público con filtros opcionales: sellerId, brand (LIKE parcial), rango de precio.
     * Pasar brand="" para omitir el filtro de marca (LIKE '%%' = sin filtro).
     * Pasar sellerId=null, minPrice=null, maxPrice=null para omitir esos filtros.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
            AND (:sellerId IS NULL OR p.sellerId = :sellerId)
            AND LOWER(p.brand) LIKE LOWER(CONCAT('%', :brand, '%'))
            AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
            AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
            """)
    Page<Product> findByStatusFiltered(EstadoProducto status, UUID sellerId, String brand,
                                       BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Catálogo por categoría con filtros opcionales: brand (LIKE parcial), rango de precio.
     * Pasar brand="" para omitir el filtro de marca.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.categoryId = :categoryId
            AND p.status = :status
            AND LOWER(p.brand) LIKE LOWER(CONCAT('%', :brand, '%'))
            AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
            AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
            """)
    Page<Product> findByCategoryIdAndStatusFiltered(UUID categoryId, EstadoProducto status, String brand,
                                                    BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Búsqueda de texto con filtros opcionales: brand (LIKE parcial), rango de precio.
     * Pasar brand="" para omitir el filtro de marca.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')))
            AND LOWER(p.brand) LIKE LOWER(CONCAT('%', :brand, '%'))
            AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
            AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
            """)
    Page<Product> buscarFiltrado(String query, EstadoProducto status, String brand,
                                 BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

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
