package com.neogaming.catalog.offer.mapper;

import com.neogaming.catalog.offer.domain.Offer;
import com.neogaming.catalog.offer.dto.response.OfferResponse;
import org.springframework.stereotype.Component;

@Component
public class OfferMapper {

    public OfferResponse toResponse(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getProductId(),
                offer.getDiscountValue(),   // discountValue == discountPercent para PERCENTAGE
                offer.getStartDate(),
                offer.getEndDate(),
                offer.estaVigente()
        );
    }
}
