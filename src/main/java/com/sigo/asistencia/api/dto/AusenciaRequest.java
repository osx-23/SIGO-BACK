package com.sigo.asistencia.api.dto; import jakarta.validation.constraints.NotNull; public record AusenciaRequest(@NotNull Long trabajadorId,@NotNull Long motivoId,String observacion){}
