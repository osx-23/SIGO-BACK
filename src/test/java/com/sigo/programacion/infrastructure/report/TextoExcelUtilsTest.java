package com.sigo.programacion.infrastructure.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextoExcelUtilsTest {

    @Test
    void normalizaAuxiliaresDeDistintosExcel() {
        assertEquals("AUX 1", TextoExcelUtils.normalizarCaseta(" aux1 "));
        assertEquals("AUX 1", TextoExcelUtils.normalizarCaseta("AUXILIAR 1"));
        assertEquals("AUX 2", TextoExcelUtils.normalizarCaseta("aux-2"));
        assertEquals("AUX 3", TextoExcelUtils.normalizarCaseta("AUX 3"));
    }

    @Test
    void conservaCodigosDeViasYDescansos() {
        assertEquals("101", TextoExcelUtils.normalizarCaseta(" 101 "));
        assertEquals("105", TextoExcelUtils.normalizarCaseta("105"));
        assertEquals("D", TextoExcelUtils.normalizar(" d "));
    }

    @Test
    void limpiaTildesYEspaciosEnEncabezados() {
        assertEquals("DISTRIBUCION SETIEMBRE P4",
                TextoExcelUtils.sinAcentos(" Distribución   Setiembre   P4 "));
    }

    @Test
    void celdaVaciaNoSeInterpretaComoCambio() {
        assertTrue(TextoExcelUtils.esCeldaVacia(null));
        assertTrue(TextoExcelUtils.esCeldaVacia(" \t  "));
        assertFalse(TextoExcelUtils.esCeldaVacia("D"));
        assertFalse(TextoExcelUtils.esCeldaVacia("AUX 1"));
    }
}
