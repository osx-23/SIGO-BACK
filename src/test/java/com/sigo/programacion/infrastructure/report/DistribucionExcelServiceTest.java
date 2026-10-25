package com.sigo.programacion.infrastructure.report;

import com.sigo.programacion.application.port.in.DistribucionUseCase;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DistribucionExcelServiceTest {
    private final DistribucionExcelService service = new DistribucionExcelService();

    @Test
    void coincideConMatrizSemanalDelPdf() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        var reporte = new DistribucionUseCase.ReportePlaza(
                4L, "P4", inicio, inicio.plusDays(6), List.of(
                item(1L, 2396, "HERRERA CALLE OSCAR MARTIN", inicio, "C", "104"),
                item(1L, 2396, "HERRERA CALLE OSCAR MARTIN", inicio.plusDays(1), "C", "102"),
                item(1L, 2396, "HERRERA CALLE OSCAR MARTIN", inicio.plusDays(2), "D", null),
                item(1L, 2396, "HERRERA CALLE OSCAR MARTIN", inicio.plusDays(4), "A", null),
                item(2L, 2857, "FRANCO ÑAHUINRIPA YAMELY PAOLA", inicio, "V", null)));

        byte[] archivo = service.generarMatriz(reporte);
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            assertEquals(1, libro.getNumberOfSheets());
            var hoja = libro.getSheet("Semana 1");
            assertNotNull(hoja);
            assertEquals("DISTRIBUCIÓN OCTUBRE P4", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("JUE", hoja.getRow(3).getCell(3).getStringCellValue());
            assertEquals("MIÉ", hoja.getRow(3).getCell(9).getStringCellValue());
            assertEquals("1", hoja.getRow(4).getCell(3).getStringCellValue());
            assertEquals("7", hoja.getRow(4).getCell(9).getStringCellValue());
            assertEquals("Cod", hoja.getRow(3).getCell(1).getStringCellValue());
            assertEquals("2396", hoja.getRow(5).getCell(1).getStringCellValue());
            assertEquals("HERRERA CALLE OSCAR MARTIN", hoja.getRow(5).getCell(2).getStringCellValue());
            assertEquals("104", hoja.getRow(5).getCell(3).getStringCellValue());
            assertEquals("102", hoja.getRow(5).getCell(4).getStringCellValue());
            assertEquals("D", hoja.getRow(5).getCell(5).getStringCellValue());
            assertEquals("SIN ASIG.", hoja.getRow(5).getCell(7).getStringCellValue());
            assertEquals("V", hoja.getRow(6).getCell(3).getStringCellValue());
            assertEquals("FFC7CE", rgb(hoja.getRow(5).getCell(3)));
            assertEquals("BFBFBF", rgb(hoja.getRow(5).getCell(5)));
            assertEquals("FFE6E6", rgb(hoja.getRow(5).getCell(7)));
            assertEquals("BDD7EE", rgb(hoja.getRow(6).getCell(3)));
        }
    }

    @Test
    void rangoMensualCreaUnaHojaPorCadaSieteDias() throws Exception {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        var reporte = new DistribucionUseCase.ReportePlaza(
                4L, "P4", desde, LocalDate.of(2026, 10, 31), List.of());
        try (XSSFWorkbook libro = new XSSFWorkbook(
                new ByteArrayInputStream(service.generarMatriz(reporte)))) {
            assertEquals(5, libro.getNumberOfSheets());
            assertEquals("Semana 5", libro.getSheetAt(4).getSheetName());
            assertTrue(libro.getSheetAt(4).getRow(1).getCell(0)
                    .getStringCellValue().contains("29/10/2026 - 31/10/2026"));
        }
    }

    @Test
    void rechazaRangoInvertido() {
        var reporte = new DistribucionUseCase.ReportePlaza(
                4L, "P4", LocalDate.of(2026, 10, 7),
                LocalDate.of(2026, 10, 1), List.of());
        assertThrows(IllegalArgumentException.class, () -> service.generarMatriz(reporte));
    }

    private static DistribucionUseCase.MatrizItem item(
            Long id, int codigo, String nombre, LocalDate fecha,
            String turno, String caseta) {
        return new DistribucionUseCase.MatrizItem(
                1L, id, codigo, nombre, "P4", fecha, turno, caseta, "CASETA");
    }

    private static String rgb(XSSFCell celda) {
        byte[] rgb = celda.getCellStyle().getFillForegroundXSSFColor().getRGB();
        return String.format("%02X%02X%02X", rgb[0] & 0xff, rgb[1] & 0xff, rgb[2] & 0xff);
    }
}
