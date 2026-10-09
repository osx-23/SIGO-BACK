package com.sigo.avi.application.port.out;

import com.sigo.avi.domain.AviAccion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AviRegistroPersistencePort {

    boolean viaActivaEnPlaza(Long plazaId, Integer via);

    Optional<RegistroData> buscarPorId(UUID id);

    RegistroData crearORecuperar(
            UUID id,
            Long usuarioId,
            Long plazaId,
            String placa,
            Integer via,
            AviAccion accion,
            OffsetDateTime fechaHoraEvento,
            String textoReconocido
    );

    RegistroData actualizar(
            UUID id,
            String placa,
            Integer via,
            AviAccion accion,
            OffsetDateTime fechaHoraEvento,
            String textoReconocido
    );

    List<RegistroData> listar(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            Long plazaId,
            Long usuarioId,
            Integer via,
            AviAccion accion
    );

    record RegistroData(
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
