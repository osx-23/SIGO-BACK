package com.sigo.avi.api.dto;

import com.sigo.avi.domain.AviAccion;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AviRegistroResponse(
        UUID id,
        Long usuarioId,
        Integer usuarioCodigo,
        String usuarioNombre,
        Long plazaId,
        String plazaCodigo,
        String placa,
        Integer via,
        AviAccion accion,
        OffsetDateTime fechaHoraEvento,
        OffsetDateTime fechaHoraRecepcion,
        String textoReconocido
) {
}
