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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
 * Lee el formato .xlsx que SIGO exporta para la distribución de casetas.
 * Solo propone asignaciones: no escribe nada en PostgreSQL.
 */
@Service
@RequiredArgsConstructor
public class DistribucionExcelImportService {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_SHEETS = 10;
    private static final int MAX_ROWS = 350;
    private static final int MAX_ERRORS = 75;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final Pattern RANGE = Pattern.compile(
            "Periodo visible:\\s*(\\d{2}/\\d{2}/\\d{4})\\s*-\\s*(\\d{2}/\\d{2}/\\d{4})",
            Pattern.CASE_INSENSITIVE);
    private final DistribucionUseCase distribucion;
    private final UbicacionUseCase ubicaciones;
    private final TrabajadorUseCase trabajadores;

    public record Cambio(Long programacionTurnoId, Long ubicacionId,
                         Integer codigoTrabajador, String trabajador,
                         LocalDate fecha, String caseta) {}

    public record VistaPrevia(boolean valido, Long plazaId, LocalDate desde, LocalDate hasta,
                             int celdasLeidas, int yaAsignadas,
                             List<Cambio> cambios, List<String> errores, List<String> omitidos) {}

    /** JSON que produce Apache POI: solo trabajador, fecha y caseta (null = sin asignacion). */
    public record RegistroJson(Integer codigoTrabajador, LocalDate fecha, String caseta,
                               String turnoExcel) {}
    public record ExtraccionJson(Long plazaId, String plazaCodigo, LocalDate desde,
                                LocalDate hasta, List<RegistroJson> registros) {}

    private record CeldaCaseta(String hoja, int fila, int codigoTrabajador,
                               LocalDate fecha, String valor, String turnoExcel) {
        private CeldaCaseta(String hoja, int fila, int codigoTrabajador,
                            LocalDate fecha, String valor) {
            this(hoja, fila, codigoTrabajador, fecha, valor, null);
        }
    }
    private record Ocupacion(LocalDate fecha, String turno, String caseta) {}
    private record Lectura(LocalDate desde, LocalDate hasta, String titulo, List<CeldaCaseta> celdas) {}

    public VistaPrevia previsualizar(Long plazaId, byte[] archivo) {
        return previsualizar(plazaId, archivo, null, null);
    }

    /** El año/mes elegido en la pantalla es obligatorio para Excel mensuales sin año. */
    public VistaPrevia previsualizar(Long plazaId, byte[] archivo, Integer anio, Integer mes) {
        if (plazaId == null || archivo == null || archivo.length == 0 || archivo.length > MAX_BYTES) {
            throw new ProgramacionValidationException("Selecciona un Excel .xlsx de máximo 2 MB");
        }
        Lectura lectura = leer(archivo, anio, mes);
        return generarVistaPrevia(plazaId, lectura);
    }

    /** Primera peticion multipart: POI extrae datos y Spring los serializa a JSON. */
    public ExtraccionJson extraerJson(Long plazaId, byte[] archivo, int anio, int mes) {
        if (plazaId == null || archivo == null || archivo.length == 0 || archivo.length > MAX_BYTES) {
            throw new ProgramacionValidationException("Selecciona un Excel .xlsx de maximo 2 MB");
        }
        Lectura lectura = leer(archivo, anio, mes);
        validarPlaza(plazaId, lectura);
        List<RegistroJson> registros = new ArrayList<>();
        for (CeldaCaseta celda : lectura.celdas()) {
            String texto = normalizar(celda.valor());
            // Una celda realmente vacia NO ordena borrar una asignacion previa.
            if (texto.isBlank()) continue;
            String caseta = sinAsignacion(texto) ? null : normalizarCaseta(texto);
            registros.add(new RegistroJson(celda.codigoTrabajador(), celda.fecha(),
                    caseta, sinAsignacion(texto) ? null : celda.turnoExcel()));
        }
        return new ExtraccionJson(plazaId,
                lectura.titulo().substring(lectura.titulo().lastIndexOf(' ') + 1),
                lectura.desde(), lectura.hasta(), List.copyOf(registros));
    }

