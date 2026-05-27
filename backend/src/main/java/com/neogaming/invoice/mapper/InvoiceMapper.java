package com.neogaming.invoice.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neogaming.invoice.domain.Invoice;
import com.neogaming.invoice.dto.InvoiceItemResponse;
import com.neogaming.invoice.dto.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Convierte la entidad Invoice a su DTO de respuesta.
 * Deserializa el campo JSONB items a una lista tipada.
 */
@Component
@RequiredArgsConstructor
public class InvoiceMapper {

    private final ObjectMapper objectMapper;

    /** Convierte una Invoice a su DTO de respuesta completo. */
    public InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getOrderId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.getBuyerName(),
                invoice.getBuyerEmail(),
                invoice.getBuyerDocument(),
                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getTotal(),
                deserializarItems(invoice.getItems()),
                invoice.getIssuedAt(),
                invoice.getCancelledAt(),
                invoice.getCancelReason(),
                invoice.getCreatedAt()
        );
    }

    private List<InvoiceItemResponse> deserializarItems(Object items) {
        if (items == null) return List.of();
        List<Map<String, Object>> raw = objectMapper.convertValue(
                items, new TypeReference<List<Map<String, Object>>>() {});
        return raw.stream()
                .map(m -> new InvoiceItemResponse(
                        m.get("productId") != null
                                ? java.util.UUID.fromString((String) m.get("productId"))
                                : null,
                        (String) m.get("productName"),
                        (String) m.get("productSku"),
                        ((Number) m.get("quantity")).intValue(),
                        new java.math.BigDecimal(m.get("unitPrice").toString()),
                        new java.math.BigDecimal(m.get("subtotal").toString())
                ))
                .toList();
    }
}
