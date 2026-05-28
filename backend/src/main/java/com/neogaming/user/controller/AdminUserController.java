package com.neogaming.user.controller;

import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.response.PageResponse;
import com.neogaming.user.dto.response.UserResponse;
import com.neogaming.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Usuarios", description = "Gestión administrativa de usuarios (solo ADMIN)")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Retorna usuarios paginados (excluye admins). Filtro por estado opcional.")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listar(
            @RequestParam(required = false) EstadoGenerico status,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(userService.listarUsuarios(status, pageable)));
    }

    @PatchMapping("/{id}/suspend")
    @Operation(summary = "Suspender usuario", description = "Suspende un usuario activo o inactivo.")
    public ResponseEntity<ApiResponse<UserResponse>> suspender(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Usuario suspendido", userService.suspenderUsuario(id)));
    }

    @PatchMapping("/{id}/reactivate")
    @Operation(summary = "Reactivar usuario", description = "Reactiva un usuario suspendido.")
    public ResponseEntity<ApiResponse<UserResponse>> reactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Usuario reactivado", userService.reactivarUsuario(id)));
    }
}
