package com.sigo.relevo.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;
public record RelevoRequest(
 @NotNull Long plazaId,@NotNull Long turnoId,@NotNull Long operadorId,
 @NotNull LocalDate fecha,@NotNull LocalTime hora,
 String observaciones,String resumen,
 @NotEmpty @Valid List<RelevoChecklistRequest> checklist,
 @Valid List<RelevoViaRequest> vias
) {}
