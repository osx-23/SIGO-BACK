package com.sigo.asistencia.programacion.service;

import com.sigo.asistencia.personal.entity.Plaza;
import com.sigo.asistencia.personal.entity.Trabajador;
import com.sigo.asistencia.personal.repository.PlazaRepository;
import com.sigo.asistencia.personal.repository.TrabajadorRepository;
import com.sigo.asistencia.programacion.dto.AgenteProgramacionExcepcionRequest;
import com.sigo.asistencia.programacion.dto.AgenteProgramacionExcepcionResponse;
import com.sigo.asistencia.programacion.entity.AgenteProgramacionExcepcion;
import com.sigo.asistencia.programacion.repository.AgenteProgramacionExcepcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgenteProgramacionExcepcionService {

    private final AgenteProgramacionExcepcionRepository repository;
    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;

    @Transactional(readOnly = true)
    public List<AgenteProgramacionExcepcionResponse> listarPorPlaza(Long plazaId) {
        validarPlaza(plazaId);
        return repository.findByPlazaIdAndActivoTrueOrderByTrabajadorNombreCompletoAsc(plazaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgenteProgramacionExcepcionResponse obtener(Long trabajadorId, Long plazaId) {
        return repository.findByTrabajadorIdAndPlazaId(trabajadorId, plazaId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El agente no tiene una excepción configurada en esta plaza"));
    }

    @Transactional
    public AgenteProgramacionExcepcionResponse guardar(AgenteProgramacionExcepcionRequest request) {
        if (!Boolean.TRUE.equals(request.permiteA())
                && !Boolean.TRUE.equals(request.permiteB())
                && !Boolean.TRUE.equals(request.permiteC())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe existir al menos un turno recomendado: A, B o C");
        }

        Trabajador trabajador = trabajadorRepository.findById(request.trabajadorId())
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trabajador no encontrado o inactivo"));

        Plaza plaza = validarPlaza(request.plazaId());

        if (trabajador.getPlaza() == null || !trabajador.getPlaza().getId().equals(plaza.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El trabajador no pertenece a la plaza indicada");
        }

        AgenteProgramacionExcepcion entidad = repository
                .findByTrabajadorIdAndPlazaId(trabajador.getId(), plaza.getId())
                .orElseGet(AgenteProgramacionExcepcion::new);

        entidad.setTrabajador(trabajador);
        entidad.setPlaza(plaza);
        entidad.setPermiteA(request.permiteA());
        entidad.setPermiteB(request.permiteB());
        entidad.setPermiteC(request.permiteC());
        entidad.setMotivo(limpiar(request.motivo()));
        entidad.setColor(request.color().toUpperCase());
        entidad.setActivo(request.activo() == null || request.activo());

        return toResponse(repository.saveAndFlush(entidad));
    }

    @Transactional
    public void desactivar(Long trabajadorId, Long plazaId) {
        AgenteProgramacionExcepcion entidad = repository
                .findByTrabajadorIdAndPlazaId(trabajadorId, plazaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Excepción no encontrada"));
        entidad.setActivo(false);
        repository.save(entidad);
    }

    private Plaza validarPlaza(Long plazaId) {
        return plazaRepository.findById(plazaId)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plaza no encontrada o inactiva"));
    }

    private String limpiar(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private AgenteProgramacionExcepcionResponse toResponse(AgenteProgramacionExcepcion e) {
        return new AgenteProgramacionExcepcionResponse(
                e.getId(),
                e.getTrabajador().getId(),
                e.getTrabajador().getCodigo(),
                e.getTrabajador().getNombreCompleto(),
                e.getPlaza().getId(),
                e.getPlaza().getCodigo(),
                e.getPermiteA(),
                e.getPermiteB(),
                e.getPermiteC(),
                e.getMotivo(),
                e.getColor(),
                e.getActivo(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
