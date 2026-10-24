package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.in.UbicacionUseCase;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vista previa COMPLETA de la distribución mensual histórica.
 * Solo lectura: NO inyecta puertos de escritura, NO crea turnos ni guarda casetas.
 * La importación existente de SOLO CASETAS permanece sin cambios.
 */
@Service
@RequiredArgsConstructor
public class DistribucionExcelTurnosPreviewService {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_ROWS = 350;
    private static final Set<String> NO_OPERATIVOS = Set.of("D", "DM", "V", "COM", "LIC");
    private static final Set<String> OPERATIVOS = Set.of("A", "B", "C");
    private static final Pattern CABECERA = Pattern.compile("^DISTRIBUCION\\s+([A-Z]+)\\s+([A-Z0-9_-]+)$");
    private static final Pattern AUXILIAR = Pattern.compile("^AUX(?:ILIAR)?\\s*[- ]?\\s*(\\d+)$");

    private final DistribucionUseCase distribucion;
    private final UbicacionUseCase ubicaciones;
    private final TrabajadorUseCase trabajadores;

    public record Propuesta(int codigoTrabajador, String trabajador, LocalDate fecha,
                            String valorExcel, String turnoExcel, String turnoSigo,
                            String casetaSigo, String accion, String detalle) {}

    public record VistaPrevia(LocalDate desde, LocalDate hasta, int leidas,
                             int crear, int actualizar, int coinciden, int revisar,
                             List<Propuesta> propuestas, List<String> advertencias) {}

