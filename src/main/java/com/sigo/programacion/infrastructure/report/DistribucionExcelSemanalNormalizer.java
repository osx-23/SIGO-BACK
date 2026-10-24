package com.sigo.programacion.infrastructure.report;

import com.sigo.programacion.domain.ProgramacionValidationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts SIGO's Semana 1, Semana 2, ... matrix to the original monthly matrix.
 * This normalizes only the file in memory. It never writes to PostgreSQL.
 */
public final class DistribucionExcelSemanalNormalizer {
    private static final Pattern RANGE = Pattern.compile(
            "Periodo visible:\\s*(\\d{2}/\\d{2}/\\d{4})\\s*-\\s*(\\d{2}/\\d{2}/\\d{4})",
            Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final int MAX_HOJAS = 10;
    private static final int MAX_FILAS = 350;

    private DistribucionExcelSemanalNormalizer() {}

    public static byte[] normalizar(byte[] archivo, int anio, int mes) {
        if (archivo == null || archivo.length == 0 || archivo.length > 2 * 1024 * 1024) {
            throw new ProgramacionValidationException("Selecciona un Excel .xlsx de máximo 2 MB");
        }
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            if (libro.getNumberOfSheets() == 0 || libro.getNumberOfSheets() > MAX_HOJAS) {
                throw new ProgramacionValidationException("Número de hojas del Excel no permitido");
            }
            if (!"Semana 1".equals(libro.getSheetAt(0).getSheetName())) {
                return archivo; // Already the original monthly file.
            }
            YearMonth periodo = YearMonth.of(anio, mes);
            DataFormatter formato = new DataFormatter(Locale.ROOT);
            Map<String, Row> filas = new LinkedHashMap<>();
            Sheet mensual = libro.createSheet("VistaMensualImportacion");
            String titulo = null;
            LocalDate finalAnterior = null;
            Set<String> visitadas = new HashSet<>();
            Row meses = mensual.createRow(4);
            meses.createCell(1).setCellValue("Cod");
            meses.createCell(2).setCellValue("Nombre");
            Row dias = mensual.createRow(5);
            for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
                LocalDate fecha = periodo.atDay(dia);
                meses.createCell(dia + 2).setCellValue(diaSemana(fecha));
                dias.createCell(dia + 2).setCellValue(dia);
            }
            int hojasOriginales = libro.getNumberOfSheets() - 1;
            for (int i = 0; i < hojasOriginales; i++) {
                Sheet hoja = libro.getSheetAt(i);
                if (!hoja.getSheetName().equals("Semana " + (i + 1))) {
                    throw new ProgramacionValidationException("Las hojas deben llamarse Semana 1, Semana 2, etc.");
                }
                if (hoja.getLastRowNum() > MAX_FILAS) {
                    throw new ProgramacionValidationException("Demasiadas filas en " + hoja.getSheetName());
                }
                String nombreMes = valor(hoja, 0, 0, formato);
                if (titulo == null) titulo = nombreMes;
                if (!sinAcentos(titulo).equals(sinAcentos(nombreMes))) {
                    throw new ProgramacionValidationException("Las hojas pertenecen a plazas o meses diferentes");
                }
                if (!"COD".equals(sinAcentos(valor(hoja, 3, 1, formato)))
                        || !"NOMBRE".equals(sinAcentos(valor(hoja, 3, 2, formato)))) {
                    throw new ProgramacionValidationException("Encabezados semanales incorrectos");
                }
                Matcher rango = RANGE.matcher(valor(hoja, 1, 0, formato));
                if (!rango.find()) {
                    throw new ProgramacionValidationException("No se encontró el periodo visible en " + hoja.getSheetName());
                }
                LocalDate desde;
                LocalDate hasta;
                try {
                    desde = LocalDate.parse(rango.group(1), FECHA);
                    hasta = LocalDate.parse(rango.group(2), FECHA);
                } catch (DateTimeParseException ex) {
                    throw new ProgramacionValidationException("Fecha semanal inválida");
                }
                if (desde.isAfter(hasta) || hasta.isAfter(desde.plusDays(6))
                        || !YearMonth.from(desde).equals(periodo)
                        || !YearMonth.from(hasta).equals(periodo)
                        || (finalAnterior != null && !finalAnterior.plusDays(1).equals(desde))) {
                    throw new ProgramacionValidationException("Las semanas están fuera de orden o del mes seleccionado");
                }
                finalAnterior = hasta;
                for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
                    int indiceCol = 3 + (int) (fecha.toEpochDay() - desde.toEpochDay());
                    String diaHoja = sinAcentos(valor(hoja, 3, indiceCol, formato));
                    String diaEsperado = diaSemana(fecha);
                    boolean diaCorrecto = diaEsperado.equals(diaHoja)
                            || ("MIER".equals(diaEsperado) && "MIE".equals(diaHoja));
                    if (!Integer.toString(fecha.getDayOfMonth()).equals(valor(hoja, 4, indiceCol, formato))
                            || !diaCorrecto) {
                        throw new ProgramacionValidationException("Fecha o día semanal incorrecto: " + fecha);
                    }
                }
                for (int f = 5; f <= hoja.getLastRowNum(); f++) {
                    Row origen = hoja.getRow(f);
                    if (origen == null) continue;
                    String codigo = valor(hoja, f, 1, formato);
                    if (codigo.isBlank()) continue;
                    try { Integer.parseInt(codigo); }
                    catch (NumberFormatException ex) { throw new ProgramacionValidationException("Código de trabajador inválido"); }
                    String nombre = valor(hoja, f, 2, formato);
                    if (nombre.isBlank()) throw new ProgramacionValidationException("Falta el nombre del trabajador " + codigo);
                    Row destino = filas.get(codigo);
                    if (destino == null) {
                        destino = mensual.createRow(6 + filas.size());
                        destino.createCell(1).setCellValue(codigo);
                        destino.createCell(2).setCellValue(nombre);
                        filas.put(codigo, destino);
                    } else if (!sinAcentos(nombre).equals(sinAcentos(destino.getCell(2).getStringCellValue()))) {
                        throw new ProgramacionValidationException("El código " + codigo + " tiene nombres diferentes");
                    }
                    for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
                        int col = 3 + (int) (fecha.toEpochDay() - desde.toEpochDay());
                        Cell celda = origen.getCell(col);
                        if (celda == null) continue;
                        if (celda.getCellType() == CellType.FORMULA) {
                            throw new ProgramacionValidationException("No se permiten fórmulas en el Excel");
                        }
                        String dato = formato.formatCellValue(celda).trim();
                        // Conservar celdas coloreadas aunque no tengan caseta: pueden
                        // expresar un turno A/B/C pendiente de ubicación.
                        if (dato.isBlank() && celda.getCellStyle().getFillPattern()
                                != org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND) continue;
                        if (!visitadas.add(codigo + "|" + fecha)) {
                            throw new ProgramacionValidationException("Trabajador y fecha duplicados: " + codigo + " " + fecha);
                        }
                        Cell nueva = destino.createCell(2 + fecha.getDayOfMonth());
                        nueva.setCellValue(dato);
                        nueva.setCellStyle(celda.getCellStyle());
                    }
                }
            }
            if (filas.isEmpty()) {
                throw new ProgramacionValidationException("El Excel no contiene trabajadores");
            }
            mensual.createRow(1).createCell(1).setCellValue(titulo);
            for (int i = 0; i < hojasOriginales; i++) libro.removeSheetAt(0);
            try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
                libro.write(salida);
                return salida.toByteArray();
            }
        } catch (ProgramacionValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProgramacionValidationException("No se pudo leer el Excel semanal");
        }
    }

    private static String valor(Sheet hoja, int fila, int col, DataFormatter formato) {
        Row row = hoja.getRow(fila);
        if (row == null || row.getCell(col) == null) return "";
        Cell cell = row.getCell(col);
        if (cell.getCellType() == CellType.FORMULA) throw new ProgramacionValidationException("No se permiten fórmulas");
        return formato.formatCellValue(cell).trim();
    }

    private static String sinAcentos(String texto) {
        return Normalizer.normalize(texto.trim().toUpperCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
    }

    private static String diaSemana(LocalDate f) {
        return switch (f.getDayOfWeek()) {
            case MONDAY -> "LUN"; case TUESDAY -> "MAR"; case WEDNESDAY -> "MIER";
            case THURSDAY -> "JUE"; case FRIDAY -> "VIE"; case SATURDAY -> "SAB"; case SUNDAY -> "DOM";
        };
    }
}
