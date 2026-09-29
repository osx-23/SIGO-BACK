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
