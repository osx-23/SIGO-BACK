package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.in.UbicacionUseCase;
import com.sigo.programacion.domain.ProgramacionValidationException;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DistribucionExcelTurnosPreviewServiceTest {
    private final DistribucionUseCase distribucion = mock(DistribucionUseCase.class);
    private final UbicacionUseCase ubicaciones = mock(UbicacionUseCase.class);
    private final TrabajadorUseCase trabajadores = mock(TrabajadorUseCase.class);
    private final DistribucionExcelTurnosPreviewService servicio =
            new DistribucionExcelTurnosPreviewService(distribucion, ubicaciones, trabajadores);

    @BeforeEach
    void preparar() {
        LocalDate sep = LocalDate.of(2026, 9, 1);
        when(distribucion.reportePlaza(3L, sep, sep.plusDays(29)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(3L, "P4", sep,
                        sep.plusDays(29), List.of()));
        when(trabajadores.listarAdministracion(3L)).thenReturn(List.of(
                new TrabajadorUseCase.TrabajadorData(1L, 2396, "TRABAJADOR DEMO", null, null, null, false, true)));
        when(ubicaciones.listarActivas(3L)).thenReturn(List.of(
                new UbicacionUseCase.Ubicacion(17L, 3L, "104", "Caseta 104", "CASETA", null, true, 1, true, true, true)));
    }

    @Test
    void reconoceTresColoresYProponeCrearSinGuardar() throws Exception {
        var vista = servicio.previsualizar(3L,
                archivo(new String[]{"104", "104", "104"}, new String[]{"C6EFCE", "FFEB9C", "FFC7CE"}), 2026, 9);
        assertEquals(3, vista.crear());
        assertEquals("A", vista.propuestas().get(0).turnoExcel());
        assertEquals("B", vista.propuestas().get(1).turnoExcel());
        assertEquals("C", vista.propuestas().get(2).turnoExcel());
        verify(distribucion, never()).guardar(any(), anyBoolean());
        verify(distribucion, never()).guardar(any());
    }

    @Test
    void celdaMensualVaciaPeroColoreadaCreaTurnoSinCaseta() throws Exception {
        var vista = servicio.previsualizar(3L,
                archivo(new String[]{""}, new String[]{"C6EFCE"}), 2026, 9);
        assertEquals(1, vista.crear());
        assertEquals("A", vista.propuestas().get(0).turnoExcel());
        assertEquals("SIN ASIG.", vista.propuestas().get(0).valorExcel());
        verify(distribucion, never()).guardar(any(), anyBoolean());
    }

    @Test
    void celdaMensualVaciaYSinColorNoInventaTurno() throws Exception {
        var vista = servicio.previsualizar(3L,
                archivo(new String[]{""}, new String[]{null}), 2026, 9);
        assertEquals(0, vista.leidas());
        assertEquals(0, vista.crear());
    }

    @Test
    void admiteMatrizSemanalConTurnoColoreadoSinCaseta() throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet("Semana 1");
            hoja.createRow(0).createCell(0).setCellValue("DISTRIBUCIÓN SEPTIEMBRE P4");
            hoja.createRow(1).createCell(0).setCellValue("Periodo visible: 01/09/2026 - 07/09/2026");
            Row cabecera = hoja.createRow(3);
            cabecera.createCell(1).setCellValue("Cod");
            cabecera.createCell(2).setCellValue("Nombre");
            Row numeros = hoja.createRow(4);
            String[] dias = {"MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM", "LUN"};
            for (int i = 0; i < 7; i++) {
                cabecera.createCell(3 + i).setCellValue(dias[i]);
                numeros.createCell(3 + i).setCellValue(i + 1);
            }
            Row trabajador = hoja.createRow(5);
            trabajador.createCell(1).setCellValue("2396");
            trabajador.createCell(2).setCellValue("TRABAJADOR DEMO");
            var celda = trabajador.createCell(3);
            celda.setCellValue("");
            var estilo = libro.createCellStyle();
            estilo.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xC6, (byte) 0xEF, (byte) 0xCE}, null));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            celda.setCellStyle(estilo);
            libro.write(salida);
            var vista = servicio.previsualizar(3L, salida.toByteArray(), 2026, 9);
            assertEquals(1, vista.crear());
            assertEquals("A", vista.propuestas().get(0).turnoExcel());
        }
    }

    @Test
    void admiteMatrizSemanalExportadaPorSigo() throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet("Semana 1");
            hoja.createRow(0).createCell(0).setCellValue("DISTRIBUCIÓN SEPTIEMBRE P4");
            hoja.createRow(1).createCell(0).setCellValue("Periodo visible: 01/09/2026 - 07/09/2026");
            var cabecera = hoja.createRow(3);
            cabecera.createCell(1).setCellValue("Cod");
            cabecera.createCell(2).setCellValue("Nombre");
            var numeros = hoja.createRow(4);
            String[] dias = {"MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM", "LUN"};
            for (int i = 0; i < 7; i++) {
                cabecera.createCell(3 + i).setCellValue(dias[i]);
                numeros.createCell(3 + i).setCellValue(i + 1);
            }
            Row trabajador = hoja.createRow(5);
            trabajador.createCell(1).setCellValue("2396");
            trabajador.createCell(2).setCellValue("TRABAJADOR DEMO");
            var celda = trabajador.createCell(3);
            celda.setCellValue("104");
            var estilo = libro.createCellStyle();
            estilo.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xC6, (byte) 0xEF, (byte) 0xCE}, null));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            celda.setCellStyle(estilo);
            libro.write(salida);
            var vista = servicio.previsualizar(3L, salida.toByteArray(), 2026, 9);
            assertEquals(1, vista.crear());
            assertEquals("A", vista.propuestas().get(0).turnoExcel());
        }
    }

    @Test
    void casetaDiferenteNoCuentaComoCambioDeTurno() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 9, 1);
        when(distribucion.reportePlaza(3L, fecha, fecha.plusDays(29)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(3L, "P4", fecha, fecha.plusDays(29),
                        List.of(new DistribucionUseCase.MatrizItem(21L, 1L, 2396,
                                "TRABAJADOR DEMO", "P4", fecha, "A", "105", "CASETA"))));
        var vista = servicio.previsualizar(3L,
                archivo(new String[]{"104"}, new String[]{"C6EFCE"}), 2026, 9);
        assertEquals(0, vista.actualizar());
        assertEquals(1, vista.coinciden());
        assertEquals("COINCIDE", vista.propuestas().get(0).accion());
        verify(distribucion, never()).guardar(any(), anyBoolean());
    }

    @Test
    void diferenteTurnoSiCuentaComoActualizacion() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 9, 1);
        when(distribucion.reportePlaza(3L, fecha, fecha.plusDays(29)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(3L, "P4", fecha, fecha.plusDays(29),
                        List.of(new DistribucionUseCase.MatrizItem(21L, 1L, 2396,
                                "TRABAJADOR DEMO", "P4", fecha, "B", "104", "CASETA"))));
        var vista = servicio.previsualizar(3L,
                archivo(new String[]{"104"}, new String[]{"C6EFCE"}), 2026, 9);
        assertEquals(1, vista.actualizar());
        assertEquals("ACTUALIZAR", vista.propuestas().get(0).accion());
    }

    @Test
    void casetaDesconocidaNoImpideProgramarPrimeroTurno() throws Exception {
        var vista = servicio.previsualizar(3L,
                archivo(new String[]{"999"}, new String[]{"C6EFCE"}), 2026, 9);
        assertEquals(1, vista.crear());
        assertEquals("A", vista.propuestas().get(0).turnoExcel());
    }

    @Test
    void creaDescansoMedicoDelExcelSinCaseta() throws Exception {
        var vista = servicio.previsualizar(3L, archivo(new String[]{"DM"}, new String[]{null}), 2026, 9);
        assertEquals(1, vista.crear());
        assertEquals(0, vista.revisar());
        assertEquals("DM", vista.propuestas().get(0).turnoExcel());
        assertEquals("DM", vista.propuestas().get(0).valorExcel());
    }

    @Test
    void alertaColorDesconocidoSinInventarTurno() throws Exception {
        var vista = servicio.previsualizar(3L, archivo(new String[]{"104"}, new String[]{"FFFFFF"}), 2026, 9);
        assertEquals(1, vista.revisar());
        assertNull(vista.propuestas().get(0).turnoExcel());
        assertEquals("REVISAR", vista.propuestas().get(0).accion());
    }

    @Test
    void noProponeModificarRegistrosDeTrabajadorInactivo() throws Exception {
        when(trabajadores.listarAdministracion(3L)).thenReturn(List.of(
                new TrabajadorUseCase.TrabajadorData(1L, 2396, "TRABAJADOR DEMO", null, null, null, false, false)));
        var vista = servicio.previsualizar(3L, archivo(new String[]{"104"}, new String[]{"C6EFCE"}), 2026, 9);
        assertEquals(0, vista.crear());
        assertEquals(1, vista.revisar());
        assertTrue(vista.propuestas().get(0).detalle().contains("inactivo"));
    }

    @Test
    void excelConfirmadoProponeReemplazarDescansoMedicoAnterior() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 9, 1);
        when(distribucion.reportePlaza(3L, fecha, fecha.plusDays(29)))
                .thenReturn(new DistribucionUseCase.ReportePlaza(3L, "P4", fecha, fecha.plusDays(29),
                        List.of(new DistribucionUseCase.MatrizItem(21L, 1L, 2396,
                                "TRABAJADOR DEMO", "P4", fecha, "DM", null, "CASETA"))));
        var vista = servicio.previsualizar(3L,
                archivo(new String[]{"104"}, new String[]{"C6EFCE"}), 2026, 9);
        assertEquals("ACTUALIZAR", vista.propuestas().get(0).accion());
        assertEquals(1, vista.actualizar());
    }

    @Test
    void noAceptaAnioConEncabezadoIncompatible() throws Exception {
        byte[] b = archivo(new String[]{"104"}, new String[]{"C6EFCE"});
        assertThrows(ProgramacionValidationException.class, () -> servicio.previsualizar(3L, b, 2025, 9));
    }

    private byte[] archivo(String[] valores, String[] colores) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet("Hoja1");
            hoja.createRow(1).createCell(1).setCellValue("DISTRIBUCION SETIEMBRE P4");
            Row encabezado = hoja.createRow(4);
            encabezado.createCell(1).setCellValue("Cod");
            encabezado.createCell(2).setCellValue("Nombre");
            Row numeros = hoja.createRow(5);
            YearMonth septiembre = YearMonth.of(2026, 9);
            for (int dia = 1; dia <= 30; dia++) {
                var f = septiembre.atDay(dia);
                String diaTexto = switch (f.getDayOfWeek()) {
                    case MONDAY -> "LUN"; case TUESDAY -> "MAR"; case WEDNESDAY -> "MIER";
                    case THURSDAY -> "JUE"; case FRIDAY -> "VIE"; case SATURDAY -> "SAB"; case SUNDAY -> "DOM";
                };
                encabezado.createCell(2 + dia).setCellValue(diaTexto);
                numeros.createCell(2 + dia).setCellValue(dia);
            }
            Row trabajador = hoja.createRow(6);
            trabajador.createCell(1).setCellValue(2396);
            trabajador.createCell(2).setCellValue("TRABAJADOR DEMO");
            for (int i = 0; i < valores.length; i++) {
                var c = trabajador.createCell(3 + i);
                c.setCellValue(valores[i]);
                if (colores[i] != null) {
                    var rgb = colores[i];
                    byte[] color = new byte[]{(byte) Integer.parseInt(rgb.substring(0, 2), 16),
                            (byte) Integer.parseInt(rgb.substring(2, 4), 16),
                            (byte) Integer.parseInt(rgb.substring(4, 6), 16)};
                    XSSFCellStyle estilo = libro.createCellStyle();
                    estilo.setFillForegroundColor(new XSSFColor(color, null));
                    estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    c.setCellStyle(estilo);
                }
            }
            libro.write(out);
            return out.toByteArray();
        }
    }
}
