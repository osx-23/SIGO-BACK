package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.in.UbicacionUseCase;
import com.sigo.programacion.domain.ProgramacionValidationException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DistribucionExcelImportServiceTest {
    private final DistribucionUseCase distribucion = mock(DistribucionUseCase.class);
    private final UbicacionUseCase ubicaciones = mock(UbicacionUseCase.class);
    private final TrabajadorUseCase trabajadores = mock(TrabajadorUseCase.class);
    private final DistribucionExcelImportService importador =
            new DistribucionExcelImportService(distribucion, ubicaciones, trabajadores);
    private final DistribucionExcelService exportador = new DistribucionExcelService();
    private final LocalDate inicio = LocalDate.of(2026, 10, 1);

    @BeforeEach
    void preparar() {
        when(trabajadores.listarAdministracion(3L)).thenReturn(List.of(
                new TrabajadorUseCase.TrabajadorData(1L, 2396, "HERRERA CALLE OSCAR MARTIN",
                        null, null, null, false, true)));
        var item = new DistribucionUseCase.MatrizItem(99L, 1L, 2396,
                "HERRERA CALLE OSCAR MARTIN", "P4", inicio, "C", null, "CASETA");
        when(distribucion.reportePlaza(3L, inicio, inicio.plusDays(6)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(3L, "P4", inicio,
                        inicio.plusDays(6), List.of(item)));
        when(ubicaciones.listarActivas(3L)).thenReturn(List.of(new UbicacionUseCase.Ubicacion(
                17L, 3L, "104", "Caseta 104", "CASETA", null, true, 1, true, true, true)));
    }

    @Test
    void leeLaMatrizExportadaYSugiereCasetaSinGuardar() throws Exception {
        byte[] archivo = conCaseta("104");
        var previa = importador.previsualizar(3L, archivo);
        assertTrue(previa.valido());
        assertEquals(1, previa.cambios().size());
        assertEquals(99L, previa.cambios().get(0).programacionTurnoId());
        assertEquals(17L, previa.cambios().get(0).ubicacionId());
        verify(distribucion, never()).guardar(any(), anyBoolean());
    }



    @Test
    void omiteCasetaCuandoTrabajadorNoTieneTurno() throws Exception {
        // En SIGO no existe ninguna programación para esta semana.
        when(distribucion.reportePlaza(3L, inicio, inicio.plusDays(6)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(
                        3L, "P4", inicio, inicio.plusDays(6), List.of()
                ));

        // El Excel sí contiene una caseta.
        byte[] archivo = conCaseta("104");

        var previa = importador.previsualizar(3L, archivo);

        assertTrue(previa.valido());
        assertTrue(previa.cambios().isEmpty());

        // Se omite la caseta porque no existe turno.
        assertEquals(1, previa.omitidos().size());
        assertTrue(previa.omitidos().get(0)
                .contains("sin turno programado"));

        assertTrue(previa.errores().isEmpty());

        // No se guarda nada en la base de datos.
        verify(distribucion, never()).guardar(any(), anyBoolean());
    }

    @Test
    void mantieneCasetaValidaAunqueOtroTrabajadorNoTengaTurno() throws Exception {
        byte[] archivo = conCaseta("104");
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo));
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Row sinTurno = libro.getSheetAt(0).createRow(6);
            sinTurno.createCell(1).setCellValue("7777");
            sinTurno.createCell(2).setCellValue("OTRO TRABAJADOR");
            sinTurno.createCell(3).setCellValue("104");
            libro.write(salida);
            var previa = importador.previsualizar(3L, salida.toByteArray());
            assertTrue(previa.valido());
            assertEquals(1, previa.cambios().size());
            assertEquals(1, previa.omitidos().size());
            assertTrue(previa.errores().isEmpty());
        }
    }

    @Test
    void rechazaCasetasInexistentes() throws Exception {
        var previa = importador.previsualizar(3L, conCaseta("999"));

        assertFalse(previa.valido());
        assertTrue(previa.errores().get(0).contains("caseta desconocida"));
        assertTrue(previa.cambios().isEmpty());
    }

    @Test
    void ignoraCeldaSinAsignar() {
        var item = new DistribucionUseCase.MatrizItem(99L, 1L, 2396,
                "HERRERA CALLE OSCAR MARTIN", "P4", inicio, "C", null, "CASETA");
        var reporte = new DistribucionUseCase.ReportePlaza(3L, "P4", inicio,
                inicio.plusDays(6), List.of(item));
        var previa = importador.previsualizar(3L, exportador.generarMatriz(reporte));
        assertTrue(previa.valido());
        assertTrue(previa.cambios().isEmpty());
    }

    @Test
    void detectaConflictoCuandoCasetaYaEstaOcupada() throws Exception {
        var ocupante = new DistribucionUseCase.MatrizItem(88L, 2L, 2520,
                "ANTIGUO", "P4", inicio, "C", "104", "CASETA");
        var solicitante = new DistribucionUseCase.MatrizItem(99L, 1L, 2396,
                "HERRERA CALLE OSCAR MARTIN", "P4", inicio, "C", null, "CASETA");
        when(distribucion.reportePlaza(3L, inicio, inicio.plusDays(6)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(3L, "P4", inicio,
                        inicio.plusDays(6), List.of(solicitante, ocupante)));
        var previa = importador.previsualizar(3L, conCaseta("104"));
        assertFalse(previa.valido());
        assertTrue(previa.errores().stream().anyMatch(e -> e.contains("ya ocupada")));
        assertEquals(1, previa.cambios().size());
        assertEquals(99L, previa.cambios().get(0).programacionTurnoId());
        verify(distribucion, never()).guardar(any(), anyBoolean());
    }

    @Test
    void excelSinAsignarPuedeLimpiarCasetaPrevia() throws Exception {
        var asignado = new DistribucionUseCase.MatrizItem(99L, 1L, 2396,
                "HERRERA CALLE OSCAR MARTIN", "P4", inicio, "C", "104", "CASETA");
        when(distribucion.reportePlaza(3L, inicio, inicio.plusDays(6)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(3L, "P4", inicio,
                        inicio.plusDays(6), List.of(asignado)));
        var previa = importador.previsualizar(3L, conCaseta("SIN ASIG."));
        assertTrue(previa.valido());
        assertEquals(1, previa.cambios().size());
        assertNull(previa.cambios().get(0).ubicacionId());
    }

    @Test
    void noAceptaArchivosQueNoSonXlsx() {
        assertThrows(ProgramacionValidationException.class,
                () -> importador.previsualizar(3L, "contenido inválido".getBytes()));
    }


    @Test
    void importaDistribucionMensualOriginalSinGuardar() throws Exception {
        LocalDate septiembre = LocalDate.of(2026, 9, 1);
        var trabajador = new DistribucionUseCase.MatrizItem(101L, 1L, 2396,
                "TRABAJADOR DEMO", "P4", septiembre, "C", null, "CASETA");
        when(distribucion.reportePlaza(3L, septiembre, septiembre.plusDays(29)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(3L, "P4", septiembre,
                        septiembre.plusDays(29), List.of(trabajador)));

        var previa = importador.previsualizar(3L, mensualOriginal("DISTRIBUCION SEPTIEMBRE P4", "104"), 2026, 9);
        assertTrue(previa.valido());
        assertEquals(septiembre, previa.desde());
        assertEquals(septiembre.plusDays(29), previa.hasta());
        assertEquals(1, previa.cambios().size());
        assertEquals("104", previa.cambios().get(0).caseta());
        verify(distribucion, never()).guardar(any(), anyBoolean());
    }

    @Test
    void rechazaMesDistintoAlSeleccionado() throws Exception {
        var archivo = mensualOriginal("DISTRIBUCION SEPTIEMBRE P4", "104");
        assertThrows(ProgramacionValidationException.class,
                () -> importador.previsualizar(3L, archivo, 2026, 10));
    }

    @Test
    void rechazaAnioIncompatibleConDiasSemana() throws Exception {
        var archivo = mensualOriginal("DISTRIBUCION SEPTIEMBRE P4", "104");
        assertThrows(ProgramacionValidationException.class,
                () -> importador.previsualizar(3L, archivo, 2025, 9));
    }

    @Test
    void reconoceAuxiliarConSeparadoresDiferentes() throws Exception {
        LocalDate septiembre = LocalDate.of(2026, 9, 1);
        var trabajador = new DistribucionUseCase.MatrizItem(101L, 1L, 2396,
                "TRABAJADOR DEMO", "P4", septiembre, "C", null, "CASETA");
        when(distribucion.reportePlaza(3L, septiembre, septiembre.plusDays(29)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(3L, "P4", septiembre,
                        septiembre.plusDays(29), List.of(trabajador)));
        when(ubicaciones.listarActivas(3L)).thenReturn(List.of(new UbicacionUseCase.Ubicacion(
                18L, 3L, "AUX1", "Auxiliar 1", "AUXILIAR", null, true, 1, true, true, true)));
        var previa = importador.previsualizar(3L, mensualOriginal("DISTRIBUCION SETIEMBRE P4", "AUX 1"), 2026, 9);
        assertTrue(previa.valido());
        assertEquals(1, previa.cambios().size());
        assertEquals(18L, previa.cambios().get(0).ubicacionId());
    }

    private byte[] mensualOriginal(String titulo, String casetaDiaUno) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Hoja1");
            hoja.createRow(1).createCell(1).setCellValue(titulo);
            Row encabezado = hoja.createRow(4);
            encabezado.createCell(0).setCellValue("N°");
            encabezado.createCell(1).setCellValue("Cod");
            encabezado.createCell(2).setCellValue("Nombre");
            Row dias = hoja.createRow(5);
            YearMonth periodo = YearMonth.of(2026, 9);
            for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
                LocalDate fecha = periodo.atDay(dia);
                String semana = switch (fecha.getDayOfWeek()) {
                    case MONDAY -> "LUN";
                    case TUESDAY -> "MAR";
                    case WEDNESDAY -> "MIER";
                    case THURSDAY -> "JUE";
                    case FRIDAY -> "VIE";
                    case SATURDAY -> "SAB";
                    case SUNDAY -> "DOM";
                };
                encabezado.createCell(2 + dia).setCellValue(semana);
                dias.createCell(2 + dia).setCellValue(dia);
            }
            Row trabajador = hoja.createRow(6);
            trabajador.createCell(0).setCellValue(1);
            trabajador.createCell(1).setCellValue(2396);
            trabajador.createCell(2).setCellValue("TRABAJADOR DEMO");
            trabajador.createCell(3).setCellValue(casetaDiaUno);
            // Las filas inferiores con totales no llevan código de trabajador.
            hoja.createRow(7).createCell(2).setCellValue("VIA 101");
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private byte[] conCaseta(String caseta) throws Exception {
        var item = new DistribucionUseCase.MatrizItem(99L, 1L, 2396,
                "HERRERA CALLE OSCAR MARTIN", "P4", inicio, "C", null, "CASETA");
        var reporte = new DistribucionUseCase.ReportePlaza(3L, "P4", inicio,
                inicio.plusDays(6), List.of(item));
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(exportador.generarMatriz(reporte)));
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            libro.getSheetAt(0).getRow(5).getCell(3).setCellValue(caseta);
            libro.write(salida);
            return salida.toByteArray();
        }
    }
}
