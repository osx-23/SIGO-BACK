package com.sigo.relevo.api.dto;
import com.sigo.relevo.domain.EstadoRelevo;
import java.util.List;
public record RelevoChecklistResponse(
 Long id,Long elementoId,String codigo,String nombre,String categoria,
 EstadoRelevo estado,String detalle,Integer cantidad,List<EvidenciaRelevoResponse> evidencias
) {}
