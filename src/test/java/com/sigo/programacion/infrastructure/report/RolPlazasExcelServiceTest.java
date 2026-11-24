package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.*;
import com.sigo.programacion.domain.ProgramacionValidationException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RolPlazasExcelServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TrabajadorUseCase trabajadores = mock(TrabajadorUseCase.class);
    private final ListarTurnosUseCase turnos = mock(ListarTurnosUseCase.class);
    private final GuardarTurnosUseCase guardarTurnos = mock(GuardarTurnosUseCase.class);
    private final ListarSecuenciasUseCase listarSecuencias = mock(ListarSecuenciasUseCase.class);
    private final AsignarSecuenciaUseCase asignarSecuencia = mock(AsignarSecuenciaUseCase.class);
    private final GuardarOrdenSecuenciaUseCase guardarOrden = mock(GuardarOrdenSecuenciaUseCase.class);
    private final ListarLideresUseCase listarLideres = mock(ListarLideresUseCase.class);
    private final AsignarLiderUseCase asignarLider = mock(AsignarLiderUseCase.class);
    private final RolPlazasExcelService lector = new RolPlazasExcelService(
            jdbc, trabajadores, turnos, guardarTurnos,
            listarSecuencias, asignarSecuencia, guardarOrden, listarLideres, asignarLider);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(4L))).thenReturn(List.of("P4"));
        when(turnos.listar(4L, 2026, 10)).thenReturn(List.of());
        var activos = new ArrayList<TrabajadorUseCase.TrabajadorData>();
        for (int i = 0; i < 5; i++) {
            activos.add(persona(1001L + i, 1001 + i, "AGENTE " + (i + 1), "OPERADOR"));
        }
        var controlador = persona(550L, 550, "CONTROLADOR P4", "CONTROLADOR");
        when(trabajadores.listarAgentesPorPlaza(4L)).thenReturn(activos);
        when(trabajadores.listarControladoresPorPlaza(4L)).thenReturn(List.of(controlador));
        var todos = new ArrayList<>(activos);
        todos.add(controlador);
        when(trabajadores.listarAdministracion(null)).thenReturn(todos);
    }

    @Test
    void reconoceCuatroSecuenciasPartTimeLideresYTurnosSoloP4() throws Exception {
        byte[] archivo = libroConP4YP5();
        var previa = lector.previsualizar(4L, 2026, 10, archivo);
        assertEquals("P4", previa.plazaCodigo());
        assertEquals(1, previa.hojasIgnoradas());
        assertEquals(6, previa.personal().size()); // Cinco agentes y un controlador P4.
        assertEquals(6, previa.celdasLeidas());
        assertEquals(6, previa.cambios().size());
        assertEquals("SECUENCIA_1", previa.personal().get(0).grupo());
        assertEquals("SECUENCIA_2", previa.personal().get(1).grupo());
        assertEquals("SECUENCIA_3", previa.personal().get(2).grupo());
        assertEquals("SECUENCIA_4", previa.personal().get(3).grupo());
        assertEquals("PART_TIME", previa.personal().get(4).grupo());
        assertEquals(550, previa.personal().get(0).controladorCodigo());
        assertEquals(550L, previa.personal().get(0).controladorId());
        assertEquals(5, previa.semanasPartTime().size());
        assertEquals(1, previa.semanasPartTime().get(0).desde().getDayOfMonth());
        assertEquals(4, previa.semanasPartTime().get(0).hasta().getDayOfMonth());
        assertEquals(5, previa.semanasPartTime().get(1).desde().getDayOfMonth());
        assertEquals(26, previa.semanasPartTime().get(4).desde().getDayOfMonth());
        assertTrue(previa.personal().stream().noneMatch(p -> p.codigo() == 249));
        assertTrue(previa.cambios().stream().noneMatch(t -> t.codigo() == 249));
        assertEquals("A", previa.cambios().get(0).nuevo()); // AP -> A
        verifyNoInteractions(guardarTurnos, asignarSecuencia, asignarLider);
    }

    @Test
    void rechazaImportarUnaPlazaSinHojaPropia() throws Exception {
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            libro.createSheet("AGENTES P5");
            libro.write(salida);
            assertThrows(ProgramacionValidationException.class,
                    () -> lector.previsualizar(4L, 2026, 10, salida.toByteArray()));
        }
    }

    private static TrabajadorUseCase.TrabajadorData persona(Long id, int codigo,
                                                            String nombre, String rol) {
        return new TrabajadorUseCase.TrabajadorData(
                id, codigo, nombre,
                new TrabajadorUseCase.PuestoData(rol.equals("CONTROLADOR") ? 2L : 1L, rol),
                new TrabajadorUseCase.PlazaData(4L, "P4", "Plaza P4", true),
                rol, false, true);
    }

    private static byte[] libroConP4YP5() throws Exception {
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            Sheet p4 = libro.createSheet("AGENTES P4 ");
            Sheet p5 = libro.createSheet("AGENTES P5 ");
            prepararCabecera(p4);
            prepararCabecera(p5);
            CellStyle separador = libro.createCellStyle();
            separador.setBorderTop(BorderStyle.MEDIUM);
            CellStyle semana = libro.createCellStyle();
            semana.setBorderLeft(BorderStyle.MEDIUM);
            for (int i = 0; i < 5; i++) {
                Row fila = p4.createRow(3 + i);
                var codigo = fila.createCell(1);
                codigo.setCellValue(1001 + i);
                codigo.setCellStyle(separador);
                fila.createCell(2).setCellValue("AGENTE " + (i + 1));
                fila.createCell(3).setCellValue(550);
                fila.createCell(4).setCellValue("CONTROLADOR P4");
                fila.createCell(8).setCellValue(i == 0 ? "AP" : "B");
                if (i == 4) {
                    for (int col : new int[]{8, 12, 19, 26, 33}) {
                        if (col != 8) fila.createCell(col);
                        fila.getCell(col).setCellStyle(semana);
                    }
                }
            }
            Row ajeno = p5.createRow(3);
            ajeno.createCell(1).setCellValue(9999);
            ajeno.createCell(2).setCellValue("AGENTE P5");
            ajeno.createCell(8).setCellValue("C");

            Sheet control = libro.createSheet("CONTROL ");
            Row cabeceraP4 = control.createRow(2);
            cabeceraP4.createCell(1).setCellValue("PILAR DE GESTION \"P4\"");
            cabeceraP4.createCell(2).setCellValue("COD.");
            cabeceraP4.createCell(3).setCellValue("NOMBRES Y APELLIDOS");
            cabeceraP4.createCell(4).setCellValue("J");
            Row dias = control.createRow(3);
            for (int dia = 1; dia <= 31; dia++) dias.createCell(3 + dia).setCellValue(dia);
            Row p4Control = control.createRow(4);
            p4Control.createCell(2).setCellValue(550);
            p4Control.createCell(3).setCellValue("CONTROLADOR P4");
            p4Control.createCell(4).setCellValue("B");
            control.createRow(8).createCell(1).setCellValue("PILAR DE GESTION \"P5\"");
            Row p5Control = control.createRow(10);
            p5Control.createCell(2).setCellValue(249);
            p5Control.createCell(3).setCellValue("CONTROLADOR P5");
            p5Control.createCell(4).setCellValue("B");
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private static void prepararCabecera(Sheet hoja) {
        Row nombres = hoja.createRow(1);
        nombres.createCell(1).setCellValue("COD.");
        nombres.createCell(2).setCellValue("NOMBRES Y APELLIDOS");
        nombres.createCell(8).setCellValue("J"); // 01/10/2026 jueves.
        Row dias = hoja.createRow(2);
        dias.createCell(5).setCellValue(28);
        dias.createCell(6).setCellValue(29);
        dias.createCell(7).setCellValue(30);
        for (int dia = 1; dia <= 31; dia++) dias.createCell(7 + dia).setCellValue(dia);
    }
}
