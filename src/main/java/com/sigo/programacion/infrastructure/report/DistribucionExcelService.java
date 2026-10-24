package com.sigo.programacion.infrastructure.report;

import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.out.DistribucionReporteExcelPort;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Misma matriz, orden, colores y fechas que el PDF de distribución por plaza. */
@Component
public class DistribucionExcelService implements DistribucionReporteExcelPort {

    private static final Locale ES_PE = new Locale("es", "PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", ES_PE);

    private static final String LEYENDA =
            "Colores por turno: A 06:00–14:00 · B 14:00–22:00 · C 22:00–06:00 · "
                    + "D descanso · V vacaciones · COM compensación · DM descanso médico · LIC licencia";

    @Override
    public byte[] generarMatriz(DistribucionUseCase.ReportePlaza reporte) {
        if (reporte.desde() == null || reporte.hasta() == null
                || reporte.desde().isAfter(reporte.hasta())) {
            throw new IllegalArgumentException("El periodo de distribución no es válido");
        }

        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Map<Long, List<DistribucionUseCase.MatrizItem>> trabajadores =
                    reporte.items().stream().collect(Collectors.groupingBy(
                            DistribucionUseCase.MatrizItem::trabajadorId,
                            LinkedHashMap::new,
                            Collectors.toList()));

            List<List<LocalDate>> semanas = dividirEnBloquesDeSiete(reporte.desde(), reporte.hasta());
            Estilos estilos = new Estilos(libro);
            for (int i = 0; i < semanas.size(); i++) {
                crearHoja(libro, reporte, trabajadores, semanas.get(i),
                        i + 1, semanas.size(), estilos);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel de distribución", e);
        }
    }

    private static void crearHoja(
            XSSFWorkbook libro,
            DistribucionUseCase.ReportePlaza reporte,
            Map<Long, List<DistribucionUseCase.MatrizItem>> trabajadores,
            List<LocalDate> fechas,
            int numeroSemana, int totalSemanas,
            Estilos estilos) {

        Sheet hoja = libro.createSheet("Semana " + numeroSemana);
        int ultimaColumna = 2 + fechas.size();
        hoja.setDisplayGridlines(false);
        hoja.setDefaultRowHeightInPoints(21);

        hoja.setColumnWidth(0, 7 * 256);    // N°
        hoja.setColumnWidth(1, 11 * 256);   // Cod
        hoja.setColumnWidth(2, 45 * 256);   // Nombre
        for (int col = 3; col <= ultimaColumna; col++) {
            hoja.setColumnWidth(col, 14 * 256);
        }

        Row titulo = hoja.createRow(0);
        titulo.setHeightInPoints(35);
        crearCelda(titulo, 0,
                "DISTRIBUCIÓN " + reporte.desde().format(MES).toUpperCase(ES_PE)
                        + " " + texto(reporte.plazaCodigo()), estilos.titulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, ultimaColumna));

