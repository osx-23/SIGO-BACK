package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class InventarioExcelService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generar(InventarioConsultaUseCase.Detalle inventario) {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle titulo = estilo(libro, IndexedColors.DARK_BLUE, true);
            CellStyle cabecera = estilo(libro, IndexedColors.BLUE, true);
            CellStyle regular = libro.createCellStyle();
            regular.setVerticalAlignment(VerticalAlignment.CENTER);
            CellStyle cantidad = libro.createCellStyle();
            cantidad.cloneStyleFrom(regular);
            cantidad.setDataFormat(libro.createDataFormat().getFormat("#,##0.##"));

            Sheet resumen = libro.createSheet("Resumen");
            resumen.createFreezePane(0, 1);
            Row encabezado = resumen.createRow(0);
            celda(encabezado, 0, "SIGO - REPORTE DE INVENTARIO", titulo);
            resumen.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

            int indice = 2;
            indice = campo(resumen, indice, "Inventario", String.valueOf(inventario.id()), regular);
            indice = campo(resumen, indice, "Plaza", inventario.plaza(), regular);
            indice = campo(resumen, indice, "Responsable", inventario.responsable(), regular);
            indice = campo(resumen, indice, "Código responsable",
                    inventario.codigoResponsable() == null ? "" : String.valueOf(inventario.codigoResponsable()), regular);
            indice = campo(resumen, indice, "Rol", inventario.rol(), regular);
            indice = campo(resumen, indice, "Estado", inventario.estado() == null ? "" : inventario.estado().name(), regular);
            indice = campo(resumen, indice, "Inicio", fecha(inventario.fechaInicio()), regular);
            indice = campo(resumen, indice, "Finalización", fecha(inventario.fechaFinalizacion()), regular);
            indice = campo(resumen, indice, "Total de productos",
                    String.valueOf(inventario.productos() == null ? 0 : inventario.productos().size()), regular);
            indice = campo(resumen, indice, "Observación", inventario.observacion(), regular);
            campo(resumen, indice, "Motivo de anulación", inventario.motivoAnulacion(), regular);
            resumen.setColumnWidth(0, 24 * 256);
            resumen.setColumnWidth(1, 65 * 256);

            Sheet productos = libro.createSheet("Productos");
            productos.createFreezePane(0, 1);
            Row columnas = productos.createRow(0);
            String[] titulos = {"ID producto", "Material", "Categoría", "Ámbito", "Unidad", "Cantidad"};
            for (int i = 0; i < titulos.length; i++) {
                celda(columnas, i, titulos[i], cabecera);
            }
            if (inventario.productos() != null) {
                int fila = 1;
                for (var producto : inventario.productos()) {
                    Row row = productos.createRow(fila++);
                    numero(row, 0, producto.productoId(), regular);
                    celda(row, 1, producto.nombre(), regular);
                    celda(row, 2, producto.categoria(), regular);
                    celda(row, 3, producto.ambito(), regular);
                    celda(row, 4, producto.unidad(), regular);
                    if (producto.cantidad() != null) {
                        Cell numero = row.createCell(5);
                        numero.setCellValue(producto.cantidad().doubleValue());
                        numero.setCellStyle(cantidad);
                    }
                }
            }
            productos.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, Math.max(0, productos.getLastRowNum()), 0, 5));
            int[] anchos = {16, 50, 27, 27, 18, 20};
            for (int i = 0; i < anchos.length; i++) {
                productos.setColumnWidth(i, anchos[i] * 256);
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte Excel del inventario", e);
        }
    }

    private int campo(Sheet hoja, int indice, String nombre, String valor, CellStyle estilo) {
        Row fila = hoja.createRow(indice);
        celda(fila, 0, nombre, estilo);
        celda(fila, 1, valor, estilo);
        return indice + 1;
    }

    private void celda(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna, CellType.STRING);
        celda.setCellValue(valor == null ? "" : valor);
        celda.setCellStyle(estilo);
    }

    private void numero(Row fila, int columna, Long valor, CellStyle estilo) {
        if (valor == null) {
            celda(fila, columna, "", estilo);
            return;
        }
        Cell celda = fila.createCell(columna, CellType.NUMERIC);
        celda.setCellValue(valor.doubleValue());
        celda.setCellStyle(estilo);
    }

    private CellStyle estilo(Workbook libro, IndexedColors fondo, boolean blanco) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setFillForegroundColor(fondo.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(blanco ? IndexedColors.WHITE.getIndex() : IndexedColors.BLACK.getIndex());
        estilo.setFont(fuente);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        return estilo;
    }

    private String fecha(OffsetDateTime valor) {
        return valor == null ? "" : FECHA.format(valor.atZoneSameInstant(LIMA));
    }
}
