package com.sigo.relevo.api.dto;
import com.sigo.relevo.domain.EstadoRelevo;
import java.util.List;
public record RelevoViaResponse(
 Long id,Long viaId,Integer numero,String nombre,EstadoRelevo estado,String detalle,
 List<EvidenciaRelevoResponse> evidencias
) {}
