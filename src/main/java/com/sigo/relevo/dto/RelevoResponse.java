package com.sigo.relevo.dto;
import java.time.*;
import java.util.List;
public record RelevoResponse(
 Long id,Long plazaId,String plazaCodigo,String plazaDescripcion,
 Long turnoId,String turnoCodigo,String turnoNombre,
 Long operadorId,Integer operadorCodigo,String operadorNombre,
 LocalDate fecha,LocalTime hora,String observaciones,String resumen,
 OffsetDateTime createdAt,OffsetDateTime updatedAt,
 List<RelevoChecklistResponse> checklist,List<RelevoViaResponse> vias
) {}
