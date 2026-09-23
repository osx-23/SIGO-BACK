package com.sigo.relevo.api.dto;
import com.sigo.relevo.domain.EstadoRelevo;
import jakarta.validation.constraints.*;
public record RelevoChecklistRequest(@NotNull Long elementoId,@NotNull EstadoRelevo estado,String detalle,@Min(0) Integer cantidad) {}
