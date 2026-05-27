package com.neogaming.review.repository;

import com.neogaming.common.enums.EstadoResena;
import com.neogaming.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Review.
 */
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /** Busca la reseña de un usuario sobre un producto específico. */
    Optional<Review> findByUserIdAndProductId(UUID userId, UUID productId);

    /** Verifica si el usuario ya reseñó el producto. */
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    /** Lista las reseñas aprobadas de un producto, ordenadas de más reciente a más antigua. */
    Page<Review> findByProductIdAndStatus(UUID productId, EstadoResena status, Pageable pageable);

    /** Lista todas las reseñas de un usuario. */
    Page<Review> findByUserId(UUID userId, Pageable pageable);

    /** Lista reseñas por estado — para moderación administrativa. */
    Page<Review> findByStatus(EstadoResena status, Pageable pageable);

    /**
     * Calcula el promedio de calificaciones aprobadas para un producto.
     * Retorna null si el producto no tiene reseñas aprobadas.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId AND r.status = 'APPROVED'")
    Double calcularPromedioRating(UUID productId);

    /**
     * Cuenta las reseñas aprobadas de un producto.
     * Usado para mostrar "X reseñas" en la ficha del producto.
     */
    long countByProductIdAndStatus(UUID productId, EstadoResena status);

    /**
     * Verifica si el usuario compró el producto buscando en order_items vía la orden.
     * Garantiza que solo compradores verificados puedan publicar reseñas.
     */
    @Query("""
            SELECT COUNT(oi) > 0
            FROM OrderItem oi
            WHERE oi.productId = :productId
              AND oi.orderId IN (
                  SELECT o.id FROM Order o WHERE o.userId = :userId
              )
            """)
    boolean usuarioComproElProducto(UUID userId, UUID productId);

    @Query("""
            SELECT AVG(r.rating) FROM Review r
            WHERE r.productId IN (SELECT p.id FROM Product p WHERE p.sellerId = :sellerId)
            AND r.status = 'APPROVED'
            """)
    Double calcularPromedioRatingPorVendedor(UUID sellerId);

    @Query("""
            SELECT COUNT(r) FROM Review r
            WHERE r.productId IN (SELECT p.id FROM Product p WHERE p.sellerId = :sellerId)
            AND r.status = 'APPROVED'
            """)
    Long contarResenasPorVendedor(UUID sellerId);
}
