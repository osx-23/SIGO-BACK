package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.*;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Lector del rol real: pestañas AGENTES P4/P5 y bloques P4/P5 de CONTROL.
 * A diferencia del importador genérico, este reconoce las separaciones por
 * borde grueso, los días del mes tras el cierre del mes anterior y AP/BP/CP.
 * Todos los resultados están limitados a la plaza seleccionada.
 */
@Service
@RequiredArgsConstructor
public class RolPlazasExcelService implements RolPlazasExcelUseCase {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_FILAS = 150;
    private static final int MAX_HOJAS = 15;
    private static final int MAX_CAMBIOS = 1500;
    private static final Set<String> ESTADOS = Set.of("A", "B", "C", "D", "DM", "V", "COM", "LIC");
    private static final List<String> GRUPOS = List.of(
            "SECUENCIA_1", "SECUENCIA_2", "SECUENCIA_3", "SECUENCIA_4", "PART_TIME");

    private final JdbcTemplate jdbc;
    private final TrabajadorUseCase trabajadores;
    private final ListarTurnosUseCase turnos;
    private final GuardarTurnosUseCase guardarTurnos;
    private final ListarSecuenciasUseCase listarSecuencias;
    private final AsignarSecuenciaUseCase asignarSecuencia;
    private final GuardarOrdenSecuenciaUseCase guardarOrden;
    private final ListarLideresUseCase listarLideres;
    private final AsignarLiderUseCase asignarLider;

