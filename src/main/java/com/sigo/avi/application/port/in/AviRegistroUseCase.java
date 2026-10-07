package com.sigo.avi.application.port.in;

import com.sigo.avi.domain.AviAccion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AviRegistroUseCase {

    Registro registrar(Command command);

    List<Registro> listar(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            Long plazaId,
            Integer via,
            AviAccion accion
    );

    record Command(
            UUID id,
            String placa,
            Integer via,
            AviAccion accion,
            OffsetDateTime fechaHoraEvento,
            String textoReconocido
    ) {
    }

    record Registro(
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
}
