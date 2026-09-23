package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.AgenteProgramacionExcepcionUseCase;
import com.sigo.programacion.application.port.out.AgenteProgramacionExcepcionPort;
import com.sigo.programacion.infrastructure.persistence.entity.AgenteProgramacionExcepcion;
import com.sigo.programacion.infrastructure.persistence.repository.AgenteProgramacionExcepcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AgenteProgramacionExcepcionJpaAdapter
        implements AgenteProgramacionExcepcionPort {

    private final AgenteProgramacionExcepcionRepository repository;
    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;

    @Override
    public List<AgenteProgramacionExcepcionUseCase.Excepcion> listarPorPlaza(
            Long plazaId
    ) {
        return repository
                .findByPlazaIdAndActivoTrueOrderByTrabajadorNombreCompletoAsc(
                        plazaId
                )
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    public AgenteProgramacionExcepcionUseCase.Excepcion obtener(
            Long trabajadorId,
            Long plazaId
    ) {
        return repository
                .findByTrabajadorIdAndPlazaId(
                        trabajadorId,
                        plazaId
                )
                .map(this::toData)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "El agente no tiene una excepción configurada en esta plaza"
                        )
                );
    }

    @Override
    public AgenteProgramacionExcepcionUseCase.Excepcion guardar(
            AgenteProgramacionExcepcionUseCase.Command command
    ) {
        Trabajador trabajador = trabajadorRepository
                .findById(command.trabajadorId())
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Trabajador no encontrado o inactivo"
                        )
                );

        Plaza plaza = plazaRepository
                .findById(command.plazaId())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Plaza no encontrada o inactiva"
                        )
                );

        AgenteProgramacionExcepcion entidad = repository
                .findByTrabajadorIdAndPlazaId(
                        trabajador.getId(),
                        plaza.getId()
                )
                .orElseGet(AgenteProgramacionExcepcion::new);

        entidad.setTrabajador(trabajador);
        entidad.setPlaza(plaza);
        entidad.setPermiteA(command.permiteA());
        entidad.setPermiteB(command.permiteB());
        entidad.setPermiteC(command.permiteC());
        entidad.setMotivo(command.motivo());
        entidad.setColor(command.color());
        entidad.setActivo(
                command.activo() == null || command.activo()
        );

        return toData(repository.saveAndFlush(entidad));
    }

    @Override
    public void desactivar(
            Long trabajadorId,
            Long plazaId
    ) {
        AgenteProgramacionExcepcion entidad = repository
                .findByTrabajadorIdAndPlazaId(
                        trabajadorId,
                        plazaId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Excepción no encontrada"
                        )
                );

        entidad.setActivo(false);
        repository.save(entidad);
    }

    @Override
    public boolean existePlazaActiva(Long plazaId) {
        return plazaRepository
                .findById(plazaId)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .isPresent();
    }

    @Override
    public boolean trabajadorActivoPertenecePlaza(
            Long trabajadorId,
            Long plazaId
    ) {
        return trabajadorRepository
                .findById(trabajadorId)
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .map(Trabajador::getPlaza)
                .map(Plaza::getId)
                .filter(id -> Objects.equals(id, plazaId))
                .isPresent();
    }

    private AgenteProgramacionExcepcionUseCase.Excepcion toData(
            AgenteProgramacionExcepcion entidad
    ) {
        return new AgenteProgramacionExcepcionUseCase.Excepcion(
                entidad.getId(),
                entidad.getTrabajador().getId(),
                entidad.getTrabajador().getCodigo(),
                entidad.getTrabajador().getNombreCompleto(),
                entidad.getPlaza().getId(),
                entidad.getPlaza().getCodigo(),
                entidad.getPermiteA(),
                entidad.getPermiteB(),
                entidad.getPermiteC(),
                entidad.getMotivo(),
                entidad.getColor(),
                entidad.getActivo(),
                entidad.getCreatedAt(),
                entidad.getUpdatedAt()
        );
    }
}