    @Override
    @Transactional(readOnly = true)
    public VistaPrevia previsualizar(Long plazaId, int anio, int mes, byte[] archivo) {
        if (plazaId == null || plazaId <= 0 || archivo == null
                || archivo.length == 0 || archivo.length > MAX_BYTES) {
            throw new ProgramacionValidationException("Selecciona un Excel .xlsx de máximo 2 MB");
        }
        if (anio < 2000 || anio > 2100 || mes < 1 || mes > 12) {
            throw new ProgramacionValidationException("Selecciona un mes y año válidos");
        }
        YearMonth periodo = YearMonth.of(anio, mes);
        // La lectura actual de SIGO valida el acceso a la plaza antes de analizar el archivo.
        var existentes = turnos.listar(plazaId, anio, mes);
        var codigos = jdbc.query("select codigo from plazas where id=? and activo=true",
                (rs, i) -> rs.getString(1), plazaId);
        if (codigos.isEmpty()) throw new ProgramacionValidationException("Plaza no existente o inactiva");
        String plaza = limpiar(codigos.get(0));

        Map<Integer, TrabajadorUseCase.TrabajadorData> global = porCodigo(trabajadores.listarAdministracion(null));
        Map<Integer, TrabajadorUseCase.TrabajadorData> agentes = porCodigo(trabajadores.listarAgentesPorPlaza(plazaId));
        Map<Integer, TrabajadorUseCase.TrabajadorData> controladores =
                porCodigo(trabajadores.listarControladoresPorPlaza(plazaId));
        Map<String, String> guardados = new HashMap<>();
        for (var t : existentes) {
            if (t.codigoTrabajador() != null)
                guardados.put(clave(t.codigoTrabajador(), t.fecha()), t.estado());
        }

        List<Persona> personal = new ArrayList<>();
        List<Turno> cambios = new ArrayList<>();
        List<Semana> semanas = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        Set<String> visitados = new HashSet<>();
        int[] contadores = new int[2]; // celdas leídas, coincidencias
        int ignoradas = 0;

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            if (libro.getNumberOfSheets() < 1 || libro.getNumberOfSheets() > MAX_HOJAS)
                throw new ProgramacionValidationException("Cantidad de hojas no permitida");
            Sheet agentesHoja = null;
            Sheet controlHoja = null;
            for (Sheet hoja : libro) {
                String nombre = limpiar(hoja.getSheetName());
                if (nombre.equals("AGENTES " + plaza)) {
                    if (agentesHoja != null)
                        throw new ProgramacionValidationException("Existen varias hojas de agentes para " + plaza);
                    agentesHoja = hoja;
                } else if (nombre.equals("CONTROL")) {
                    controlHoja = hoja;
                } else ignoradas++;
            }
            if (agentesHoja == null)
                throw new ProgramacionValidationException(
                        "El Excel no contiene una hoja 'AGENTES " + plaza + "'. No se importan otras plazas.");
            leerAgentes(agentesHoja, periodo, agentes, controladores, global, guardados,
                    personal, cambios, semanas, avisos, visitados, contadores);
            if (controlHoja != null)
                leerControl(controlHoja, plaza, periodo, controladores, global, guardados,
                        personal, cambios, avisos, visitados, contadores);
            else avisar(avisos, "No existe hoja CONTROL: no se propondrán los turnos de controladores.");
        } catch (ProgramacionValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProgramacionValidationException("No se pudo analizar el formato del rol P4/P5");
        }
        if (cambios.size() > MAX_CAMBIOS)
            throw new ProgramacionValidationException("El Excel excede el máximo de cambios admitidos");
        if (contadores[0] == 0)
            throw new ProgramacionValidationException("No se encontraron turnos para " + plaza + " en ese mes.");
        avisar(avisos, "Este rol contiene turnos y responsables, pero no códigos de casetas. Las casetas no se modifican.");
        if (periodo.isBefore(YearMonth.from(LocalDate.now()))) {
            avisar(avisos, "Periodo histórico: los responsables se mostrarán para revisión, pero no se reemplazarán relaciones de líder actualmente vigentes.");
        }
        return new VistaPrevia(plaza, anio, mes, ignoradas, contadores[0], contadores[1],
                List.copyOf(personal), List.copyOf(cambios),
                List.copyOf(semanas), List.copyOf(avisos));
    }

    @Override
    @Transactional
    public Resultado confirmar(Long plazaId, int anio, int mes, byte[] archivo) {
        // Análisis NUEVO dentro de la misma transacción; nunca se aceptan IDs del navegador.
        VistaPrevia previa = previsualizar(plazaId, anio, mes, archivo);
        if (previa.cambios().size() > MAX_CAMBIOS)
            throw new ProgramacionValidationException("Demasiados cambios de turnos");
        var guardables = previa.cambios().stream().map(t ->
                new GuardarTurnosUseCase.Item(t.trabajadorId(), t.fecha(), t.nuevo())).toList();
        if (!guardables.isEmpty())
            guardarTurnos.guardar(new GuardarTurnosUseCase.Command(plazaId, guardables));

        // Las secuencias se aplican solo a trabajadores activos, identificados por código y nombre.
        // Los bloques > 5 quedan pendientes, en vez de inventar una secuencia.
        var actuales = new HashMap<Long, String>();
        for (var s : listarSecuencias.listar(plazaId)) actuales.put(s.agenteId(), s.grupo());
        int gruposCambiados = 0;
        for (var p : previa.personal()) {
            if (!"AGENTE".equals(p.tipo()) || !"LISTO".equals(p.estado())
                    || p.trabajadorId() == null || p.grupo() == null) continue;
            if (!Objects.equals(actuales.get(p.trabajadorId()), p.grupo())) {
                asignarSecuencia.asignar(new AsignarSecuenciaUseCase.Command(
                        p.trabajadorId(), plazaId, p.grupo()));
                gruposCambiados++;
            }
        }
        // Orden de las secuencias: primero el orden del Excel, luego miembros ya
        // existentes pero que no aparecen en el fichero, sin eliminarlos.
        var despues = listarSecuencias.listar(plazaId);
        for (String grupo : GRUPOS) {
            var miembros = despues.stream().filter(p -> grupo.equals(p.grupo())).toList();
            if (miembros.isEmpty()) continue;
            Map<Long, Integer> ordenExcel = new HashMap<>();
            for (var p : previa.personal()) {
                if ("LISTO".equals(p.estado()) && grupo.equals(p.grupo()) && p.trabajadorId() != null)
                    ordenExcel.put(p.trabajadorId(), p.orden());
            }
            var ordenados = miembros.stream().sorted(
                    Comparator.comparingInt((ListarSecuenciasUseCase.Secuencia p) ->
                            ordenExcel.containsKey(p.agenteId()) ? 0 : 1)
                    .thenComparingInt(p -> ordenExcel.getOrDefault(p.agenteId(),
                            p.orden() == null ? Integer.MAX_VALUE : p.orden()))
                    .thenComparing(ListarSecuenciasUseCase.Secuencia::agenteId)).toList();
            List<GuardarOrdenSecuenciaUseCase.Item> ordenFinal = new ArrayList<>();
            for (int i = 0; i < ordenados.size(); i++)
                ordenFinal.add(new GuardarOrdenSecuenciaUseCase.Item(ordenados.get(i).agenteId(), i + 1));
            guardarOrden.guardar(new GuardarOrdenSecuenciaUseCase.Command(plazaId, grupo, ordenFinal));
        }

        int lideresCambiados = 0;
        // La relación agente-controlador actual no es mensual en SIGO. Un archivo
        // histórico no debe alterar quién es responsable actualmente.
        if (!YearMonth.of(anio, mes).isBefore(YearMonth.from(LocalDate.now()))) {
            Map<Long, Long> liderActual = new HashMap<>();
            for (var p : listarLideres.listar(plazaId))
                if (Boolean.TRUE.equals(p.activo())) liderActual.put(p.agenteId(), p.controladorId());
            for (var p : previa.personal()) {
                if (!"AGENTE".equals(p.tipo()) || !"LISTO".equals(p.estado())
                        || p.trabajadorId() == null || p.controladorId() == null) continue;
                if (!Objects.equals(liderActual.get(p.trabajadorId()), p.controladorId())) {
                    asignarLider.asignar(new AsignarLiderUseCase.Command(
                            p.trabajadorId(), p.controladorId(), plazaId,
                            YearMonth.of(anio, mes).atDay(1)));
                    lideresCambiados++;
                }
            }
        }

        int omitidos = (int) previa.personal().stream().filter(p -> !"LISTO".equals(p.estado())).count();
        return new Resultado(guardables.size(), gruposCambiados, lideresCambiados, omitidos,
                "Aplicados turnos y secuencias de " + previa.plazaCodigo() +
                ". Las casetas no se modificaron; personal sin validar se omitió.");
    }

    private static void leerAgentes(Sheet hoja, YearMonth periodo,
            Map<Integer, TrabajadorUseCase.TrabajadorData> agentes,
            Map<Integer, TrabajadorUseCase.TrabajadorData> controladores,
            Map<Integer, TrabajadorUseCase.TrabajadorData> global,
            Map<String, String> guardados, List<Persona> personal, List<Turno> cambios,
            List<Semana> semanas, List<String> avisos, Set<String> visitados, int[] contadores) {
        if (hoja.getLastRowNum() > MAX_FILAS)
            throw new ProgramacionValidationException("Demasiadas filas en AGENTES");
        DataFormatter fmt = new DataFormatter(Locale.ROOT);
        Row cabecera = hoja.getRow(1);
        Row encabezadoDias = hoja.getRow(2);
        if (cabecera == null || encabezadoDias == null
                || !limpiar(valor(cabecera.getCell(1), fmt)).startsWith("COD")
                || !limpiar(valor(cabecera.getCell(2), fmt)).startsWith("NOMBRES"))
            throw new ProgramacionValidationException("No se reconocieron columnas Cod y Nombres en AGENTES");
        int inicioMes = buscarMes(encabezadoDias, 5, periodo, fmt);
        validarDiaSemana(cabecera, inicioMes, periodo, fmt);
        int bloque = 0, orden = 0;
        boolean finBloqueAnterior = false;
        Row primeraPartTime = null;
        Set<Integer> trabajadoresVistos = new HashSet<>();
        for (int r = 3; r <= hoja.getLastRowNum(); r++) {
            Row fila = hoja.getRow(r);
            if (fila == null) continue;
            Integer codigo = numero(fila.getCell(1), fmt);
            if (codigo == null) continue; // Leyendas y totales.
            String nombre = valor(fila.getCell(2), fmt).trim();
            if (nombre.isBlank()) { avisar(avisos, "Código " + codigo + " sin nombre; fila omitida"); continue; }
            if (!trabajadoresVistos.add(codigo)) {
                avisar(avisos, "Código " + codigo + " aparece dos veces en AGENTES; verificar duplicidad");
                continue;
            }
            boolean delimitador = bloque == 0 || hayBorde(fila, 1, 2, inicioMes, "top")
                    || finBloqueAnterior;
            if (delimitador) { bloque++; orden = 0; }
            finBloqueAnterior = hayBorde(fila, 1, 2, inicioMes, "bottom");
            orden++;
            String grupo = bloque <= GRUPOS.size() ? GRUPOS.get(bloque - 1) : null;
            if ("PART_TIME".equals(grupo) && primeraPartTime == null) primeraPartTime = fila;

            Integer codLider = numero(fila.getCell(3), fmt);
            TrabajadorUseCase.TrabajadorData lider = codLider == null ? null : controladores.get(codLider);
            TrabajadorUseCase.TrabajadorData trabajador = agentes.get(codigo);
            boolean valido = trabajador != null && Boolean.TRUE.equals(trabajador.activo())
                    && limpiar(nombre).equals(limpiar(trabajador.nombreCompleto()));
            String motivo = !valido ? motivoPersonal(codigo, nombre, global)
                    : grupo == null ? "La fila está fuera de los cuatro bloques de secuencia y del bloque part-time; grupo sin asignar."
                    : codLider != null && lider == null ? "Controlador " + codLider + " no está activo en la plaza."
                    : "";
            String estado = !valido ? "REVISAR" : grupo == null ? "REVISAR_GRUPO" : "LISTO";
            if (!motivo.isBlank()) avisar(avisos, "Código " + codigo + ": " + motivo);
            personal.add(new Persona(codigo, nombre, valido ? trabajador.id() : null,
                    grupo, orden, codLider, lider == null ? null : lider.id(),
                    "AGENTE", estado, motivo));
            if (valido) leerTurnos(fila, fmt, inicioMes, periodo, codigo, trabajador.id(),
                    guardados, cambios, avisos, visitados, contadores, hoja.getSheetName());
        }
        if (primeraPartTime != null) {
            List<Integer> inicios = new ArrayList<>();
            for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
                int col = inicioMes + dia - 1;
                if (dia == 1 || grueso(primeraPartTime.getCell(col), "left")) inicios.add(dia);
            }
            for (int i = 0; i < inicios.size(); i++) {
                int desde = inicios.get(i), hasta = i + 1 < inicios.size()
                        ? inicios.get(i + 1) - 1 : periodo.lengthOfMonth();
                semanas.add(new Semana(periodo.atDay(desde), periodo.atDay(hasta)));
            }
            if (inicios.size() < 4) avisar(avisos,
                    "Los bordes de semanas part-time son incompletos; comprueba el calendario.");
        } else avisar(avisos, "No se identificó un bloque part-time delimitado.");
    }

    private static void leerControl(Sheet hoja, String plaza, YearMonth periodo,
            Map<Integer, TrabajadorUseCase.TrabajadorData> controladores,
            Map<Integer, TrabajadorUseCase.TrabajadorData> global,
            Map<String, String> guardados, List<Persona> personal, List<Turno> cambios,
            List<String> avisos, Set<String> visitados, int[] contadores) {
        if (hoja.getLastRowNum() > MAX_FILAS)
            throw new ProgramacionValidationException("Demasiadas filas en CONTROL");
        DataFormatter fmt = new DataFormatter(Locale.ROOT);
        boolean dentro = false;
        int inicioMes = -1, orden = 0;
        for (int r = 0; r <= hoja.getLastRowNum(); r++) {
            Row fila = hoja.getRow(r);
            if (fila == null) continue;
            String nombreBloque = limpiar(valor(fila.getCell(1), fmt));
            if (nombreBloque.startsWith("PILAR DE GESTION")) {
                dentro = nombreBloque.contains("\"" + plaza + "\"") ||
                        nombreBloque.endsWith(" " + plaza);
                inicioMes = -1;
                continue;
            }
            if (!dentro) continue;
            // La fila de números de día sigue a la cabecera con COD. y NOMBRES.
            if (inicioMes < 0 && "1".equals(valor(fila.getCell(4), fmt).trim())) {
                inicioMes = buscarMes(fila, 4, periodo, fmt);
                Row diasSemana = hoja.getRow(r - 1);
                validarDiaSemana(diasSemana, inicioMes, periodo, fmt);
                continue;
            }
            if (inicioMes < 0) continue;
            Integer codigo = numero(fila.getCell(2), fmt);
            if (codigo == null) continue;
            String nombre = valor(fila.getCell(3), fmt).trim();
            var trabajador = controladores.get(codigo);
            boolean valido = trabajador != null && Boolean.TRUE.equals(trabajador.activo())
                    && limpiar(nombre).equals(limpiar(trabajador.nombreCompleto()));
            String motivo = valido ? "" : motivoPersonal(codigo, nombre, global);
            if (!motivo.isBlank()) avisar(avisos, "Controlador " + codigo + ": " + motivo);
            personal.add(new Persona(codigo, nombre, valido ? trabajador.id() : null,
                    null, ++orden, null, null, "CONTROLADOR",
                    valido ? "LISTO" : "REVISAR", motivo));
            if (valido) leerTurnos(fila, fmt, inicioMes, periodo, codigo, trabajador.id(),
                    guardados, cambios, avisos, visitados, contadores, hoja.getSheetName());
        }
    }

    private static void leerTurnos(Row fila, DataFormatter fmt, int primerDia,
            YearMonth periodo, int codigo, Long id,
            Map<String, String> guardados, List<Turno> cambios,
            List<String> avisos, Set<String> visitados, int[] contadores, String hoja) {
        for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
            String excel = limpiar(valor(fila.getCell(primerDia + dia - 1), fmt));
            if (excel.isBlank()) continue;
            contadores[0]++;
            String turno = switch (excel) {
                case "AP" -> "A";
                case "BP" -> "B";
                case "CP" -> "C";
                default -> excel;
            };
            LocalDate fecha = periodo.atDay(dia);
            String clave = clave(codigo, fecha);
            if (!ESTADOS.contains(turno)) {
                avisar(avisos, hoja + ": código " + codigo + " " + fecha
                        + ", valor '" + excel + "' no reconocido; se omitió.");
                continue;
            }
            if (!visitados.add(clave)) {
                throw new ProgramacionValidationException(
                        "Duplicado de código " + codigo + " y fecha " + fecha + " en el Excel.");
            }
            String anterior = guardados.get(clave);
            if (Objects.equals(anterior, turno)) contadores[1]++;
            else cambios.add(new Turno(id, codigo, fecha, anterior, turno, hoja));
        }
    }

    private static int buscarMes(Row fechas, int desde, YearMonth periodo, DataFormatter fmt) {
        if (fechas == null) throw new ProgramacionValidationException("Falta fila de fechas");
        for (int columna = desde; columna < Math.min(70, fechas.getLastCellNum()); columna++) {
            if (!"1".equals(valor(fechas.getCell(columna), fmt).trim())) continue;
            boolean consecutivos = true;
            for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
                if (!Integer.toString(dia).equals(valor(fechas.getCell(columna + dia - 1), fmt).trim())) {
                    consecutivos = false; break;
                }
            }
            if (consecutivos) return columna;
        }
        throw new ProgramacionValidationException(
                "No se encontraron los días consecutivos del " + periodo
                + ". Selecciona el mes/año correspondiente al rol.");
    }

    private static void validarDiaSemana(Row encabezado, int primerDia,
                                         YearMonth periodo, DataFormatter fmt) {
        if (encabezado == null) return;
        String excel = limpiar(valor(encabezado.getCell(primerDia), fmt));
        String esperado = switch (periodo.atDay(1).getDayOfWeek()) {
            case MONDAY -> "L";
            case TUESDAY -> "M";
            case WEDNESDAY -> "M";
            case THURSDAY -> "J";
            case FRIDAY -> "V";
            case SATURDAY -> "S";
            case SUNDAY -> "D";
        };
        if (!excel.isBlank() && !esperado.equals(excel))
            throw new ProgramacionValidationException("El día de la semana no coincide con " + periodo);
    }

    private static boolean hayBorde(Row fila, int colCodigo, int colNombre,
                                    int primerDia, String lado) {
        return grueso(fila.getCell(colCodigo), lado)
                || grueso(fila.getCell(colNombre), lado)
                || grueso(fila.getCell(primerDia), lado);
    }

    private static boolean grueso(Cell celda, String lado) {
        if (celda == null) return false;
        CellStyle estilo = celda.getCellStyle();
        BorderStyle borde = switch (lado) {
            case "top" -> estilo.getBorderTop();
            case "bottom" -> estilo.getBorderBottom();
            case "left" -> estilo.getBorderLeft();
            default -> BorderStyle.NONE;
        };
        return borde == BorderStyle.MEDIUM || borde == BorderStyle.THICK || borde == BorderStyle.DOUBLE;
    }

    private static String valor(Cell celda, DataFormatter fmt) {
        if (celda == null) return "";
        if (celda.getCellType() == CellType.FORMULA)
            throw new ProgramacionValidationException("No se permiten fórmulas en el rol de turnos.");
        return fmt.formatCellValue(celda).trim();
    }

    private static Integer numero(Cell celda, DataFormatter fmt) {
        String t = valor(celda, fmt);
        if (t.isBlank() || !t.matches("\\d{1,8}")) return null;
        return Integer.parseInt(t);
    }

    private static Map<Integer, TrabajadorUseCase.TrabajadorData> porCodigo(
            List<TrabajadorUseCase.TrabajadorData> origen) {
        Map<Integer, TrabajadorUseCase.TrabajadorData> mapa = new HashMap<>();
        if (origen != null) for (var persona : origen) {
            if (persona.codigo() != null) mapa.put(persona.codigo(), persona);
        }
        return mapa;
    }

    private static String motivoPersonal(int codigo, String nombre,
            Map<Integer, TrabajadorUseCase.TrabajadorData> global) {
        var globalPersona = global.get(codigo);
        if (globalPersona == null) return "No figura en SIGO; requiere alta supervisada antes de importar sus turnos.";
        if (!Boolean.TRUE.equals(globalPersona.activo()))
            return "Figura en SIGO, pero está inactivo.";
        if (!limpiar(nombre).equals(limpiar(globalPersona.nombreCompleto())))
            return "El código pertenece a otra persona en SIGO; verifica la identidad.";
        return "El trabajador existe en SIGO, pero no está activo con el cargo correspondiente en esta plaza.";
    }

    private static String limpiar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor.replace('\u00A0', ' ').trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }
    private static String clave(int codigo, LocalDate fecha) {
        return codigo + "|" + fecha;
    }
    private static void avisar(List<String> avisos, String mensaje) {
        if (avisos.size() < 100) avisos.add(mensaje);
    }
}
