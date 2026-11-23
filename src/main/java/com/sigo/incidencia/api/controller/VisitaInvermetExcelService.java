package com.sigo.incidencia.api.controller;

import com.sigo.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class VisitaInvermetExcelService {
    private static final DateTimeFormatter FECHA=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA=DateTimeFormatter.ofPattern("HH:mm:ss");

    public byte[] generar(VisitaInvermetController.VisitaResponse v) {
        try (XSSFWorkbook wb=new XSSFWorkbook(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            var ficha=wb.createSheet("Acta de visita");
            int[] widths={3100,6600,3600,6400,6000,9600};
            for(int i=0;i<widths.length;i++)ficha.setColumnWidth(i,widths[i]);
            CellStyle titulo=estilo(wb,IndexedColors.DARK_BLUE,true,true);
            CellStyle encabezado=estilo(wb,IndexedColors.GREY_25_PERCENT,true,false);
            CellStyle dato=estilo(wb,IndexedColors.WHITE,false,false);
            CellStyle duracion=estilo(wb,IndexedColors.WHITE,true,false);
            duracion.setFont(fuente(wb,true,IndexedColors.RED));
            fila(ficha,0,30);
            fusion(ficha,0,0,5,"REGISTRO DE VISITA INVERMET · SIGO",titulo);
            fila(ficha,2,26);
            texto(ficha,2,0,"Plaza",encabezado); texto(ficha,2,2,"Fecha",encabezado);
            texto(ficha,2,3,"Hora inicio",encabezado);texto(ficha,2,4,"Hora final",encabezado);
            texto(ficha,2,5,"Tiempo de visita",encabezado);
            fusion(ficha,2,0,1,"Plaza",encabezado);
            fila(ficha,3,27);
            fusion(ficha,3,0,1,v.plazaCodigo(),dato);
            texto(ficha,3,2,v.fecha().format(FECHA),dato);
            texto(ficha,3,3,v.horaInicio().format(HORA),dato);
            texto(ficha,3,4,v.horaFinal().format(HORA),dato);
            texto(ficha,3,5,duracion(v.duracionMinutos()),duracion);

            fila(ficha,5,25);
            fusion(ficha,5,0,1,"Controlador",encabezado);
            fusion(ficha,5,2,3,"Supervisor",encabezado);
            fusion(ficha,5,4,5,"Casetas",encabezado);
            fila(ficha,6,27);
            fusion(ficha,6,0,1,v.controlador(),dato);
            fusion(ficha,6,2,3,v.supervisor(),dato);
            fusion(ficha,6,4,5,v.casetasAbiertas()+" abiertas / "+v.casetasEnPlaza()+" en plaza",dato);
            fila(ficha,8,24);
            fusion(ficha,8,0,1,"Datos de la visita",encabezado);
            fusion(ficha,8,2,5,"Detalle registrado",encabezado);
            fila(ficha,9,26);
            fusion(ficha,9,0,1,"1",dato);
            fusion(ficha,9,2,5,v.datoVisita1(),dato);
            fila(ficha,10,26);
            fusion(ficha,10,0,1,"2",dato);
            fusion(ficha,10,2,5,v.datoVisita2(),dato);

            fila(ficha,12,28);
            fusion(ficha,12,0,2,"Preguntas de la visita",encabezado);
            texto(ficha,12,3,"Quién respondió",encabezado);
            fusion(ficha,12,4,5,"Respuesta",encabezado);
            List<VisitaInvermetController.Pregunta> preguntas=v.preguntas();
            for(int numero=1;numero<=5;numero++){
                int row=12+numero;
                fila(ficha,row,49);
                final int n=numero;
                var pregunta=preguntas.stream().filter(p->p.numero()==n).findFirst().orElse(null);
                fusion(ficha,row,0,2,numero+". "+(pregunta==null?"":pregunta.pregunta()),dato);
                texto(ficha,row,3,pregunta==null?"":pregunta.quienRespondio(),dato);
                fusion(ficha,row,4,5,pregunta==null?"":pregunta.respuesta(),dato);
            }
            fila(ficha,19,27);
            fusion(ficha,19,0,5,"OBSERVACIONES",encabezado);
            fila(ficha,20,95);
            fusion(ficha,20,0,5,v.observaciones(),dato);
            ficha.createFreezePane(0,3);
            ficha.getPrintSetup().setLandscape(true);
            ficha.setFitToPage(true);
            ficha.getPrintSetup().setFitWidth((short)1);

            var fotos=wb.createSheet("Fotografías");
            fotos.setDefaultRowHeightInPoints(22);
            for(int c=0;c<8;c++) fotos.setColumnWidth(c,4300);
            fusion(fotos,0,0,7,"EVIDENCIAS FOTOGRÁFICAS · VISITA #"+v.id(),titulo);
            var drawing=fotos.createDrawingPatriarch();
            for(int i=0;i<v.fotos().size()&&i<4;i++){
                var f=v.fotos().get(i);
                int x=(i%2)*4, y=2+(i/2)*18;
                fusion(fotos,y,x,x+3,"Fotografía "+(i+1),encabezado);
                Row links=fotos.getRow(y+1);
                if(links==null)links=fotos.createRow(y+1);
                var cell=links.createCell(x);
                cell.setCellValue(f.urlArchivo());
                var enlace=wb.getCreationHelper().createHyperlink(HyperlinkType.URL);
                enlace.setAddress(f.urlArchivo());
                cell.setHyperlink(enlace);
                try {
                    var bytes=descargarFoto(f.urlArchivo());
                    if(bytes==null)continue;
                    int tipo=(bytes[0]&0xFF)==0xFF?Workbook.PICTURE_TYPE_JPEG:Workbook.PICTURE_TYPE_PNG;
                    int pictureId=wb.addPicture(bytes,tipo);
                    var anchor=wb.getCreationHelper().createClientAnchor();
                    anchor.setCol1(x);anchor.setCol2(x+4);anchor.setRow1(y+2);anchor.setRow2(y+16);
                    drawing.createPicture(anchor,pictureId);
                } catch(Exception ex) {
                    log.warn("No fue posible incrustar foto {} en Excel; se conserva el enlace.",f.id());
                }
            }
            fotos.getPrintSetup().setLandscape(true);
            wb.write(out);
            return out.toByteArray();
        } catch(Exception ex) {
            log.error("No se pudo generar acta Excel de visita {}",v.id(),ex);
            throw new BusinessException("No se pudo generar el archivo Excel.");
        }
    }

    private byte[] descargarFoto(String url) throws Exception {
        URI uri=URI.create(url);
        // No permitir descargas de hosts arbitrarios (SSRF).
        if(!"https".equalsIgnoreCase(uri.getScheme()) || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost()))
            return null;
        String src=url;
        // Cloudinary puede servir WEBP: normalizar a JPEG para Excel.
        if(src.contains("/image/upload/"))src=src.replace("/image/upload/","/image/upload/f_jpg/");
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER).build();
        var request=HttpRequest.newBuilder(URI.create(src)).timeout(Duration.ofSeconds(8)).GET().build();
        var response=client.send(request,HttpResponse.BodyHandlers.ofByteArray());
        byte[] b=response.body();
        if(response.statusCode()!=200 || b.length<12 || b.length>5*1024*1024) return null;
        boolean jpg=(b[0]&0xFF)==255&&(b[1]&0xFF)==216;
        boolean png=(b[0]&0xFF)==137&&b[1]==80&&b[2]==78&&b[3]==71;
        return jpg||png?b:null;
    }
    private static String duracion(long minutos) {
        return String.format("%02d:%02d:00",minutos/60,minutos%60);
    }
    private static Font fuente(Workbook wb,boolean negrita,IndexedColors color) {
        Font font=wb.createFont();
        font.setBold(negrita);font.setColor(color.getIndex());font.setFontHeightInPoints((short)10);
        return font;
    }
    private static CellStyle estilo(Workbook wb,IndexedColors fondo,boolean bold,boolean oscuro) {
        var cs=wb.createCellStyle();
        cs.setFillForegroundColor(fondo.getIndex());cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setFont(fuente(wb,bold,oscuro?IndexedColors.WHITE:IndexedColors.BLACK));
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setWrapText(true);
        cs.setBorderBottom(BorderStyle.THIN);
        cs.setBorderTop(BorderStyle.THIN);
        cs.setBorderLeft(BorderStyle.THIN);
        cs.setBorderRight(BorderStyle.THIN);
        return cs;
    }
    private static void fila(Sheet s,int n,float height) {
        Row row=s.getRow(n);
        if(row==null)row=s.createRow(n);
        row.setHeightInPoints(height);
    }
    private static void texto(Sheet s,int r,int c,String valor,CellStyle estilo) {
        Row row=s.getRow(r);if(row==null)row=s.createRow(r);
        Cell cell=row.getCell(c);if(cell==null)cell=row.createCell(c);
        cell.setCellValue(valor==null?"":valor);cell.setCellStyle(estilo);
    }
    private static void fusion(Sheet s,int r,int desde,int hasta,String valor,CellStyle estilo) {
        texto(s,r,desde,valor,estilo);
        for(int c=desde+1;c<=hasta;c++)texto(s,r,c,"",estilo);
        if(hasta>desde)s.addMergedRegion(new CellRangeAddress(r,r,desde,hasta));
    }
}
