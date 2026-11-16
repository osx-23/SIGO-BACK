package com.sigo.asistencia.infrastructure.report;

import com.sigo.asistencia.application.port.in.ExportarPermutasExcelUseCase;
import com.sigo.asistencia.application.port.in.PermutaUseCase;
import com.sigo.asistencia.application.port.in.PermutaUseCase.PermutaDetalle;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exportación inspirada en Histórico de Permutas PEAJE:
 * RESUMEN (12 meses, plazas y ranking) + REGISTRO (10 columnas).
 * Los valores se obtienen de SIGO, no de un Excel antiguo.
 */
@Service
@RequiredArgsConstructor
public class ExportarPermutasExcelService implements ExportarPermutasExcelUseCase {
    private static final String[] MESES = {
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Set", "Oct", "Nov", "Dic"
    };
    private final PermutaUseCase permutas;
    private final PlazaRepository plazas;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportar(int anio, LocalDate inicio, LocalDate fin,
                          Long plazaId, Long turnoId, Integer codigo) {
        if (anio < 2000 || anio > 2100) throw new IllegalArgumentException("Año no válido");
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha inicial no puede superar la final");
        }
        List<PermutaDetalle> anuales = permutas.listar(
                LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31),
                null, null, null);
        List<PermutaDetalle> filtrados = permutas.listar(inicio, fin, plazaId, codigo, turnoId);
        SortedSet<String> nombresPlaza = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        plazas.findAll().forEach(p -> nombresPlaza.add(p.getCodigo()));
        for (PermutaDetalle p : anuales) {
            nombresPlaza.add(p.plazaSolicitante());
            nombresPlaza.add(p.plazaAceptante());
        }
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles estilos = new Styles(wb);
            resumen(wb, estilos, anio, anuales, new ArrayList<>(nombresPlaza));
            registro(wb, estilos, filtrados, inicio, fin, plazaId, turnoId, codigo);
            wb.setForceFormulaRecalculation(true);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel de permutas", e);
        }
    }

    private void resumen(XSSFWorkbook wb, Styles estilos, int anio,
                         List<PermutaDetalle> datos, List<String> plazas) {
        Sheet hoja = wb.createSheet("RESUMEN");
        int totalCol = plazas.size() + 1;
        int rankingCol = totalCol + 2;
        title(hoja, 0, 0, rankingCol + 2,
                "HISTÓRICO DE PERMUTAS " + anio + " · SIGO", estilos.titulo);
        title(hoja, 1, 0, rankingCol + 2,
                "Resumen anual por plaza y colaboradores. Cada intercambio cuenta una vez por mes.", estilos.nota);
        cell(hoja, 4, 0, "PERMUTAS POR PLAZA Y MES", estilos.seccion);
        cell(hoja, 4, rankingCol, "RANKING DE COLABORADORES", estilos.seccion);
        cell(hoja, 5, 0, "MES", estilos.cabecera);
        for (int i = 0; i < plazas.size(); i++) {
            cell(hoja, 5, i + 1, plazas.get(i), estilos.cabecera);
        }
        cell(hoja, 5, totalCol, "TOTAL", estilos.cabecera);
        cell(hoja, 5, rankingCol, "CÓDIGO", estilos.cabecera);
        cell(hoja, 5, rankingCol + 1, "COLABORADOR", estilos.cabecera);
        cell(hoja, 5, rankingCol + 2, "PERMUTAS", estilos.cabecera);

        Map<String, int[]> porPlaza = new HashMap<>();
        for (String plaza : plazas) porPlaza.put(plaza, new int[12]);
        int[] totales = new int[12];
        Map<Integer, Integer> participantes = new HashMap<>();
        Map<Integer, String> nombres = new HashMap<>();
        for (PermutaDetalle p : datos) {
            boolean sol = p.fechaSolicitante().getYear() == anio;
            boolean ace = p.fechaAceptante().getYear() == anio;
            int mesSol = p.fechaSolicitante().getMonthValue() - 1;
            int mesAce = p.fechaAceptante().getMonthValue() - 1;
            if (sol) {
                totales[mesSol]++;
                porPlaza.computeIfAbsent(p.plazaSolicitante(), k -> new int[12])[mesSol]++;
                participantes.merge(p.codigoSolicitante(), 1, Integer::sum);
                nombres.put(p.codigoSolicitante(), p.nombreSolicitante());
            }
            if (ace) {
                if (!sol || mesSol != mesAce) totales[mesAce]++;
                if (!sol || mesSol != mesAce ||
                        !p.plazaSolicitante().equalsIgnoreCase(p.plazaAceptante())) {
                    porPlaza.computeIfAbsent(p.plazaAceptante(), k -> new int[12])[mesAce]++;
                }
                participantes.merge(p.codigoAceptante(), 1, Integer::sum);
                nombres.put(p.codigoAceptante(), p.nombreAceptante());
            }
        }

        for (int m = 0; m < 12; m++) {
            int row = m + 6;
            cell(hoja, row, 0, MESES[m], estilos.mes);
            for (int i = 0; i < plazas.size(); i++) {
                number(hoja, row, i + 1, porPlaza.get(plazas.get(i))[m], estilos.numero);
            }
            number(hoja, row, totalCol, totales[m], estilos.totalDato);
        }
        cell(hoja, 18, 0, "TOTAL", estilos.cabecera);
        for (int i = 0; i < plazas.size(); i++) {
            int[] meses = porPlaza.get(plazas.get(i));
            number(hoja, 18, i + 1, Arrays.stream(meses).sum(), estilos.totalDato);
        }
        number(hoja, 18, totalCol, Arrays.stream(totales).sum(), estilos.totalDato);

        List<Map.Entry<Integer, Integer>> ranking = participantes.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        for (int i = 0; i < ranking.size(); i++) {
            var agente = ranking.get(i);
            int row = 6 + i;
            number(hoja, row, rankingCol, agente.getKey(), estilos.numero);
            cell(hoja, row, rankingCol + 1, nombres.get(agente.getKey()),
                    i % 2 == 0 ? estilos.fila : estilos.alternada);
            number(hoja, row, rankingCol + 2, agente.getValue(), estilos.totalDato);
        }
        hoja.setColumnWidth(0, 13 * 256);
        for (int i = 0; i < plazas.size(); i++) hoja.setColumnWidth(i + 1, 11 * 256);
        hoja.setColumnWidth(totalCol, 13 * 256);
        hoja.setColumnWidth(rankingCol, 15 * 256);
        hoja.setColumnWidth(rankingCol + 1, 48 * 256);
        hoja.setColumnWidth(rankingCol + 2, 15 * 256);
        hoja.createFreezePane(1, 6);
    }

    private void registro(XSSFWorkbook wb, Styles estilos,
                          List<PermutaDetalle> datos, LocalDate inicio,
                          LocalDate fin, Long plazaId, Long turnoId, Integer codigo) {
        Sheet hoja = wb.createSheet("REGISTRO");
        title(hoja, 0, 0, 9, "REGISTRO HISTÓRICO DE PERMUTAS · SIGO", estilos.titulo);
        String filtros = "Desde: " + (inicio == null ? "Todas" : inicio) +
                "   Hasta: " + (fin == null ? "Todas" : fin) +
                "   Plaza ID: " + (plazaId == null ? "Todas" : plazaId) +
                "   Turno ID: " + (turnoId == null ? "Todos" : turnoId) +
                "   Código: " + (codigo == null ? "Todos" : codigo);
        title(hoja, 1, 0, 9, filtros, estilos.nota);
        String[] headers = {
                "Plaza solicitante", "Código solicitante", "Nombre solicitante",
                "Turno solicitado", "Fecha solicitada", "Plaza aceptante",
                "Código aceptante", "Nombre aceptante",
                "Turno aceptante", "Fecha aceptante"
        };
        for (int col = 0; col < headers.length; col++) {
            cell(hoja, 3, col, headers[col], estilos.cabecera);
        }
        for (int i = 0; i < datos.size(); i++) {
            PermutaDetalle p = datos.get(i);
            int r = i + 4;
            CellStyle base = i % 2 == 0 ? estilos.fila : estilos.alternada;
            cell(hoja, r, 0, p.plazaSolicitante(), base);
            number(hoja, r, 1, p.codigoSolicitante(), estilos.numero);
            cell(hoja, r, 2, p.nombreSolicitante(), base);
            cell(hoja, r, 3, p.turnoSolicitante(), base);
            date(hoja, r, 4, p.fechaSolicitante(), estilos.fecha);
            cell(hoja, r, 5, p.plazaAceptante(), base);
            number(hoja, r, 6, p.codigoAceptante(), estilos.numero);
            cell(hoja, r, 7, p.nombreAceptante(), base);
            cell(hoja, r, 8, p.turnoAceptante(), base);
            date(hoja, r, 9, p.fechaAceptante(), estilos.fecha);
        }
        if (datos.isEmpty()) {
            title(hoja, 4, 0, 9,
                    "No existen permutas que coincidan con los filtros seleccionados.",
                    estilos.nota);
        }
        int[] widths = {18, 20, 46, 18, 19, 18, 20, 46, 18, 19};
        for (int i = 0; i < widths.length; i++) hoja.setColumnWidth(i, widths[i] * 256);
        hoja.createFreezePane(0, 4);
        hoja.setAutoFilter(new CellRangeAddress(3, Math.max(4, 3 + datos.size()), 0, 9));
    }

    private static Row row(Sheet hoja, int index) {
        Row r = hoja.getRow(index);
        return r == null ? hoja.createRow(index) : r;
    }
    private static Cell cell(Sheet hoja, int r, int col, String value, CellStyle style) {
        Cell c = row(hoja, r).createCell(col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
        row(hoja, r).setHeightInPoints(25);
        return c;
    }
    private static void number(Sheet hoja, int r, int col, int value, CellStyle style) {
        Cell c = row(hoja, r).createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
        row(hoja, r).setHeightInPoints(25);
    }
    private static void date(Sheet hoja, int r, int col, LocalDate value, CellStyle style) {
        Cell c = row(hoja, r).createCell(col);
        c.setCellValue(Date.valueOf(value));
        c.setCellStyle(style);
        row(hoja, r).setHeightInPoints(25);
    }
    private static void title(Sheet hoja, int row, int left, int right,
                              String txt, CellStyle style) {
        cell(hoja, row, left, txt, style);
        if (right > left) hoja.addMergedRegion(new CellRangeAddress(row, row, left, right));
        hoja.getRow(row).setHeightInPoints(row == 0 ? 32 : 24);
    }

    private static final class Styles {
        final CellStyle titulo;
        final CellStyle nota;
        final CellStyle cabecera;
        final CellStyle seccion;
        final CellStyle mes;
        final CellStyle fila;
        final CellStyle alternada;
        final CellStyle numero;
        final CellStyle totalDato;
        final CellStyle fecha;

        Styles(Workbook wb) {
            titulo = style(wb, IndexedColors.DARK_BLUE, IndexedColors.WHITE, true);
            nota = style(wb, IndexedColors.GREY_25_PERCENT, IndexedColors.BLACK, false);
            cabecera = style(wb, IndexedColors.BLUE, IndexedColors.WHITE, true);
            seccion = style(wb, IndexedColors.LIGHT_CORNFLOWER_BLUE, IndexedColors.DARK_BLUE, true);
            mes = style(wb, IndexedColors.GREY_25_PERCENT, IndexedColors.DARK_BLUE, true);
            fila = style(wb, IndexedColors.WHITE, IndexedColors.BLACK, false);
            alternada = style(wb, IndexedColors.PALE_BLUE, IndexedColors.BLACK, false);
            numero = style(wb, IndexedColors.WHITE, IndexedColors.DARK_BLUE, false);
            totalDato = style(wb, IndexedColors.LIGHT_CORNFLOWER_BLUE, IndexedColors.DARK_BLUE, true);
            fecha = style(wb, IndexedColors.WHITE, IndexedColors.BLACK, false);
            fecha.setDataFormat(wb.createDataFormat().getFormat("dd/mm/yyyy"));
        }
        private static CellStyle style(Workbook wb, IndexedColors bg, IndexedColors fg,
                                       boolean bold) {
            CellStyle s = wb.createCellStyle();
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setFillForegroundColor(bg.getIndex());
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setBorderBottom(BorderStyle.HAIR);
            s.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            Font f = wb.createFont();
            f.setBold(bold);
            f.setColor(fg.getIndex());
            s.setFont(f);
            return s;
        }
    }
}
