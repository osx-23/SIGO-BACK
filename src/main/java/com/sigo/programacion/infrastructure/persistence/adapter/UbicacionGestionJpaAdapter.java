package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.programacion.application.port.in.UbicacionUseCase;
import com.sigo.programacion.application.port.out.UbicacionGestionPort;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionUbicacion;
import com.sigo.programacion.infrastructure.persistence.entity.TipoUbicacion;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionUbicacionRepository;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UbicacionGestionJpaAdapter
        implements UbicacionGestionPort {

    private final ProgramacionUbicacionRepository ubicacionRepository;
    private final PlazaRepository plazaRepository;

    @Override
    public List<UbicacionUseCase.Ubicacion> listarActivas(Long plazaId) {
        return ubicacionRepository
                .findByPlazaIdAndActivoTrueOrderByOrdenAscCodigoAsc(plazaId)
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    public List<UbicacionUseCase.Ubicacion> listarTodas(Long plazaId) {
        plazaRepository
                .findById(plazaId)
                .orElseThrow(() -> notFound("Plaza no encontrada"));

        return ubicacionRepository
                .findByPlazaIdOrderByOrdenAscCodigoAsc(plazaId)
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    public UbicacionUseCase.Ubicacion crear(
            Long plazaId,
            String codigo,
            String nombre,
            String tipo,
            Integer orden
    ) {
        Plaza plaza = plazaRepository
                .findById(plazaId)
                .orElseThrow(() -> notFound("Plaza no encontrada"));

        if (ubicacionRepository
                .existsByPlazaIdAndCodigoIgnoreCase(
                        plazaId,
                        codigo
                )) {
            throw bad(
                    "Ya existe una ubicación con el código "
                            + codigo
                            + " en esta plaza"
            );
        }

        ProgramacionUbicacion ubicacion =
                new ProgramacionUbicacion();

        ubicacion.setPlaza(plaza);
        ubicacion.setCodigo(codigo);
        ubicacion.setNombre(nombre);
        ubicacion.setTipo(parseTipo(tipo));
        ubicacion.setViaId(null);
        ubicacion.setActivo(true);
        ubicacion.setOrden(
                orden != null && orden > 0
                        ? orden
                        : siguienteOrden(plazaId)
        );

        return toData(
                ubicacionRepository.saveAndFlush(ubicacion)
        );
    }

    @Override
    public UbicacionUseCase.Ubicacion actualizar(
            Long ubicacionId,
            Long plazaId,
            String codigo,
            String nombre,
            String tipo,
            Integer orden
    ) {
        ProgramacionUbicacion ubicacion = ubicacionRepository
                .findById(ubicacionId)
                .orElseThrow(() ->
                        notFound("Ubicación no encontrada")
                );

        if (!Objects.equals(
                ubicacion.getPlaza().getId(),
                plazaId
        )) {
            throw bad(
                    "La ubicación no pertenece a la plaza seleccionada"
            );
        }

        if (ubicacionRepository
                .existsByPlazaIdAndCodigoIgnoreCaseAndIdNot(
                        plazaId,
                        codigo,
                        ubicacionId
                )) {
            throw bad(
                    "Ya existe otra ubicación con el código "
                            + codigo
                            + " en esta plaza"
            );
        }

        TipoUbicacion tipoUbicacion = parseTipo(tipo);

        ubicacion.setCodigo(codigo);
        ubicacion.setNombre(nombre);
        ubicacion.setTipo(tipoUbicacion);

        if (tipoUbicacion != TipoUbicacion.VIA) {
            ubicacion.setVia(null);
        }

        if (orden != null && orden > 0) {
            ubicacion.setOrden(orden);
        }

        return toData(
                ubicacionRepository.saveAndFlush(ubicacion)
        );
    }

    @Override
    public UbicacionUseCase.Ubicacion cambiarEstado(
            Long ubicacionId,
            boolean activo
    ) {
        ProgramacionUbicacion ubicacion = ubicacionRepository
                .findById(ubicacionId)
                .orElseThrow(() ->
                        notFound("Ubicación no encontrada")
                );

        ubicacion.setActivo(activo);

        return toData(
                ubicacionRepository.saveAndFlush(ubicacion)
        );
    }

    private int siguienteOrden(Long plazaId) {
        return ubicacionRepository
                .findByPlazaIdOrderByOrdenAscCodigoAsc(plazaId)
                .stream()
                .map(ProgramacionUbicacion::getOrden)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0)
                + 1;
    }

    private TipoUbicacion parseTipo(String tipo) {
        try {
            return TipoUbicacion.valueOf(tipo);
        } catch (Exception exception) {
            throw bad("Tipo de ubicación no válido");
        }
    }

    private UbicacionUseCase.Ubicacion toData(
            ProgramacionUbicacion ubicacion
    ) {
        return new UbicacionUseCase.Ubicacion(
                ubicacion.getId(),
                ubicacion.getPlaza().getId(),
                ubicacion.getCodigo(),
                ubicacion.getNombre(),
                ubicacion.getTipo().name(),
                ubicacion.getViaId(),
                ubicacion.getActivo(),
                ubicacion.getOrden()
        );
    }

    private BusinessException bad(String message) {
        return new BusinessException(message);
    }

    private ResourceNotFoundException notFound(String message) {
        return new ResourceNotFoundException(message);
    }
}
