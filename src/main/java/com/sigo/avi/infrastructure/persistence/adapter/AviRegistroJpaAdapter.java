package com.sigo.avi.infrastructure.persistence.adapter;

import com.sigo.avi.application.port.out.AviRegistroPersistencePort;
import com.sigo.avi.domain.AviAccion;
import com.sigo.avi.infrastructure.persistence.entity.AviRegistroEntity;
import com.sigo.avi.infrastructure.persistence.repository.AviRegistroRepository;
import com.sigo.relevo.infrastructure.persistence.repository.ViaRepository;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AviRegistroJpaAdapter
        implements AviRegistroPersistencePort {

    private final AviRegistroRepository repository;
    private final ViaRepository viaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean viaActivaEnPlaza(Long plazaId, Integer via) {
        return viaRepository
                .findByPlazaIdAndActivaTrueOrderByOrdenAscNumeroAsc(
                        plazaId
                )
                .stream()
                .anyMatch(item ->
                        Objects.equals(item.getNumero(), via)
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegistroData> buscarPorId(UUID id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    @Transactional
    public RegistroData crearORecuperar(
            UUID id,
            Long usuarioId,
            Long plazaId,
            String placa,
            Integer via,
            AviAccion accion,
            OffsetDateTime fechaHoraEvento,
            String textoReconocido
    ) {
        repository.insertarSiAusente(
                id,
                usuarioId,
                plazaId,
                placa,
                via,
                accion.name(),
                fechaHoraEvento,
                textoReconocido
        );

        return repository
                .findById(id)
                .map(this::map)
                .orElseThrow(() ->
                        new BusinessException(
                                "No se pudo recuperar el registro AVI"
                        )
                );
    }

    @Override
    @Transactional
    public RegistroData actualizar(
            UUID id,
            String placa,
            Integer via,
            AviAccion accion,
            OffsetDateTime fechaHoraEvento,
            String textoReconocido
    ) {
        AviRegistroEntity entity = repository
                .findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "Registro AVI no encontrado"
                        )
                );

        entity.setPlaca(placa);
        entity.setVia(via);
        entity.setAccion(accion);
        entity.setFechaHoraEvento(fechaHoraEvento);
        entity.setTextoReconocido(textoReconocido);

        return map(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroData> listar(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            Long plazaId,
            Long usuarioId,
            Integer via,
            AviAccion accion
    ) {
        return repository.buscar(
                        desde,
                        hasta,
                        plazaId,
                        usuarioId,
                        via,
                        accion
                )
                .stream()
                .map(this::map)
                .toList();
    }

    private RegistroData map(AviRegistroEntity item) {
        return new RegistroData(
                item.getId(),
                item.getUsuario().getId(),
                item.getUsuario().getCodigo(),
                item.getUsuario().getNombreCompleto(),
                item.getPlaza().getId(),
                item.getPlaza().getCodigo(),
                item.getPlaca(),
                item.getVia(),
                item.getAccion(),
                item.getFechaHoraEvento(),
                item.getFechaHoraRecepcion(),
                item.getTextoReconocido()
        );
    }
}
