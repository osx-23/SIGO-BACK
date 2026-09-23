package com.sigo.asistencia.relevo.dto;
import com.sigo.asistencia.relevo.entity.EstadoOperativo;
import jakarta.validation.constraints.NotNull;
public record RelevoViaRequest(@NotNull Long viaId,@NotNull EstadoOperativo estado,String detalle) {}
