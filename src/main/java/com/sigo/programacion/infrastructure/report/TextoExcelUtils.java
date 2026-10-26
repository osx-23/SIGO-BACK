package com.sigo.programacion.infrastructure.report;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilidades de Apache Commons Lang para textos de distribucion Excel. */
public final class TextoExcelUtils {
    private static final Pattern AUXILIAR =
            Pattern.compile("^AUX(?:ILIAR)?\\s*[- ]?\\s*(\\d+)$");

    private TextoExcelUtils() {
    }

    /** Una celda vacia no representa una orden para borrar asignaciones. */
    public static boolean esCeldaVacia(String valor) {
        return StringUtils.isBlank(valor);
    }

    public static String normalizar(String valor) {
        return StringUtils.trimToEmpty(valor).toUpperCase(Locale.ROOT);
    }

    public static String sinAcentos(String valor) {
        return StringUtils.normalizeSpace(StringUtils.stripAccents(normalizar(valor)));
    }

    /** Reconoce AUX1, AUX 1, AUX-1 y AUXILIAR 1 como AUX 1. */
    public static String normalizarCaseta(String valor) {
        String limpio = sinAcentos(valor);
        Matcher auxiliar = AUXILIAR.matcher(limpio);
        return auxiliar.matches() ? "AUX " + auxiliar.group(1) : limpio;
    }
}
