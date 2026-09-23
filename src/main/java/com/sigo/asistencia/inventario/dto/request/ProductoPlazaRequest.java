package com.sigo.asistencia.inventario.dto.request;
import jakarta.validation.constraints.*;
public record ProductoPlazaRequest(@NotNull Long plazaId,@NotNull @Min(0) Integer stockMinimo){}
