package com.neogaming.payment.repository;

import com.neogaming.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Payment.
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Busca un pago por el ID asignado por Mercado Pago.
     * Usado al recibir webhooks de MP para identificar el pago.
     *
     * @param mpPaymentId ID del pago en Mercado Pago
     * @return El pago si existe
     */
    Optional<Payment> findByMpPaymentId(String mpPaymentId);

    /**
     * Busca el pago asociado a un checkout.
     *
     * @param checkoutId UUID del checkout
     * @return El pago si existe
     */
    Optional<Payment> findByCheckoutId(UUID checkoutId);
}
