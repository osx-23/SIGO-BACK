package com.sigo.inventario.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CorregirInventarioRequest(
        @NotEmpty List<@Valid GuardarConteoItemRequest> productos,
        @NotBlank @Size(min = 5, max = 500) String motivo
) {}
