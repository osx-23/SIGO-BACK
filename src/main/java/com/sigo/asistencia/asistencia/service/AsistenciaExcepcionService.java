package com.sigo.asistencia.asistencia.service;

import com.sigo.asistencia.asistencia.dto.AsistenciaRequest;
import com.sigo.asistencia.asistencia.dto.AsistenciaResponse;
import com.sigo.asistencia.asistencia.dto.AusenciaRequest;
import com.sigo.asistencia.asistencia.entity.AsistenciaAusencia;
import com.sigo.asistencia.asistencia.entity.AsistenciaEvidencia;
import com.sigo.asistencia.asistencia.entity.AsistenciaRegistro;
import com.sigo.asistencia.asistencia.entity.MotivoAusencia;
import com.sigo.asistencia.asistencia.repository.AsistenciaAusenciaRepository;
import com.sigo.asistencia.asistencia.repository.AsistenciaEvidenciaRepository;
import com.sigo.asistencia.asistencia.repository.AsistenciaRepository;
import com.sigo.asistencia.asistencia.repository.MotivoAusenciaRepository;
import com.sigo.asistencia.personal.entity.Plaza;
import com.sigo.asistencia.personal.entity.RolSistema;
import com.sigo.asistencia.personal.entity.Trabajador;
import com.sigo.asistencia.personal.entity.Turno;
import com.sigo.asistencia.personal.repository.PlazaRepository;
import com.sigo.asistencia.personal.repository.TrabajadorRepository;
import com.sigo.asistencia.personal.repository.TurnoRepository;
import com.sigo.asistencia.security.service.CurrentUserService;
import com.sigo.asistencia.shared.exception.BusinessException;
import com.sigo.asistencia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AsistenciaExcepcionService {

    private final AsistenciaRepository asistenciaRepository;
    private final AsistenciaAusenciaRepository ausenciaRepository;
    private final AsistenciaEvidenciaRepository evidenciaRepository;
    private final PlazaRepository plazaRepository;
    private final TurnoRepository turnoRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final MotivoAusenciaRepository motivoRepository;
    private final CurrentUserService currentUserService;
    private final AsistenciaService asistenciaService;

    @Transactional
    public AsistenciaResponse registrar(AsistenciaRequest request) {
        Trabajador actual = currentUserService.requireCurrent();
        if (actual.getRolSistema() != RolSistema.SUPERVISOR && actual.getRolSistema() != RolSistema.CONTROLADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La excepción de controlador solo está disponible para supervisores y controladores");
        }

        Plaza plaza = plazaRepository.findById(request.plazaId())
                .orElseThrow(() -> new ResourceNotFoundException("Plaza no encontrada"));
        Turno turno = turnoRepository.findById(request.turnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
        Trabajador controlador = trabajadorRepository.findById(request.controladorId())
                .orElseThrow(() -> new ResourceNotFoundException("Controlador no encontrado"));

        validarControlador(controlador);

        if (asistenciaRepository.existsByPlazaIdAndTurnoIdAndFecha(plaza.getId(), turno.getId(), request.fecha())) {
            throw new BusinessException("Ya existe una asistencia para esa plaza, turno y fecha");
        }

        int programados = request.programados();
        int presentes = request.presentes();
        int apoyo = request.apoyoSolicitado() == null ? 0 : request.apoyoSolicitado();
        if (programados < 0) throw new BusinessException("La cantidad de programados no puede ser negativa");
        if (presentes < 0 || presentes > programados) throw new BusinessException("Los presentes deben estar entre 0 y " + programados);
        if (apoyo < 0) throw new BusinessException("El apoyo solicitado no puede ser negativo");

        List<AusenciaRequest> ausencias = request.ausencias() == null ? List.of() : request.ausencias();
        int esperadas = programados - presentes;
        if (ausencias.size() != esperadas) throw new BusinessException("Debe registrar exactamente " + esperadas + " ausencia(s)");

        Set<Long> unicos = new HashSet<>();
        for (AusenciaRequest x : ausencias) {
            if (!unicos.add(x.trabajadorId())) throw new BusinessException("No puede registrar al mismo trabajador ausente dos veces");
            Trabajador trabajador = trabajadorRepository.findById(x.trabajadorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado: " + x.trabajadorId()));
            if (!Boolean.TRUE.equals(trabajador.getActivo())) throw new BusinessException("El trabajador está inactivo");
            if (trabajador.getPlaza() == null || !trabajador.getPlaza().getId().equals(plaza.getId())) {
                throw new BusinessException("El trabajador " + trabajador.getNombreCompleto() + " no pertenece a la plaza seleccionada");
            }
            motivoRepository.findById(x.motivoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Motivo no encontrado: " + x.motivoId()));
        }

        AsistenciaRegistro asistencia = new AsistenciaRegistro();
        asistencia.setPlaza(plaza);
        asistencia.setTurno(turno);
        asistencia.setControlador(controlador);
        asistencia.setFecha(request.fecha());
        asistencia.setProgramados(programados);
        asistencia.setPresentes(presentes);
        asistencia.setApoyoSolicitado(apoyo);
        asistencia.setDetalleApoyo(apoyo > 0 ? limpiar(request.detalleApoyo()) : null);
        asistencia.setNotas(limpiar(request.notas()));
        asistencia = asistenciaRepository.saveAndFlush(asistencia);

        for (AusenciaRequest x : ausencias) {
            Trabajador trabajador = trabajadorRepository.findById(x.trabajadorId()).orElseThrow();
            MotivoAusencia motivo = motivoRepository.findById(x.motivoId()).orElseThrow();
            AsistenciaAusencia ausencia = new AsistenciaAusencia();
            ausencia.setAsistencia(asistencia);
            ausencia.setTrabajador(trabajador);
            ausencia.setMotivo(motivo);
            ausencia.setObservacion(limpiar(x.observacion()));
            ausenciaRepository.save(ausencia);
        }

        if (request.evidencias() != null) {
            for (String url : request.evidencias()) {
                if (url == null || url.isBlank()) continue;
                AsistenciaEvidencia e = new AsistenciaEvidencia();
                e.setAsistencia(asistencia);
                e.setUrlArchivo(url.trim());
                e.setTipo("foto");
                evidenciaRepository.save(e);
            }
        }

        return asistenciaService.obtenerPorId(asistencia.getId());
    }

    private void validarControlador(Trabajador controlador) {
        if (!Boolean.TRUE.equals(controlador.getActivo())) {
            throw new BusinessException("El controlador seleccionado está inactivo");
        }
        String puesto = controlador.getPuesto() == null ? "" : controlador.getPuesto().getNombre();
        boolean esControlador = controlador.getRolSistema() == RolSistema.CONTROLADOR
                || "Controlador".equalsIgnoreCase(puesto)
                || "Controlador ATF".equalsIgnoreCase(puesto);
        if (!esControlador) throw new BusinessException("El trabajador seleccionado no es controlador");
    }

    private String limpiar(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }
}