    public VistaPrevia previsualizar(Long plazaId, byte[] archivo, Integer anio, Integer mes) {
        if (plazaId == null || archivo == null || archivo.length == 0 || archivo.length > MAX_BYTES) {
            throw new ProgramacionValidationException("Selecciona un Excel .xlsx de máximo 2 MB");
        }
        if (anio == null || anio < 2000 || anio > 2100 || mes == null || mes < 1 || mes > 12) {
            throw new ProgramacionValidationException("Selecciona el año y mes del Excel mensual");
        }
        archivo = DistribucionExcelSemanalNormalizer.normalizar(archivo, anio, mes);
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();

        // Misma validación de acceso/plaza que el importador existente.
        var reporte = distribucion.reportePlaza(plazaId, desde, hasta);
        if (reporte == null || reporte.plazaCodigo() == null) {
            throw new ProgramacionValidationException("No se pudo consultar la plaza");
        }
        Map<String, DistribucionUseCase.MatrizItem> existentes = new HashMap<>();
        for (var item : reporte.items()) {
            if (item.codigoTrabajador() != null) {
                existentes.put(clave(item.codigoTrabajador(), item.fecha()), item);
            }
        }
        Map<Integer, TrabajadorUseCase.TrabajadorData> personal = new HashMap<>();
        for (var trabajador : trabajadores.listarAdministracion(plazaId)) {
            if (trabajador.codigo() != null) personal.put(trabajador.codigo(), trabajador);
        }
        Map<String, UbicacionUseCase.Ubicacion> casetas = new HashMap<>();
        for (var u : ubicaciones.listarActivas(plazaId)) {
            if (u.codigo() != null) casetas.put(normalizarCaseta(u.codigo()), u);
        }

        List<Propuesta> propuestas = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();
        Set<String> visitas = new HashSet<>();
        int crear = 0, actualizar = 0, coinciden = 0, revisar = 0;
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            if (libro.getNumberOfSheets() != 1) {
                throw new ProgramacionValidationException("La vista previa completa admite el Excel mensual original de una sola hoja");
            }
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter fmt = new DataFormatter(Locale.ROOT);
            if (!"COD".equals(normalizar(valor(hoja, 4, 1, fmt)))
                    || !"NOMBRE".equals(normalizar(valor(hoja, 4, 2, fmt)))) {
                throw new ProgramacionValidationException("No se reconocieron las columnas Cod y Nombre en la fila 5");
            }
            String encabezado = sinAcentos(valor(hoja, 1, 1, fmt));
            Matcher coincidencia = CABECERA.matcher(encabezado);
            if (!coincidencia.matches() || !mesValido(coincidencia.group(1), mes)
                    || !coincidencia.group(2).equals(sinAcentos(reporte.plazaCodigo()))) {
                throw new ProgramacionValidationException("La plaza o el mes del Excel no coinciden con los seleccionados");
            }
            if (hoja.getLastRowNum() > MAX_ROWS) {
                throw new ProgramacionValidationException("Demasiadas filas en el Excel");
            }
            for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
                LocalDate fecha = periodo.atDay(dia);
                if (!Integer.toString(dia).equals(valor(hoja, 5, 2 + dia, fmt))
                        || !diaSemana(fecha).equals(sinAcentos(valor(hoja, 4, 2 + dia, fmt)))) {
                    throw new ProgramacionValidationException("El día " + dia + " no coincide con el mes y año seleccionados");
                }
            }
            for (int fila = 6; fila <= hoja.getLastRowNum(); fila++) {
                Row row = hoja.getRow(fila);
                if (row == null) continue;
                String codigoTexto = valor(hoja, fila, 1, fmt);
                if (codigoTexto.isBlank()) continue; // Totales, leyendas o filas vacías.
                int codigo;
                try { codigo = Integer.parseInt(codigoTexto); }
                catch (NumberFormatException e) { throw new ProgramacionValidationException("Código inválido en fila " + (fila + 1)); }
                String nombre = valor(hoja, fila, 2, fmt);
                if (nombre.isBlank()) throw new ProgramacionValidationException("Falta el nombre en fila " + (fila + 1));
                for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
                    Cell celda = row.getCell(2 + dia);
                    if (celda == null) continue;
                    if (celda.getCellType() == CellType.FORMULA) {
                        throw new ProgramacionValidationException("No se permiten fórmulas en fila " + (fila + 1));
                    }
                    String dato = normalizar(fmt.formatCellValue(celda));
                    // En la matriz histórica el color también representa el turno,
                    // incluso si la caseta está sin asignar (celda sin texto).
                    String turnoPorColor = turnoColor(celda);
                    if (dato.isBlank() && turnoPorColor == null) continue;
                    if (dato.isBlank()) dato = "SIN ASIG."; // Solo turno; no crea caseta.
                    LocalDate fecha = periodo.atDay(dia);
                    String llave = clave(codigo, fecha);
                    if (!visitas.add(llave)) throw new ProgramacionValidationException("Trabajador y fecha duplicados: " + llave);
                    String turnoExcel = NO_OPERATIVOS.contains(dato) ? dato : turnoPorColor;
                    var persona = personal.get(codigo);
                    var existente = existentes.get(llave);
                    String turnoSigo = existente == null ? null : normalizar(existente.estado());
                    String casetaSigo = existente == null ? null : existente.ubicacionCodigo();
                    String accion;
                    String detalle = "";
                    if (persona == null) {
                        accion = "REVISAR";
                        detalle = "El código no corresponde a un trabajador de la plaza; comprobar identidad";
                    } else if (!Boolean.TRUE.equals(persona.activo())) {
                        accion = "REVISAR";
                        detalle = "Trabajador actualmente inactivo; verificar situación histórica";
                    } else if (!sinAcentos(persona.nombreCompleto()).equals(sinAcentos(nombre))) {
                        accion = "REVISAR";
                        detalle = "El nombre del Excel no coincide con el registrado para el código";
                    } else if (turnoExcel == null) {
                        accion = "REVISAR";
                        detalle = "Color de turno no reconocido; no se infiere A/B/C";
                    } else if (OPERATIVOS.contains(turnoExcel)) {
                        // PASO 1: comparar SOLO el turno A/B/C. La caseta se revisa en el PASO 2.
                        // Así una caseta distinta NO infla el contador "Por actualizar".
                        if (existente == null) {
                            accion = "CREAR";
                            detalle = "Crear turno " + turnoExcel + "; caseta pendiente del paso 2";
                        } else if (turnoExcel.equals(turnoSigo)) {
                            accion = "COINCIDE";
                            if (!normalizarCaseta(dato).equals(normalizarCaseta(casetaSigo))) {
                                detalle = "El turno ya coincide; revisar la caseta en el paso 2";
                            }
                        } else {
                            accion = "ACTUALIZAR";
                            detalle = "Cambiar SOLO turno " + (turnoSigo == null ? "sin turno" : turnoSigo)
                                    + " a " + turnoExcel + "; caseta pendiente del paso 2";
                        }
                    } else if (NO_OPERATIVOS.contains(turnoExcel)) {
                        // El Excel confirmado también manda sobre D, DM, V, COM y LIC.
                        // No crea casetas; el repositorio elimina la antigua cuando el
                        // turno deja de ser operativo.
                        if (existente == null) {
                            accion = "CREAR";
                            detalle = "Registrar estado " + turnoExcel + " del Excel";
                        } else if (turnoExcel.equals(turnoSigo)
                                && (casetaSigo == null || casetaSigo.isBlank())) {
                            accion = "COINCIDE";
                        } else {
                            accion = "ACTUALIZAR";
                            detalle = "Aplicar " + turnoExcel + " desde Excel y retirar caseta si existe";
                        }
                    } else {
                        accion = "REVISAR";
                        detalle = "Turno sin clasificación segura; revisar manualmente";
                    }
                    propuestas.add(new Propuesta(codigo, nombre, fecha, dato, turnoExcel,
                            turnoSigo, casetaSigo, accion, detalle));
                    switch (accion) {
                        case "CREAR" -> crear++;
                        case "ACTUALIZAR" -> actualizar++;
                        case "COINCIDE" -> coinciden++;
                        default -> { revisar++; if (advertencias.size() < 100) {
                            advertencias.add("Fila " + (fila + 1) + ", " + fecha + ", código " + codigo + ": " + detalle);
                        }}
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ProgramacionValidationException p) throw p;
            throw new ProgramacionValidationException("No se pudo leer la matriz mensual");
        }
        return new VistaPrevia(desde, hasta, propuestas.size(), crear, actualizar,
                coinciden, revisar, List.copyOf(propuestas), List.copyOf(advertencias));
    }

    private static String turnoColor(Cell celda) {
        if (!(celda.getCellStyle() instanceof XSSFCellStyle estilo)) return null;
        XSSFColor color = estilo.getFillForegroundColorColor();
        if (color == null || color.getRGB() == null) return null;
        byte[] rgb = color.getRGB();
        String hex = String.format("#%02X%02X%02X", rgb[0] & 0xff, rgb[1] & 0xff, rgb[2] & 0xff);
        return switch (hex) {
            case "#C6EFCE" -> "A";
            case "#FFEB9C" -> "B";
            case "#FFC7CE" -> "C";
            default -> null;
        };
    }

    private static boolean habilitada(UbicacionUseCase.Ubicacion u, String turno) {
        return switch (turno) {
            case "A" -> !Boolean.FALSE.equals(u.permiteTurnoA());
            case "B" -> !Boolean.FALSE.equals(u.permiteTurnoB());
            default -> !Boolean.FALSE.equals(u.permiteTurnoC());
        };
    }
    private static String valor(Sheet s, int fila, int col, DataFormatter f) {
        Row row = s.getRow(fila);
        if (row == null || row.getCell(col) == null) return "";
        Cell c = row.getCell(col);
        if (c.getCellType() == CellType.FORMULA) throw new ProgramacionValidationException("No se permiten fórmulas en el Excel");
        return f.formatCellValue(c).trim();
    }
    private static boolean mesValido(String nombre, int mes) {
        String[] meses = {"ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};
        return mes >= 1 && mes <= 12 && (nombre.equals(meses[mes - 1]) || (mes == 9 && nombre.equals("SETIEMBRE")));
    }
    private static String diaSemana(LocalDate f) {
        return switch (f.getDayOfWeek()) {
            case MONDAY -> "LUN"; case TUESDAY -> "MAR"; case WEDNESDAY -> "MIER";
            case THURSDAY -> "JUE"; case FRIDAY -> "VIE"; case SATURDAY -> "SAB"; case SUNDAY -> "DOM";
        };
    }
    private static String sinAcentos(String texto) {
        return Normalizer.normalize(normalizar(texto), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }
    private static String normalizar(String s) { return s == null ? "" : s.trim().toUpperCase(Locale.ROOT); }
    private static String normalizarCaseta(String s) {
        String v = sinAcentos(s);
        Matcher m = AUXILIAR.matcher(v);
        return m.matches() ? "AUX " + m.group(1) : v;
    }
    private static String clave(int codigo, LocalDate f) { return codigo + "|" + f; }
}
