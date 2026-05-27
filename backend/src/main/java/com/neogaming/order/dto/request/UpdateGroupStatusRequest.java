package com.neogaming.order.dto.request;

import com.neogaming.common.enums.EstadoGrupo;
import jakarta.validation.constraints.NotNull;

public record UpdateGroupStatusRequest(
        @NotNull EstadoGrupo status,
        String trackingNumber
) {}
