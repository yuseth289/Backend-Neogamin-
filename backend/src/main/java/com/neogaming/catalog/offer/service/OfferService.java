package com.neogaming.catalog.offer.service;

import com.neogaming.catalog.offer.domain.Offer;
import com.neogaming.catalog.offer.dto.request.OfferRequest;
import com.neogaming.catalog.offer.dto.response.OfferResponse;
import com.neogaming.catalog.offer.mapper.OfferMapper;
import com.neogaming.catalog.offer.repository.OfferRepository;
import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.TipoDescuento;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión de ofertas y descuentos para NeoGaming.
 *
 * Reglas de negocio implementadas:
 *  - Solo el vendedor propietario puede crear/modificar ofertas de su producto
 *  - No se pueden solapar dos ofertas ACTIVE para el mismo producto
 *  - El descuento PERCENTAGE no puede superar el 90% del precio
 *  - El descuento FIXED no puede dejar el precio en negativo
 *  - El precio con descuento se persiste para optimizar queries del catálogo
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OfferService {

    private final OfferRepository offerRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final OfferMapper offerMapper;

    /**
     * Crea una nueva oferta de descuento para un producto del vendedor.
     *
     * Valida que no haya solapamiento con otra oferta activa y que
     * el descuento sea válido respecto al precio del producto.
     *
     * @param productId UUID del producto
     * @param request   Datos de la oferta
     * @param userId    UUID del vendedor
     * @return La oferta recién creada
     */
    public OfferResponse crear(UUID productId, OfferRequest request, UUID userId) {
        Product product = buscarProductoDelVendedor(productId, userId);

        if (!request.endDate().isAfter(request.startDate())) {
            throw new BusinessRuleException(
                    "La fecha de fin debe ser posterior a la fecha de inicio",
                    "FECHAS_OFERTA_INVALIDAS"
            );
        }

        Instant startInstant = request.startDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant   = request.endDate().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC).toInstant();

        if (offerRepository.existeSolapamiento(productId, EstadoGenerico.ACTIVE, startInstant, endInstant)) {
            throw new BusinessRuleException(
                    "Ya existe una oferta activa para este producto en ese período",
                    "OFERTA_SOLAPADA"
            );
        }

        BigDecimal precioFinal = product.getBasePrice()
                .multiply(BigDecimal.ONE.add(
                        product.getIvaPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                )).setScale(2, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.subtract(
                request.discountPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        BigDecimal precioConDescuento = precioFinal.multiply(factor).setScale(2, RoundingMode.HALF_UP);

        if (precioConDescuento.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(
                    "El descuento dejaría el precio en $0 o negativo", "DESCUENTO_MAYOR_QUE_PRECIO");
        }

        Offer offer = Offer.builder()
                .productId(productId)
                .name("Descuento " + request.discountPercent().stripTrailingZeros().toPlainString() + "%")
                .discountType(TipoDescuento.PERCENTAGE)
                .discountValue(request.discountPercent())
                .discountedPrice(precioConDescuento)
                .startDate(startInstant)
                .endDate(endInstant)
                .status(EstadoGenerico.ACTIVE)
                .build();

        return offerMapper.toResponse(offerRepository.save(offer));
    }

    /**
     * Lista todas las ofertas de un producto del vendedor.
     *
     * @param productId UUID del producto
     * @param userId    UUID del vendedor
     * @return Lista de todas las ofertas del producto
     */
    @Transactional(readOnly = true)
    public List<OfferResponse> listar(UUID productId, UUID userId) {
        buscarProductoDelVendedor(productId, userId);
        return offerRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(offerMapper::toResponse)
                .toList();
    }

    /**
     * Obtiene la oferta vigente de un producto (para el catálogo público).
     * Retorna la oferta si está activa y dentro del período de vigencia.
     *
     * @param productId UUID del producto
     * @return La oferta vigente o null si no hay ninguna
     */
    @Transactional(readOnly = true)
    public OfferResponse obtenerVigente(UUID productId) {
        return offerRepository
                .findOfertaVigente(productId, EstadoGenerico.ACTIVE, Instant.now())
                .map(offerMapper::toResponse)
                .orElse(null);
    }

    /**
     * Desactiva una oferta de un producto del vendedor.
     *
     * @param productId UUID del producto
     * @param offerId   UUID de la oferta a desactivar
     * @param userId    UUID del vendedor
     */
    public void desactivar(UUID productId, UUID offerId, UUID userId) {
        buscarProductoDelVendedor(productId, userId);

        Offer offer = offerRepository.findById(offerId)
                .filter(o -> o.getProductId().equals(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Oferta", offerId.toString()));

        offer.setStatus(EstadoGenerico.INACTIVE);
        offerRepository.save(offer);
    }

    /**
     * Busca un producto verificando que pertenezca al vendedor autenticado.
     *
     * @param productId UUID del producto
     * @param userId    UUID del usuario vendedor
     * @return El producto si existe y pertenece al vendedor
     */
    private Product buscarProductoDelVendedor(UUID productId, UUID userId) {
        return sellerRepository.findByUserId(userId)
                .flatMap(seller -> productRepository.findByIdAndSellerId(productId, seller.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId.toString()));
    }
}
