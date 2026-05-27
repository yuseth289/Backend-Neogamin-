package com.neogaming.analytics.service;

import com.neogaming.analytics.dto.AdminDashboardResponse;
import com.neogaming.analytics.dto.SellerDashboardResponse;
import com.neogaming.analytics.dto.TopProductoResponse;
import com.neogaming.analytics.dto.TopSellerResponse;
import com.neogaming.common.enums.*;
import com.neogaming.order.repository.OrderGroupRepository;
import com.neogaming.order.repository.OrderRepository;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.seller.repository.SellerRepository;
import com.neogaming.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de analíticas de NeoGaming.
 *
 * Calcula métricas de negocio para el dashboard del vendedor y del administrador.
 * Todas las consultas son de solo lectura. Los rangos de fechas se calculan en
 * Java (zona horaria America/Bogota) y se pasan como parámetros Instant a las
 * consultas JPQL para evitar sintaxis nativa de base de datos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private final OrderRepository orderRepository;
    private final OrderGroupRepository orderGroupRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /**
     * Genera el dashboard de analíticas para el vendedor indicado.
     *
     * @param sellerId UUID del vendedor
     * @return Métricas del mes actual, mes anterior, acumulados y top productos
     */
    public SellerDashboardResponse dashboardVendedor(UUID sellerId) {
        Instant[] esteMes      = rangoMes(0);
        Instant[] mesAnterior  = rangoMes(-1);

        long ordenesEsteMes     = contarOrdenesPorVendedorEnRango(sellerId, esteMes);
        long ordenesMesAnterior = contarOrdenesPorVendedorEnRango(sellerId, mesAnterior);

        BigDecimal ingresosEsteMes    = ingresosVendedorEnRango(sellerId, esteMes);
        BigDecimal ingresosMesAnterior = ingresosVendedorEnRango(sellerId, mesAnterior);

        long unidadesEsteMes = unidadesVendidasVendedorEnRango(sellerId, esteMes);

        long ordenesTotales  = orderGroupRepository.findBySellerId(sellerId).size();
        BigDecimal ingresosTotales = ingresosVendedorTotales(sellerId);

        long pendientes  = gruposVendedorPorEstado(sellerId, EstadoGrupo.PENDING);
        long preparando  = gruposVendedorPorEstado(sellerId, EstadoGrupo.PREPARING);
        long enviadas    = gruposVendedorPorEstado(sellerId, EstadoGrupo.SHIPPED);

        List<TopProductoResponse> topProductos = topProductosVendedor(sellerId, 5);

        Double promedio   = promedioResenasVendedor(sellerId);
        long totalResenas = resenasVendedor(sellerId);

        return new SellerDashboardResponse(
                ordenesEsteMes,
                ingresosEsteMes,
                unidadesEsteMes,
                ordenesMesAnterior,
                ingresosMesAnterior,
                ordenesTotales,
                ingresosTotales,
                pendientes,
                preparando,
                enviadas,
                topProductos,
                promedio != null ? Math.round(promedio * 10.0) / 10.0 : 0.0,
                totalResenas
        );
    }

    /**
     * Genera el dashboard de analíticas global para administradores.
     *
     * @return Métricas globales de usuarios, órdenes, facturación y catálogo
     */
    public AdminDashboardResponse dashboardAdmin() {
        Instant[] esteMes = rangoMes(0);

        long totalUsuarios          = userRepository.count();
        long usuariosNuevosEsteMes  = contarEntidadesEnRango("User", esteMes);
        long totalVendedores        = sellerRepository.count();
        long vendedoresPendientes   = sellerRepository.countByStatus(EstadoGenerico.PENDING);

        long ordenesTotales   = orderRepository.count();
        long ordenesEsteMes   = contarEntidadesEnRango("Order", esteMes);
        long ordenesPendientes = orderRepository.findByStatus(
                EstadoPedido.PAYMENT_PENDING, PageRequest.of(0, 1)).getTotalElements();

        BigDecimal ingresosTotales   = ingresosPlataformaTotales();
        BigDecimal ingresosEsteMes   = ingresosPlataformaEnRango(esteMes);
        BigDecimal comisionesEsteMes = ingresosEsteMes
                .multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);

        long pagosAprobados  = pagosPorEstadoEnRango(EstadoPago.APPROVED, esteMes);
        long pagosRechazados = pagosPorEstadoEnRango(EstadoPago.REJECTED, esteMes);
        BigDecimal montoAprobado = montoAprobadoEnRango(esteMes);

        long productosActivos    = productRepository.findByStatus(
                EstadoProducto.ACTIVE, PageRequest.of(0, 1)).getTotalElements();
        long productosPendientes = productRepository.findByStatus(
                EstadoProducto.DRAFT, PageRequest.of(0, 1)).getTotalElements();

        List<TopProductoResponse> topProductos = topProductosGlobal(10);

        return new AdminDashboardResponse(
                totalUsuarios,
                usuariosNuevosEsteMes,
                totalVendedores,
                vendedoresPendientes,
                ordenesTotales,
                ordenesEsteMes,
                ordenesPendientes,
                ingresosTotales,
                ingresosEsteMes,
                comisionesEsteMes,
                pagosAprobados,
                pagosRechazados,
                montoAprobado,
                productosActivos,
                productosPendientes,
                topProductos
        );
    }

    // ─── BI — Top sellers by revenue ────────────────────────────────────────

    public List<TopSellerResponse> topVendedoresPorIngreso(int limite) {
        List<Object[]> rows = entityManager.createQuery("""
                SELECT og.sellerId, SUM(oi.subtotal), COUNT(DISTINCT og.orderId), SUM(oi.quantity)
                FROM OrderGroup og
                JOIN OrderItem oi ON oi.orderGroupId = og.id
                WHERE og.status <> :cancelado
                GROUP BY og.sellerId
                ORDER BY SUM(oi.subtotal) DESC
                """, Object[].class)
                .setParameter("cancelado", EstadoGrupo.CANCELLED)
                .setMaxResults(limite)
                .getResultList();

        List<TopSellerResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID sellerId = (UUID) row[0];
            String storeName = sellerRepository.findById(sellerId)
                    .map(s -> s.getStoreName())
                    .orElse("Tienda " + sellerId.toString().substring(0, 8));
            BigDecimal revenue = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
            result.add(new TopSellerResponse(sellerId, storeName, revenue, ((Number) row[2]).longValue(), ((Number) row[3]).longValue()));
        }
        return result;
    }

    // ─── BI — Trending products (most sold in last N days) ──────────────────

    public List<TopProductoResponse> topProductosTendencia(int dias, int limite) {
        Instant desde = Instant.now().minus(dias, ChronoUnit.DAYS);
        return entityManager.createQuery("""
                SELECT new com.neogaming.analytics.dto.TopProductoResponse(
                    oi.productId, oi.productName,
                    SUM(oi.quantity), SUM(oi.subtotal), COUNT(DISTINCT oi.orderId))
                FROM OrderItem oi
                JOIN Order o ON o.id = oi.orderId
                WHERE o.status NOT IN :excluidos AND o.createdAt >= :desde
                GROUP BY oi.productId, oi.productName
                ORDER BY SUM(oi.quantity) DESC
                """, TopProductoResponse.class)
                .setParameter("excluidos", List.of(EstadoPedido.CANCELLED, EstadoPedido.REFUNDED))
                .setParameter("desde", desde)
                .setMaxResults(limite)
                .getResultList();
    }

    // ─── BI — Revenue breakdown by category ─────────────────────────────────

    public List<Map<String, Object>> ingresosPorCategoria() {
        List<Object[]> rows = entityManager.createQuery("""
                SELECT p.category.name, SUM(oi.subtotal), SUM(oi.quantity), COUNT(DISTINCT oi.orderId)
                FROM OrderItem oi
                JOIN Product p ON p.id = oi.productId
                JOIN Order o ON o.id = oi.orderId
                WHERE o.status NOT IN :excluidos
                GROUP BY p.category.name
                ORDER BY SUM(oi.subtotal) DESC
                """, Object[].class)
                .setParameter("excluidos", List.of(EstadoPedido.CANCELLED, EstadoPedido.REFUNDED))
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("category", row[0] != null ? row[0] : "Sin categoría");
            entry.put("revenue", row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString()));
            entry.put("unidades", ((Number) row[2]).longValue());
            entry.put("ordenes", ((Number) row[3]).longValue());
            result.add(entry);
        }
        return result;
    }

    // ─── Cálculo de rangos de fecha ──────────────────────────────────────────

    /**
     * Devuelve [inicio, fin) del mes indicado por offset respecto al mes actual.
     * offset=0 → mes actual, offset=-1 → mes anterior.
     */
    private Instant[] rangoMes(int offsetMeses) {
        ZonedDateTime ahora = ZonedDateTime.now(ZONA_BOGOTA);
        ZonedDateTime inicio = ahora.plusMonths(offsetMeses)
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime fin = inicio.plusMonths(1);
        return new Instant[]{inicio.toInstant(), fin.toInstant()};
    }

    // ─── Consultas JPQL del vendedor ──────────────────────────────────────────

    private long contarOrdenesPorVendedorEnRango(UUID sellerId, Instant[] rango) {
        return ((Number) entityManager.createQuery("""
                SELECT COUNT(DISTINCT og.orderId)
                FROM OrderGroup og
                JOIN Order o ON o.id = og.orderId
                WHERE og.sellerId = :sid
                  AND og.status <> :cancelado
                  AND o.createdAt >= :inicio AND o.createdAt < :fin
                """)
                .setParameter("sid", sellerId)
                .setParameter("cancelado", EstadoGrupo.CANCELLED)
                .setParameter("inicio", rango[0])
                .setParameter("fin", rango[1])
                .getSingleResult()).longValue();
    }

    private BigDecimal ingresosVendedorEnRango(UUID sellerId, Instant[] rango) {
        Object r = entityManager.createQuery("""
                SELECT COALESCE(SUM(oi.subtotal), 0)
                FROM OrderItem oi
                JOIN OrderGroup og ON og.id = oi.orderGroupId
                JOIN Order o ON o.id = og.orderId
                WHERE og.sellerId = :sid
                  AND og.status <> :cancelado
                  AND o.createdAt >= :inicio AND o.createdAt < :fin
                """)
                .setParameter("sid", sellerId)
                .setParameter("cancelado", EstadoGrupo.CANCELLED)
                .setParameter("inicio", rango[0])
                .setParameter("fin", rango[1])
                .getSingleResult();
        return r instanceof BigDecimal bd ? bd : new BigDecimal(r.toString());
    }

    private long unidadesVendidasVendedorEnRango(UUID sellerId, Instant[] rango) {
        Object r = entityManager.createQuery("""
                SELECT COALESCE(SUM(oi.quantity), 0)
                FROM OrderItem oi
                JOIN OrderGroup og ON og.id = oi.orderGroupId
                JOIN Order o ON o.id = og.orderId
                WHERE og.sellerId = :sid
                  AND og.status <> :cancelado
                  AND o.createdAt >= :inicio AND o.createdAt < :fin
                """)
                .setParameter("sid", sellerId)
                .setParameter("cancelado", EstadoGrupo.CANCELLED)
                .setParameter("inicio", rango[0])
                .setParameter("fin", rango[1])
                .getSingleResult();
        return ((Number) r).longValue();
    }

    private BigDecimal ingresosVendedorTotales(UUID sellerId) {
        Object r = entityManager.createQuery("""
                SELECT COALESCE(SUM(oi.subtotal), 0)
                FROM OrderItem oi
                JOIN OrderGroup og ON og.id = oi.orderGroupId
                WHERE og.sellerId = :sid AND og.status <> :cancelado
                """)
                .setParameter("sid", sellerId)
                .setParameter("cancelado", EstadoGrupo.CANCELLED)
                .getSingleResult();
        return r instanceof BigDecimal bd ? bd : new BigDecimal(r.toString());
    }

    private long gruposVendedorPorEstado(UUID sellerId, EstadoGrupo estado) {
        return ((Number) entityManager.createQuery(
                "SELECT COUNT(og) FROM OrderGroup og WHERE og.sellerId = :sid AND og.status = :e")
                .setParameter("sid", sellerId)
                .setParameter("e", estado)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<TopProductoResponse> topProductosVendedor(UUID sellerId, int limite) {
        return entityManager.createQuery("""
                SELECT new com.neogaming.analytics.dto.TopProductoResponse(
                    oi.productId, oi.productName,
                    SUM(oi.quantity), SUM(oi.subtotal), COUNT(DISTINCT oi.orderId))
                FROM OrderItem oi
                JOIN OrderGroup og ON og.id = oi.orderGroupId
                WHERE og.sellerId = :sid AND og.status <> :cancelado
                GROUP BY oi.productId, oi.productName
                ORDER BY SUM(oi.quantity) DESC
                """)
                .setParameter("sid", sellerId)
                .setParameter("cancelado", EstadoGrupo.CANCELLED)
                .setMaxResults(limite)
                .getResultList();
    }

    private Double promedioResenasVendedor(UUID sellerId) {
        return (Double) entityManager.createQuery("""
                SELECT AVG(r.rating)
                FROM Review r
                JOIN Product p ON p.id = r.productId
                WHERE p.sellerId = :sid AND r.status = :aprobado
                """)
                .setParameter("sid", sellerId)
                .setParameter("aprobado", EstadoResena.APPROVED)
                .getSingleResult();
    }

    private long resenasVendedor(UUID sellerId) {
        return ((Number) entityManager.createQuery("""
                SELECT COUNT(r)
                FROM Review r
                JOIN Product p ON p.id = r.productId
                WHERE p.sellerId = :sid AND r.status = :aprobado
                """)
                .setParameter("sid", sellerId)
                .setParameter("aprobado", EstadoResena.APPROVED)
                .getSingleResult()).longValue();
    }

    // ─── Consultas JPQL globales (admin) ────────────────────────────────────

    private long contarEntidadesEnRango(String entidad, Instant[] rango) {
        return ((Number) entityManager.createQuery(
                "SELECT COUNT(e) FROM " + entidad + " e WHERE e.createdAt >= :inicio AND e.createdAt < :fin")
                .setParameter("inicio", rango[0])
                .setParameter("fin", rango[1])
                .getSingleResult()).longValue();
    }

    private BigDecimal ingresosPlataformaTotales() {
        Object r = entityManager.createQuery(
                "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :aprobado")
                .setParameter("aprobado", EstadoPago.APPROVED)
                .getSingleResult();
        return r instanceof BigDecimal bd ? bd : new BigDecimal(r.toString());
    }

    private BigDecimal ingresosPlataformaEnRango(Instant[] rango) {
        Object r = entityManager.createQuery("""
                SELECT COALESCE(SUM(p.amount), 0)
                FROM Payment p
                WHERE p.status = :aprobado AND p.createdAt >= :inicio AND p.createdAt < :fin
                """)
                .setParameter("aprobado", EstadoPago.APPROVED)
                .setParameter("inicio", rango[0])
                .setParameter("fin", rango[1])
                .getSingleResult();
        return r instanceof BigDecimal bd ? bd : new BigDecimal(r.toString());
    }

    private long pagosPorEstadoEnRango(EstadoPago estado, Instant[] rango) {
        return ((Number) entityManager.createQuery("""
                SELECT COUNT(p)
                FROM Payment p
                WHERE p.status = :estado AND p.createdAt >= :inicio AND p.createdAt < :fin
                """)
                .setParameter("estado", estado)
                .setParameter("inicio", rango[0])
                .setParameter("fin", rango[1])
                .getSingleResult()).longValue();
    }

    private BigDecimal montoAprobadoEnRango(Instant[] rango) {
        Object r = entityManager.createQuery("""
                SELECT COALESCE(SUM(p.amount), 0)
                FROM Payment p
                WHERE p.status = :aprobado AND p.createdAt >= :inicio AND p.createdAt < :fin
                """)
                .setParameter("aprobado", EstadoPago.APPROVED)
                .setParameter("inicio", rango[0])
                .setParameter("fin", rango[1])
                .getSingleResult();
        return r instanceof BigDecimal bd ? bd : new BigDecimal(r.toString());
    }

    @SuppressWarnings("unchecked")
    private List<TopProductoResponse> topProductosGlobal(int limite) {
        return entityManager.createQuery("""
                SELECT new com.neogaming.analytics.dto.TopProductoResponse(
                    oi.productId, oi.productName,
                    SUM(oi.quantity), SUM(oi.subtotal), COUNT(DISTINCT oi.orderId))
                FROM OrderItem oi
                JOIN Order o ON o.id = oi.orderId
                WHERE o.status NOT IN :excluidos
                GROUP BY oi.productId, oi.productName
                ORDER BY SUM(oi.quantity) DESC
                """)
                .setParameter("excluidos", List.of(EstadoPedido.CANCELLED, EstadoPedido.REFUNDED))
                .setMaxResults(limite)
                .getResultList();
    }
}
