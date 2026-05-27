package com.neogaming.review.mapper;

import com.neogaming.review.domain.Review;
import com.neogaming.review.dto.ReviewResponse;
import com.neogaming.user.domain.User;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Convierte la entidad Review a su DTO de respuesta.
 */
@Component
public class ReviewMapper {

    /**
     * Convierte una Review a su DTO de respuesta.
     * Si la review tiene buyerNameOverride (reseñas creadas por admin), lo usa directamente.
     * En ese caso buyer puede ser null.
     */
    public ReviewResponse toResponse(Review review, @Nullable User buyer) {
        String name;
        if (review.getBuyerNameOverride() != null && !review.getBuyerNameOverride().isBlank()) {
            name = review.getBuyerNameOverride();
        } else if (buyer != null) {
            String combined = (buyer.getFirstName().trim() + " " + buyer.getLastName().trim()).trim();
            name = combined.isEmpty() ? "Usuario" : combined;
        } else {
            name = "Usuario";
        }
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                name,
                review.getRating(),
                review.getTitle(),
                review.getBody(),
                review.getStatus(),
                review.getRejectReason(),
                review.getCreatedAt()
        );
    }
}
