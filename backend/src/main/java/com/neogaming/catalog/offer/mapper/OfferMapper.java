package com.neogaming.catalog.offer.mapper;

import com.neogaming.catalog.offer.domain.Offer;
import com.neogaming.catalog.offer.dto.response.OfferResponse;
import com.neogaming.common.enums.EstadoGenerico;
import org.springframework.stereotype.Component;

@Component
public class OfferMapper {

    public OfferResponse toResponse(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getProductId(),
                offer.getDiscountValue(),
                offer.getStartDate(),
                offer.getEndDate(),
                offer.getStatus() == EstadoGenerico.ACTIVE,
                offer.estaVigente()
        );
    }
}
