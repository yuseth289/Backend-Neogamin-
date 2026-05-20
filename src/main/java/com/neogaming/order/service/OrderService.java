package com.neogaming.order.service;

import com.neogaming.checkout.domain.Checkout;
import com.neogaming.checkout.domain.CheckoutItem;
import com.neogaming.checkout.repository.CheckoutItemRepository;
import com.neogaming.checkout.repository.CheckoutRepository;
import com.neogaming.common.enums.EstadoGrupo;
import com.neogaming.common.enums.EstadoPedido;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.common.response.PageResponse;
import com.neogaming.inventory.service.InventoryService;
import com.neogaming.order.domain.Order;
import com.neogaming.order.domain.OrderGroup;
import com.neogaming.order.domain.OrderItem;
import com.neogaming.order.dto.response.OrderResponse;
import com.neogaming.order.dto.response.OrderSummaryResponse;
import com.neogaming.order.mapper.OrderMapper;
import com.neogaming.order.repository.OrderGroupRepository;
import com.neogaming.order.repository.OrderItemRepository;
import com.neogaming.order.repository.OrderRepository;
import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de órdenes para NeoGaming.
 *
 * Responsabilidades:
 *  - Crear órdenes a partir de checkouts confirmados por el pago (llamado por PaymentService)
 *  - Listar órdenes del comprador
 *  - Actualizar estado de grupos (vendedor marca como enviado)
 *  - Confirmar la salida de stock al crear la orden
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderGroupRepository orderGroupRepository;
    private final OrderItemRepository orderItemRepository;
    private final CheckoutRepository checkoutRepository;
    private final CheckoutItemRepository checkoutItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    /**
     * Crea una orden a partir de un checkout confirmado por el pago.
     *
     * Este método es llamado por PaymentService cuando Mercado Pago
     * confirma que el pago fue exitoso. Orquesta:
     *  1. Crear la cabecera de la orden
     *  2. Agrupar ítems por vendedor y crear los OrderGroups
     *  3. Crear los OrderItems con snapshot de datos del producto
     *  4. Confirmar la salida de stock (physicalStock -= quantity)
     *
     * @param checkoutId UUID del checkout confirmado
     * @param paymentId  UUID del pago registrado
     * @return La orden recién creada
     */
    public OrderResponse crearDesdeCheckout(UUID checkoutId, UUID paymentId) {
        Checkout checkout = checkoutRepository.findById(checkoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout", checkoutId.toString()));

        // Verificar que no exista ya una orden para este checkout
        if (orderRepository.findByCheckoutId(checkoutId).isPresent()) {
            throw new BusinessRuleException(
                    "Ya existe una orden para este checkout",
                    "ORDEN_YA_CREADA"
            );
        }

        // 1. Crear la cabecera de la orden
        Order order = Order.builder()
                .userId(checkout.getUserId())
                .checkoutId(checkoutId)
                .status(EstadoPedido.PAYMENT_APPROVED)
                .shippingAddress(checkout.getShippingAddress())
                .subtotal(checkout.getSubtotal())
                .shippingCost(checkout.getShippingCost())
                .total(checkout.getTotal())
                .paymentId(paymentId)
                .build();

        Order savedOrder = orderRepository.save(order);

        // 2. Obtener ítems del checkout y agrupar por vendedor
        List<CheckoutItem> checkoutItems = checkoutItemRepository.findByCheckoutId(checkoutId);

        Map<UUID, List<CheckoutItem>> itemsPorVendedor = checkoutItems.stream()
                .collect(Collectors.groupingBy(CheckoutItem::getSellerId));

        List<OrderGroup> grupos = new ArrayList<>();
        List<OrderItem> todosLosItems = new ArrayList<>();

        for (Map.Entry<UUID, List<CheckoutItem>> entry : itemsPorVendedor.entrySet()) {
            UUID sellerId = entry.getKey();
            List<CheckoutItem> itemsDelVendedor = entry.getValue();

            BigDecimal subtotalGrupo = itemsDelVendedor.stream()
                    .map(CheckoutItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 3. Crear el grupo del vendedor
            OrderGroup grupo = OrderGroup.builder()
                    .orderId(savedOrder.getId())
                    .sellerId(sellerId)
                    .status(EstadoGrupo.CONFIRMED)
                    .subtotal(subtotalGrupo)
                    .build();
            OrderGroup savedGrupo = orderGroupRepository.save(grupo);
            grupos.add(savedGrupo);

            // 4. Crear los ítems del grupo con snapshot de producto
            for (CheckoutItem ci : itemsDelVendedor) {
                Product product = productRepository.findById(ci.getProductId()).orElse(null);

                OrderItem item = OrderItem.builder()
                        .orderId(savedOrder.getId())
                        .orderGroupId(savedGrupo.getId())
                        .productId(ci.getProductId())
                        .productName(product != null ? product.getName() : "Producto")
                        .productSku(product != null ? product.getSku() : null)
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .subtotal(ci.getSubtotal())
                        .build();
                OrderItem savedItem = orderItemRepository.save(item);
                todosLosItems.add(savedItem);

                // 5. Confirmar salida de stock (physicalStock -= quantity)
                inventoryService.confirmarSalidaStock(
                        ci.getProductId(), ci.getQuantity(), savedOrder.getId()
                );
            }
        }

        // Construir la respuesta con el mapa de ítems por grupo
        Map<UUID, List<OrderItem>> itemsByGroup = todosLosItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderGroupId));

        return orderMapper.toResponse(savedOrder, grupos, itemsByGroup);
    }

    /**
     * Lista las órdenes del comprador autenticado con paginación.
     *
     * @param userId   UUID del comprador
     * @param pageable Paginación
     * @return Página de órdenes del usuario
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listarMisOrdenes(UUID userId, Pageable pageable) {
        Page<OrderSummaryResponse> page = orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(order -> {
                    int totalItems = orderItemRepository.findByOrderId(order.getId())
                            .stream().mapToInt(OrderItem::getQuantity).sum();
                    return orderMapper.toSummaryResponse(order, totalItems);
                });
        return PageResponse.from(page);
    }

    /**
     * Obtiene el detalle completo de una orden del comprador.
     *
     * @param orderId UUID de la orden
     * @param userId  UUID del comprador (para validar propiedad)
     * @return Detalle completo de la orden con grupos e ítems
     */
    @Transactional(readOnly = true)
    public OrderResponse obtenerMiOrden(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden", orderId.toString()));
        return construirOrdenResponse(order);
    }

    /**
     * Permite al vendedor actualizar el estado de su grupo de la orden.
     * Típicamente usado para marcar como SHIPPED con número de seguimiento.
     *
     * @param orderId        UUID de la orden
     * @param groupId        UUID del grupo
     * @param nuevoEstado    Nuevo estado del grupo
     * @param trackingNumber Número de seguimiento (requerido al marcar como SHIPPED)
     * @param sellerId       UUID del vendedor (para validar propiedad del grupo)
     * @return La orden actualizada
     */
    public OrderResponse actualizarEstadoGrupo(UUID orderId, UUID groupId,
                                               EstadoGrupo nuevoEstado, String trackingNumber,
                                               UUID sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden", orderId.toString()));

        OrderGroup grupo = orderGroupRepository
                .findByIdAndOrderIdAndSellerId(groupId, orderId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de orden", groupId.toString()));

        grupo.setStatus(nuevoEstado);
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            grupo.setTrackingNumber(trackingNumber);
        }
        orderGroupRepository.save(grupo);

        // Recalcular el estado global de la orden según los estados de sus grupos
        actualizarEstadoGlobalOrden(order);

        return construirOrdenResponse(order);
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Recalcula y actualiza el estado global de la orden basado en el estado
     * de todos sus grupos (sub-pedidos por vendedor).
     *
     * Reglas:
     *  - Todos DELIVERED → orden DELIVERED
     *  - Todos SHIPPED → orden SHIPPED
     *  - Alguno SHIPPED → orden PARTIALLY_SHIPPED
     *  - Alguno PREPARING → orden PROCESSING
     *
     * @param order La orden a actualizar
     */
    private void actualizarEstadoGlobalOrden(Order order) {
        List<OrderGroup> grupos = orderGroupRepository.findByOrderId(order.getId());

        boolean todosDelivered = grupos.stream().allMatch(g -> g.getStatus() == EstadoGrupo.DELIVERED);
        boolean todosShipped = grupos.stream().allMatch(g -> g.getStatus() == EstadoGrupo.SHIPPED
                || g.getStatus() == EstadoGrupo.DELIVERED);
        boolean algunoShipped = grupos.stream().anyMatch(g -> g.getStatus() == EstadoGrupo.SHIPPED
                || g.getStatus() == EstadoGrupo.DELIVERED);
        boolean algunoPreparing = grupos.stream().anyMatch(g -> g.getStatus() == EstadoGrupo.PREPARING);

        if (todosDelivered) {
            order.setStatus(EstadoPedido.DELIVERED);
        } else if (todosShipped) {
            order.setStatus(EstadoPedido.SHIPPED);
        } else if (algunoShipped) {
            order.setStatus(EstadoPedido.PARTIALLY_SHIPPED);
        } else if (algunoPreparing) {
            order.setStatus(EstadoPedido.PROCESSING);
        }

        orderRepository.save(order);
    }

    /**
     * Construye el OrderResponse completo con grupos e ítems cargados.
     *
     * @param order Entidad de la orden
     * @return DTO completo de la orden
     */
    private OrderResponse construirOrdenResponse(Order order) {
        List<OrderGroup> grupos = orderGroupRepository.findByOrderId(order.getId());
        List<OrderItem> todosLosItems = orderItemRepository.findByOrderId(order.getId());

        Map<UUID, List<OrderItem>> itemsByGroup = todosLosItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderGroupId));

        return orderMapper.toResponse(order, grupos, itemsByGroup);
    }
}
