package com.neogaming.payment.service;

import com.neogaming.checkout.domain.Checkout;
import com.neogaming.checkout.domain.CheckoutItem;
import com.neogaming.checkout.repository.CheckoutItemRepository;
import com.neogaming.checkout.repository.CheckoutRepository;
import com.neogaming.common.enums.EstadoCheckout;
import com.neogaming.common.enums.EstadoPago;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.invoice.service.InvoiceService;
import com.neogaming.order.dto.response.OrderResponse;
import com.neogaming.order.service.OrderService;
import com.neogaming.payment.domain.Payment;
import com.neogaming.payment.dto.response.PaymentResponse;
import com.neogaming.payment.mapper.PaymentMapper;
import com.neogaming.payment.mercadopago.MercadoPagoClient;
import com.neogaming.payment.mercadopago.MercadoPagoPreferenceResponse;
import com.neogaming.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de gestión de pagos con Mercado Pago Colombia.
 *
 * Flujo de pago:
 *  1. Cliente llama a iniciarPago() → se crea preferencia en MP y se retorna la URL de checkout
 *  2. Cliente paga en la interfaz de Mercado Pago
 *  3. MP envía webhook a POST /webhooks/mercadopago
 *  4. procesarWebhook() actualiza el estado del pago y, si fue aprobado, crea la orden
 *
 * Métodos de pago disponibles en Colombia:
 *  - MP_PSE:          Débito bancario en línea (2-3 días hábiles de acreditación)
 *  - MP_NEQUI:        Billetera Nequi (inmediato)
 *  - MP_EFECTY:       Pago en efectivo en puntos Efecty (hasta 3 días)
 *  - MP_CREDIT_CARD:  Tarjeta de crédito (inmediato)
 *  - MP_DEBIT_CARD:   Tarjeta débito (inmediato)
 *  - MP_ACCOUNT_MONEY: Saldo Mercado Pago (inmediato)
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CheckoutRepository checkoutRepository;
    private final CheckoutItemRepository checkoutItemRepository;
    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final MercadoPagoClient mpClient;
    private final PaymentMapper paymentMapper;

    /** Indicador si usar sandbox de MP (true en desarrollo) */
    @Value("${mercadopago.sandbox:true}")
    private boolean useSandbox;

    /**
     * Inicia el proceso de pago para el checkout activo del usuario.
     *
     * Crea una preferencia de pago en Mercado Pago y devuelve la URL
     * de checkout de MP a la que el frontend debe redirigir al usuario.
     *
     * @param checkoutId UUID del checkout a pagar
     * @param userId     UUID del usuario (para validar propiedad)
     * @return Respuesta con la URL de checkout de MP y el ID del pago creado
     */
    public PaymentResponse iniciarPago(UUID checkoutId, UUID userId) {
        Checkout checkout = checkoutRepository.findByIdAndUserId(checkoutId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout", checkoutId.toString()));

        if (checkout.getStatus() != EstadoCheckout.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "El checkout no está en estado activo para procesar el pago",
                    "CHECKOUT_NO_ACTIVO"
            );
        }

        if (checkout.haExpirado()) {
            throw new BusinessRuleException(
                    "El checkout expiró. Por favor inicia el proceso de compra nuevamente.",
                    "CHECKOUT_EXPIRADO"
            );
        }

        // Verificar que no haya ya un pago para este checkout
        if (paymentRepository.findByCheckoutId(checkoutId).isPresent()) {
            throw new BusinessRuleException(
                    "Ya existe un pago iniciado para este checkout",
                    "PAGO_YA_INICIADO"
            );
        }

        // Obtener ítems del checkout para la preferencia de MP
        List<CheckoutItem> items = checkoutItemRepository.findByCheckoutId(checkoutId);

        // Crear preferencia en Mercado Pago
        MercadoPagoPreferenceResponse preferencia = mpClient.crearPreferencia(
                checkoutId.toString(),
                items,
                checkout.getTotal(),
                checkout.getPaymentMethod()
        );

        // Crear el registro de pago en estado PENDING
        Payment payment = Payment.builder()
                .checkoutId(checkoutId)
                .userId(userId)
                .mpPreferenceId(preferencia.id())
                .status(EstadoPago.PENDING)
                .paymentMethod(checkout.getPaymentMethod())
                .amount(checkout.getTotal())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Devolver la URL apropiada según el entorno (sandbox o producción)
        String checkoutUrl = useSandbox ? preferencia.sandboxInitPoint() : preferencia.initPoint();
        return paymentMapper.toResponse(savedPayment, checkoutUrl);
    }

    /**
     * Procesa el webhook de Mercado Pago al recibir una notificación de pago.
     *
     * MP envía el ID del pago y el tipo de notificación.
     * Este método consulta el estado actual del pago en MP y actualiza
     * el registro interno.
     *
     * Si el pago fue APROBADO, crea la orden definitiva.
     * Si fue RECHAZADO, libera el stock reservado (vía OrderService).
     *
     * @param mpPaymentId  ID del pago en Mercado Pago
     * @param topic        Tipo de notificación de MP (esperamos "payment")
     */
    public void procesarWebhook(String mpPaymentId, String topic) {
        // Solo procesar notificaciones de tipo "payment"
        if (!"payment".equals(topic)) {
            log.info("Webhook ignorado — tipo no soportado: {}", topic);
            return;
        }

        // Consultar el estado actual del pago en MP
        Map<String, Object> mpPago = mpClient.consultarPago(mpPaymentId);
        String mpStatus = (String) mpPago.get("status");
        String externalReference = (String) mpPago.get("external_reference");

        String statusDetail = (String) mpPago.getOrDefault("status_detail", "sin_detalle");
        log.info("Webhook MP recibido — paymentId: {}, status: {}, status_detail: {}, ref: {}",
                mpPaymentId, mpStatus, statusDetail, externalReference);

        // Buscar el pago por el external_reference (es el checkoutId)
        if (externalReference == null || externalReference.isBlank()) {
            log.warn("Webhook MP — external_reference nulo para paymentId: {}. Ignorando.", mpPaymentId);
            return;
        }
        UUID checkoutId = UUID.fromString(externalReference);
        Payment payment = paymentRepository.findByCheckoutId(checkoutId)
                .orElseGet(() -> {
                    // El pago puede no existir si el usuario pagó sin usar nuestro flujo
                    log.warn("Pago no encontrado para checkout: {}", checkoutId);
                    return null;
                });

        if (payment == null) return;

        // Actualizar el mpPaymentId si aún no lo tenía
        if (payment.getMpPaymentId() == null) {
            payment.setMpPaymentId(mpPaymentId);
        }

        // Guardar la respuesta raw de MP para debugging
        payment.setMpStatus(mpStatus);
        payment.setMpResponse(mpPago);

        // Mapear el estado de MP al estado interno de NeoGaming
        EstadoPago nuevoEstado = mapearEstadoMP(mpStatus);
        payment.setStatus(nuevoEstado);
        paymentRepository.save(payment);

        if (nuevoEstado == EstadoPago.REJECTED) {
            log.warn("Pago rechazado — checkoutId: {}, paymentId: {}, motivo: {}",
                    checkoutId, mpPaymentId, statusDetail);
        }

        // Si el pago fue aprobado, crear la orden y la factura
        if (nuevoEstado == EstadoPago.APPROVED) {
            log.info("Pago aprobado para checkout {}. Creando orden...", checkoutId);
            OrderResponse orden = orderService.crearDesdeCheckout(checkoutId, payment.getId());

            // Generar factura electrónica de forma asíncrona (no bloquea el webhook)
            invoiceService.generarFactura(orden.id(), payment.getId());

            // Marcar el checkout como COMPLETED
            checkoutRepository.findById(checkoutId).ifPresent(checkout -> {
                checkout.setStatus(EstadoCheckout.COMPLETED);
                checkoutRepository.save(checkout);
            });
        }
    }

    /**
     * Simula la aprobación de un pago en sandbox.
     * Solo disponible cuando mercadopago.sandbox=true.
     */
    public void simularPagoAprobado(UUID checkoutId) {
        if (!useSandbox) {
            throw new BusinessRuleException("Solo disponible en modo sandbox", "SANDBOX_ONLY");
        }

        Checkout checkout = checkoutRepository.findById(checkoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout", checkoutId.toString()));

        Payment payment = paymentRepository.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new BusinessRuleException("No hay pago para este checkout", "PAGO_NO_ENCONTRADO"));

        String fakeMpPaymentId = "SANDBOX-TEST-" + System.currentTimeMillis();
        payment.setMpPaymentId(fakeMpPaymentId);
        payment.setMpStatus("approved");
        payment.setStatus(EstadoPago.APPROVED);
        paymentRepository.save(payment);

        log.info("[SANDBOX] Pago simulado aprobado — checkoutId: {}, fakePaymentId: {}", checkoutId, fakeMpPaymentId);

        OrderResponse orden = orderService.crearDesdeCheckout(checkoutId, payment.getId());
        invoiceService.generarFactura(orden.id(), payment.getId());

        checkout.setStatus(EstadoCheckout.COMPLETED);
        checkoutRepository.save(checkout);
    }

    /**
     * Mapea el estado de Mercado Pago al enum EstadoPago de NeoGaming.
     *
     * Estados de MP:
     *  pending      → pago iniciado, esperando acción del usuario (Efecty, PSE)
     *  approved     → pago confirmado y acreditado
     *  rejected     → pago rechazado (fondos insuficientes, tarjeta bloqueada, etc.)
     *  cancelled    → cancelado por el usuario o por timeout de MP
     *  refunded     → monto devuelto al comprador
     *  in_mediation → disputa abierta en MP (contracargo)
     *
     * @param mpStatus Estado devuelto por Mercado Pago
     * @return Estado interno correspondiente en NeoGaming
     */
    private EstadoPago mapearEstadoMP(String mpStatus) {
        return switch (mpStatus) {
            case "approved"     -> EstadoPago.APPROVED;
            case "rejected"     -> EstadoPago.REJECTED;
            case "cancelled"    -> EstadoPago.CANCELLED;
            case "refunded"     -> EstadoPago.REFUNDED;
            case "in_mediation" -> EstadoPago.IN_MEDIATION;
            case "in_process"   -> EstadoPago.PROCESSING;
            default             -> EstadoPago.PENDING;
        };
    }
}
