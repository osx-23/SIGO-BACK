package com.sigo.asistencia.relevo.dto;
import com.sigo.asistencia.relevo.entity.EstadoOperativo;
import java.util.List;
public record RelevoChecklistResponse(
 Long id,Long elementoId,String codigo,String nombre,String categoria,
 EstadoOperativo estado,String detalle,Integer cantidad,List<EvidenciaRelevoResponse> evidencias
) {}
