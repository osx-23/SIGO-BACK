package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.ListarTurnosUseCase;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vista previa de turnos EXCLUSIVAMENTE por el texto escrito en el .xlsx.
 * No interpreta colores, no importa casetas y NUNCA escribe en la BD.
 * El usuario aplica propuestas a la matriz y guarda con PUT /api/programacion/turnos.
 */
@Service
@RequiredArgsConstructor
public class ProgramacionExcelTextoService {
    private static final Set<String> ESTADOS = Set.of("A", "B", "C", "D", "V", "COM", "DM", "LIC", "-");
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_HOJAS = 10;
    private static final int MAX_FILAS = 500;
    private static final int MAX_MENSAJES = 100;
    private static final Pattern TITULO = Pattern.compile(
            "^(?:DISTRIBUCION|PROGRAMACION|TURNOS)\\s+([A-Z]+)\\s+([A-Z0-9_-]+)$");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d/M/uuuu");

    private final ListarTurnosUseCase turnos;
    private final TrabajadorUseCase trabajadores;

    public record Cambio(Long trabajadorId, Integer codigoTrabajador, String nombre,
                         LocalDate fecha, String anterior, String nuevo) {}

    public record VistaPrevia(int leidos, int coinciden, List<Cambio> cambios,
                             List<String> errores, List<String> omitidos) {}

    private record Clave(Integer codigo, LocalDate fecha) {}
    private record Registro(int codigo, LocalDate fecha, String estado, String origen) {}
    private record Encabezado(int fila, int colCodigo, int colNombre, int filaFechas,
                             Map<Integer, LocalDate> fechas) {}

    public VistaPrevia previsualizar(Long plazaId, int anio, int mes, byte[] archivo) {
        if (plazaId == null || plazaId <= 0 || archivo == null
                || archivo.length == 0 || archivo.length > MAX_BYTES) {
            throw new ProgramacionValidationException("Selecciona un Excel .xlsx de maximo 2 MB");
        }
        if (anio < 2000 || anio > 2100 || mes < 1 || mes > 12) {
            throw new ProgramacionValidationException("Selecciona el mes y anio validos antes de importar");
        }
        YearMonth periodo = YearMonth.of(anio, mes);
        // Listar turnos y trabajadores reutiliza las comprobaciones de acceso a la plaza.
        List<ListarTurnosUseCase.Turno> guardados = turnos.listar(plazaId, anio, mes);
        Map<Clave, String> estadosSigo = new HashMap<>();
        for (var turno : guardados) {
            if (turno.codigoTrabajador() != null) {
                estadosSigo.put(new Clave(turno.codigoTrabajador(), turno.fecha()), turno.estado());
            }
        }

        Map<Integer, TrabajadorUseCase.TrabajadorData> personas = new HashMap<>();
        Set<Integer> codigosRepetidos = new HashSet<>();
        for (var persona : trabajadores.listarAgentesPorPlaza(plazaId)) {
            incorporar(personas, codigosRepetidos, persona);
        }
        for (var persona : trabajadores.listarControladoresPorPlaza(plazaId)) {
            incorporar(personas, codigosRepetidos, persona);
        }
        Map<Clave, Registro> registros = new LinkedHashMap<>();
        Set<Clave> repetidos = new HashSet<>();
        List<String> errores = new ArrayList<>();
        List<String> omitidos = new ArrayList<>();
        int leidos = 0;
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            if (libro.getNumberOfSheets() < 1 || libro.getNumberOfSheets() > MAX_HOJAS) {
                throw new ProgramacionValidationException("Numero de hojas de Excel no permitido");
            }
            DataFormatter formato = new DataFormatter(Locale.ROOT);
            for (Sheet hoja : libro) {
                if (hoja.getLastRowNum() > MAX_FILAS) {
                    throw new ProgramacionValidationException("Demasiadas filas en " + hoja.getSheetName());
                }
                validarTitulo(hoja, formato, periodo, guardados, personas, plazaId);
                Encabezado cabecera = encontrarEncabezado(hoja, formato, periodo);
                if (cabecera == null) {
                    throw new ProgramacionValidationException("No se encontraron columnas Cod, Nombre y dias en "
                            + hoja.getSheetName() + ". Usa la matriz mensual o semanal de SIGO.");
                }
                // Si hay nombres de dias, verifica el anio y mes seleccionados.
                Row encabezadoDias = hoja.getRow(cabecera.fila());
                if (encabezadoDias != null && cabecera.filaFechas() != cabecera.fila()) {
                    for (var entrada : cabecera.fechas().entrySet()) {
                        String etiqueta = limpiar(leer(encabezadoDias.getCell(entrada.getKey()), formato));
                        String esperado = abreviatura(entrada.getValue());
                        if (Set.of("LUN", "MAR", "MIE", "MIER", "JUE", "VIE", "SAB", "DOM").contains(etiqueta)
                                && !(esperado.equals(etiqueta) || ("MIER".equals(esperado) && "MIE".equals(etiqueta)))) {
                            throw new ProgramacionValidationException("El dia de la semana no coincide con "
                                    + entrada.getValue() + "; revisa el anio o mes seleccionado");
                        }
                    }
                }
                for (int f = cabecera.filaFechas() + 1; f <= hoja.getLastRowNum(); f++) {
                    Row fila = hoja.getRow(f);
                    if (fila == null) continue;
                    String codigoTexto = leer(fila.getCell(cabecera.colCodigo()), formato);
                    if (codigoTexto.isBlank()) continue; // Leyendas y totales sin codigo.
                    int codigo;
                    try {
                        codigo = Integer.parseInt(codigoTexto);
                    } catch (NumberFormatException ex) {
                        agregar(omitidos, hoja.getSheetName() + ", fila " + (f + 1)
                                + ": codigo no numerico, fila omitida");
                        continue;
                    }
                    for (var fecha : cabecera.fechas().entrySet()) {
                        String valor = TextoExcelUtils.normalizar(leer(fila.getCell(fecha.getKey()), formato));
                        if (valor.isBlank()) continue; // Vacio: nunca modifica nada.
                        leidos++;
                        String origen = hoja.getSheetName() + ", fila " + (f + 1) + ", " + fecha.getValue();
                        Clave clave = new Clave(codigo, fecha.getValue());
                        if (!ESTADOS.contains(valor)) {
                            agregar(errores, origen + ": codigo " + codigo + ", texto '" + valor
                                    + "' no admitido (A/B/C/D/V/COM/DM/LIC/-)");
                            registros.remove(clave);
                            repetidos.add(clave); // Evita aplicar otra propuesta para esa celda.
                            continue;
                        }
                        if (repetidos.contains(clave)) continue;
                        Registro anterior = registros.putIfAbsent(clave,
                                new Registro(codigo, fecha.getValue(), valor, origen));
                        if (anterior != null) {
                            registros.remove(clave);
                            repetidos.add(clave);
                            agregar(errores, origen + ": codigo " + codigo
                                    + " y fecha duplicados en el archivo; no se aplicaran");
                        }
                    }
                }
            }
        } catch (ProgramacionValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProgramacionValidationException("No se pudo leer el Excel .xlsx de turnos");
        }

