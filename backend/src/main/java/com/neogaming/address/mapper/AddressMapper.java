package com.neogaming.address.mapper;

import com.neogaming.address.domain.Address;
import com.neogaming.address.dto.response.AddressResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper responsable de convertir entidades Address a DTOs de respuesta.
 *
 * Mapeo manual para control explícito sobre los campos expuestos.
 * El campo "status" no se incluye en la respuesta (los clientes no necesitan
 * ver el estado interno de la dirección).
 */
@Component
public class AddressMapper {

    /**
     * Convierte una entidad Address en un AddressResponse para enviar al cliente.
     *
     * @param address Entidad persistida en la base de datos
     * @return DTO listo para serializar como JSON
     */
    public AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getStreet(),
                address.getNumber(),
                address.getFloor(),
                address.getApartment(),
                address.getCity(),
                address.getDepartment(),
                address.getCountry(),
                address.getPostalCode(),
                address.isPrimary(),
                address.getCreatedAt()
        );
    }
}
