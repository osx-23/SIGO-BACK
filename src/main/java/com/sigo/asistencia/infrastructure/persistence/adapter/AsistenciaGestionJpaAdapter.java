package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaGestionPort;
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

import java.util.List;

@Component
@RequiredArgsConstructor
public class AsistenciaGestionJpaAdapter
        implements AsistenciaGestionPort {

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
        Plaza plaza = requirePlaza(command.plazaId());
        Turno turno = requireTurno(command.turnoId());
        Trabajador controlador =
                requireControlador(
                        command.controladorId(),
                        plaza.getId()
                );

        if (asistenciaRepository
                .existsByPlazaIdAndTurnoIdAndFecha(
                        command.plazaId(),
                        command.turnoId(),
                        command.fecha()
                )) {
            throw new BusinessException(
                    "Ya existe una asistencia para esa plaza, turno y fecha"
            );
        }

        validarAusencias(
                command.ausencias(),
                plaza.getId()
        );

        AsistenciaRegistro asistencia =
                new AsistenciaRegistro();

        aplicar(asistencia, command, plaza, turno, controlador);

        asistencia =
                asistenciaRepository.saveAndFlush(asistencia);

        guardarAusencias(
                asistencia,
                command.ausencias()
        );

        guardarEvidenciasIniciales(
                asistencia,
                command.evidencias()
        );

        return asistencia.getId();
    }

    @Override
    public Long actualizar(
            Long id,
            GestionarAsistenciaUseCase.Command command
    ) {
        AsistenciaRegistro asistencia = asistenciaRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asistencia no encontrada"
                        )
                );

        Plaza plaza = requirePlaza(command.plazaId());
        Turno turno = requireTurno(command.turnoId());
        Trabajador controlador =
                requireControlador(
                        command.controladorId(),
                        plaza.getId()
                );

        if (asistenciaRepository
                .existsByPlazaIdAndTurnoIdAndFechaAndIdNot(
                        command.plazaId(),
                        command.turnoId(),
                        command.fecha(),
                        id
                )) {
            throw new BusinessException(
                    "Ya existe otra asistencia para esa plaza, turno y fecha"
            );
        }

        validarAusencias(
                command.ausencias(),
                plaza.getId()
        );

        aplicar(
                asistencia,
                command,
                plaza,
                turno,
                controlador
        );

        asistenciaRepository.saveAndFlush(asistencia);

        ausenciaRepository.deleteByAsistenciaId(id);
        ausenciaRepository.flush();

        guardarAusencias(
                asistencia,
                command.ausencias()
        );

        ausenciaRepository.flush();

        return asistencia.getId();
    }

    private void aplicar(
            AsistenciaRegistro asistencia,
            GestionarAsistenciaUseCase.Command command,
            Plaza plaza,
            Turno turno,
            Trabajador controlador
    ) {
        asistencia.setPlaza(plaza);
        asistencia.setTurno(turno);
        asistencia.setControlador(controlador);
        asistencia.setFecha(command.fecha());
        asistencia.setProgramados(command.programados());
        asistencia.setPresentes(command.presentes());
        asistencia.setApoyoSolicitado(command.apoyoSolicitado());
        asistencia.setDetalleApoyo(command.detalleApoyo());
        asistencia.setNotas(command.notas());
    }

    private Plaza requirePlaza(Long plazaId) {
        return plazaRepository
                .findById(plazaId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plaza no encontrada"
                        )
                );
    }

    private Turno requireTurno(Long turnoId) {
        return turnoRepository
                .findById(turnoId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Turno no encontrado"
                        )
                );
    }

    private Trabajador requireControlador(
            Long controladorId,
            Long plazaId
    ) {
        Trabajador controlador = trabajadorRepository
                .findById(controladorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Controlador no encontrado"
                        )
                );

        validarControlador(controlador);

        if (controlador.getPlaza() == null
                || !plazaId.equals(
                        controlador.getPlaza().getId()
                )) {
            throw new BusinessException(
                    "El controlador seleccionado no pertenece a la plaza"
            );
        }

        return controlador;
    }

    private void validarControlador(
            Trabajador trabajador
    ) {
        if (!Boolean.TRUE.equals(trabajador.getActivo())) {
            throw new BusinessException(
                    "El controlador está inactivo"
            );
        }

        String puesto =
                trabajador.getPuesto() == null
                        ? ""
                        : trabajador.getPuesto().getNombre();

        boolean puestoValido =
                "Controlador".equalsIgnoreCase(puesto)
                        || "Controlador ATF".equalsIgnoreCase(puesto)
                        || "Supervisor".equalsIgnoreCase(puesto);

        boolean rolValido =
                trabajador.getRolSistema() == RolSistema.CONTROLADOR
                        || trabajador.getRolSistema() == RolSistema.SUPERVISOR;

        if (!puestoValido && !rolValido) {
            throw new BusinessException(
                    "El trabajador seleccionado no es controlador"
            );
        }
    }

    private void validarAusencias(
            List<GestionarAsistenciaUseCase.AusenciaCommand> ausencias,
            Long plazaId
    ) {
        for (GestionarAsistenciaUseCase.AusenciaCommand ausencia : ausencias) {
            Trabajador trabajador = trabajadorRepository
                    .findById(ausencia.trabajadorId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Trabajador no encontrado: "
                                            + ausencia.trabajadorId()
                            )
                    );

            if (!Boolean.TRUE.equals(trabajador.getActivo())) {
                throw new BusinessException(
                        "El trabajador "
                                + trabajador.getCodigo()
                                + " está inactivo"
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
                    .findById(ausencia.motivoId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Motivo no encontrado: "
                                            + ausencia.motivoId()
                            )
                    );
        }
    }

    private void guardarAusencias(
            AsistenciaRegistro asistencia,
            List<GestionarAsistenciaUseCase.AusenciaCommand> ausencias
    ) {
        for (GestionarAsistenciaUseCase.AusenciaCommand item : ausencias) {
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
    }

    private void guardarEvidenciasIniciales(
            AsistenciaRegistro asistencia,
            List<String> evidencias
    ) {
        for (String url : evidencias) {
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
    }
}
