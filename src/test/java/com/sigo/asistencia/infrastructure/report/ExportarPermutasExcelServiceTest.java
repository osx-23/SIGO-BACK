package com.sigo.asistencia.infrastructure.report;

import com.sigo.asistencia.application.port.in.PermutaUseCase;
import com.sigo.asistencia.application.port.in.PermutaUseCase.PermutaDetalle;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportarPermutasExcelServiceTest {
    @Mock PermutaUseCase permutas;
    @Mock PlazaRepository plazas;
    @InjectMocks ExportarPermutasExcelService exportador;

    @Test
    void exportaResumenAnualYRegistroFiltradoConFechasExcel() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 30);
        LocalDate fecha = LocalDate.of(2026, 11, 16);
        var registro = new PermutaDetalle(
                5L, 4L, "P4", 2214, "MENDIVIL", 1L, "A", fecha,
                4L, "P4", 2393, "HUAMAN", 2L, "B", fecha);
        when(permutas.listar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                null, null, null)).thenReturn(List.of(registro));
        when(permutas.listar(inicio, fin, 4L, 2214, 1L)).thenReturn(List.of(registro));
        Plaza plaza = new Plaza();
        plaza.setId(4L);
        plaza.setCodigo("P4");
        when(plazas.findAll()).thenReturn(List.of(plaza));

        byte[] contenido = exportador.exportar(2026, inicio, fin, 4L, 1L, 2214);
        assertTrue(contenido.length > 1000);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            assertEquals(2, wb.getNumberOfSheets());
            assertNotNull(wb.getSheet("RESUMEN"));
            assertNotNull(wb.getSheet("REGISTRO"));
            assertEquals("Nov", wb.getSheet("RESUMEN").getRow(16).getCell(0).getStringCellValue());
            assertEquals(1, wb.getSheet("RESUMEN").getRow(16).getCell(2).getNumericCellValue());
            assertEquals("Plaza solicitante", wb.getSheet("REGISTRO").getRow(3).getCell(0).getStringCellValue());
            assertEquals("P4", wb.getSheet("REGISTRO").getRow(4).getCell(0).getStringCellValue());
            assertEquals(2214, wb.getSheet("REGISTRO").getRow(4).getCell(1).getNumericCellValue());
            assertEquals(CellType.NUMERIC, wb.getSheet("REGISTRO").getRow(4).getCell(4).getCellType());
            assertEquals(fecha, wb.getSheet("REGISTRO").getRow(4).getCell(4)
                    .getLocalDateTimeCellValue().toLocalDate());
        }
    }
}
