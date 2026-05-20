package com.neogaming.review.service;

import com.neogaming.common.enums.EstadoResena;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ConflictException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.order.domain.Order;
import com.neogaming.order.repository.OrderRepository;
import com.neogaming.review.domain.Review;
import com.neogaming.review.dto.CreateReviewRequest;
import com.neogaming.review.dto.ProductRatingSummary;
import com.neogaming.review.dto.ReviewModerationRequest;
import com.neogaming.review.dto.ReviewResponse;
import com.neogaming.review.mapper.ReviewMapper;
import com.neogaming.review.repository.ReviewRepository;
import com.neogaming.user.domain.User;
import com.neogaming.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de gestión de reseñas de productos.
 *
 * Reglas de negocio:
 *  - Solo compradores verificados (con la orden) pueden publicar reseñas.
 *  - Máximo una reseña por usuario por producto.
 *  - Las reseñas pasan por moderación antes de ser visibles (PENDING → APPROVED/REJECTED).
 *  - Solo administradores moderan reseñas.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    /**
     * Crea una nueva reseña para un producto comprado por el usuario.
     *
     * Valida que:
     *  1. El usuario no haya reseñado ya el producto.
     *  2. La orden pertenezca al usuario.
     *  3. El usuario efectivamente compró el producto en esa orden.
     *
     * @param request Datos de la reseña
     * @param userId  UUID del comprador autenticado
     * @return DTO de la reseña creada (en estado PENDING)
     */
    public ReviewResponse crearResena(CreateReviewRequest request, UUID userId) {
        if (reviewRepository.existsByUserIdAndProductId(userId, request.productId())) {
            throw new ConflictException("Ya tienes una reseña para este producto", "RESENA_DUPLICADA");
        }

        // Validar que la orden pertenezca al usuario
        Order orden = orderRepository.findByIdAndUserId(request.orderId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden", request.orderId().toString()));

        // Validar que el producto esté en la orden (compra verificada)
        if (!reviewRepository.usuarioComproElProducto(userId, request.productId())) {
            throw new BusinessRuleException(
                    "Solo puedes reseñar productos que hayas comprado",
                    "COMPRA_NO_VERIFICADA"
            );
        }

        User comprador = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId.toString()));

        Review review = Review.builder()
                .productId(request.productId())
                .userId(userId)
                .orderId(orden.getId())
                .rating(request.rating())
                .title(request.title())
                .body(request.body())
                .status(EstadoResena.PENDING)
                .build();

        return reviewMapper.toResponse(reviewRepository.save(review), comprador);
    }

    /**
     * Lista las reseñas aprobadas de un producto, de más reciente a más antigua.
     * Endpoint público — no requiere autenticación.
     *
     * @param productId UUID del producto
     * @param pageable  Paginación
     * @return Página de reseñas aprobadas
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> listarResenasPorProducto(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatus(productId, EstadoResena.APPROVED, pageable)
                .map(review -> {
                    User comprador = userRepository.findById(review.getUserId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Usuario", review.getUserId().toString()));
                    return reviewMapper.toResponse(review, comprador);
                });
    }

    /**
     * Retorna el resumen de calificaciones de un producto (promedio y total de reseñas).
     * Endpoint público.
     *
     * @param productId UUID del producto
     * @return Promedio de calificaciones y conteo total
     */
    @Transactional(readOnly = true)
    public ProductRatingSummary obtenerResumenRating(UUID productId) {
        Double promedio = reviewRepository.calcularPromedioRating(productId);
        long total = reviewRepository.countByProductIdAndStatus(productId, EstadoResena.APPROVED);
        return new ProductRatingSummary(
                promedio != null ? Math.round(promedio * 10.0) / 10.0 : 0.0,
                total
        );
    }

    /**
     * Lista todas las reseñas del usuario autenticado (en cualquier estado).
     *
     * @param userId   UUID del comprador
     * @param pageable Paginación
     * @return Página de reseñas del usuario
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> listarMisResenas(UUID userId, Pageable pageable) {
        User comprador = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId.toString()));
        return reviewRepository.findByUserId(userId, pageable)
                .map(review -> reviewMapper.toResponse(review, comprador));
    }

    /**
     * Lista reseñas por estado para moderación administrativa.
     *
     * @param status   Estado a filtrar (PENDING, APPROVED, REJECTED)
     * @param pageable Paginación
     * @return Página de reseñas con ese estado
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> listarPorEstado(EstadoResena status, Pageable pageable) {
        return reviewRepository.findByStatus(status, pageable)
                .map(review -> {
                    User comprador = userRepository.findById(review.getUserId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Usuario", review.getUserId().toString()));
                    return reviewMapper.toResponse(review, comprador);
                });
    }

    /**
     * Modera una reseña (aprueba o rechaza). Solo administradores.
     * Al rechazar, el motivo de rechazo es obligatorio.
     *
     * @param reviewId UUID de la reseña
     * @param request  Nuevo estado y motivo (si aplica)
     * @return DTO actualizado de la reseña
     */
    public ReviewResponse moderarResena(UUID reviewId, ReviewModerationRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña", reviewId.toString()));

        if (request.status() == EstadoResena.REJECTED && (request.rejectReason() == null
                || request.rejectReason().isBlank())) {
            throw new BusinessRuleException(
                    "El motivo de rechazo es obligatorio al rechazar una reseña",
                    "MOTIVO_RECHAZO_REQUERIDO"
            );
        }

        review.setStatus(request.status());
        review.setRejectReason(request.status() == EstadoResena.REJECTED
                ? request.rejectReason() : null);

        User comprador = userRepository.findById(review.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", review.getUserId().toString()));

        return reviewMapper.toResponse(reviewRepository.save(review), comprador);
    }

    /**
     * Elimina la propia reseña de un usuario (solo si está en PENDING o REJECTED).
     * Las reseñas APPROVED no pueden ser eliminadas por el comprador.
     *
     * @param reviewId UUID de la reseña
     * @param userId   UUID del comprador autenticado
     */
    public void eliminarMiResena(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña", reviewId.toString()));

        if (!review.getUserId().equals(userId)) {
            throw new BusinessRuleException(
                    "No tienes permiso para eliminar esta reseña", "RESENA_ACCESO_DENEGADO");
        }

        if (review.getStatus() == EstadoResena.APPROVED) {
            throw new BusinessRuleException(
                    "No puedes eliminar una reseña que ya fue aprobada",
                    "RESENA_APROBADA_NO_ELIMINABLE"
            );
        }

        reviewRepository.delete(review);
    }
}
