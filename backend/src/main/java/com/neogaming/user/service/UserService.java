package com.neogaming.user.service;

import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.RolUsuario;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.common.response.PageResponse;
import com.neogaming.user.domain.User;
import com.neogaming.user.dto.request.ChangePasswordRequest;
import com.neogaming.user.dto.request.UpdateProfileRequest;
import com.neogaming.user.dto.response.UserResponse;
import com.neogaming.user.mapper.UserMapper;
import com.neogaming.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de gestión del perfil de usuario para NeoGaming.
 *
 * Operaciones disponibles:
 *  - obtenerPorId      : Retorna el perfil de un usuario por su UUID
 *  - actualizarPerfil  : Actualiza los datos personales del usuario autenticado
 *  - cambiarContrasena : Cambia la contraseña verificando la actual
 *
 * Todos los métodos de escritura son @Transactional para garantizar
 * atomicidad en la base de datos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Obtiene el perfil completo de un usuario por su UUID.
     * Operación de solo lectura (no modifica datos).
     *
     * @param id UUID del usuario a buscar
     * @return DTO con los datos públicos del usuario
     * @throws ResourceNotFoundException si no existe un usuario con ese UUID
     */
    @Transactional(readOnly = true)
    public UserResponse obtenerPorId(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id.toString()));
    }

    /**
     * Actualiza los datos del perfil del usuario.
     *
     * Solo se actualizan los campos que lleguen con valor no-null en el request.
     * Los campos null se conservan con su valor actual (actualización parcial).
     *
     * @param id      UUID del usuario a actualizar
     * @param request Campos a actualizar (todos opcionales)
     * @return DTO con el perfil actualizado
     * @throws ResourceNotFoundException si no existe un usuario con ese UUID
     */
    public UserResponse actualizarPerfil(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id.toString()));

        // Actualizar solo los campos que llegaron con valor (null = no cambiar)
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName()  != null) user.setLastName(request.lastName());
        if (request.phone()     != null) user.setPhone(request.phone());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * Validaciones:
     *  1. La contraseña actual debe coincidir con el hash almacenado
     *  2. La nueva contraseña y su confirmación deben ser iguales
     *
     * @param id      UUID del usuario
     * @param request Contraseña actual, nueva contraseña y confirmación
     * @throws BusinessRuleException     si la contraseña actual es incorrecta o las nuevas no coinciden
     * @throws ResourceNotFoundException si no existe un usuario con ese UUID
     */
    public void cambiarContrasena(UUID id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id.toString()));

        // Verificar que la contraseña actual es correcta
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException(
                    "La contraseña actual es incorrecta",
                    "CONTRASENA_ACTUAL_INCORRECTA"
            );
        }

        // Verificar que la nueva contraseña y la confirmación coinciden
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessRuleException(
                    "La nueva contraseña y su confirmación no coinciden",
                    "CONTRASENAS_NO_COINCIDEN"
            );
        }

        // Guardar el nuevo hash BCrypt
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listarUsuarios(EstadoGenerico status, String q, Pageable pageable) {
        var page = (q != null && !q.isBlank())
                ? userRepository.searchByRoleNot(RolUsuario.ADMIN, q, pageable)
                : (status != null)
                    ? userRepository.findByRoleNotAndStatus(RolUsuario.ADMIN, status, pageable)
                    : userRepository.findByRoleNot(RolUsuario.ADMIN, pageable);
        return PageResponse.from(page.map(userMapper::toResponse));
    }

    public UserResponse suspenderUsuario(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id.toString()));
        if (user.getStatus() == EstadoGenerico.SUSPENDED) {
            throw new BusinessRuleException("El usuario ya está suspendido", "USUARIO_YA_SUSPENDIDO");
        }
        user.setStatus(EstadoGenerico.SUSPENDED);
        return userMapper.toResponse(userRepository.save(user));
    }

    public UserResponse reactivarUsuario(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id.toString()));
        if (user.getStatus() != EstadoGenerico.SUSPENDED) {
            throw new BusinessRuleException("El usuario no está suspendido", "USUARIO_NO_SUSPENDIDO");
        }
        user.setStatus(EstadoGenerico.ACTIVE);
        return userMapper.toResponse(userRepository.save(user));
    }
}
