package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.out.ProgramacionGeneradorDataPort;
import com.sigo.programacion.domain.ProgramacionEstado;
import com.sigo.programacion.domain.ProgramacionGrupo;
import com.sigo.programacion.domain.generador.GeneradorExcepcion;
import com.sigo.programacion.domain.generador.GeneradorPlaza;
import com.sigo.programacion.domain.generador.GeneradorSecuencia;
import com.sigo.programacion.domain.generador.GeneradorTrabajador;
import com.sigo.programacion.domain.generador.GeneradorTurno;
import com.sigo.programacion.infrastructure.persistence.entity.AgenteProgramacionExcepcion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionSecuenciaAgente;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import com.sigo.programacion.infrastructure.persistence.repository.AgenteProgramacionExcepcionRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionSecuenciaAgenteRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProgramacionGeneradorJpaAdapter
        implements ProgramacionGeneradorDataPort {

    private final ProgramacionTurnoRepository programacionRepository;
    private final ProgramacionSecuenciaAgenteRepository secuenciaRepository;
    private final AgenteProgramacionExcepcionRepository excepcionRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;

    @Override
    public GeneradorPlaza requirePlazaActiva(Long plazaId) {
        return plazaRepository
                .findById(plazaId)
                .filter(plaza -> Boolean.TRUE.equals(plaza.getActivo()))
                .map(this::toPlaza)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Plaza no válida o inactiva"
                        )
                );
    }

    @Override
    public List<GeneradorTrabajador> agentesPorPlaza(Long plazaId) {
        return trabajadorRepository
                .findAgentesByPlaza(plazaId)
                .stream()
                .map(this::toTrabajador)
                .toList();
    }

    @Override
    public List<GeneradorSecuencia> secuenciasPorPlaza(Long plazaId) {
        return secuenciaRepository
                .findActivasByPlazaId(plazaId)
                .stream()
                .map(this::toSecuencia)
                .toList();
    }

    @Override
    public List<GeneradorExcepcion> excepcionesPorPlaza(Long plazaId) {
        return excepcionRepository
                .findByPlazaIdAndActivoTrueOrderByTrabajadorNombreCompletoAsc(
                        plazaId
                )
                .stream()
                .map(this::toExcepcion)
                .toList();
    }

    @Override
    public List<GeneradorTurno> historial(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    ) {
        return programacionRepository
                .findMes(plazaId, desde, hasta)
                .stream()
                .map(this::toTurno)
                .toList();
    }

    private GeneradorPlaza toPlaza(Plaza plaza) {
        if (plaza == null) {
            return null;
        }

        return new GeneradorPlaza(
                plaza.getId(),
                plaza.getCodigo()
        );
    }

    private GeneradorTrabajador toTrabajador(
            Trabajador trabajador
    ) {
        return new GeneradorTrabajador(
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                toPlaza(trabajador.getPlaza())
        );
    }

    private GeneradorSecuencia toSecuencia(
            ProgramacionSecuenciaAgente secuencia
    ) {
        return new GeneradorSecuencia(
                toTrabajador(secuencia.getAgente()),
                toPlaza(secuencia.getPlaza()),
                secuencia.getGrupo() == null
                        ? null
                        : ProgramacionGrupo.valueOf(
                                secuencia.getGrupo().name()
                        ),
                secuencia.getOrden()
        );
    }

    private GeneradorExcepcion toExcepcion(
            AgenteProgramacionExcepcion excepcion
    ) {
        return new GeneradorExcepcion(
                toTrabajador(excepcion.getTrabajador()),
                toPlaza(excepcion.getPlaza()),
                excepcion.getPermiteA(),
                excepcion.getPermiteB(),
                excepcion.getPermiteC(),
                excepcion.getMotivo(),
                excepcion.getActivo()
        );
    }

    private GeneradorTurno toTurno(
            ProgramacionTurno turno
    ) {
        return new GeneradorTurno(
                toTrabajador(turno.getTrabajador()),
                toPlaza(turno.getPlaza()),
                turno.getFecha(),
                ProgramacionEstado.valueOf(
                        turno.getEstado().name()
                )
        );
    }
}
