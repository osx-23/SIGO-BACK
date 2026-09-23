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
import java.util.List;

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

        return mapRelevo(relevo);
    }

    @Override
    public List<ConsultarRelevosUseCase.Relevo> listar(
            LocalDate inicio,
            LocalDate fin
    ) {
        return relevoRepository
                .findByFechaBetweenOrderByFechaDescHoraDesc(
                        inicio,
                        fin
                )
                .stream()
                .map(this::mapRelevo)
                .toList();
    }

    private ConsultarRelevosUseCase.Relevo mapRelevo(
            Relevo relevo
    ) {
        List<ConsultarRelevosUseCase.Checklist> checklist =
                checklistRepository
                        .findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(
                                relevo.getId()
                        )
                        .stream()
                        .map(this::mapChecklist)
                        .toList();

        List<ConsultarRelevosUseCase.Via> vias =
                relevoViaRepository
                        .findByRelevoIdOrderByViaNumeroAsc(
                                relevo.getId()
                        )
                        .stream()
                        .map(this::mapVia)
                        .toList();

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
                checklist,
                vias
        );
    }

    private ConsultarRelevosUseCase.Checklist mapChecklist(
            RelevoChecklist checklist
    ) {
        List<ConsultarRelevosUseCase.Evidencia> evidencias =
                checklistEvidenciaRepository
                        .findByChecklistIdOrderByIdAsc(
                                checklist.getId()
                        )
                        .stream()
                        .map(this::mapEvidencia)
                        .toList();

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
        );
    }

    private ConsultarRelevosUseCase.Via mapVia(
            RelevoVia relevoVia
    ) {
        List<ConsultarRelevosUseCase.Evidencia> evidencias =
                viaEvidenciaRepository
                        .findByRelevoViaIdOrderByIdAsc(
                                relevoVia.getId()
                        )
                        .stream()
                        .map(this::mapEvidencia)
                        .toList();

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