    /** Segunda peticion application/json: valida el JSON contra la BD actual. */
    public VistaPrevia previsualizarJson(ExtraccionJson json) {
        if (json == null || json.plazaId() == null || json.plazaCodigo() == null
                || json.desde() == null || json.hasta() == null || json.registros() == null
                || json.desde().isAfter(json.hasta()) || json.desde().plusDays(62).isBefore(json.hasta())
                || json.registros().size() > 4000) {
            throw new ProgramacionValidationException("JSON de casetas invalido o demasiado grande");
        }
        List<CeldaCaseta> celdas = new ArrayList<>();
        int i = 0;
        for (RegistroJson r : json.registros()) {
            i++;
            if (r == null || r.codigoTrabajador() == null || r.codigoTrabajador() <= 0
                    || r.fecha() == null || r.fecha().isBefore(json.desde())
                    || r.fecha().isAfter(json.hasta())
                    || (r.caseta() != null && r.caseta().length() > 30)
                    || (r.turnoExcel() != null && !Set.of("A", "B", "C").contains(r.turnoExcel()))) {
                throw new ProgramacionValidationException("Registro JSON invalido en posicion " + i);
            }
            String valor = r.caseta() == null || sinAsignacion(normalizar(r.caseta()))
                    ? "SIN ASIG." : r.caseta();
            celdas.add(new CeldaCaseta("JSON", i, r.codigoTrabajador(), r.fecha(),
                    valor, r.turnoExcel()));
        }
        Lectura lectura = new Lectura(json.desde(), json.hasta(),
                "DISTRIBUCION JSON " + json.plazaCodigo(), List.copyOf(celdas));
        return generarVistaPrevia(json.plazaId(), lectura);
    }

    private static boolean sinAsignacion(String valor) {
        return Set.of("D", "DM", "V", "COM", "LIC", "SIN ASIG.", "SIN ASIG")
                .contains(valor);
    }

    private void validarPlaza(Long plazaId, Lectura lectura) {
        DistribucionUseCase.ReportePlaza reporte = distribucion.reportePlaza(
                plazaId, lectura.desde(), lectura.hasta());
        if (reporte.plazaCodigo() == null || !lectura.titulo().toUpperCase(Locale.ROOT)
                .endsWith(" " + reporte.plazaCodigo().toUpperCase(Locale.ROOT))) {
            throw new ProgramacionValidationException("El Excel o JSON pertenece a otra plaza");
        }
    }

