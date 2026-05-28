package com.neogaming.seller.service;

import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.RolUsuario;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.common.response.PageResponse;
import com.neogaming.common.util.SlugUtils;
import com.neogaming.seller.domain.PaymentAccount;
import com.neogaming.seller.domain.Seller;
import com.neogaming.seller.dto.request.PaymentAccountRequest;
import com.neogaming.seller.dto.request.SellerRegistrationRequest;
import com.neogaming.seller.dto.request.UpdateSellerRequest;
import com.neogaming.seller.dto.response.PaymentAccountResponse;
import com.neogaming.seller.dto.response.PublicSellerResponse;
import com.neogaming.seller.dto.response.SellerResponse;
import com.neogaming.seller.mapper.SellerMapper;
import com.neogaming.order.repository.OrderItemRepository;
import com.neogaming.review.repository.ReviewRepository;
import com.neogaming.seller.repository.PaymentAccountRepository;
import com.neogaming.seller.repository.SellerRepository;
import com.neogaming.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión de vendedores para NeoGaming.
 *
 * Reglas de negocio implementadas:
 *  - Un usuario solo puede tener un perfil de vendedor
 *  - El perfil inicia en estado PENDING y debe ser aprobado por un ADMIN
 *  - Solo el propio vendedor puede actualizar su perfil y cuentas bancarias
 *  - El nombre de tienda genera automáticamente un slug único
 *  - Solo una cuenta bancaria puede estar activa a la vez
 *  - Las tiendas suspendidas no aparecen en el catálogo público
 *
 * Flujo de aprobación:
 *  1. Usuario llama a registrarVendedor() → status = PENDING
 *  2. Admin llama a aprobar() → status = ACTIVE, rol del usuario → SELLER
 *  3. Admin puede llamar a suspender() → status = SUSPENDED
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SellerService {

    private final SellerRepository sellerRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final SellerMapper sellerMapper;
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final com.neogaming.seller.repository.FollowedSellerRepository followedSellerRepository;
    private final UserRepository userRepository;

    // ===== OPERACIONES DEL VENDEDOR =====

    /**
     * Registra un nuevo perfil de vendedor para el usuario autenticado.
     *
     * El perfil queda en PENDING hasta la aprobación del administrador.
     * Se genera automáticamente un slug único a partir del nombre de la tienda.
     *
     * @param request Datos del perfil de vendedor
     * @param userId  UUID del usuario que solicita ser vendedor
     * @return El perfil recién creado en estado PENDING
     * @throws BusinessRuleException si el usuario ya tiene un perfil de vendedor
     */
    public SellerResponse registrarVendedor(SellerRegistrationRequest request, UUID userId) {
        // Verificar que el usuario no tenga ya un perfil de vendedor
        if (sellerRepository.existsByUserId(userId)) {
            throw new BusinessRuleException(
                    "Ya tienes un perfil de vendedor registrado",
                    "VENDEDOR_YA_EXISTE"
            );
        }

        // Generar slug único para la tienda
        String slug = generarSlugUnico(request.storeName());

        Seller seller = Seller.builder()
                .userId(userId)
                .storeName(request.storeName())
                .storeSlug(slug)
                .storeDescription(request.storeDescription())
                .tipoDocumento(request.tipoDocumento())
                .numeroDocumento(request.numeroDocumento())
                .razonSocial(request.razonSocial())
                .tipoRegimen(request.tipoRegimen())
                .phone(request.phone())
                .address(request.address())
                .city(request.city())
                .department(request.department())
                .status(EstadoGenerico.PENDING)
                .build();

        return sellerMapper.toResponse(sellerRepository.save(seller));
    }

    /**
     * Obtiene el perfil del vendedor autenticado.
     *
     * @param userId UUID del usuario vendedor
     * @return Su perfil de vendedor
     * @throws ResourceNotFoundException si no tiene perfil de vendedor
     */
    @Transactional(readOnly = true)
    public SellerResponse obtenerMiPerfil(UUID userId) {
        Seller seller = buscarPorUserId(userId);
        return sellerMapper.toResponse(seller);
    }

    /**
     * Actualiza los datos editables del perfil del vendedor.
     *
     * No permite modificar datos fiscales (documento, régimen) ya que
     * esos requieren validación por parte del administrador.
     *
     * @param request Campos a actualizar (todos opcionales)
     * @param userId  UUID del vendedor
     * @return El perfil con los datos actualizados
     */
    public SellerResponse actualizarPerfil(UpdateSellerRequest request, UUID userId) {
        Seller seller = buscarPorUserId(userId);

        // Actualizar nombre de tienda y regenerar slug si cambia
        if (request.storeName() != null && !request.storeName().isBlank()) {
            if (!seller.getStoreName().equals(request.storeName())) {
                seller.setStoreSlug(generarSlugUnico(request.storeName()));
            }
            seller.setStoreName(request.storeName());
        }
        if (request.storeDescription() != null) {
            seller.setStoreDescription(request.storeDescription());
        }
        if (request.phone() != null) {
            seller.setPhone(request.phone());
        }
        if (request.address() != null) {
            seller.setAddress(request.address());
        }
        if (request.city() != null) {
            seller.setCity(request.city());
        }
        if (request.department() != null) {
            seller.setDepartment(request.department());
        }
        if (request.storeLogoUrl() != null) {
            seller.setStoreLogoUrl(request.storeLogoUrl());
        }
        if (request.storeBannerUrl() != null) {
            seller.setStoreBannerUrl(request.storeBannerUrl());
        }

        return sellerMapper.toResponse(sellerRepository.save(seller));
    }

    // ===== OPERACIONES PÚBLICAS =====

    /**
     * Obtiene la información pública de una tienda por su slug.
     * Solo devuelve tiendas con status ACTIVE.
     *
     * @param storeSlug Slug URL de la tienda (ej: "gaming-shop-bogota")
     * @return Datos públicos de la tienda
     * @throws ResourceNotFoundException si la tienda no existe o no está activa
     */
    @Transactional(readOnly = true)
    public com.neogaming.common.response.PageResponse<PublicSellerResponse> buscarTiendasPublicas(String query, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<PublicSellerResponse> page =
                sellerRepository.findByStoreNameContainingIgnoreCaseAndStatus(query, EstadoGenerico.ACTIVE, pageable)
                        .map(s -> sellerMapper.toPublicResponse(s, null, null, null));
        return com.neogaming.common.response.PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PublicSellerResponse obtenerTiendaPublica(String storeSlug) {
        Seller seller = sellerRepository.findByStoreSlug(storeSlug)
                .filter(s -> s.getStatus() == EstadoGenerico.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda", storeSlug));
        Long totalSales = orderItemRepository.contarVentasPorVendedor(seller.getId());
        Double avgRating = reviewRepository.calcularPromedioRatingPorVendedor(seller.getId());
        Long totalReviews = reviewRepository.contarResenasPorVendedor(seller.getId());
        return sellerMapper.toPublicResponse(seller, totalSales, avgRating, totalReviews);
    }

    // ===== OPERACIONES DE CUENTAS BANCARIAS =====

    /**
     * Lista todas las cuentas bancarias del vendedor autenticado.
     *
     * @param userId UUID del vendedor
     * @return Lista de cuentas con número enmascarado
     */
    @Transactional(readOnly = true)
    public List<PaymentAccountResponse> listarCuentas(UUID userId) {
        Seller seller = buscarPorUserId(userId);
        return paymentAccountRepository
                .findBySellerIdOrderByCreatedAtDesc(seller.getId())
                .stream()
                .map(sellerMapper::toPaymentAccountResponse)
                .toList();
    }

    /**
     * Registra una nueva cuenta bancaria para el vendedor.
     *
     * @param request Datos de la cuenta bancaria
     * @param userId  UUID del vendedor
     * @return La cuenta recién registrada (inactiva por defecto)
     */
    public PaymentAccountResponse agregarCuenta(PaymentAccountRequest request, UUID userId) {
        Seller seller = buscarPorUserId(userId);

        PaymentAccount cuenta = PaymentAccount.builder()
                .sellerId(seller.getId())
                .bankName(request.bankName())
                .accountType(request.accountType())
                .accountNumber(request.accountNumber())
                .accountHolder(request.accountHolder())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .active(false) // Inactiva hasta que el vendedor la active explícitamente
                .build();

        return sellerMapper.toPaymentAccountResponse(paymentAccountRepository.save(cuenta));
    }

    /**
     * Establece una cuenta bancaria como la activa para recibir pagos.
     *
     * Desactiva automáticamente todas las demás cuentas del vendedor.
     * Solo puede haber una cuenta activa en todo momento.
     *
     * @param cuentaId UUID de la cuenta a activar
     * @param userId   UUID del vendedor
     * @return La cuenta activada
     * @throws ResourceNotFoundException si la cuenta no existe o no pertenece al vendedor
     */
    public PaymentAccountResponse activarCuenta(UUID cuentaId, UUID userId) {
        Seller seller = buscarPorUserId(userId);

        PaymentAccount cuenta = paymentAccountRepository
                .findByIdAndSellerId(cuentaId, seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria", cuentaId.toString()));

        // Desactivar todas las cuentas del vendedor antes de activar la nueva
        paymentAccountRepository.deactivateAllBySellerId(seller.getId());

        cuenta.setActive(true);
        return sellerMapper.toPaymentAccountResponse(paymentAccountRepository.save(cuenta));
    }

    /**
     * Elimina una cuenta bancaria del vendedor.
     *
     * No se puede eliminar la cuenta activa — debe activarse otra primero.
     *
     * @param cuentaId UUID de la cuenta a eliminar
     * @param userId   UUID del vendedor
     * @throws BusinessRuleException si se intenta eliminar la cuenta activa
     */
    public void eliminarCuenta(UUID cuentaId, UUID userId) {
        Seller seller = buscarPorUserId(userId);

        PaymentAccount cuenta = paymentAccountRepository
                .findByIdAndSellerId(cuentaId, seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria", cuentaId.toString()));

        if (cuenta.isActive()) {
            throw new BusinessRuleException(
                    "No puedes eliminar la cuenta bancaria activa. Activa otra cuenta primero.",
                    "CUENTA_ACTIVA_NO_ELIMINABLE"
            );
        }

        paymentAccountRepository.delete(cuenta);
    }

    // ===== TIENDAS SEGUIDAS =====

    public void seguirTienda(UUID sellerId, UUID userId) {
        if (!sellerRepository.existsById(sellerId))
            throw new com.neogaming.common.exception.ResourceNotFoundException("Tienda", sellerId.toString());
        if (!followedSellerRepository.existsByUserIdAndSellerId(userId, sellerId)) {
            followedSellerRepository.save(
                com.neogaming.seller.domain.FollowedSeller.builder()
                    .userId(userId).sellerId(sellerId).build());
        }
    }

    public void dejarDeSeguirTienda(UUID sellerId, UUID userId) {
        followedSellerRepository.findByUserIdAndSellerId(userId, sellerId)
            .ifPresent(followedSellerRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean estaSigniendo(UUID sellerId, UUID userId) {
        return followedSellerRepository.existsByUserIdAndSellerId(userId, sellerId);
    }

    @Transactional(readOnly = true)
    public java.util.List<PublicSellerResponse> listarTiendasSeguidas(UUID userId) {
        return followedSellerRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(f -> sellerRepository.findById(f.getSellerId()).orElse(null))
            .filter(s -> s != null && s.getStatus() == EstadoGenerico.ACTIVE)
            .map(s -> sellerMapper.toPublicResponse(s, null, null, null))
            .toList();
    }

    // ===== OPERACIONES DE ADMINISTRADOR =====

    /**
     * Lista todos los vendedores con un estado dado (para el panel de administración).
     *
     * @param status   Estado a filtrar
     * @param pageable Configuración de paginación
     * @return Página de vendedores
     */
    @Transactional(readOnly = true)
    public PageResponse<SellerResponse> listarPorEstado(EstadoGenerico status, Pageable pageable) {
        Page<SellerResponse> page = (status != null
                ? sellerRepository.findByStatus(status, pageable)
                : sellerRepository.findAll(pageable))
                .map(sellerMapper::toResponse);
        return PageResponse.from(page);
    }

    /**
     * Aprueba el perfil de un vendedor (operación de administrador).
     *
     * Cambia el status de PENDING a ACTIVE, permitiéndole publicar productos.
     *
     * @param sellerId UUID del perfil de vendedor a aprobar
     * @return El perfil aprobado
     * @throws BusinessRuleException si el vendedor no está en estado PENDING
     */
    public SellerResponse aprobar(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor", sellerId.toString()));

        if (seller.getStatus() != EstadoGenerico.PENDING) {
            throw new BusinessRuleException(
                    "Solo se pueden aprobar vendedores en estado PENDIENTE",
                    "ESTADO_INVALIDO_PARA_APROBACION"
            );
        }

        seller.setStatus(EstadoGenerico.ACTIVE);
        sellerRepository.save(seller);

        userRepository.findById(seller.getUserId()).ifPresent(user -> {
            user.setRole(RolUsuario.SELLER);
            userRepository.save(user);
        });

        return sellerMapper.toResponse(seller);
    }

    /**
     * Suspende el perfil de un vendedor (operación de administrador).
     *
     * Sus productos quedan invisible en el catálogo mientras esté suspendido.
     *
     * @param sellerId UUID del perfil de vendedor a suspender
     * @return El perfil suspendido
     */
    public SellerResponse suspender(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor", sellerId.toString()));

        if (seller.getStatus() == EstadoGenerico.SUSPENDED) {
            throw new BusinessRuleException(
                    "El vendedor ya está suspendido",
                    "VENDEDOR_YA_SUSPENDIDO"
            );
        }

        seller.setStatus(EstadoGenerico.SUSPENDED);
        return sellerMapper.toResponse(sellerRepository.save(seller));
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Busca el perfil de vendedor de un usuario.
     * Método auxiliar reutilizado en múltiples operaciones del vendedor autenticado.
     *
     * @param userId UUID del usuario
     * @return Su entidad Seller
     * @throws ResourceNotFoundException si no tiene perfil de vendedor
     */
    public UUID obtenerSellerIdByUserId(UUID userId) {
        return buscarPorUserId(userId).getId();
    }

    private Seller buscarPorUserId(UUID userId) {
        return sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de vendedor", userId.toString()));
    }

    /**
     * Genera un slug URL-amigable único a partir del nombre de la tienda.
     *
     * Si el slug base ya está en uso, agrega un sufijo numérico incremental
     * hasta encontrar uno disponible. Ejemplo: "gaming-shop" → "gaming-shop-2"
     *
     * @param storeName Nombre de la tienda
     * @return Slug único garantizado
     */
    private String generarSlugUnico(String storeName) {
        String baseSlug = SlugUtils.toSlug(storeName);
        String candidato = baseSlug;
        int contador = 2;

        // Incrementar el sufijo hasta encontrar un slug libre
        while (sellerRepository.existsByStoreSlug(candidato)) {
            candidato = baseSlug + "-" + contador;
            contador++;
        }

        return candidato;
    }
}
