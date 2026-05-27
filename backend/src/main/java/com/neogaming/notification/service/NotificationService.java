package com.neogaming.notification.service;

import com.neogaming.notification.dto.EmailContext;
import com.neogaming.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Orquesta el envío de notificaciones de negocio.
 * Construye el contexto de cada tipo de correo y delega el envío a EmailService.
 *
 * Notificaciones implementadas:
 *  - Bienvenida al registrarse
 *  - Confirmación de pago aprobado al comprador (con detalle de la orden)
 *  - Alerta de nueva orden al vendedor
 *  - Notificación de envío al comprador (con número de seguimiento)
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmailService emailService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    /**
     * Envía el correo de bienvenida al usuario recién registrado.
     *
     * @param email  Correo del usuario
     * @param nombre Nombre de pila del usuario
     */
    public void enviarBienvenida(String email, String nombre) {
        emailService.enviar(new EmailContext(
                email,
                "¡Bienvenido a NeoGaming! 🎮",
                "bienvenida",
                Map.of(
                        "nombre", nombre,
                        "appUrl", appBaseUrl
                )
        ));
    }

    /**
     * Envía la confirmación de pago aprobado al comprador con el resumen de la orden.
     *
     * @param email         Correo del comprador
     * @param nombre        Nombre del comprador
     * @param numeroOrden   ID legible de la orden
     * @param numeroFactura Número de factura electrónica (ej. NG-2026-000001)
     * @param metodoPago    Método usado (PSE, Nequi, etc.)
     * @param items         Ítems comprados
     * @param total         Monto total pagado en COP
     */
    public void enviarConfirmacionPago(
            String email,
            String nombre,
            String numeroOrden,
            String numeroFactura,
            String metodoPago,
            List<OrderItem> items,
            BigDecimal total) {

        String fechaFormateada = ZonedDateTime.now(ZoneId.of("America/Bogota"))
                .format(FORMATO_FECHA);

        emailService.enviar(new EmailContext(
                email,
                "✅ Pago aprobado — Orden " + numeroOrden,
                "pago_aprobado",
                Map.of(
                        "nombre",        nombre,
                        "numeroOrden",   numeroOrden,
                        "numeroFactura", numeroFactura,
                        "metodoPago",    metodoPago,
                        "fechaPago",     fechaFormateada,
                        "items",         items,
                        "total",         total,
                        "ordenUrl",      appBaseUrl + "/orders"
                )
        ));
    }

    /**
     * Notifica al vendedor que recibió una nueva orden y debe prepararla.
     *
     * @param emailVendedor    Correo del vendedor
     * @param nombreTienda     Nombre de la tienda del vendedor
     * @param numeroOrden      ID de la orden
     * @param nombreComprador  Nombre del comprador
     * @param direccionEntrega Dirección de envío como texto legible
     * @param items            Ítems de la orden que pertenecen a este vendedor
     */
    public void enviarNuevaOrdenVendedor(
            String emailVendedor,
            String nombreTienda,
            String numeroOrden,
            String nombreComprador,
            String direccionEntrega,
            List<OrderItem> items) {

        emailService.enviar(new EmailContext(
                emailVendedor,
                "📦 Nueva orden recibida — " + numeroOrden,
                "nueva_orden_vendedor",
                Map.of(
                        "nombreTienda",    nombreTienda,
                        "numeroOrden",     numeroOrden,
                        "nombreComprador", nombreComprador,
                        "direccionEntrega", direccionEntrega,
                        "items",           items,
                        "gestionUrl",      appBaseUrl + "/seller/orders"
                )
        ));
    }

    /**
     * Notifica al comprador que el vendedor despachó su paquete.
     *
     * @param email           Correo del comprador
     * @param nombre          Nombre del comprador
     * @param numeroOrden     ID de la orden
     * @param nombreTienda    Nombre del vendedor que despachó
     * @param trackingNumber  Número de seguimiento (puede ser null)
     * @param direccionEntrega Dirección de destino
     */
    public void enviarNotificacionEnvio(
            String email,
            String nombre,
            String numeroOrden,
            String nombreTienda,
            String trackingNumber,
            String direccionEntrega) {

        emailService.enviar(new EmailContext(
                email,
                "🚚 Tu pedido de " + nombreTienda + " fue enviado",
                "pedido_enviado",
                Map.of(
                        "nombre",           nombre,
                        "numeroOrden",      numeroOrden,
                        "nombreTienda",     nombreTienda,
                        "trackingNumber",   trackingNumber != null ? trackingNumber : "",
                        "direccionEntrega", direccionEntrega,
                        "ordenUrl",         appBaseUrl + "/orders"
                )
        ));
    }
}