    private VistaPrevia generarVistaPrevia(Long plazaId, Lectura lectura) {
        // Los casos de uso existentes comprueban el acceso del usuario a la plaza.
        DistribucionUseCase.ReportePlaza reporte = distribucion.reportePlaza(
                plazaId, lectura.desde(), lectura.hasta());
        String titulo = lectura.titulo().toUpperCase(Locale.ROOT);
        if (reporte.plazaCodigo() == null
                || !titulo.endsWith(" " + reporte.plazaCodigo().toUpperCase(Locale.ROOT))) {
            throw new ProgramacionValidationException("El Excel pertenece a otra plaza");
        }
        List<UbicacionUseCase.Ubicacion> casetas = ubicaciones.listarActivas(plazaId);
        Set<Integer> activos = new HashSet<>();
        for (var trabajador : trabajadores.listarAdministracion(plazaId)) {
            if (trabajador.codigo() != null && Boolean.TRUE.equals(trabajador.activo())) {
                activos.add(trabajador.codigo());
            }
        }

        Map<String, DistribucionUseCase.MatrizItem> programadas = new HashMap<>();
        for (var item : reporte.items()) {
            if (item.codigoTrabajador() != null) {
                programadas.put(clave(item.codigoTrabajador(), item.fecha()), item);
            }
        }
        Map<String, UbicacionUseCase.Ubicacion> casetasPorCodigo = new HashMap<>();
        for (var u : casetas) {
            if (u.codigo() != null) {
                casetasPorCodigo.put(normalizarCaseta(u.codigo()), u);
            }
        }

        List<Cambio> cambios = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        List<String> omitidos = new ArrayList<>();
        Set<String> visitadas = new HashSet<>();
        Map<Ocupacion, Integer> solicitudesExcel = new HashMap<>();
        Map<Long, DistribucionUseCase.MatrizItem> actualesPorId = new HashMap<>();
        Map<Ocupacion, DistribucionUseCase.MatrizItem> ocupantes = new HashMap<>();
        for (var item : reporte.items()) {
            if (item.programacionTurnoId() == null) continue;
            actualesPorId.put(item.programacionTurnoId(), item);
            if (item.ubicacionCodigo() != null && !item.ubicacionCodigo().isBlank()) {
                ocupantes.put(new Ocupacion(item.fecha(), normalizar(item.estado()),
                        normalizarCaseta(item.ubicacionCodigo())), item);
            }
        }
        int yaAsignadas = 0;
        int leidas = 0;
        for (CeldaCaseta celda : lectura.celdas()) {
            String valor = normalizar(celda.valor());
            if (valor.isBlank()) continue;
            leidas++;
            String llave = clave(celda.codigoTrabajador(), celda.fecha());
            String referencia = celda.hoja() + ", fila " + celda.fila() + ": ";
            if (!visitadas.add(llave)) {
                error(errores, referencia + "trabajador y fecha duplicados en el Excel");
                continue;
            }
            if (!activos.contains(celda.codigoTrabajador())) {
                omitidos.add(referencia + "Código " + celda.codigoTrabajador()
                        + ": trabajador no activo en esta plaza; sin cambios.");
                continue;
            }
            var programacion = programadas.get(llave);
            if (programacion == null || programacion.programacionTurnoId() == null) {
                omitidos.add(referencia + "Código " + celda.codigoTrabajador() + ", "
                        + celda.fecha() + ": sin turno programado en SIGO; caseta omitida.");
                continue;
            }
            // Estados explicitamente no operativos limpian SOLO la caseta, nunca el turno.
            // La celda vacia se descarto arriba y NO provoca eliminaciones.
            if (sinAsignacion(valor)) {
                if (programacion.ubicacionCodigo() != null
                        && !programacion.ubicacionCodigo().isBlank()) {
                    cambios.add(new Cambio(programacion.programacionTurnoId(), null,
                            celda.codigoTrabajador(), programacion.nombreTrabajador(),
                            celda.fecha(), "SIN ASIG."));
                } else {
                    yaAsignadas++;
                }
                continue;
            }
            String turno = normalizar(programacion.estado());
            if (!Set.of("A", "B", "C").contains(turno)) {
                omitidos.add(referencia + "Código " + celda.codigoTrabajador()
                        + ": SIGO tiene turno " + turno + "; no se asigna caseta ni se altera el turno.");
                continue;
            }
            // En el Excel mensual el color sirve exclusivamente para COMPROBAR.
            // No se crean ni se modifican turnos desde el importador de casetas.
            if (celda.turnoExcel() != null && !celda.turnoExcel().equals(turno)) {
                omitidos.add(referencia + "OBSERVACIÓN: Código " + celda.codigoTrabajador()
                        + " tiene turno " + celda.turnoExcel() + " en Excel y " + turno
                        + " en SIGO; verificar manualmente.");
                continue;
            }
            var caseta = casetasPorCodigo.get(normalizarCaseta(valor));
            if (caseta == null) {
                error(errores, referencia + "caseta desconocida o inactiva: " + valor);
                continue;
            }
            boolean habilitada = switch (turno) {
                case "A" -> !Boolean.FALSE.equals(caseta.permiteTurnoA());
                case "B" -> !Boolean.FALSE.equals(caseta.permiteTurnoB());
                default -> !Boolean.FALSE.equals(caseta.permiteTurnoC());
            };
            if (!habilitada) {
                error(errores, referencia + "caseta " + valor
                        + " no habilitada para turno " + turno);
                continue;
            }
            Ocupacion solicitud = new Ocupacion(celda.fecha(), turno, normalizarCaseta(valor));
            Integer primero = solicitudesExcel.putIfAbsent(solicitud, celda.codigoTrabajador());
            if (primero != null && primero != celda.codigoTrabajador()) {
                error(errores, referencia + "caseta " + valor + " duplicada en Excel en turno "
                        + turno + " el " + celda.fecha() + " (códigos " + primero
                        + " y " + celda.codigoTrabajador() + ")");
                continue;
            }
            if (normalizarCaseta(valor).equals(normalizarCaseta(programacion.ubicacionCodigo()))) {
                yaAsignadas++;
                continue;
            }
            // El modelo acordado NO reemplaza una asignacion anterior sin resolverla.
            if (programacion.ubicacionCodigo() != null
                    && !programacion.ubicacionCodigo().isBlank()) {
                omitidos.add(referencia + "OBSERVACIÓN: Código " + celda.codigoTrabajador()
                        + ": SIGO ya tiene caseta " + programacion.ubicacionCodigo()
                        + " y el Excel indica " + valor + "; revisar la diferencia.");
                continue;
            }
            cambios.add(new Cambio(programacion.programacionTurnoId(), caseta.id(),
                    celda.codigoTrabajador(), programacion.nombreTrabajador(),
                    celda.fecha(), normalizarCaseta(valor)));
        }
        // Una caseta ya ocupada NO se quita por sorpresa a otra persona.
        // Puede liberarse solo cuando esa persona también tiene un cambio explícito
        // válido en el lote (por ejemplo D/DM, SIN ASIG. o intercambio autorizado).
        Map<Long, Cambio> modificaciones = new HashMap<>();
        for (Cambio cambio : cambios) modificaciones.put(cambio.programacionTurnoId(), cambio);
        for (Cambio cambio : cambios) {
            if (cambio.ubicacionId() == null) continue;
            var solicitante = actualesPorId.get(cambio.programacionTurnoId());
            if (solicitante == null) continue;
            Ocupacion destino = new Ocupacion(cambio.fecha(), normalizar(solicitante.estado()),
                    normalizarCaseta(cambio.caseta()));
            var ocupado = ocupantes.get(destino);
            if (ocupado == null || ocupado.programacionTurnoId().equals(cambio.programacionTurnoId())) {
                continue;
            }
            Cambio cambioOcupante = modificaciones.get(ocupado.programacionTurnoId());
            if (cambioOcupante == null || java.util.Objects.equals(cambioOcupante.ubicacionId(), cambio.ubicacionId())) {
                error(errores, "Caseta " + cambio.caseta() + " en turno " + destino.turno()
                        + " del " + cambio.fecha() + " ya ocupada por código "
                        + ocupado.codigoTrabajador() + " en SIGO. Resuelve antes de aplicar.");
            }
        }
        return new VistaPrevia(errores.isEmpty(), plazaId, lectura.desde(), lectura.hasta(),
                leidas, yaAsignadas, List.copyOf(cambios), List.copyOf(errores),
                List.copyOf(omitidos));
    }

