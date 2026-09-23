package com.sigo.inventario.api.dto.request;
import jakarta.validation.constraints.*;
public record ProductoPlazaRequest(@NotNull Long plazaId,@NotNull @Min(0) Integer stockMinimo){}
