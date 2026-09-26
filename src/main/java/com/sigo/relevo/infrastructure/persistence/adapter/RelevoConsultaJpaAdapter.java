package com.sigo.relevo.infrastructure.persistence.adapter;

import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;
import com.sigo.relevo.application.port.out.RelevoConsultaPort;
import com.sigo.relevo.domain.EstadoRelevo;
import com.sigo.relevo.infrastructure.persistence.entity.Relevo;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoChecklist;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoChecklistEvidencia;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoVia;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoViaEvidencia;
import com.sigo.relevo.infrastructure.persistence.repository.ElementoRelevoRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoChecklistEvidenciaRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoChecklistRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoViaEvidenciaRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoViaRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RelevoConsultaJpaAdapter
        implements RelevoConsultaPort {

    private final RelevoRepository relevoRepository;
    private final RelevoChecklistRepository checklistRepository;
    private final RelevoChecklistEvidenciaRepository checklistEvidenciaRepository;
    private final RelevoViaRepository relevoViaRepository;
    private final RelevoViaEvidenciaRepository viaEvidenciaRepository;
    private final ElementoRelevoRepository elementoRepository;

    @Override
    public List<ConsultarRelevosUseCase.Elemento> listarElementos() {
        return elementoRepository
                .findByActivoTrueOrderByCategoriaAscOrdenAscNombreAsc()
                .stream()
                .map(elemento ->
                        new ConsultarRelevosUseCase.Elemento(
                                elemento.getId(),
                                elemento.getCodigo(),
                                elemento.getNombre(),
                                elemento.getCategoria(),
                                elemento.getRequiereCantidad(),
                                elemento.getOrden()
                        )
                )
                .toList();
    }

    @Override
    public ConsultarRelevosUseCase.Relevo obtener(Long id) {
        Relevo relevo = relevoRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Relevo no encontrado"
                        )
                );

        return mapRelevos(List.of(relevo)).getFirst();
    }

    @Override
    public List<ConsultarRelevosUseCase.Relevo> listar(
            LocalDate inicio,
            LocalDate fin
    ) {
        List<Relevo> relevos = relevoRepository
                .findByFechaBetweenOrderByFechaDescHoraDesc(
                        inicio,
                        fin
                );

        return mapRelevos(relevos);
    }

    /**
     * Carga el detalle de todos los relevos en consultas por lote.
     *
     * Antes, cada relevo disparaba consultas adicionales para checklist,
     * vías y evidencias (patrón N+1). Con este método el costo queda
     * prácticamente constante:
     *
     * 1 consulta de relevos
     * 1 consulta de checklist
     * 1 consulta de vías
     * 1 consulta de evidencias de checklist
     * 1 consulta de evidencias de vías
     */
    private List<ConsultarRelevosUseCase.Relevo> mapRelevos(
            List<Relevo> relevos
    ) {
        if (relevos == null || relevos.isEmpty()) {
            return List.of();
        }

        List<Long> relevoIds = relevos
                .stream()
                .map(Relevo::getId)
                .toList();

        List<RelevoChecklist> checklist = checklistRepository
                .findByRelevoIdInOrderByRelevoIdAscElementoCategoriaAscElementoOrdenAsc(
                        relevoIds
                );

        List<RelevoVia> vias = relevoViaRepository
                .findByRelevoIdInOrderByRelevoIdAscViaNumeroAsc(
                        relevoIds
                );

        List<Long> checklistIds = checklist
                .stream()
                .map(RelevoChecklist::getId)
                .toList();

        List<Long> relevoViaIds = vias
                .stream()
                .map(RelevoVia::getId)
                .toList();

        List<RelevoChecklistEvidencia> evidenciasChecklist =
                checklistIds.isEmpty()
                        ? List.of()
                        : checklistEvidenciaRepository
                                .findByChecklistIdInOrderByChecklistIdAscIdAsc(
                                        checklistIds
                                );

        List<RelevoViaEvidencia> evidenciasVia =
                relevoViaIds.isEmpty()
                        ? List.of()
                        : viaEvidenciaRepository
                                .findByRelevoViaIdInOrderByRelevoViaIdAscIdAsc(
                                        relevoViaIds
                                );

        Map<Long, List<RelevoChecklist>> checklistPorRelevo =
                agruparChecklistPorRelevo(checklist);

        Map<Long, List<RelevoVia>> viasPorRelevo =
                agruparViasPorRelevo(vias);

        Map<Long, List<RelevoChecklistEvidencia>> evidenciasPorChecklist =
                agruparEvidenciasPorChecklist(evidenciasChecklist);

        Map<Long, List<RelevoViaEvidencia>> evidenciasPorVia =
                agruparEvidenciasPorVia(evidenciasVia);

        return relevos
                .stream()
                .map(relevo ->
                        mapRelevo(
                                relevo,
                                checklistPorRelevo.getOrDefault(
                                        relevo.getId(),
                                        List.of()
                                ),
                                viasPorRelevo.getOrDefault(
                                        relevo.getId(),
                                        List.of()
                                ),
                                evidenciasPorChecklist,
                                evidenciasPorVia
                        )
                )
                .toList();
    }

    private Map<Long, List<RelevoChecklist>> agruparChecklistPorRelevo(
            List<RelevoChecklist> items
    ) {
        Map<Long, List<RelevoChecklist>> resultado = new HashMap<>();

        for (RelevoChecklist item : items) {
            resultado
                    .computeIfAbsent(
                            item.getRelevo().getId(),
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }

        return resultado;
    }

    private Map<Long, List<RelevoVia>> agruparViasPorRelevo(
            List<RelevoVia> items
    ) {
        Map<Long, List<RelevoVia>> resultado = new HashMap<>();

        for (RelevoVia item : items) {
            resultado
                    .computeIfAbsent(
                            item.getRelevo().getId(),
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }

        return resultado;
    }

    private Map<Long, List<RelevoChecklistEvidencia>> agruparEvidenciasPorChecklist(
            List<RelevoChecklistEvidencia> items
    ) {
        Map<Long, List<RelevoChecklistEvidencia>> resultado =
                new HashMap<>();

        for (RelevoChecklistEvidencia item : items) {
            resultado
                    .computeIfAbsent(
                            item.getChecklist().getId(),
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }

        return resultado;
    }

    private Map<Long, List<RelevoViaEvidencia>> agruparEvidenciasPorVia(
            List<RelevoViaEvidencia> items
    ) {
        Map<Long, List<RelevoViaEvidencia>> resultado =
                new HashMap<>();

        for (RelevoViaEvidencia item : items) {
            resultado
                    .computeIfAbsent(
                            item.getRelevoVia().getId(),
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }

        return resultado;
    }

    private ConsultarRelevosUseCase.Relevo mapRelevo(
            Relevo relevo,
            List<RelevoChecklist> checklist,
            List<RelevoVia> vias,
            Map<Long, List<RelevoChecklistEvidencia>> evidenciasPorChecklist,
            Map<Long, List<RelevoViaEvidencia>> evidenciasPorVia
    ) {
        return new ConsultarRelevosUseCase.Relevo(
                relevo.getId(),
                relevo.getPlaza().getId(),
                relevo.getPlaza().getCodigo(),
                relevo.getPlaza().getDescripcion(),
                relevo.getTurno().getId(),
                relevo.getTurno().getCodigo(),
                relevo.getTurno().getNombre(),
                relevo.getOperador().getId(),
                relevo.getOperador().getCodigo(),
                relevo.getOperador().getNombreCompleto(),
                relevo.getFecha(),
                relevo.getHora(),
                relevo.getObservaciones(),
                relevo.getResumen(),
                relevo.getCreatedAt(),
                relevo.getUpdatedAt(),
                checklist
                        .stream()
                        .map(item ->
                                mapChecklist(
                                        item,
                                        evidenciasPorChecklist.getOrDefault(
                                                item.getId(),
                                                List.of()
                                        )
                                )
                        )
                        .toList(),
                vias
                        .stream()
                        .map(item ->
                                mapVia(
                                        item,
                                        evidenciasPorVia.getOrDefault(
                                                item.getId(),
                                                List.of()
                                        )
                                )
                        )
                        .toList()
        );
    }

    private ConsultarRelevosUseCase.Checklist mapChecklist(
            RelevoChecklist checklist,
            List<RelevoChecklistEvidencia> evidencias
    ) {
        return new ConsultarRelevosUseCase.Checklist(
                checklist.getId(),
                checklist.getElemento().getId(),
                checklist.getElemento().getCodigo(),
                checklist.getElemento().getNombre(),
                checklist.getElemento().getCategoria(),
                EstadoRelevo.valueOf(
                        checklist.getEstado().name()
                ),
                checklist.getDetalle(),
                checklist.getCantidad(),
                evidencias
                        .stream()
                        .map(this::mapEvidencia)
                        .toList()
        );
    }

    private ConsultarRelevosUseCase.Via mapVia(
            RelevoVia relevoVia,
            List<RelevoViaEvidencia> evidencias
    ) {
        return new ConsultarRelevosUseCase.Via(
                relevoVia.getId(),
                relevoVia.getVia().getId(),
                relevoVia.getVia().getNumero(),
                relevoVia.getVia().getNombre(),
                EstadoRelevo.valueOf(
                        relevoVia.getEstado().name()
                ),
                relevoVia.getDetalle(),
                evidencias
                        .stream()
                        .map(this::mapEvidencia)
                        .toList()
        );
    }

    private ConsultarRelevosUseCase.Evidencia mapEvidencia(
            RelevoChecklistEvidencia evidencia
    ) {
        return new ConsultarRelevosUseCase.Evidencia(
                evidencia.getId(),
                evidencia.getUrlArchivo(),
                evidencia.getPublicId(),
                evidencia.getTipo(),
                evidencia.getCreatedAt()
        );
    }

    private ConsultarRelevosUseCase.Evidencia mapEvidencia(
            RelevoViaEvidencia evidencia
    ) {
        return new ConsultarRelevosUseCase.Evidencia(
                evidencia.getId(),
                evidencia.getUrlArchivo(),
                evidencia.getPublicId(),
                evidencia.getTipo(),
                evidencia.getCreatedAt()
        );
    }
}
