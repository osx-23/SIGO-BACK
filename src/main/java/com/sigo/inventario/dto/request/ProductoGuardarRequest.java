package com.sigo.inventario.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
public record ProductoGuardarRequest(
  @NotBlank @Size(max=60) String codigo,
  @NotBlank @Size(max=150) String nombre,
  @Size(max=350) String descripcion,
  @NotNull Long categoriaId,
  @NotNull Long ambitoId,
  @NotBlank @Size(max=50) String unidadMedida,
  Boolean activo,
  @NotEmpty Set<String> roles,
  @NotEmpty List<@Valid ProductoPlazaRequest> plazas
){}
