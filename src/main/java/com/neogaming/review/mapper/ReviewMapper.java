package com.neogaming.review.mapper;

import com.neogaming.review.domain.Review;
import com.neogaming.review.dto.ReviewResponse;
import com.neogaming.user.domain.User;
import org.springframework.stereotype.Component;

/**
 * Convierte la entidad Review a su DTO de respuesta.
 */
@Component
public class ReviewMapper {

    /** Convierte una Review a su DTO de respuesta, incluyendo el nombre del comprador. */
    public ReviewResponse toResponse(Review review, User buyer) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                buyer.getFirstName() + " " + buyer.getLastName(),
                review.getRating(),
                review.getTitle(),
                review.getBody(),
                review.getStatus(),
                review.getRejectReason(),
                review.getCreatedAt()
        );
    }
}
