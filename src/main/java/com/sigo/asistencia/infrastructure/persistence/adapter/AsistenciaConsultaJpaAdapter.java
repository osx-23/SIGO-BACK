package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.ConsultarAsistenciasUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaConsultaPort;
import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaRegistro;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaAusenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaEvidenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaRepository;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AsistenciaConsultaJpaAdapter
        implements AsistenciaConsultaPort {

    private final AsistenciaRepository asistenciaRepository;
    private final AsistenciaAusenciaRepository ausenciaRepository;
    private final AsistenciaEvidenciaRepository evidenciaRepository;
    private final PlazaRepository plazaRepository;

    @Override
    public ConsultarAsistenciasUseCase.Asistencia obtenerPorId(
            Long id
    ) {
        AsistenciaRegistro asistencia = asistenciaRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asistencia no encontrada"
                        )
                );

        List<ConsultarAsistenciasUseCase.Ausencia> ausencias =
                ausenciaRepository
                        .findByAsistenciaId(id)
                        .stream()
                        .map(ausencia ->
                                new ConsultarAsistenciasUseCase.Ausencia(
                                        ausencia.getId(),
                                        ausencia.getTrabajador().getId(),
                                        ausencia.getTrabajador().getCodigo(),
                                        ausencia.getTrabajador().getNombreCompleto(),
                                        ausencia.getMotivo().getId(),
                                        ausencia.getMotivo().getNombre(),
                                        ausencia.getObservacion()
                                )
                        )
                        .toList();

        List<ConsultarAsistenciasUseCase.Evidencia> evidencias =
                evidenciaRepository
                        .findByAsistenciaId(id)
                        .stream()
                        .map(evidencia ->
                                new ConsultarAsistenciasUseCase.Evidencia(
                                        evidencia.getId(),
                                        evidencia.getUrlArchivo(),
                                        evidencia.getTipo()
                                )
                        )
                        .toList();

        return toData(
                asistencia,
                ausencias,
                evidencias
        );
    }

    @Override
    public List<ConsultarAsistenciasUseCase.Asistencia> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId
    ) {
        List<AsistenciaRegistro> registros =
                plazaId == null
                        ? asistenciaRepository.listarHistorial(
                                inicio,
                                fin
                        )
                        : asistenciaRepository.listarHistorialPorPlaza(
                                inicio,
                                fin,
                                plazaId
                        );

        if (registros.isEmpty()) {
            return List.of();
        }

        List<Long> ids = registros
                .stream()
                .map(AsistenciaRegistro::getId)
                .toList();

        Map<Long, List<ConsultarAsistenciasUseCase.Ausencia>>
                ausenciasPorAsistencia =
                ausenciaRepository
                        .findAllForHistorial(ids)
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        ausencia ->
                                                ausencia
                                                        .getAsistencia()
                                                        .getId(),
                                        Collectors.mapping(
                                                ausencia ->
                                                        new ConsultarAsistenciasUseCase.Ausencia(
                                                                ausencia.getId(),
                                                                ausencia.getTrabajador().getId(),
                                                                ausencia.getTrabajador().getCodigo(),
                                                                ausencia.getTrabajador().getNombreCompleto(),
                                                                ausencia.getMotivo().getId(),
                                                                ausencia.getMotivo().getNombre(),
                                                                ausencia.getObservacion()
                                                        ),
                                                Collectors.toList()
                                        )
                                )
                        );

        Map<Long, List<ConsultarAsistenciasUseCase.Evidencia>>
                evidenciasPorAsistencia =
                evidenciaRepository
                        .findByAsistenciaIdIn(ids)
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        evidencia ->
                                                evidencia
                                                        .getAsistencia()
                                                        .getId(),
                                        Collectors.mapping(
                                                evidencia ->
                                                        new ConsultarAsistenciasUseCase.Evidencia(
                                                                evidencia.getId(),
                                                                evidencia.getUrlArchivo(),
                                                                evidencia.getTipo()
                                                        ),
                                                Collectors.toList()
                                        )
                                )
                        );

        return registros
                .stream()
                .map(registro ->
                        toData(
                                registro,
                                ausenciasPorAsistencia.getOrDefault(
                                        registro.getId(),
                                        List.of()
                                ),
                                evidenciasPorAsistencia.getOrDefault(
                                        registro.getId(),
                                        List.of()
                                )
                        )
                )
                .toList();
    }

    @Override
    public boolean existePlaza(Long plazaId) {
        return plazaRepository.existsById(plazaId);
    }

    private ConsultarAsistenciasUseCase.Asistencia toData(
            AsistenciaRegistro asistencia,
            List<ConsultarAsistenciasUseCase.Ausencia> ausencias,
            List<ConsultarAsistenciasUseCase.Evidencia> evidencias
    ) {
        return new ConsultarAsistenciasUseCase.Asistencia(
                asistencia.getId(),
                asistencia.getPlaza().getId(),
                asistencia.getPlaza().getCodigo(),
                asistencia.getTurno().getId(),
                asistencia.getTurno().getCodigo(),
                asistencia.getControlador().getId(),
                asistencia.getControlador().getNombreCompleto(),
                asistencia.getFecha(),
                asistencia.getProgramados(),
                asistencia.getPresentes(),
                asistencia.getProgramados() - asistencia.getPresentes(),
                asistencia.getApoyoSolicitado(),
                asistencia.getDetalleApoyo(),
                asistencia.getPorcentaje(),
                asistencia.getNotas(),
                ausencias,
                evidencias
        );
    }
}
