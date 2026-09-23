package com.sigo.chat.dto;

import java.time.LocalDate;

public record SigoToolRequest(
        SigoTool tool,
        Integer codigo,
        String trabajador,
        String plaza,
        String turno,
        String motivo,
        String estado,
        Long relevoId,
        Long asistenciaId,
        LocalDate desde,
        LocalDate hasta,
        Integer limite
) {
    public int limiteSeguro() {
        if (limite == null) return 20;
        return Math.max(1, Math.min(limite, 50));
    }
}