        if (leidos == 0) {
            throw new ProgramacionValidationException("No hay textos de turnos para importar");
        }
        List<Cambio> cambios = new ArrayList<>();
        int coinciden = 0;
        for (var entry : registros.entrySet()) {
            Registro registro = entry.getValue();
            var persona = personas.get(registro.codigo());
            if (codigosRepetidos.contains(registro.codigo()) || persona == null
                    || !Boolean.TRUE.equals(persona.activo())) {
                agregar(omitidos, registro.origen() + ": codigo " + registro.codigo()
                        + " no corresponde a un agente/controlador activo y unico de la plaza");
                continue;
            }
            String anterior = estadosSigo.get(entry.getKey());
            String nuevo = "-".equals(registro.estado()) ? null : registro.estado();
            if (java.util.Objects.equals(anterior, nuevo)) {
                coinciden++;
            } else {
                cambios.add(new Cambio(persona.id(), persona.codigo(), persona.nombreCompleto(),
                        registro.fecha(), anterior, nuevo));
            }
        }
        return new VistaPrevia(leidos, coinciden, List.copyOf(cambios),
                List.copyOf(errores), List.copyOf(omitidos));
    }

    private static void incorporar(Map<Integer, TrabajadorUseCase.TrabajadorData> mapa,
                                   Set<Integer> repetidos, TrabajadorUseCase.TrabajadorData persona) {
        if (persona.codigo() == null) return;
        var anterior = mapa.putIfAbsent(persona.codigo(), persona);
        if (anterior != null && !anterior.id().equals(persona.id())) repetidos.add(persona.codigo());
    }

    private static Encabezado encontrarEncabezado(Sheet hoja, DataFormatter formato, YearMonth periodo) {
        for (int f = 0; f <= Math.min(25, hoja.getLastRowNum()); f++) {
            Row encabezado = hoja.getRow(f);
            if (encabezado == null) continue;
            for (int c = 0; c < Math.min(encabezado.getLastCellNum(), 12); c++) {
                if (!"COD".equals(limpiar(leer(encabezado.getCell(c), formato)))) continue;
                if (!limpiar(leer(encabezado.getCell(c + 1), formato)).startsWith("NOMBRE")) continue;
                for (int filaFechas : new int[] {f + 1, f}) {
                    Row filaDias = hoja.getRow(filaFechas);
                    if (filaDias == null) continue;
                    Map<Integer, LocalDate> fechas = new LinkedHashMap<>();
                    LocalDate anterior = null;
                    for (int columna = c + 2;
                         columna < Math.min(filaDias.getLastCellNum(), c + 65); columna++) {
                        LocalDate fecha = fecha(leer(filaDias.getCell(columna), formato), periodo);
                        // Solo columnas de dias CONSECUTIVOS. No leer indicadores laterales.
                        if (fecha == null) {
                            if (!fechas.isEmpty()) break;
                            continue;
                        }
                        if (anterior != null && !fecha.equals(anterior.plusDays(1))) break;
                        fechas.put(columna, fecha);
                        anterior = fecha;
                    }
                    if (!fechas.isEmpty()) return new Encabezado(f, c, c + 1, filaFechas, fechas);
                }
            }
        }
        return null;
    }

    private static LocalDate fecha(String dato, YearMonth periodo) {
        if (dato.isBlank()) return null;
        try {
            int dia = Integer.parseInt(dato);
            return dia >= 1 && dia <= periodo.lengthOfMonth() ? periodo.atDay(dia) : null;
        } catch (NumberFormatException ignored) {
            // Tambien admite encabezados con fecha completa: 2026-10-01 o 01/10/2026.
        }
        try {
            LocalDate fecha = dato.contains("-") ? LocalDate.parse(dato)
                    : LocalDate.parse(dato, FECHA_CORTA);
            return YearMonth.from(fecha).equals(periodo) ? fecha : null;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static void validarTitulo(Sheet hoja, DataFormatter formato, YearMonth periodo,
                                     List<ListarTurnosUseCase.Turno> guardados,
                                     Map<Integer, TrabajadorUseCase.TrabajadorData> personas,
                                     Long plazaId) {
        String codigoPlaza = null;
        for (var turno : guardados) {
            if (turno.plazaCodigo() != null) { codigoPlaza = turno.plazaCodigo(); break; }
        }
        if (codigoPlaza == null) {
            for (var persona : personas.values()) {
                if (persona.plaza() != null && persona.plaza().id() != null
                        && persona.plaza().id().equals(plazaId)) {
                    codigoPlaza = persona.plaza().codigo(); break;
                }
            }
        }
        for (int f = 0; f <= Math.min(3, hoja.getLastRowNum()); f++) {
            Row row = hoja.getRow(f);
            if (row == null) continue;
            for (int c = 0; c < 5; c++) {
                Matcher m = TITULO.matcher(limpiar(leer(row.getCell(c), formato)));
                if (!m.matches()) continue;
                String mesTitulo = m.group(1);
                String plazaTitulo = m.group(2);
                if (!nombreMes(periodo.getMonthValue()).equals(mesTitulo)
                        && !(periodo.getMonthValue() == 9 && "SETIEMBRE".equals(mesTitulo))) {
                    throw new ProgramacionValidationException("El mes del Excel no coincide con el seleccionado");
                }
                if (codigoPlaza != null && !codigoPlaza.equalsIgnoreCase(plazaTitulo)) {
                    throw new ProgramacionValidationException("El Excel pertenece a otra plaza: " + plazaTitulo);
                }
            }
        }
    }

    private static String abreviatura(LocalDate fecha) {
        return switch (fecha.getDayOfWeek()) {
            case MONDAY -> "LUN";
            case TUESDAY -> "MAR";
            case WEDNESDAY -> "MIER";
            case THURSDAY -> "JUE";
            case FRIDAY -> "VIE";
            case SATURDAY -> "SAB";
            case SUNDAY -> "DOM";
        };
    }

    private static String nombreMes(int mes) {
        return new String[] {"", "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
                "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"}[mes];
    }

    private static String leer(Cell celda, DataFormatter formato) {
        if (celda == null) return "";
        if (celda.getCellType() == CellType.FORMULA) {
            throw new ProgramacionValidationException("No se permiten formulas en los Excel importados");
        }
        return formato.formatCellValue(celda).trim();
    }

    private static String limpiar(String texto) {
        return TextoExcelUtils.sinAcentos(texto);
    }

    private static void agregar(List<String> lista, String mensaje) {
        if (lista.size() < MAX_MENSAJES) lista.add(mensaje);
    }
}
