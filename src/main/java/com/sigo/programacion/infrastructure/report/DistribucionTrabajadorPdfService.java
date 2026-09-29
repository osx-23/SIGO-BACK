package com.sigo.programacion.infrastructure.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.out.DistribucionReportePdfPort;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DistribucionTrabajadorPdfService
        implements DistribucionReportePdfPort {

    private static final Color AZUL =
            new Color(29, 78, 216);

    private static final Color AZUL_SUAVE =
            new Color(239, 246, 255);

    private static final Color GRIS =
            new Color(71, 85, 105);

    private static final Color BORDE =
            new Color(226, 232, 240);

    private static final Color MATRIZ_CABECERA =
            new Color(173, 190, 211);

    private static final Color MATRIZ_DIA =
            new Color(31, 78, 121);

    private static final Color MATRIZ_A =
            new Color(198, 239, 206);

    private static final Color MATRIZ_B =
            new Color(255, 235, 156);

    private static final Color MATRIZ_C =
            new Color(255, 199, 206);

    private static final Color MATRIZ_DESCANSO =
            new Color(191, 191, 191);

    private static final Color MATRIZ_VACACIONES =
            new Color(189, 215, 238);

    private static final Color MATRIZ_COMPENSACION =
            new Color(217, 210, 233);

    private static final Color MATRIZ_DM =
            new Color(244, 204, 204);

    private static final Color MATRIZ_LICENCIA =
            new Color(234, 209, 220);

    private static final Color MATRIZ_SIN_ASIGNAR =
            new Color(255, 230, 230);

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public byte[] generar(
            DistribucionUseCase.ReporteTrabajador reporte,
            LocalDate desde,
            LocalDate hasta
    ) {
        try {
            ByteArrayOutputStream salida =
                    new ByteArrayOutputStream();

            Document documento =
                    new Document(
                            PageSize.A4.rotate(),
                            34,
                            34,
                            30,
                            30
                    );

            PdfWriter.getInstance(
                    documento,
                    salida
            );

            documento.open();

            agregarCabecera(
                    documento,
                    reporte,
                    desde,
                    hasta
            );

            agregarResumen(
                    documento,
                    reporte
            );

            agregarFrecuencia(
                    documento,
                    reporte
            );

            agregarDetalle(
                    documento,
                    reporte
            );

            documento.close();

            return salida.toByteArray();
        }
        catch (DocumentException exception) {
            throw new IllegalStateException(
                    "No se pudo construir el PDF de distribución",
                    exception
            );
        }
    }

    @Override
    public byte[] generarMatriz(
            DistribucionUseCase.ReportePlaza reporte
    ) {
        try {
            ByteArrayOutputStream salida =
                    new ByteArrayOutputStream();

            Document documento =
                    new Document(
                            PageSize.A4.rotate(),
                            18,
                            18,
                            18,
                            18
                    );

            PdfWriter.getInstance(
                    documento,
                    salida
            );

            documento.open();

            List<LocalDate> fechas =
                    fechasEntre(
                            reporte.desde(),
                            reporte.hasta()
                    );

            List<List<LocalDate>> bloques =
                    dividirFechas(
                            fechas,
                            7
                    );

            if (bloques.isEmpty()) {
                bloques =
                        List.of(
                                List.of(
                                        reporte.desde()
                                )
                        );
            }

            Map<Long, List<DistribucionUseCase.MatrizItem>> porTrabajador =
                    reporte.items()
                            .stream()
                            /*
                             * El adapter ya entrega los agentes en el mismo
                             * orden visual de la pantalla:
                             * SECUENCIA_1..4 y luego PART_TIME.
                             * LinkedHashMap conserva ese orden en el PDF.
                             */
                            .collect(
                                    java.util.stream.Collectors.groupingBy(
                                            DistribucionUseCase.MatrizItem::trabajadorId,
                                            LinkedHashMap::new,
                                            java.util.stream.Collectors.toList()
                                    )
                            );

            for (
                    int indice = 0;
                    indice < bloques.size();
                    indice++
            ) {
                if (indice > 0) {
                    documento.newPage();
                }

                List<LocalDate> bloque =
                        bloques.get(
                                indice
                        );

                agregarCabeceraMatriz(
                        documento,
                        reporte,
                        bloque,
                        indice + 1,
                        bloques.size()
                );

                agregarTablaMatriz(
                        documento,
                        porTrabajador,
                        bloque
                );

                agregarLeyendaMatriz(
                        documento
                );
            }

            documento.close();

            return salida.toByteArray();
        }
        catch (DocumentException exception) {
            throw new IllegalStateException(
                    "No se pudo construir el PDF matricial de distribución",
                    exception
            );
        }
    }


    private void agregarCabeceraMatriz(
            Document documento,
            DistribucionUseCase.ReportePlaza reporte,
            List<LocalDate> fechas,
            int pagina,
            int totalPaginas
    ) throws DocumentException {

        String mes =
                reporte.desde()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "MMMM",
                                        new Locale(
                                                "es",
                                                "PE"
                                        )
                                )
                        )
                        .toUpperCase(
                                new Locale(
                                        "es",
                                        "PE"
                                )
                        );

        Font titulo =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        17,
                        new Color(
                                15,
                                23,
                                42
                        )
                );

        Paragraph encabezado =
                new Paragraph(
                        "DISTRIBUCIÓN "
                                + mes
                                + " "
                                + reporte.plazaCodigo(),
                        titulo
                );

        encabezado.setAlignment(
                Element.ALIGN_CENTER
        );

        encabezado.setSpacingAfter(
                4
        );

        documento.add(
                encabezado
        );

        Font detalle =
                FontFactory.getFont(
                        FontFactory.HELVETICA,
                        7.5f,
                        GRIS
                );

        String rango =
                FECHA.format(
                        fechas.get(0)
                )
                        + " - "
                        + FECHA.format(
                                fechas.get(
                                        fechas.size() - 1
                                )
                        );

        Paragraph periodo =
                new Paragraph(
                        "Periodo visible: "
                                + rango
                                + (
                                        totalPaginas > 1
                                                ? "   ·   Página "
                                                        + pagina
                                                        + " de "
                                                        + totalPaginas
                                                : ""
                                ),
                        detalle
                );

        periodo.setAlignment(
                Element.ALIGN_CENTER
        );

        periodo.setSpacingAfter(
                8
        );

        documento.add(
                periodo
        );
    }


    private void agregarTablaMatriz(
            Document documento,
            Map<Long, List<DistribucionUseCase.MatrizItem>> porTrabajador,
            List<LocalDate> fechas
    ) throws DocumentException {

        float[] anchos =
                new float[
                        3
                                + fechas.size()
                ];

        anchos[0] =
                .7f;

        anchos[1] =
                1.15f;

        anchos[2] =
                4.3f;

        for (
                int i = 3;
                i < anchos.length;
                i++
        ) {
            anchos[i] =
                    1.15f;
        }

        PdfPTable tabla =
                new PdfPTable(
                        anchos
                );

        tabla.setWidthPercentage(
                100
        );

        tabla.setHeaderRows(
                2
        );

        tabla.addCell(
                cabeceraMatrizFija(
                        "N°"
                )
        );

        tabla.addCell(
                cabeceraMatrizFija(
                        "Cod"
                )
        );

        tabla.addCell(
                cabeceraMatrizFija(
                        "Nombre"
                )
        );

        for (
                LocalDate fecha :
                fechas
        ) {
            tabla.addCell(
                    cabeceraMatrizDia(
                            diaSemanaCorto(
                                    fecha
                            )
                    )
            );
        }

        for (
                LocalDate fecha :
                fechas
        ) {
            tabla.addCell(
                    cabeceraMatrizNumeroDia(
                            String.valueOf(
                                    fecha.getDayOfMonth()
                            )
                    )
            );
        }

        if (
                porTrabajador.isEmpty()
        ) {
            PdfPCell vacia =
                    celda(
                            "No existen programaciones para el periodo seleccionado"
                    );

            vacia.setColspan(
                    3 + fechas.size()
            );

            vacia.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            tabla.addCell(
                    vacia
            );
        }
        else {
            int numero =
                    1;

            for (
                    List<DistribucionUseCase.MatrizItem> items :
                    porTrabajador.values()
            ) {
                DistribucionUseCase.MatrizItem base =
                        items.get(0);

                Map<LocalDate, DistribucionUseCase.MatrizItem> porFecha =
                        items.stream()
                                .collect(
                                        java.util.stream.Collectors.toMap(
                                                DistribucionUseCase.MatrizItem::fecha,
                                                item ->
                                                        item,
                                                (a, b) ->
                                                        a,
                                                LinkedHashMap::new
                                        )
                                );

                tabla.addCell(
                        celdaMatrizTexto(
                                String.valueOf(
                                        numero++
                                ),
                                Element.ALIGN_CENTER,
                                true
                        )
                );

                tabla.addCell(
                        celdaMatrizTexto(
                                base.codigoTrabajador() == null
                                        ? ""
                                        : String.valueOf(
                                                base.codigoTrabajador()
                                        ),
                                Element.ALIGN_CENTER,
                                false
                        )
                );

                tabla.addCell(
                        celdaMatrizTexto(
                                base.nombreTrabajador(),
                                Element.ALIGN_LEFT,
                                false
                        )
                );

                for (
                        LocalDate fecha :
                        fechas
                ) {
                    DistribucionUseCase.MatrizItem item =
                            porFecha.get(
                                    fecha
                            );

                    tabla.addCell(
                            celdaMatrizAsignacion(
                                    item
                            )
                    );
                }
            }
        }

        documento.add(
                tabla
        );
    }


    private PdfPCell cabeceraMatrizFija(
            String texto
    ) {

        PdfPCell cell =
                nuevaCeldaMatriz(
                        texto,
                        8.5f,
                        true,
                        Color.BLACK,
                        MATRIZ_CABECERA,
                        Element.ALIGN_CENTER
                );

        cell.setRowspan(
                2
        );

        return cell;
    }


    private PdfPCell cabeceraMatrizDia(
            String texto
    ) {

        return nuevaCeldaMatriz(
                texto,
                7.5f,
                true,
                Color.BLACK,
                MATRIZ_CABECERA,
                Element.ALIGN_CENTER
        );
    }


    private PdfPCell cabeceraMatrizNumeroDia(
            String texto
    ) {

        return nuevaCeldaMatriz(
                texto,
                8.5f,
                true,
                Color.WHITE,
                MATRIZ_DIA,
                Element.ALIGN_CENTER
        );
    }


    private PdfPCell celdaMatrizTexto(
            String texto,
            int alineacion,
            boolean negrita
    ) {

        return nuevaCeldaMatriz(
                texto,
                7.3f,
                negrita,
                new Color(
                        15,
                        23,
                        42
                ),
                Color.WHITE,
                alineacion
        );
    }


    private PdfPCell celdaMatrizAsignacion(
            DistribucionUseCase.MatrizItem item
    ) {

        if (item == null) {
            return nuevaCeldaMatriz(
                    "",
                    7.2f,
                    false,
                    GRIS,
                    Color.WHITE,
                    Element.ALIGN_CENTER
            );
        }

        String estado =
                item.estado() == null
                        ? ""
                        : item.estado()
                                .trim()
                                .toUpperCase();

        boolean operativo =
                estado.equals("A")
                        || estado.equals("B")
                        || estado.equals("C");

        String texto =
                operativo
                        ? (
                                item.ubicacionCodigo() == null
                                        || item.ubicacionCodigo().isBlank()
                                        ? "SIN ASIG."
                                        : item.ubicacionCodigo()
                        )
                        : estado;

        Color fondo =
                colorEstadoMatriz(
                        estado,
                        operativo
                                && (
                                        item.ubicacionCodigo() == null
                                                || item.ubicacionCodigo().isBlank()
                                )
                );

        Color textoColor =
                texto.equals(
                        "SIN ASIG."
                )
                        ? new Color(
                                185,
                                28,
                                28
                        )
                        : new Color(
                                31,
                                41,
                                55
                        );

        return nuevaCeldaMatriz(
                texto,
                texto.length() > 7
                        ? 6.1f
                        : 7.5f,
                true,
                textoColor,
                fondo,
                Element.ALIGN_CENTER
        );
    }


    private PdfPCell nuevaCeldaMatriz(
            String texto,
            float tamanio,
            boolean negrita,
            Color colorTexto,
            Color fondo,
            int alineacion
    ) {

        Font font =
                FontFactory.getFont(
                        negrita
                                ? FontFactory.HELVETICA_BOLD
                                : FontFactory.HELVETICA,
                        tamanio,
                        colorTexto
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                texto == null
                                        ? ""
                                        : texto,
                                font
                        )
                );

        cell.setBackgroundColor(
                fondo
        );

        cell.setBorderColor(
                new Color(
                        71,
                        85,
                        105
                )
        );

        cell.setBorderWidth(
                .55f
        );

        cell.setPaddingTop(
                5
        );

        cell.setPaddingBottom(
                5
        );

        cell.setPaddingLeft(
                4
        );

        cell.setPaddingRight(
                4
        );

        cell.setHorizontalAlignment(
                alineacion
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        return cell;
    }


    private Color colorEstadoMatriz(
            String estado,
            boolean sinAsignar
    ) {

        if (sinAsignar) {
            return MATRIZ_SIN_ASIGNAR;
        }

        return switch (
                estado
        ) {
            case "A" ->
                    MATRIZ_A;

            case "B" ->
                    MATRIZ_B;

            case "C" ->
                    MATRIZ_C;

            case "D" ->
                    MATRIZ_DESCANSO;

            case "V" ->
                    MATRIZ_VACACIONES;

            case "COM" ->
                    MATRIZ_COMPENSACION;

            case "DM" ->
                    MATRIZ_DM;

            case "LIC" ->
                    MATRIZ_LICENCIA;

            default ->
                    Color.WHITE;
        };
    }


    private void agregarLeyendaMatriz(
            Document documento
    ) throws DocumentException {

        Font font =
                FontFactory.getFont(
                        FontFactory.HELVETICA,
                        6.8f,
                        GRIS
                );

        Paragraph leyenda =
                new Paragraph(
                        "Colores por turno: A 06:00–14:00 · B 14:00–22:00 · C 22:00–06:00 · "
                                + "D descanso · V vacaciones · COM compensación · DM descanso médico · LIC licencia",
                        font
                );

        leyenda.setSpacingBefore(
                6
        );

        leyenda.setAlignment(
                Element.ALIGN_LEFT
        );

        documento.add(
                leyenda
        );
    }


    private List<LocalDate> fechasEntre(
            LocalDate desde,
            LocalDate hasta
    ) {

        List<LocalDate> fechas =
                new ArrayList<>();

        LocalDate actual =
                desde;

        while (
                !actual.isAfter(
                        hasta
                )
        ) {
            fechas.add(
                    actual
            );

            actual =
                    actual.plusDays(
                            1
                    );
        }

        return fechas;
    }


    private List<List<LocalDate>> dividirFechas(
            List<LocalDate> fechas,
            int tamanio
    ) {

        List<List<LocalDate>> bloques =
                new ArrayList<>();

        for (
                int i = 0;
                i < fechas.size();
                i += tamanio
        ) {
            bloques.add(
                    new ArrayList<>(
                            fechas.subList(
                                    i,
                                    Math.min(
                                            i + tamanio,
                                            fechas.size()
                                    )
                            )
                    )
            );
        }

        return bloques;
    }


    private String diaSemanaCorto(
            LocalDate fecha
    ) {

        return switch (
                fecha.getDayOfWeek()
        ) {
            case MONDAY ->
                    "LUN";

            case TUESDAY ->
                    "MAR";

            case WEDNESDAY ->
                    "MIÉ";

            case THURSDAY ->
                    "JUE";

            case FRIDAY ->
                    "VIE";

            case SATURDAY ->
                    "SÁB";

            case SUNDAY ->
                    "DOM";
        };
    }


    private void agregarCabecera(
            Document documento,
            DistribucionUseCase.ReporteTrabajador reporte,
            LocalDate desde,
            LocalDate hasta
    ) throws DocumentException {

        Font titulo =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        17,
                        AZUL
                );

        Font subtitulo =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        11,
                        Color.DARK_GRAY
                );

        Font texto =
                FontFactory.getFont(
                        FontFactory.HELVETICA,
                        9,
                        GRIS
                );

        Paragraph encabezado =
                new Paragraph(
                        "SIGO · Reporte de asignación de casetas",
                        titulo
                );

        encabezado.setSpacingAfter(8);

        documento.add(
                encabezado
        );

        DistribucionUseCase.ResumenTrabajador resumen =
                reporte.resumen();

        Paragraph agente =
                new Paragraph(
                        "Agente: "
                                + resumen.codigo()
                                + " · "
                                + resumen.nombre(),
                        subtitulo
                );

        agente.setSpacingAfter(4);

        documento.add(
                agente
        );

        Paragraph periodo =
                new Paragraph(
                        "Periodo: "
                                + FECHA.format(desde)
                                + " al "
                                + FECHA.format(hasta),
                        texto
                );

        periodo.setSpacingAfter(14);

        documento.add(
                periodo
        );
    }

    private void agregarResumen(
            Document documento,
            DistribucionUseCase.ReporteTrabajador reporte
    ) throws DocumentException {

        long total =
                reporte.distribuciones()
                        .size();

        long distintas =
                reporte.resumen()
                        .ubicaciones()
                        .stream()
                        .filter(
                                item ->
                                        item.veces() > 0
                        )
                        .count();

        PdfPTable tabla =
                new PdfPTable(2);

        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(16);

        tabla.addCell(
                tarjeta(
                        "Asignaciones",
                        String.valueOf(total)
                )
        );

        tabla.addCell(
                tarjeta(
                        "Casetas distintas",
                        String.valueOf(distintas)
                )
        );

        documento.add(
                tabla
        );
    }

    private void agregarFrecuencia(
            Document documento,
            DistribucionUseCase.ReporteTrabajador reporte
    ) throws DocumentException {

        Paragraph titulo =
                seccion(
                        "Frecuencia por caseta"
                );

        documento.add(
                titulo
        );

        PdfPTable tabla =
                new PdfPTable(
                        new float[]{
                                1.2f,
                                4.2f,
                                1f
                        }
                );

        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(16);

        cabecera(
                tabla,
                "Código"
        );

        cabecera(
                tabla,
                "Caseta / ubicación"
        );

        cabecera(
                tabla,
                "Veces"
        );

        if (
                reporte.resumen()
                        .ubicaciones()
                        .isEmpty()
        ) {
            PdfPCell vacia =
                    celda(
                            "Sin asignaciones en el periodo"
                    );

            vacia.setColspan(3);

            tabla.addCell(
                    vacia
            );
        }
        else {
            reporte.resumen()
                    .ubicaciones()
                    .stream()
                    .sorted(
                            (a, b) ->
                                    Long.compare(
                                            b.veces(),
                                            a.veces()
                                    )
                    )
                    .forEach(
                            item -> {
                                tabla.addCell(
                                        celda(
                                                item.codigo()
                                        )
                                );

                                tabla.addCell(
                                        celda(
                                                item.nombre()
                                        )
                                );

                                tabla.addCell(
                                        celda(
                                                String.valueOf(
                                                        item.veces()
                                                )
                                        )
                                );
                            }
                    );
        }

        documento.add(
                tabla
        );
    }

    private void agregarDetalle(
            Document documento,
            DistribucionUseCase.ReporteTrabajador reporte
    ) throws DocumentException {

        documento.add(
                seccion(
                        "Detalle diario"
                )
        );

        PdfPTable tabla =
                new PdfPTable(
                        new float[]{
                                1.2f,
                                .8f,
                                1f,
                                2.8f,
                                1.2f
                        }
                );

        tabla.setWidthPercentage(100);

        cabecera(
                tabla,
                "Fecha"
        );

        cabecera(
                tabla,
                "Turno"
        );

        cabecera(
                tabla,
                "Código"
        );

        cabecera(
                tabla,
                "Caseta / ubicación"
        );

        cabecera(
                tabla,
                "Tipo"
        );

        if (
                reporte.distribuciones()
                        .isEmpty()
        ) {
            PdfPCell vacia =
                    celda(
                            "No existen asignaciones guardadas en el rango seleccionado"
                    );

            vacia.setColspan(5);

            tabla.addCell(
                    vacia
            );
        }
        else {
            for (
                    DistribucionUseCase.Distribucion item :
                    reporte.distribuciones()
            ) {
                tabla.addCell(
                        celda(
                                FECHA.format(
                                        item.fecha()
                                )
                        )
                );

                tabla.addCell(
                        celda(
                                item.estado()
                        )
                );

                tabla.addCell(
                        celda(
                                item.ubicacionCodigo()
                        )
                );

                tabla.addCell(
                        celda(
                                item.ubicacionNombre()
                        )
                );

                tabla.addCell(
                        celda(
                                item.ubicacionTipo()
                        )
                );
            }
        }

        documento.add(
                tabla
        );
    }

    private Paragraph seccion(
            String texto
    ) {

        Font font =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        10,
                        Color.DARK_GRAY
                );

        Paragraph paragraph =
                new Paragraph(
                        texto,
                        font
                );

        paragraph.setSpacingAfter(
                7
        );

        return paragraph;
    }

    private PdfPCell tarjeta(
            String etiqueta,
            String valor
    ) {

        Font label =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        8,
                        GRIS
                );

        Font value =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        16,
                        AZUL
                );

        PdfPCell cell =
                new PdfPCell();

        cell.setPadding(12);
        cell.setBorderColor(BORDE);
        cell.setBackgroundColor(AZUL_SUAVE);

        Paragraph pLabel =
                new Paragraph(
                        etiqueta,
                        label
                );

        pLabel.setAlignment(
                Element.ALIGN_CENTER
        );

        Paragraph pValue =
                new Paragraph(
                        valor,
                        value
                );

        pValue.setAlignment(
                Element.ALIGN_CENTER
        );

        cell.addElement(
                pLabel
        );

        cell.addElement(
                pValue
        );

        return cell;
    }

    private void cabecera(
            PdfPTable tabla,
            String texto
    ) {

        Font font =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        8,
                        Color.WHITE
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                texto,
                                font
                        )
                );

        cell.setPadding(7);
        cell.setBackgroundColor(AZUL);
        cell.setBorderColor(AZUL);
        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        tabla.addCell(
                cell
        );
    }

    private PdfPCell celda(
            String texto
    ) {

        Font font =
                FontFactory.getFont(
                        FontFactory.HELVETICA,
                        8,
                        Color.DARK_GRAY
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                texto == null
                                        ? ""
                                        : texto,
                                font
                        )
                );

        cell.setPadding(6);
        cell.setBorderColor(BORDE);
        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        return cell;
    }
}
