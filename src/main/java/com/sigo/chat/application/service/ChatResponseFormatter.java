package com.sigo.chat.application.service;

import com.sigo.chat.api.dto.SigoToolResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ChatResponseFormatter {

    public String formatear(List<SigoToolResult> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            return "No encontré información para responder esa consulta.";
        }

        StringBuilder out = new StringBuilder();

        for (SigoToolResult resultado : resultados) {
            if (resultado == null) continue;
            if (!out.isEmpty()) out.append("\n\n");

            out.append(resultado.titulo()).append("\n");

            if (resultado.nota() != null && !resultado.nota().isBlank()) {
                out.append(resultado.nota()).append("\n");
            }

            if (resultado.filas() == null || resultado.filas().isEmpty()) {
                if (resultado.nota() == null || resultado.nota().isBlank()) {
                    out.append("No se encontraron registros.");
                }
                continue;
            }

            if (resultado.filas().size() == 1) {
                for (Map.Entry<String, Object> entry : resultado.filas().getFirst().entrySet()) {
                    out.append("- ")
                            .append(etiqueta(entry.getKey()))
                            .append(": ")
                            .append(valor(entry.getValue()))
                            .append("\n");
                }
            } else {
                int i = 1;
                for (Map<String, Object> fila : resultado.filas()) {
                    out.append(i++).append(". ");
                    boolean primero = true;
                    for (Map.Entry<String, Object> entry : fila.entrySet()) {
                        if (entry.getValue() == null) continue;
                        if (!primero) out.append(" | ");
                        out.append(etiqueta(entry.getKey()))
                                .append(": ")
                                .append(valor(entry.getValue()));
                        primero = false;
                    }
                    out.append("\n");
                }
            }
        }

        return out.toString().trim();
    }

    private String etiqueta(String key) {
        if (key == null) return "";
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "nombre_completo" -> "Nombre";
            case "codigo_trabajador" -> "Código";
            case "apoyo_solicitado" -> "Apoyo solicitado";
            case "detalle_apoyo" -> "Detalle de apoyo";
            case "porcentaje_asistencia" -> "% asistencia";
            case "total_ausencias" -> "Total de ausencias";
            case "elementos_observados" -> "Elementos observados";
            case "vias_observadas" -> "Vías observadas";
            case "elementos_con_observacion" -> "Elementos con observación";
            case "vias_con_observacion" -> "Vías con observación";
            case "relevo_id" -> "Relevo";
            case "asistencia_id" -> "Asistencia";
            case "evidencia_id" -> "Evidencia";
            case "url_archivo" -> "URL evidencia";
            case "nombre_via" -> "Nombre de vía";
            case "codigo_operador" -> "Código del operador";
            default -> humanizar(key);
        };
    }

    private String humanizar(String key) {
        String texto = key.replace('_', ' ').trim();
        if (texto.isEmpty()) return texto;
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private String valor(Object value) {
        if (value == null) return "-";
        if (value instanceof BigDecimal bd) return bd.stripTrailingZeros().toPlainString();
        if (value instanceof Boolean b) return b ? "Sí" : "No";
        return String.valueOf(value);
    }
}
