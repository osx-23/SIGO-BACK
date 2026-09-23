package com.sigo.relevo.dto;
import com.sigo.relevo.entity.EstadoOperativo;
import jakarta.validation.constraints.NotNull;
public record RelevoViaRequest(@NotNull Long viaId,@NotNull EstadoOperativo estado,String detalle) {}
