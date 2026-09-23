package com.sigo.relevo.infrastructure.persistence.adapter;

import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;
import com.sigo.relevo.application.port.out.RelevoEvidenciaPort;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoChecklist;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoChecklistEvidencia;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoVia;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoViaEvidencia;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoChecklistEvidenciaRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoChecklistRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoViaEvidenciaRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoViaRepository;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RelevoEvidenciaJpaAdapter
        implements RelevoEvidenciaPort {

    private final RelevoChecklistRepository checklistRepository;
    private final RelevoChecklistEvidenciaRepository checklistEvidenciaRepository;
    private final RelevoViaRepository relevoViaRepository;
    private final RelevoViaEvidenciaRepository viaEvidenciaRepository;

    @Override
    public boolean existeChecklist(Long checklistId) {
        return checklistRepository.existsById(checklistId);
    }

    @Override
    public boolean existeRelevoVia(Long relevoViaId) {
        return relevoViaRepository.existsById(relevoViaId);
    }

    @Override
    public GestionarEvidenciaRelevoUseCase.Evidencia guardarChecklist(
            Long checklistId,
            String urlArchivo,
            String publicId
    ) {
        RelevoChecklist checklist = checklistRepository
                .findById(checklistId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Elemento de checklist no encontrado"
                        )
                );

        RelevoChecklistEvidencia evidencia =
                new RelevoChecklistEvidencia();

        evidencia.setChecklist(checklist);
        evidencia.setUrlArchivo(urlArchivo);
        evidencia.setPublicId(publicId);
        evidencia.setTipo("foto");

        return map(
                checklistEvidenciaRepository.save(evidencia)
        );
    }

    @Override
    public GestionarEvidenciaRelevoUseCase.Evidencia guardarVia(
            Long relevoViaId,
            String urlArchivo,
            String publicId
    ) {
        RelevoVia relevoVia = relevoViaRepository
                .findById(relevoViaId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reporte de vía no encontrado"
                        )
                );

        RelevoViaEvidencia evidencia =
                new RelevoViaEvidencia();

        evidencia.setRelevoVia(relevoVia);
        evidencia.setUrlArchivo(urlArchivo);
        evidencia.setPublicId(publicId);
        evidencia.setTipo("foto");

        return map(
                viaEvidenciaRepository.save(evidencia)
        );
    }

    @Override
    public EvidenciaAlmacenada requireChecklist(
            Long checklistId,
            Long evidenciaId
    ) {
        RelevoChecklistEvidencia evidencia =
                checklistEvidenciaRepository
                        .findById(evidenciaId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Evidencia no encontrada"
                                )
                        );

        if (!checklistId.equals(
                evidencia.getChecklist().getId()
        )) {
            throw new BusinessException(
                    "La evidencia no pertenece al elemento indicado"
            );
        }

        return data(evidencia);
    }

    @Override
    public EvidenciaAlmacenada requireVia(
            Long relevoViaId,
            Long evidenciaId
    ) {
        RelevoViaEvidencia evidencia =
                viaEvidenciaRepository
                        .findById(evidenciaId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Evidencia no encontrada"
                                )
                        );

        if (!relevoViaId.equals(
                evidencia.getRelevoVia().getId()
        )) {
            throw new BusinessException(
                    "La evidencia no pertenece a la vía indicada"
            );
        }

        return data(evidencia);
    }

    @Override
    public void eliminarChecklist(Long evidenciaId) {
        checklistEvidenciaRepository.deleteById(evidenciaId);
    }

    @Override
    public void eliminarVia(Long evidenciaId) {
        viaEvidenciaRepository.deleteById(evidenciaId);
    }

    private GestionarEvidenciaRelevoUseCase.Evidencia map(
            RelevoChecklistEvidencia evidencia
    ) {
        return new GestionarEvidenciaRelevoUseCase.Evidencia(
                evidencia.getId(),
                evidencia.getUrlArchivo(),
                evidencia.getPublicId(),
                evidencia.getTipo(),
                evidencia.getCreatedAt()
        );
    }

    private GestionarEvidenciaRelevoUseCase.Evidencia map(
            RelevoViaEvidencia evidencia
    ) {
        return new GestionarEvidenciaRelevoUseCase.Evidencia(
                evidencia.getId(),
                evidencia.getUrlArchivo(),
                evidencia.getPublicId(),
                evidencia.getTipo(),
                evidencia.getCreatedAt()
        );
    }

    private EvidenciaAlmacenada data(
            RelevoChecklistEvidencia evidencia
    ) {
        return new EvidenciaAlmacenada(
                evidencia.getId(),
                evidencia.getUrlArchivo(),
                evidencia.getPublicId(),
                evidencia.getTipo()
        );
    }

    private EvidenciaAlmacenada data(
            RelevoViaEvidencia evidencia
    ) {
        return new EvidenciaAlmacenada(
                evidencia.getId(),
                evidencia.getUrlArchivo(),
                evidencia.getPublicId(),
                evidencia.getTipo()
        );
    }
}
