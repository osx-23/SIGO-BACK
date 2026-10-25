package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.ListarTurnosUseCase;
import com.sigo.programacion.domain.ProgramacionValidationException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgramacionExcelTextoServiceTest {
    private final ListarTurnosUseCase turnos = mock(ListarTurnosUseCase.class);
    private final TrabajadorUseCase trabajadores = mock(TrabajadorUseCase.class);
    private final ProgramacionExcelTextoService servicio = new ProgramacionExcelTextoService(turnos, trabajadores);

    @BeforeEach
    void preparar() {
        when(turnos.listar(3L, 2026, 10)).thenReturn(List.of(
                new ListarTurnosUseCase.Turno(10L, 7L, 2396, "PRUEBA", 3L,
                        "P4", LocalDate.of(2026, 10, 1), "B"),
                new ListarTurnosUseCase.Turno(11L, 7L, 2396, "PRUEBA", 3L,
                        "P4", LocalDate.of(2026, 10, 2), "A"),
                new ListarTurnosUseCase.Turno(12L, 7L, 2396, "PRUEBA", 3L,
                        "P4", LocalDate.of(2026, 10, 3), "D")
        ));
        var persona = new TrabajadorUseCase.TrabajadorData(7L, 2396, "PRUEBA", null,
                new TrabajadorUseCase.PlazaData(3L, "P4", "Plaza P4", true),
                "OPERADOR", false, true);
        when(trabajadores.listarAgentesPorPlaza(3L)).thenReturn(List.of(persona));
        when(trabajadores.listarControladoresPorPlaza(3L)).thenReturn(List.of());
    }

    @Test
    void aplicaSoloTextosValidosYGuionSignificaSinTurno() throws Exception {
        var previa = servicio.previsualizar(3L, 2026, 10,
                mensual("PROGRAMACION OCTUBRE P4", "a", "-", "D", ""));
        assertEquals(3, previa.leidos());
        assertEquals(1, previa.coinciden());
        assertEquals(2, previa.cambios().size());
        assertEquals("A", previa.cambios().get(0).nuevo());
        assertNull(previa.cambios().get(1).nuevo());
        assertTrue(previa.errores().isEmpty());
    }

    @Test
    void celdaVaciaNoEliminaProgramacion() throws Exception {
        var previa = servicio.previsualizar(3L, 2026, 10,
                mensual("PROGRAMACION OCTUBRE P4", "", "", "DM", ""));
        assertEquals(1, previa.leidos());
        assertEquals(1, previa.cambios().size());
        assertEquals(LocalDate.of(2026, 10, 3), previa.cambios().get(0).fecha());
    }

    @Test
    void rechazaTextoDeCasetasYSoloAdmiteEstadosDeTurno() throws Exception {
        var previa = servicio.previsualizar(3L, 2026, 10,
                mensual("PROGRAMACION OCTUBRE P4", "101", "COM", "", ""));
        assertEquals(1, previa.errores().size());
        assertTrue(previa.errores().get(0).contains("101"));
        assertEquals(1, previa.cambios().size());
    }

    @Test
    void evitaImportarArchivoDeOtraPlazaOMes() throws Exception {
        assertThrows(ProgramacionValidationException.class,
                () -> servicio.previsualizar(3L, 2026, 10,
                        mensual("PROGRAMACION SEPTIEMBRE P4", "A", "", "", "")));
        assertThrows(ProgramacionValidationException.class,
                () -> servicio.previsualizar(3L, 2026, 10,
                        mensual("PROGRAMACION OCTUBRE P5", "A", "", "", "")));
    }

    @Test
    void leeMatrizSemanalComoLaDeSigo() throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Semana 1");
            hoja.createRow(0).createCell(0).setCellValue("PROGRAMACIÓN OCTUBRE P4");
            Row h = hoja.createRow(3);
            h.createCell(1).setCellValue("Cod");
            h.createCell(2).setCellValue("Nombre");
            Row dias = hoja.createRow(4);
            dias.createCell(3).setCellValue(1);
            Row fila = hoja.createRow(5);
            fila.createCell(1).setCellValue(2396);
            fila.createCell(2).setCellValue("PRUEBA");
            fila.createCell(3).setCellValue("LIC");
            libro.write(out);
            var previa = servicio.previsualizar(3L, 2026, 10, out.toByteArray());
            assertEquals(1, previa.cambios().size());
            assertEquals("LIC", previa.cambios().get(0).nuevo());
        }
    }

    private byte[] mensual(String titulo, String d1, String d2, String d3, String d4) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Hoja1");
            hoja.createRow(1).createCell(1).setCellValue(titulo);
            Row encabezados = hoja.createRow(4);
            encabezados.createCell(1).setCellValue("Cod");
            encabezados.createCell(2).setCellValue("Nombre");
            Row fechas = hoja.createRow(5);
            YearMonth mes = YearMonth.of(2026, 10);
            for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
                fechas.createCell(dia + 2).setCellValue(dia);
            }
            Row fila = hoja.createRow(6);
            fila.createCell(1).setCellValue(2396);
            fila.createCell(2).setCellValue("PRUEBA");
            fila.createCell(3).setCellValue(d1);
            fila.createCell(4).setCellValue(d2);
            fila.createCell(5).setCellValue(d3);
            fila.createCell(6).setCellValue(d4);
            libro.write(out);
            return out.toByteArray();
        }
    }
}
