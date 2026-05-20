package com.neogaming.address.service;

import com.neogaming.address.domain.Address;
import com.neogaming.address.dto.request.AddressRequest;
import com.neogaming.address.dto.response.AddressResponse;
import com.neogaming.address.mapper.AddressMapper;
import com.neogaming.address.repository.AddressRepository;
import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión de direcciones para NeoGaming.
 *
 * Reglas de negocio implementadas:
 *  - Un usuario puede tener múltiples direcciones activas
 *  - Solo una dirección puede ser principal (is_primary = true) a la vez
 *  - La primera dirección creada se convierte automáticamente en principal
 *  - Si se elimina la dirección principal, la más antigua se convierte en la nueva principal
 *  - Las direcciones se eliminan con soft delete (status = DELETED), no se borran de la BD
 *  - Un usuario solo puede acceder y modificar sus propias direcciones
 *
 * Todas las operaciones de escritura son @Transactional para garantizar
 * consistencia al actualizar la dirección principal.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    /**
     * Lista todas las direcciones activas del usuario.
     * Las muestra con la principal primero, luego por fecha de creación.
     *
     * @param userId UUID del usuario autenticado
     * @return Lista de direcciones activas del usuario
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> listarPorUsuario(UUID userId) {
        return addressRepository
                .findByUserIdAndStatusOrderByPrimaryDescCreatedAtAsc(userId, EstadoGenerico.ACTIVE)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    /**
     * Obtiene el detalle de una dirección específica del usuario.
     *
     * @param id     UUID de la dirección
     * @param userId UUID del usuario (para verificar que le pertenece)
     * @return Detalle de la dirección
     * @throws ResourceNotFoundException si la dirección no existe o no pertenece al usuario
     */
    @Transactional(readOnly = true)
    public AddressResponse obtenerPorId(UUID id, UUID userId) {
        Address address = buscarPorIdYUsuario(id, userId);
        return addressMapper.toResponse(address);
    }

    /**
     * Crea una nueva dirección para el usuario.
     *
     * Si es la primera dirección del usuario, se marca automáticamente como principal.
     *
     * @param request Datos de la nueva dirección
     * @param userId  UUID del usuario propietario
     * @return La dirección creada
     */
    public AddressResponse crear(AddressRequest request, UUID userId) {
        // Determinar si esta será la primera dirección (se convierte en principal automáticamente)
        boolean esLaPrimera = addressRepository
                .countByUserIdAndStatus(userId, EstadoGenerico.ACTIVE) == 0;

        Address address = Address.builder()
                .userId(userId)
                .label(request.label())
                .street(request.street())
                .number(request.number())
                .floor(request.floor())
                .apartment(request.apartment())
                .city(request.city())
                .department(request.department())
                .country("Colombia")
                .postalCode(request.postalCode())
                .primary(esLaPrimera) // Primera dirección = principal automáticamente
                .status(EstadoGenerico.ACTIVE)
                .build();

        return addressMapper.toResponse(addressRepository.save(address));
    }

    /**
     * Actualiza los datos de una dirección existente del usuario.
     *
     * @param id      UUID de la dirección a actualizar
     * @param request Nuevos datos de la dirección
     * @param userId  UUID del usuario (para verificar que le pertenece)
     * @return La dirección con los datos actualizados
     * @throws ResourceNotFoundException si la dirección no existe o no pertenece al usuario
     */
    public AddressResponse actualizar(UUID id, AddressRequest request, UUID userId) {
        Address address = buscarPorIdYUsuario(id, userId);

        address.setLabel(request.label());
        address.setStreet(request.street());
        address.setNumber(request.number());
        address.setFloor(request.floor());
        address.setApartment(request.apartment());
        address.setCity(request.city());
        address.setDepartment(request.department());
        address.setPostalCode(request.postalCode());

        return addressMapper.toResponse(addressRepository.save(address));
    }

    /**
     * Elimina una dirección del usuario (soft delete: cambia status a DELETED).
     *
     * Si la dirección eliminada era la principal, asigna automáticamente
     * la dirección activa más antigua como la nueva principal.
     *
     * @param id     UUID de la dirección a eliminar
     * @param userId UUID del usuario (para verificar que le pertenece)
     * @throws ResourceNotFoundException si la dirección no existe o no pertenece al usuario
     */
    public void eliminar(UUID id, UUID userId) {
        Address address = buscarPorIdYUsuario(id, userId);
        boolean eraPrincipal = address.isPrimary();

        // Soft delete: cambiar estado a DELETED (no borrar de la BD)
        address.setStatus(EstadoGenerico.DELETED);
        address.setPrimary(false);
        addressRepository.save(address);

        // Si era la principal, reasignar la principal a la dirección más antigua restante
        if (eraPrincipal) {
            addressRepository
                    .findFirstByUserIdAndStatusOrderByCreatedAtAsc(userId, EstadoGenerico.ACTIVE)
                    .ifPresent(nuevaPrincipal -> {
                        nuevaPrincipal.setPrimary(true);
                        addressRepository.save(nuevaPrincipal);
                    });
        }
    }

    /**
     * Establece una dirección como la principal del usuario.
     *
     * Desmarca la principal actual y marca la nueva. Solo puede haber una principal.
     *
     * @param id     UUID de la dirección que se quiere marcar como principal
     * @param userId UUID del usuario
     * @return La dirección recién marcada como principal
     * @throws ResourceNotFoundException si la dirección no existe o no pertenece al usuario
     * @throws BusinessRuleException     si la dirección ya es la principal
     */
    public AddressResponse establecerPrincipal(UUID id, UUID userId) {
        Address nuevaPrincipal = buscarPorIdYUsuario(id, userId);

        // Verificar que no sea ya la principal (operación innecesaria)
        if (nuevaPrincipal.isPrimary()) {
            throw new BusinessRuleException(
                    "Esta dirección ya es tu dirección principal",
                    "DIRECCION_YA_ES_PRINCIPAL"
            );
        }

        // Desmarcar la principal actual
        addressRepository
                .findByUserIdAndStatusAndPrimary(userId, EstadoGenerico.ACTIVE, true)
                .ifPresent(actualPrincipal -> {
                    actualPrincipal.setPrimary(false);
                    addressRepository.save(actualPrincipal);
                });

        // Marcar la nueva como principal
        nuevaPrincipal.setPrimary(true);
        return addressMapper.toResponse(addressRepository.save(nuevaPrincipal));
    }

    /**
     * Método auxiliar: busca una dirección verificando que pertenezca al usuario.
     * Centraliza la lógica de acceso para evitar que un usuario acceda a direcciones ajenas.
     *
     * @param id     UUID de la dirección
     * @param userId UUID del usuario que debe ser el dueño
     * @return La entidad Address si existe y pertenece al usuario
     * @throws ResourceNotFoundException si no existe o no pertenece al usuario
     */
    private Address buscarPorIdYUsuario(UUID id, UUID userId) {
        return addressRepository
                .findByIdAndUserIdAndStatus(id, userId, EstadoGenerico.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección", id.toString()));
    }
}
