package com.sigo.relevo.api.dto;
import com.sigo.relevo.infrastructure.persistence.entity.EstadoOperativo;
import java.util.List;
public record RelevoViaResponse(
 Long id,Long viaId,Integer numero,String nombre,EstadoOperativo estado,String detalle,
 List<EvidenciaRelevoResponse> evidencias
) {}
