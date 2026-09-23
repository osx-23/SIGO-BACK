package com.sigo.inventario.api.dto.request;
import jakarta.validation.constraints.*;
public record AnularInventarioRequest(@NotBlank @Size(max=500) String motivo){}
