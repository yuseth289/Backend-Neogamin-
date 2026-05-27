package com.neogaming.review.service;

import com.neogaming.common.enums.EstadoResena;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ConflictException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.order.domain.Order;
import com.neogaming.order.repository.OrderRepository;
import com.neogaming.review.domain.Review;
import com.neogaming.review.dto.AdminCreateReviewRequest;
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

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    // ── Buyer flow ────────────────────────────────────────────────────────

    public ReviewResponse crearResena(CreateReviewRequest request, UUID userId) {
        if (reviewRepository.existsByUserIdAndProductId(userId, request.productId())) {
            throw new ConflictException("Ya tienes una reseña para este producto", "RESENA_DUPLICADA");
        }

        Order orden = orderRepository.findByIdAndUserId(request.orderId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden", request.orderId().toString()));

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

    // ── Admin flow ────────────────────────────────────────────────────────

    /**
     * Crea una reseña editorial desde el panel de administración.
     * No requiere orden de compra. Se aprueba automáticamente.
     * El buyerName es libre (e.g. "Compra Verificada", nombre del cliente).
     */
    public ReviewResponse crearResenaAdmin(AdminCreateReviewRequest request, UUID adminId) {
        Review review = Review.builder()
                .productId(request.productId())
                .userId(adminId)
                .orderId(null)
                .rating(request.rating())
                .title(request.title())
                .body(request.body())
                .buyerNameOverride(request.buyerName())
                .status(EstadoResena.APPROVED)
                .build();

        return reviewMapper.toResponse(reviewRepository.save(review), null);
    }

    // ── Public queries ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listarResenasPorProducto(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatus(productId, EstadoResena.APPROVED, pageable)
                .map(this::toResponseSafe);
    }

    @Transactional(readOnly = true)
    public ProductRatingSummary obtenerResumenRating(UUID productId) {
        Double promedio = reviewRepository.calcularPromedioRating(productId);
        long total = reviewRepository.countByProductIdAndStatus(productId, EstadoResena.APPROVED);
        return new ProductRatingSummary(
                promedio != null ? Math.round(promedio * 10.0) / 10.0 : 0.0,
                total
        );
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listarMisResenas(UUID userId, Pageable pageable) {
        User comprador = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId.toString()));
        return reviewRepository.findByUserId(userId, pageable)
                .map(review -> reviewMapper.toResponse(review, comprador));
    }

    // ── Admin moderation ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listarPorEstado(EstadoResena status, Pageable pageable) {
        return reviewRepository.findByStatus(status, pageable)
                .map(this::toResponseSafe);
    }

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

        return toResponseSafe(reviewRepository.save(review));
    }

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

    // ── Private helpers ───────────────────────────────────────────────────

    /** Convierte una reseña a DTO sin buscar al usuario si tiene buyerNameOverride. */
    private ReviewResponse toResponseSafe(Review review) {
        if (review.getBuyerNameOverride() != null) {
            return reviewMapper.toResponse(review, null);
        }
        User comprador = userRepository.findById(review.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", review.getUserId().toString()));
        return reviewMapper.toResponse(review, comprador);
    }
}