        Row subtitulo = hoja.createRow(1);
        subtitulo.setHeightInPoints(24);
        String rango = "Periodo visible: " + FECHA.format(fechas.get(0))
                + " - " + FECHA.format(fechas.get(fechas.size() - 1));
        if (totalSemanas > 1) {
            rango += "   ·   Página " + numeroSemana + " de " + totalSemanas;
        }
        crearCelda(subtitulo, 0, rango, estilos.subtitulo);
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, ultimaColumna));

        Row encabezado = hoja.createRow(3);
        encabezado.setHeightInPoints(23);
        Row diasNumeros = hoja.createRow(4);
        diasNumeros.setHeightInPoints(23);
        String[] fijas = {"N°", "Cod", "Nombre"};
        for (int columna = 0; columna < 3; columna++) {
            crearCelda(encabezado, columna, fijas[columna], estilos.cabecera);
            crearCelda(diasNumeros, columna, "", estilos.cabecera);
            hoja.addMergedRegion(new CellRangeAddress(3, 4, columna, columna));
        }
        for (int i = 0; i < fechas.size(); i++) {
            LocalDate fecha = fechas.get(i);
            crearCelda(encabezado, 3 + i, diaCorto(fecha), estilos.cabecera);
            crearCelda(diasNumeros, 3 + i,
                    String.valueOf(fecha.getDayOfMonth()), estilos.diaNumero);
        }

        if (trabajadores.isEmpty()) {
            Row vacia = hoja.createRow(5);
            crearCelda(vacia, 0, "No existen programaciones para el periodo seleccionado", estilos.nombre);
            hoja.addMergedRegion(new CellRangeAddress(5, 5, 0, ultimaColumna));
        } else {
            int filaActual = 5;
            int numeroTrabajador = 1;
            for (List<DistribucionUseCase.MatrizItem> items : trabajadores.values()) {
                DistribucionUseCase.MatrizItem base = items.get(0);
                Map<LocalDate, DistribucionUseCase.MatrizItem> porFecha = items.stream()
                        .collect(Collectors.toMap(DistribucionUseCase.MatrizItem::fecha,
                                item -> item, (primero, siguiente) -> primero,
                                LinkedHashMap::new));

                Row fila = hoja.createRow(filaActual++);
                fila.setHeightInPoints(24);
                crearCelda(fila, 0, String.valueOf(numeroTrabajador++), estilos.numero);
                crearCelda(fila, 1,
                        base.codigoTrabajador() == null ? "" : base.codigoTrabajador().toString(),
                        estilos.codigo);
                crearCelda(fila, 2, texto(base.nombreTrabajador()), estilos.nombre);

                for (int i = 0; i < fechas.size(); i++) {
                    DistribucionUseCase.MatrizItem item = porFecha.get(fechas.get(i));
                    if (item == null) {
                        crearCelda(fila, 3 + i, "", estilos.vacia);
                        continue;
                    }
                    String estado = texto(item.estado()).trim().toUpperCase(Locale.ROOT);
                    boolean operativo = estado.equals("A") || estado.equals("B") || estado.equals("C");
                    boolean sinAsignar = operativo && texto(item.ubicacionCodigo()).isBlank();
                    String valor = operativo
                            ? (sinAsignar ? "SIN ASIG." : item.ubicacionCodigo())
                            : estado;
                    crearCelda(fila, 3 + i, valor, estilos.asignacion(estado, sinAsignar));
                }
            }
        }

        int filaLeyenda = 6 + trabajadores.size();
        Row leyenda = hoja.createRow(filaLeyenda);
        leyenda.setHeightInPoints(27);
        crearCelda(leyenda, 0, LEYENDA, estilos.leyenda);
        hoja.addMergedRegion(new CellRangeAddress(filaLeyenda, filaLeyenda, 0, ultimaColumna));

        hoja.createFreezePane(3, 5);
        hoja.setFitToPage(true);
        PrintSetup impresion = hoja.getPrintSetup();
        impresion.setLandscape(true);
        impresion.setPaperSize(PrintSetup.A4_PAPERSIZE);
        impresion.setFitWidth((short) 1);
        impresion.setFitHeight((short) 0);
        hoja.setRepeatingRows(CellRangeAddress.valueOf("$4:$5"));
        libro.setPrintArea(libro.getSheetIndex(hoja), 0, ultimaColumna, 0, filaLeyenda);
    }

    private static List<List<LocalDate>> dividirEnBloquesDeSiete(LocalDate desde, LocalDate hasta) {
        List<List<LocalDate>> bloques = new ArrayList<>();
        List<LocalDate> semana = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            semana.add(fecha);
            if (semana.size() == 7) {
                bloques.add(semana);
                semana = new ArrayList<>();
            }
        }
        if (!semana.isEmpty()) bloques.add(semana);
        return bloques;
    }

    private static String diaCorto(LocalDate fecha) {
        return switch (fecha.getDayOfWeek()) {
            case MONDAY -> "LUN";
            case TUESDAY -> "MAR";
            case WEDNESDAY -> "MIÉ";
            case THURSDAY -> "JUE";
            case FRIDAY -> "VIE";
            case SATURDAY -> "SÁB";
            case SUNDAY -> "DOM";
        };
    }

    private static String texto(String texto) { return texto == null ? "" : texto; }

    private static void crearCelda(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(texto(valor));
        celda.setCellStyle(estilo);
    }

    private static class Estilos {
        final CellStyle titulo, subtitulo, cabecera, diaNumero;
        final CellStyle numero, codigo, nombre, vacia, leyenda;
        final CellStyle turnoA, turnoB, turnoC, descanso, vacaciones;
        final CellStyle compensacion, descansoMedico, licencia, sinAsignar;

        Estilos(XSSFWorkbook libro) {
            titulo = crear(libro, "FFFFFF", "0F172A", 17, true, HorizontalAlignment.CENTER, false);
            subtitulo = crear(libro, "FFFFFF", "475569", 9, false, HorizontalAlignment.CENTER, false);
            cabecera = crear(libro, "ADBED3", "000000", 10, true, HorizontalAlignment.CENTER, true);
            diaNumero = crear(libro, "1F4E79", "FFFFFF", 10, true, HorizontalAlignment.CENTER, true);
            numero = crear(libro, "FFFFFF", "0F172A", 10, true, HorizontalAlignment.CENTER, true);
            codigo = crear(libro, "FFFFFF", "0F172A", 10, false, HorizontalAlignment.CENTER, true);
            nombre = crear(libro, "FFFFFF", "0F172A", 10, false, HorizontalAlignment.LEFT, true);
            vacia = crear(libro, "FFFFFF", "475569", 9, false, HorizontalAlignment.CENTER, true);
            leyenda = crear(libro, "FFFFFF", "475569", 9, false, HorizontalAlignment.LEFT, false);
            turnoA = crear(libro, "C6EFCE", "1F2937", 10, true, HorizontalAlignment.CENTER, true);
            turnoB = crear(libro, "FFEB9C", "1F2937", 10, true, HorizontalAlignment.CENTER, true);
            turnoC = crear(libro, "FFC7CE", "1F2937", 10, true, HorizontalAlignment.CENTER, true);
            descanso = crear(libro, "BFBFBF", "1F2937", 10, true, HorizontalAlignment.CENTER, true);
            vacaciones = crear(libro, "BDD7EE", "1F2937", 10, true, HorizontalAlignment.CENTER, true);
            compensacion = crear(libro, "D9D2E9", "1F2937", 10, true, HorizontalAlignment.CENTER, true);
            descansoMedico = crear(libro, "F4CCCC", "1F2937", 10, true, HorizontalAlignment.CENTER, true);
            licencia = crear(libro, "EAD1DC", "1F2937", 10, true, HorizontalAlignment.CENTER, true);
            sinAsignar = crear(libro, "FFE6E6", "B91C1C", 9, true, HorizontalAlignment.CENTER, true);
        }

        CellStyle asignacion(String estado, boolean faltaCaseta) {
            if (faltaCaseta) return sinAsignar;
            return switch (estado) {
                case "A" -> turnoA;
                case "B" -> turnoB;
                case "C" -> turnoC;
                case "D" -> descanso;
                case "V" -> vacaciones;
                case "COM" -> compensacion;
                case "DM" -> descansoMedico;
                case "LIC" -> licencia;
                default -> vacia;
            };
        }

        private static XSSFCellStyle crear(
                XSSFWorkbook libro, String fondo, String colorTexto,
                int tamano, boolean negrita, HorizontalAlignment alineacion,
                boolean bordes) {
            XSSFCellStyle estilo = libro.createCellStyle();
            estilo.setFillForegroundColor(color(fondo));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estilo.setAlignment(alineacion);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);
            estilo.setWrapText(false);
            if (bordes) {
                estilo.setBorderTop(BorderStyle.THIN);
                estilo.setBorderBottom(BorderStyle.THIN);
                estilo.setBorderLeft(BorderStyle.THIN);
                estilo.setBorderRight(BorderStyle.THIN);
                estilo.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                estilo.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                estilo.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                estilo.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
            }
            Font fuente = libro.createFont();
            fuente.setFontName("Arial");
            fuente.setFontHeightInPoints((short) tamano);
            fuente.setBold(negrita);
            ((org.apache.poi.xssf.usermodel.XSSFFont) fuente).setColor(color(colorTexto));
            estilo.setFont(fuente);
            return estilo;
        }

        private static XSSFColor color(String rgb) {
            byte[] componentes = new byte[] {
                    (byte) Integer.parseInt(rgb.substring(0, 2), 16),
                    (byte) Integer.parseInt(rgb.substring(2, 4), 16),
                    (byte) Integer.parseInt(rgb.substring(4, 6), 16)
            };
            return new XSSFColor(componentes, new DefaultIndexedColorMap());
        }
    }
}
