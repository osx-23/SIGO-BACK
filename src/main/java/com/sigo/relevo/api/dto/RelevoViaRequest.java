package com.sigo.relevo.api.dto;
import com.sigo.relevo.infrastructure.persistence.entity.EstadoOperativo;
import jakarta.validation.constraints.NotNull;
public record RelevoViaRequest(@NotNull Long viaId,@NotNull EstadoOperativo estado,String detalle) {}
