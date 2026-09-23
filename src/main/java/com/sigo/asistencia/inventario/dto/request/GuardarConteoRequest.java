package com.sigo.asistencia.inventario.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record GuardarConteoRequest(@NotEmpty List<@Valid GuardarConteoItemRequest> productos){}