    private Lectura leer(byte[] bytes, Integer anio, Integer mes) {
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (libro.getNumberOfSheets() < 1 || libro.getNumberOfSheets() > MAX_SHEETS) {
                throw new ProgramacionValidationException("Número de hojas Excel no permitido");
            }
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            // El formato histórico es una sola hoja con B2="DISTRIBUCION SEPTIEMBRE P4",
            // encabezados en fila 5 y números de día en fila 6 (D:AG).
            if (libro.getNumberOfSheets() == 1
                    && esFormatoMensual(libro.getSheetAt(0), formatter)) {
                return leerMensualOriginal(libro.getSheetAt(0), formatter, anio, mes);
            }
            LocalDate primero = null;
            LocalDate ultimo = null;
            String titulo = null;
            List<CeldaCaseta> datos = new ArrayList<>();
            for (int i = 0; i < libro.getNumberOfSheets(); i++) {
                Sheet hoja = libro.getSheetAt(i);
                if (!hoja.getSheetName().equals("Semana " + (i + 1))) {
                    throw new ProgramacionValidationException("Las hojas deben llamarse Semana 1, Semana 2, etc.");
                }
                String tituloHoja = valor(hoja, 0, 0, formatter);
                if (!tituloHoja.toUpperCase(Locale.ROOT).startsWith("DISTRIBUCIÓN ")
                        || (titulo != null && !titulo.equals(tituloHoja))) {
                    throw new ProgramacionValidationException("El Excel no corresponde al formato de distribución de SIGO");
                }
                if (titulo == null) titulo = tituloHoja;
                if (!"Cod".equalsIgnoreCase(valor(hoja, 3, 1, formatter))
                        || !"Nombre".equalsIgnoreCase(valor(hoja, 3, 2, formatter))) {
                    throw new ProgramacionValidationException("Encabezados incorrectos en " + hoja.getSheetName());
                }
                Matcher rango = RANGE.matcher(valor(hoja, 1, 0, formatter));
                if (!rango.find()) {
                    throw new ProgramacionValidationException("No se encontró el periodo visible en " + hoja.getSheetName());
                }
                LocalDate inicio;
                LocalDate fin;
                try {
                    inicio = LocalDate.parse(rango.group(1), DATE);
                    fin = LocalDate.parse(rango.group(2), DATE);
                } catch (DateTimeParseException ex) {
                    throw new ProgramacionValidationException("Fecha inválida en " + hoja.getSheetName());
                }
                if (inicio.isAfter(fin) || inicio.plusDays(6).isBefore(fin)
                        || (ultimo != null && !ultimo.plusDays(1).equals(inicio))) {
                    throw new ProgramacionValidationException("Semanas fuera de orden o con fechas inválidas");
                }
                if (primero == null) primero = inicio;
                ultimo = fin;
                int columnas = (int) (fin.toEpochDay() - inicio.toEpochDay()) + 1;
                for (int dia = 0; dia < columnas; dia++) {
                    LocalDate fecha = inicio.plusDays(dia);
                    String numero = valor(hoja, 4, 3 + dia, formatter);
                    if (!Integer.toString(fecha.getDayOfMonth()).equals(numero)) {
                        throw new ProgramacionValidationException("La columna de fecha no coincide en " + hoja.getSheetName());
                    }
                }
                if (hoja.getLastRowNum() > MAX_ROWS) {
                    throw new ProgramacionValidationException("Demasiadas filas en " + hoja.getSheetName());
                }
                for (int fila = 5; fila <= hoja.getLastRowNum(); fila++) {
                    Row row = hoja.getRow(fila);
                    if (row == null) continue;
                    String codigo = valor(hoja, fila, 1, formatter);
                    if (codigo.isBlank()) continue;  // leyenda / filas vacías
                    int trabajador;
                    try {
                        trabajador = Integer.parseInt(codigo);
                    } catch (NumberFormatException ex) {
                        throw new ProgramacionValidationException("Código de trabajador inválido en fila " + (fila + 1));
                    }
                    for (int dia = 0; dia < columnas; dia++) {
                        datos.add(new CeldaCaseta(hoja.getSheetName(), fila + 1,
                                trabajador, inicio.plusDays(dia), valor(hoja, fila, 3 + dia, formatter),
                                turnoDesdeColor(row.getCell(3 + dia))));
                    }
                }
            }
            if (primero == null || ultimo == null || primero.plusDays(62).isBefore(ultimo)) {
                throw new ProgramacionValidationException("El periodo no puede exceder 63 días");
            }
            return new Lectura(primero, ultimo, titulo, datos);
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof ProgramacionValidationException pv) throw pv;
            throw new ProgramacionValidationException("No se pudo leer el Excel. Revisa el formato de distribución");
        }
    }

    private boolean esFormatoMensual(Sheet hoja, DataFormatter formato) {
        return "Cod".equalsIgnoreCase(valor(hoja, 4, 1, formato))
                && "Nombre".equalsIgnoreCase(valor(hoja, 4, 2, formato))
                && sinAcentos(valor(hoja, 1, 1, formato)).startsWith("DISTRIBUCION ");
    }

    /** Formato de matriz histórica de P4: fechas en D6:AG6 y trabajadores desde fila 7.
     *  No importa el bloque lateral de indicadores ni las filas inferiores de totales. */
    private Lectura leerMensualOriginal(Sheet hoja, DataFormatter formato,
                                        Integer anio, Integer mes) {
        if (anio == null || anio < 2000 || anio > 2100 || mes == null || mes < 1 || mes > 12) {
            throw new ProgramacionValidationException(
                    "Selecciona el año y mes del Excel original antes de importarlo");
        }
        if (hoja.getLastRowNum() > MAX_ROWS) {
            throw new ProgramacionValidationException("Demasiadas filas en el Excel");
        }
        String titulo = valor(hoja, 1, 1, formato);
        Matcher cabecera = Pattern.compile("^DISTRIBUCION\\s+([A-Z]+)\\s+([A-Z0-9_-]+)$")
                .matcher(sinAcentos(titulo));
        if (!cabecera.matches()) {
            throw new ProgramacionValidationException("Encabezado mensual de distribución no reconocido");
        }
        Integer mesTitulo = numeroMes(cabecera.group(1));
        if (mesTitulo == null || mesTitulo != mes) {
            throw new ProgramacionValidationException(
                    "El mes indicado en el Excel no coincide con el mes seleccionado en SIGO");
        }
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.atEndOfMonth();
        for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
            LocalDate fecha = periodo.atDay(dia);
            if (!Integer.toString(dia).equals(valor(hoja, 5, 2 + dia, formato))) {
                throw new ProgramacionValidationException("Falta o no coincide el día " + dia + " en la fila 6");
            }
            String diaSemana = sinAcentos(valor(hoja, 4, 2 + dia, formato));
            if (!diaSemana.equals(nombreDia(fecha.getDayOfWeek()))) {
                throw new ProgramacionValidationException("El encabezado del día " + dia
                        + " no coincide con el año " + anio + "; comprueba el año seleccionado");
            }
        }
        List<CeldaCaseta> datos = new ArrayList<>();
        for (int fila = 6; fila <= hoja.getLastRowNum(); fila++) {
            Row row = hoja.getRow(fila);
            if (row == null) continue;
            String codigo = valor(hoja, fila, 1, formato);
            // Resúmenes de cobertura, filas numeradas sin trabajador y leyendas sin código.
            if (codigo.isBlank()) continue;
            String nombre = valor(hoja, fila, 2, formato);
            if (nombre.isBlank()) {
                throw new ProgramacionValidationException("Falta el nombre del trabajador en fila " + (fila + 1));
            }
            int trabajador;
            try {
                trabajador = Integer.parseInt(codigo);
            } catch (NumberFormatException ex) {
                throw new ProgramacionValidationException("Código de trabajador inválido en fila " + (fila + 1));
            }
            for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
                String dato = valor(hoja, fila, 2 + dia, formato);
                datos.add(new CeldaCaseta(hoja.getSheetName(), fila + 1, trabajador,
                        periodo.atDay(dia), dato, turnoDesdeColor(row.getCell(2 + dia))));
            }
        }
        if (datos.isEmpty()) {
            throw new ProgramacionValidationException("El Excel mensual no contiene trabajadores con código");
        }
        return new Lectura(inicio, fin, titulo, datos);
    }

    /** Los colores sirven SOLO para comparar A/B/C; no modifican programaciones. */
    private static String turnoDesdeColor(Cell celda) {
        if (celda == null || !(celda.getCellStyle() instanceof XSSFCellStyle estilo)) return null;
        XSSFColor color = estilo.getFillForegroundColorColor();
        if (color == null || color.getRGB() == null) return null;
        byte[] rgb = color.getRGB();
        if (rgb.length < 3) return null;
        String hex = String.format("#%02X%02X%02X", rgb[0] & 255, rgb[1] & 255, rgb[2] & 255);
        return switch (hex) {
            case "#C6EFCE" -> "A";
            case "#FFEB9C" -> "B";
            case "#FFC7CE" -> "C";
            default -> null;
        };
    }

    private static Integer numeroMes(String nombre) {
        return switch (nombre) {
            case "ENERO" -> 1;
            case "FEBRERO" -> 2;
            case "MARZO" -> 3;
            case "ABRIL" -> 4;
            case "MAYO" -> 5;
            case "JUNIO" -> 6;
            case "JULIO" -> 7;
            case "AGOSTO" -> 8;
            case "SETIEMBRE", "SEPTIEMBRE" -> 9;
            case "OCTUBRE" -> 10;
            case "NOVIEMBRE" -> 11;
            case "DICIEMBRE" -> 12;
            default -> null;
        };
    }

    private static String nombreDia(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "LUN";
            case TUESDAY -> "MAR";
            case WEDNESDAY -> "MIER";
            case THURSDAY -> "JUE";
            case FRIDAY -> "VIE";
            case SATURDAY -> "SAB";
            case SUNDAY -> "DOM";
        };
    }

    private static String sinAcentos(String texto) {
        return Normalizer.normalize(normalizar(texto), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }

    // Algunos Excel antiguos escriben AUX1, AUX 1 o AUXILIAR 1.
    private static String normalizarCaseta(String texto) {
        String limpio = sinAcentos(texto);
        Matcher auxiliar = Pattern.compile("^AUX(?:ILIAR)?\\s*[- ]?\\s*(\\d+)$")
                .matcher(limpio);
        return auxiliar.matches() ? "AUX " + auxiliar.group(1) : limpio;
    }

    private String valor(Sheet hoja, int fila, int columna, DataFormatter formatter) {
        Row row = hoja.getRow(fila);
        if (row == null) return "";
        Cell celda = row.getCell(columna);
        if (celda == null) return "";
        if (celda.getCellType() == CellType.FORMULA) {
            throw new ProgramacionValidationException("No se permiten fórmulas en el Excel importado");
        }
        return formatter.formatCellValue(celda).trim();
    }

    private static void error(List<String> errores, String mensaje) {
        if (errores.size() < MAX_ERRORS) errores.add(mensaje);
    }
    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toUpperCase(Locale.ROOT);
    }
    private static String clave(int codigo, LocalDate fecha) {
        return codigo + "|" + fecha;
    }
}
