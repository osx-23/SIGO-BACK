package com.sigo.relevo.api.dto;
import com.sigo.relevo.domain.EstadoRelevo;
import jakarta.validation.constraints.NotNull;
public record RelevoViaRequest(@NotNull Long viaId,@NotNull EstadoRelevo estado,String detalle) {}
