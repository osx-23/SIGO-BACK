package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaExcepcionPort;
import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaAusencia;
import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaEvidencia;
import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaRegistro;
import com.sigo.asistencia.infrastructure.persistence.entity.MotivoAusencia;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaAusenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaEvidenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.MotivoAusenciaRepository;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsistenciaExcepcionJpaAdapter
        implements AsistenciaExcepcionPort {

    private final AsistenciaRepository asistenciaRepository;
    private final AsistenciaAusenciaRepository ausenciaRepository;
    private final AsistenciaEvidenciaRepository evidenciaRepository;
    private final PlazaRepository plazaRepository;
    private final TurnoRepository turnoRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final MotivoAusenciaRepository motivoRepository;

    @Override
    public Long registrar(
            GestionarAsistenciaUseCase.Command command
    ) {
        Plaza plaza = plazaRepository
                .findById(command.plazaId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plaza no encontrada"
                        )
                );

        Turno turno = turnoRepository
                .findById(command.turnoId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Turno no encontrado"
                        )
                );

        Trabajador controlador = trabajadorRepository
                .findById(command.controladorId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Controlador no encontrado"
                        )
                );

        validarControlador(controlador);

        if (asistenciaRepository
                .existsByPlazaIdAndTurnoIdAndFecha(
                        plaza.getId(),
                        turno.getId(),
                        command.fecha()
                )) {
            throw new BusinessException(
                    "Ya existe una asistencia para esa plaza, turno y fecha"
            );
        }

        validarAusencias(
                command,
                plaza.getId()
        );

        AsistenciaRegistro asistencia =
                new AsistenciaRegistro();

        asistencia.setPlaza(plaza);
        asistencia.setTurno(turno);
        asistencia.setControlador(controlador);
        asistencia.setFecha(command.fecha());
        asistencia.setProgramados(command.programados());
        asistencia.setPresentes(command.presentes());
        asistencia.setApoyoSolicitado(command.apoyoSolicitado());
        asistencia.setDetalleApoyo(command.detalleApoyo());
        asistencia.setNotas(command.notas());

        asistencia =
                asistenciaRepository.saveAndFlush(asistencia);

        for (GestionarAsistenciaUseCase.AusenciaCommand item
                : command.ausencias()) {
            Trabajador trabajador = trabajadorRepository
                    .findById(item.trabajadorId())
                    .orElseThrow();

            MotivoAusencia motivo = motivoRepository
                    .findById(item.motivoId())
                    .orElseThrow();

            AsistenciaAusencia ausencia =
                    new AsistenciaAusencia();

            ausencia.setAsistencia(asistencia);
            ausencia.setTrabajador(trabajador);
            ausencia.setMotivo(motivo);
            ausencia.setObservacion(item.observacion());

            ausenciaRepository.save(ausencia);
        }

        for (String url : command.evidencias()) {
            if (url == null || url.isBlank()) {
                continue;
            }

            AsistenciaEvidencia evidencia =
                    new AsistenciaEvidencia();

            evidencia.setAsistencia(asistencia);
            evidencia.setUrlArchivo(url.trim());
            evidencia.setTipo("foto");

            evidenciaRepository.save(evidencia);
        }

        return asistencia.getId();
    }

    private void validarControlador(
            Trabajador controlador
    ) {
        if (!Boolean.TRUE.equals(controlador.getActivo())) {
            throw new BusinessException(
                    "El controlador seleccionado está inactivo"
            );
        }

        String puesto = controlador.getPuesto() == null
                ? ""
                : controlador.getPuesto().getNombre();

        boolean esControlador =
                controlador.getRolSistema()
                        == RolSistema.CONTROLADOR
                        || "Controlador".equalsIgnoreCase(puesto)
                        || "Controlador ATF".equalsIgnoreCase(puesto);

        if (!esControlador) {
            throw new BusinessException(
                    "El trabajador seleccionado no es controlador"
            );
        }
    }

    private void validarAusencias(
            GestionarAsistenciaUseCase.Command command,
            Long plazaId
    ) {
        for (GestionarAsistenciaUseCase.AusenciaCommand item
                : command.ausencias()) {
            Trabajador trabajador = trabajadorRepository
                    .findById(item.trabajadorId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Trabajador no encontrado: "
                                            + item.trabajadorId()
                            )
                    );

            if (!Boolean.TRUE.equals(trabajador.getActivo())) {
                throw new BusinessException(
                        "El trabajador está inactivo"
                );
            }

            if (trabajador.getPlaza() == null
                    || !plazaId.equals(
                            trabajador.getPlaza().getId()
                    )) {
                throw new BusinessException(
                        "El trabajador "
                                + trabajador.getNombreCompleto()
                                + " no pertenece a la plaza seleccionada"
                );
            }

            motivoRepository
                    .findById(item.motivoId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Motivo no encontrado: "
                                            + item.motivoId()
                            )
                    );
        }
    }
}
