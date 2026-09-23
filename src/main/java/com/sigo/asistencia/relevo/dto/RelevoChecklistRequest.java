package com.sigo.asistencia.relevo.dto;
import com.sigo.asistencia.relevo.entity.EstadoOperativo;
import jakarta.validation.constraints.*;
public record RelevoChecklistRequest(@NotNull Long elementoId,@NotNull EstadoOperativo estado,String detalle,@Min(0) Integer cantidad) {}
