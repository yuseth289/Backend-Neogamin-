package com.neogaming.payment.mapper;

import com.neogaming.payment.domain.Payment;
import com.neogaming.payment.dto.response.PaymentResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades Payment a su DTO de respuesta.
 */
@Component
public class PaymentMapper {

    /**
     * Convierte un Payment a su DTO de respuesta.
     * El mpResponse (raw de MP) no se incluye por seguridad.
     *
     * @param payment    Entidad del pago
     * @param checkoutUrl URL de checkout de MP para redirigir al usuario (puede ser null si ya fue procesado)
     * @return DTO del pago
     */
    public PaymentResponse toResponse(Payment payment, String checkoutUrl) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCheckoutId(),
                payment.getMpPaymentId(),
                payment.getMpPreferenceId(),
                checkoutUrl,
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getCreatedAt()
        );
    }
}
