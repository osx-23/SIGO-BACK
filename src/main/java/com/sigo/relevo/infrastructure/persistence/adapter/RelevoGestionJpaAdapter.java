package com.sigo.relevo.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.out.RelevoGestionPort;
import com.sigo.relevo.infrastructure.persistence.entity.ElementoRelevo;
import com.sigo.relevo.infrastructure.persistence.entity.EstadoOperativo;
import com.sigo.relevo.infrastructure.persistence.entity.Relevo;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoChecklist;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoVia;
import com.sigo.relevo.infrastructure.persistence.entity.Via;
import com.sigo.relevo.infrastructure.persistence.repository.ElementoRelevoRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoChecklistRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoViaRepository;
import com.sigo.relevo.infrastructure.persistence.repository.ViaRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RelevoGestionJpaAdapter
        implements RelevoGestionPort {

    private final RelevoRepository relevoRepository;
    private final RelevoChecklistRepository checklistRepository;
    private final RelevoViaRepository relevoViaRepository;
    private final ElementoRelevoRepository elementoRepository;
    private final ViaRepository viaRepository;
    private final PlazaRepository plazaRepository;
    private final TurnoRepository turnoRepository;
    private final TrabajadorRepository trabajadorRepository;

    @Override
    public java.util.List<ElementoConfig> elementosActivos() {
        return elementoRepository
                .findByActivoTrueOrderByCategoriaAscOrdenAscNombreAsc()
                .stream()
                .map(elemento ->
                        new ElementoConfig(
                                elemento.getId(),
                                elemento.getNombre(),
                                elemento.getRequiereCantidad()
                        )
                )
                .toList();
    }

    @Override
    public ViaConfig requireVia(Long viaId) {
        Via via = viaRepository
                .findById(viaId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vía no encontrada: " + viaId
                        )
                );

        return new ViaConfig(
                via.getId(),
                via.getPlaza().getId(),
                via.getNumero(),
                via.getActiva()
        );
    }

    @Override
    public Long registrar(
            GestionarRelevoUseCase.Command command
    ) {
        Plaza plaza = requirePlaza(command.plazaId());
        Turno turno = requireTurno(command.turnoId());
        Trabajador operador = requireOperador(command.operadorId());

        Map<Long, ElementoRelevo> elementos =
                elementoRepository
                        .findByActivoTrueOrderByCategoriaAscOrdenAscNombreAsc()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        ElementoRelevo::getId,
                                        Function.identity()
                                )
                        );

        Relevo relevo = new Relevo();
        aplicar(relevo, command, plaza, turno, operador);
        relevo = relevoRepository.save(relevo);

        for (GestionarRelevoUseCase.ChecklistItem item
                : command.checklist()) {
            RelevoChecklist checklist =
                    new RelevoChecklist();

            checklist.setRelevo(relevo);
            checklist.setElemento(elementos.get(item.elementoId()));
            checklist.setEstado(
                    EstadoOperativo.valueOf(
                            item.estado().name()
                    )
            );
            checklist.setDetalle(item.detalle());
            checklist.setCantidad(item.cantidad());

            checklistRepository.save(checklist);
        }

        for (GestionarRelevoUseCase.ViaItem item
                : command.vias()) {
            Via via = viaRepository
                    .findById(item.viaId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Vía no encontrada"
                            )
                    );

            RelevoVia relevoVia = new RelevoVia();
            relevoVia.setRelevo(relevo);
            relevoVia.setVia(via);
            relevoVia.setEstado(
                    EstadoOperativo.valueOf(
                            item.estado().name()
                    )
            );
            relevoVia.setDetalle(item.detalle());

            relevoViaRepository.save(relevoVia);
        }

        return relevo.getId();
    }

    @Override
    public Long actualizar(
            Long id,
            GestionarRelevoUseCase.Command command
    ) {
        Relevo relevo = relevoRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Relevo no encontrado"
                        )
                );

        Plaza plaza = requirePlaza(command.plazaId());
        Turno turno = requireTurno(command.turnoId());
        Trabajador operador = requireOperador(command.operadorId());

        Map<Long, ElementoRelevo> elementos =
                elementoRepository
                        .findByActivoTrueOrderByCategoriaAscOrdenAscNombreAsc()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        ElementoRelevo::getId,
                                        Function.identity()
                                )
                        );

        aplicar(relevo, command, plaza, turno, operador);
        relevoRepository.save(relevo);

        Map<Long, RelevoChecklist> checklistActual =
                checklistRepository
                        .findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(
                                id
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        item -> item.getElemento().getId(),
                                        Function.identity()
                                )
                        );

        for (GestionarRelevoUseCase.ChecklistItem item
                : command.checklist()) {
            RelevoChecklist checklist =
                    checklistActual.get(item.elementoId());

            if (checklist == null) {
                checklist = new RelevoChecklist();
                checklist.setRelevo(relevo);
                checklist.setElemento(
                        elementos.get(item.elementoId())
                );
            }

            checklist.setEstado(
                    EstadoOperativo.valueOf(
                            item.estado().name()
                    )
            );
            checklist.setDetalle(item.detalle());
            checklist.setCantidad(item.cantidad());

            checklistRepository.save(checklist);
        }

        Map<Long, RelevoVia> viasActual =
                relevoViaRepository
                        .findByRelevoIdOrderByViaNumeroAsc(id)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        item -> item.getVia().getId(),
                                        Function.identity()
                                )
                        );

        for (GestionarRelevoUseCase.ViaItem item
                : command.vias()) {
            RelevoVia relevoVia =
                    viasActual.get(item.viaId());

            if (relevoVia == null) {
                Via via = viaRepository
                        .findById(item.viaId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vía no encontrada"
                                )
                        );

                relevoVia = new RelevoVia();
                relevoVia.setRelevo(relevo);
                relevoVia.setVia(via);
            }

            relevoVia.setEstado(
                    EstadoOperativo.valueOf(
                            item.estado().name()
                    )
            );
            relevoVia.setDetalle(item.detalle());

            relevoViaRepository.save(relevoVia);
        }

        return relevo.getId();
    }

    private void aplicar(
            Relevo relevo,
            GestionarRelevoUseCase.Command command,
            Plaza plaza,
            Turno turno,
            Trabajador operador
    ) {
        relevo.setPlaza(plaza);
        relevo.setTurno(turno);
        relevo.setOperador(operador);
        relevo.setFecha(command.fecha());
        relevo.setHora(command.hora());
        relevo.setObservaciones(command.observaciones());
        relevo.setResumen(command.resumen());
    }

    private Plaza requirePlaza(Long id) {
        return plazaRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plaza no encontrada"
                        )
                );
    }

    private Turno requireTurno(Long id) {
        return turnoRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Turno no encontrado"
                        )
                );
    }

    private Trabajador requireOperador(Long id) {
        return trabajadorRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Operador no encontrado"
                        )
                );
    }
}
