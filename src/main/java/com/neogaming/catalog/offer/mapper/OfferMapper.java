package com.neogaming.catalog.offer.mapper;

import com.neogaming.catalog.offer.domain.Offer;
import com.neogaming.catalog.offer.dto.response.OfferResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades Offer a su DTO de respuesta.
 */
@Component
public class OfferMapper {

    /**
     * Convierte una oferta a su DTO de respuesta.
     * Evalúa en tiempo real si la oferta está vigente.
     *
     * @param offer Entidad de la oferta
     * @return DTO con el campo vigente calculado
     */
    public OfferResponse toResponse(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getProductId(),
                offer.getName(),
                offer.getDiscountType(),
                offer.getDiscountValue(),
                offer.getDiscountedPrice(),
                offer.getStartDate(),
                offer.getEndDate(),
                offer.estaVigente(),  // Calculado en tiempo real
                offer.getStatus(),
                offer.getCreatedAt()
        );
    }
}
