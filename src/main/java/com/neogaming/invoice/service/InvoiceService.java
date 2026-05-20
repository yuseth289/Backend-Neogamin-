package com.neogaming.invoice.service;

import com.neogaming.common.enums.EstadoFactura;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.invoice.domain.Invoice;
import com.neogaming.invoice.dto.CancelInvoiceRequest;
import com.neogaming.invoice.dto.InvoiceResponse;
import com.neogaming.invoice.mapper.InvoiceMapper;
import com.neogaming.invoice.repository.InvoiceRepository;
import com.neogaming.order.domain.Order;
import com.neogaming.order.domain.OrderItem;
import com.neogaming.order.repository.OrderItemRepository;
import com.neogaming.order.repository.OrderRepository;
import com.neogaming.payment.repository.PaymentRepository;
import com.neogaming.user.domain.User;
import com.neogaming.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio que gestiona el ciclo de vida de las facturas electrónicas.
 *
 * Las facturas se generan automáticamente cuando el pago es aprobado.
 * El proceso es asíncrono para no bloquear el flujo principal de pago.
 *
 * Flujo normal:
 *  1. PaymentService llama a generarFactura() tras aprobar el pago
 *  2. Se crea la factura en estado DRAFT
 *  3. Se llama a emitirFactura() que actualiza el estado a ISSUED
 *     y (en producción) enviaría el correo con el PDF de la factura
 *
 * Zona horaria: America/Bogota (UTC-5) para formatear el número de factura.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final String ZONA_BOGOTA = "America/Bogota";
    private static final BigDecimal TASA_IVA = new BigDecimal("0.19");

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final InvoiceMapper invoiceMapper;

    /**
     * Genera y emite una factura electrónica para la orden correspondiente al pago.
     * Se ejecuta de forma asíncrona para no bloquear el webhook de Mercado Pago.
     *
     * Si ya existe una factura para la orden, el método retorna sin hacer nada
     * (protección contra reenvíos duplicados del webhook).
     *
     * @param orderId   UUID de la orden a facturar
     * @param paymentId UUID del pago aprobado
     */
    @Async
    @Transactional
    public void generarFactura(UUID orderId, UUID paymentId) {
        if (invoiceRepository.existsByOrderId(orderId)) {
            log.info("La factura para la orden {} ya existe — se omite la regeneración", orderId);
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden", orderId.toString()));

        paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", paymentId.toString()));

        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", order.getUserId().toString()));

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        BigDecimal subtotal = calcularSubtotal(items);
        BigDecimal taxAmount = subtotal.multiply(TASA_IVA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount);

        Invoice invoice = Invoice.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .userId(user.getId())
                .invoiceNumber(generarNumeroFactura())
                .status(EstadoFactura.DRAFT)
                .buyerName(user.getFirstName() + " " + user.getLastName())
                .buyerEmail(user.getEmail())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .total(total)
                .items(construirSnapshotItems(items))
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Factura {} generada para la orden {}", saved.getInvoiceNumber(), orderId);

        emitirFactura(saved);
    }

    /**
     * Marca la factura como ISSUED y registra la fecha de emisión.
     * En producción también enviaría el correo con el PDF al comprador.
     */
    @Transactional
    public void emitirFactura(Invoice invoice) {
        invoice.setStatus(EstadoFactura.ISSUED);
        invoice.setIssuedAt(Instant.now());
        invoiceRepository.save(invoice);
        log.info("Factura {} emitida al comprador {}", invoice.getInvoiceNumber(), invoice.getBuyerEmail());
    }

    /**
     * Retorna la factura de una orden específica.
     * El comprador solo puede ver sus propias facturas.
     *
     * @param orderId UUID de la orden
     * @param userId  UUID del usuario autenticado
     * @return DTO de la factura
     */
    @Transactional(readOnly = true)
    public InvoiceResponse obtenerPorOrden(UUID orderId, UUID userId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura para orden", orderId.toString()));

        if (!invoice.getUserId().equals(userId)) {
            throw new BusinessRuleException(
                    "No tienes permiso para ver esta factura", "FACTURA_ACCESO_DENEGADO");
        }

        return invoiceMapper.toResponse(invoice);
    }

    /**
     * Lista todas las facturas del usuario autenticado de forma paginada.
     *
     * @param userId   UUID del usuario
     * @param pageable configuración de paginación
     * @return página de facturas del usuario
     */
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listarMisFacturas(UUID userId, Pageable pageable) {
        return invoiceRepository.findByUserId(userId, pageable)
                .map(invoiceMapper::toResponse);
    }

    /**
     * Retorna cualquier factura por su ID — solo para administradores.
     *
     * @param invoiceId UUID de la factura
     * @return DTO de la factura
     */
    @Transactional(readOnly = true)
    public InvoiceResponse obtenerPorId(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", invoiceId.toString()));
        return invoiceMapper.toResponse(invoice);
    }

    /**
     * Lista todas las facturas del sistema paginadas — solo para administradores.
     *
     * @param pageable configuración de paginación
     * @return página de todas las facturas
     */
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listarTodas(Pageable pageable) {
        return invoiceRepository.findAll(pageable).map(invoiceMapper::toResponse);
    }

    /**
     * Anula una factura. Solo administradores pueden hacerlo.
     * No se pueden anular facturas ya canceladas.
     *
     * @param invoiceId UUID de la factura a anular
     * @param request   DTO con el motivo de anulación
     */
    @Transactional
    public InvoiceResponse cancelarFactura(UUID invoiceId, CancelInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", invoiceId.toString()));

        if (invoice.getStatus() == EstadoFactura.CANCELLED) {
            throw new BusinessRuleException(
                    "La factura ya está cancelada", "FACTURA_YA_CANCELADA");
        }

        invoice.setStatus(EstadoFactura.CANCELLED);
        invoice.setCancelledAt(Instant.now());
        invoice.setCancelReason(request.cancelReason());

        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    // ─── Métodos privados ────────────────────────────────────────────────────

    /** Suma los subtotales de todos los ítems de la orden. */
    private BigDecimal calcularSubtotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Genera el número de factura en formato NG-{AÑO}-{SECUENCIAL 6 dígitos}.
     * Usa la zona horaria de Bogotá para el año.
     */
    private String generarNumeroFactura() {
        int anio = ZonedDateTime.now(ZoneId.of(ZONA_BOGOTA)).getYear();
        long secuencia = invoiceRepository.nextSequenceForYear(anio);
        return String.format("NG-%d-%06d", anio, secuencia);
    }

    /** Construye el snapshot JSONB de ítems a partir de los OrderItems de la orden. */
    private List<Map<String, Object>> construirSnapshotItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> Map.<String, Object>of(
                        "productId",   item.getProductId().toString(),
                        "productName", item.getProductName(),
                        "productSku",  item.getProductSku() != null ? item.getProductSku() : "",
                        "quantity",    item.getQuantity(),
                        "unitPrice",   item.getUnitPrice(),
                        "subtotal",    item.getSubtotal()
                ))
                .toList();
    }
}
