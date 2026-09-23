package com.sigo.relevo.api.dto;
import com.sigo.relevo.infrastructure.persistence.entity.EstadoOperativo;
import jakarta.validation.constraints.*;
public record RelevoChecklistRequest(@NotNull Long elementoId,@NotNull EstadoOperativo estado,String detalle,@Min(0) Integer cantidad) {}
