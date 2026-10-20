package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.domain.InventarioEstado;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventarioExcelServiceTest {
    @Test
    void exportaResumenYProductosComoExcelLegible() throws Exception {
        var datos = new InventarioConsultaUseCase.Detalle(
                42L, 1L, "P1", 11L, 1234, "Operador", "CONTROLADOR",
                OffsetDateTime.parse("2026-10-19T13:00:00-05:00"),
                OffsetDateTime.parse("2026-10-19T13:20:00-05:00"),
                InventarioEstado.FINALIZADO, "Revisado", null,
                List.of(new InventarioConsultaUseCase.DetalleItem(
                        8L, "Conos de seguridad", "unidad", "Seguridad", "PLAZA", new BigDecimal("12.5")))
        );

        byte[] contenido = new InventarioExcelService().generar(datos);
        assertTrue(contenido.length > 1000);

        try (var libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            assertEquals(2, libro.getNumberOfSheets());
            assertEquals("SIGO - REPORTE DE INVENTARIO", libro.getSheet("Resumen").getRow(0).getCell(0).getStringCellValue());
            assertEquals("P1", libro.getSheet("Resumen").getRow(3).getCell(1).getStringCellValue());
            assertEquals("Conos de seguridad", libro.getSheet("Productos").getRow(1).getCell(1).getStringCellValue());
            assertEquals(12.5, libro.getSheet("Productos").getRow(1).getCell(5).getNumericCellValue());
        }
    }
}
