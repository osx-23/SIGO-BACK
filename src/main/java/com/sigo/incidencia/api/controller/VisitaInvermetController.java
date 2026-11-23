package com.sigo.incidencia.api.controller;

import com.sigo.incidencia.application.service.VisitaInvermetExcelService;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ForbiddenException;
import com.sigo.shared.exception.ResourceNotFoundException;
import com.sigo.shared.storage.CloudinaryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/incidencias/visitas-invermet")
@RequiredArgsConstructor
public class VisitaInvermetController {
    private final VisitaInvermetService service;
    private final VisitaInvermetExcelService excel;

    public record Pregunta(int numero, String pregunta, String quienRespondio, String respuesta) {}
    public record Foto(Long id, String urlArchivo) {}
    public record VisitaRequest(
            @NotNull Long plazaId,
            @NotBlank @Size(max=150) String controlador,
            @NotBlank @Size(max=150) String supervisor,
            @NotNull LocalDate fecha,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFinal,
            @NotNull @Min(0) @Max(200) Integer casetasEnPlaza,
            @NotNull @Min(0) @Max(200) Integer casetasAbiertas,
            @Size(max=300) String datoVisita1,
            @Size(max=300) String datoVisita2,
            @Size(max=2000) String observaciones,
            List<@Valid Pregunta> preguntas
    ) {}
    public record VisitaResponse(
            Long id, Long plazaId, String plazaCodigo, String controlador, String supervisor,
            LocalDate fecha, LocalTime horaInicio, LocalTime horaFinal, long duracionMinutos,
            int casetasEnPlaza, int casetasAbiertas, String datoVisita1, String datoVisita2,
            String observaciones, List<Pregunta> preguntas, List<Foto> fotos
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaResponse registrar(@Valid @RequestBody VisitaRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public List<VisitaResponse> listar(
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required=false) Long plazaId) {
        return service.listar(inicio,fin,plazaId);
    }

    @GetMapping("/{id}")
    public VisitaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping(path="/{id}/fotos", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Foto subirFoto(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return service.subirFoto(id,file);
    }

    @GetMapping(value="/{id}/excel", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportar(@PathVariable Long id) {
        VisitaResponse visita = service.obtener(id); // verifica acceso por plaza
        byte[] archivo = excel.generar(visita);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"visita-invermet-" + id + ".xlsx\"")
                .body(archivo);
    }
}

@Service
@RequiredArgsConstructor
class VisitaInvermetService {
    private final JdbcTemplate jdbc;
    private final UsuarioActualUseCase usuarioActual;
    private final CloudinaryService cloudinary;

    @Transactional
    public VisitaInvermetController.VisitaResponse registrar(VisitaInvermetController.VisitaRequest r) {
        var u = usuarioActual.requireActual();
        long plazaId = plazaRegistro(u,r.plazaId());
        var activa = jdbc.queryForList("select id from plazas where id=? and activo=true", plazaId);
        if (activa.isEmpty()) throw new BusinessException("La plaza seleccionada no está activa.");
        if (r.casetasAbiertas() > r.casetasEnPlaza()) throw new BusinessException("Las casetas abiertas no pueden superar las casetas de la plaza.");
        if (r.horaInicio().equals(r.horaFinal())) throw new BusinessException("La hora final debe ser diferente de la hora inicial.");
        if (r.preguntas()!=null && r.preguntas().size()>5) throw new BusinessException("Se permiten como máximo 5 preguntas.");
        String controlador = r.controlador().trim(), supervisor = r.supervisor().trim();
        if (controlador.isEmpty() || supervisor.isEmpty()) throw new BusinessException("Completa controlador y supervisor.");
        Long id = jdbc.queryForObject("""
            insert into visitas_invermet (plaza_id, registrado_por_id, fecha, hora_inicio, hora_final,
              controlador, supervisor, casetas_en_plaza, casetas_abiertas, dato_visita_1, dato_visita_2, observaciones)
            values (?,?,?,?,?,?,?,?,?,?,?,?) returning id
            """, Long.class, plazaId,u.id(),r.fecha(),r.horaInicio(),r.horaFinal(),
            controlador,supervisor,r.casetasEnPlaza(),r.casetasAbiertas(),
            limpiar(r.datoVisita1()),limpiar(r.datoVisita2()),limpiar(r.observaciones()));
        if (r.preguntas()!=null) {
            Set<Integer> numeros = new HashSet<>();
            for (var p : r.preguntas()) {
                if (p.numero()<1 || p.numero()>5 || !numeros.add(p.numero()))
                    throw new BusinessException("Cada pregunta debe tener un número único del 1 al 5.");
                String titulo=limpiar(p.pregunta()), quien=limpiar(p.quienRespondio()), respuesta=limpiar(p.respuesta());
                if (titulo.isEmpty()) {
                    if (!quien.isEmpty() || !respuesta.isEmpty()) throw new BusinessException("Escribe el enunciado de las preguntas respondidas.");
                    continue;
                }
                if (titulo.length()>300 || quien.length()>150 || respuesta.length()>1000)
                    throw new BusinessException("Revisa la longitud de las preguntas y respuestas.");
                jdbc.update("""
                    insert into visita_invermet_preguntas (visita_id, numero, pregunta, quien_respondio, respuesta)
                    values (?,?,?,?,?)
                    """,id,p.numero(),titulo,quien,respuesta);
            }
        }
        return obtener(id);
    }

    @Transactional(readOnly=true)
    public List<VisitaInvermetController.VisitaResponse> listar(LocalDate inicio,LocalDate fin,Long plazaId) {
        var u=usuarioActual.requireActual();
        Long efectiva=plazaConsulta(u,plazaId);
        LocalDate hasta=fin==null?LocalDate.now():fin, desde=inicio==null?hasta.minusDays(30):inicio;
        if (desde.isAfter(hasta) || ChronoUnit.DAYS.between(desde,hasta)>366)
            throw new BusinessException("Consulta un rango válido de hasta 366 días.");
        String sql = """
             select v.*, p.codigo as plaza_codigo from visitas_invermet v
             join plazas p on p.id=v.plaza_id where v.fecha between ? and ?
             """ + (efectiva==null?"":" and v.plaza_id=?") + " order by v.fecha desc, v.hora_inicio desc, v.id desc";
        if (efectiva==null) return jdbc.query(sql,(rs,i)->map(rs),desde,hasta);
        return jdbc.query(sql,(rs,i)->map(rs),desde,hasta,efectiva);
    }

    @Transactional(readOnly=true)
    public VisitaInvermetController.VisitaResponse obtener(Long id) {
        var items=jdbc.query("""
            select v.*, p.codigo as plaza_codigo from visitas_invermet v join plazas p on p.id=v.plaza_id where v.id=?
            """,(rs,i)->map(rs),id);
        if(items.isEmpty())throw new ResourceNotFoundException("Visita Invermet no encontrada.");
        var visita=items.get(0);
        validarAcceso(usuarioActual.requireActual(),visita.plazaId());
        return visita;
    }

    @Transactional
    public VisitaInvermetController.Foto subirFoto(Long id,MultipartFile file) {
        // Bloqueo por acta: dos subidas concurrentes no pueden exceder las cuatro imágenes.
        var rows=jdbc.queryForList("select plaza_id from visitas_invermet where id=? for update",id);
        if(rows.isEmpty())throw new ResourceNotFoundException("Visita Invermet no encontrada.");
        long plazaId=((Number)rows.get(0).get("plaza_id")).longValue();
        validarAcceso(usuarioActual.requireActual(),plazaId);
        Integer n=jdbc.queryForObject("select count(*) from visita_invermet_fotos where visita_id=?",Integer.class,id);
        if(n!=null && n>=4)throw new BusinessException("La visita ya tiene cuatro fotografías.");
        if(file.isEmpty() || file.getSize()>10*1024*1024)
            throw new BusinessException("Cada foto debe pesar entre 1 byte y 10 MB.");
        if(!Set.of("image/jpeg","image/png","image/webp").contains(file.getContentType()))
            throw new BusinessException("Solo se permiten fotos JPG, PNG o WEBP.");
        String publicId=null;
        try {
            var subida=cloudinary.subirImagen(file,"sigo/visitas-invermet");
            String url=String.valueOf(subida.get("secure_url"));
            publicId=String.valueOf(subida.get("public_id"));
            Long fotoId=jdbc.queryForObject("""
                insert into visita_invermet_fotos (visita_id,url_archivo,public_id)
                values (?,?,?) returning id
                """,Long.class,id,url,publicId);
            return new VisitaInvermetController.Foto(fotoId,url);
        } catch (IOException e) {
            throw new BusinessException("No se pudo subir la fotografía.");
        } catch (RuntimeException e) {
            if(publicId!=null) {
                try { cloudinary.eliminarImagen(publicId); } catch (Exception ignored) { }
            }
            throw e;
        }
    }

    private VisitaInvermetController.VisitaResponse map(ResultSet rs) throws SQLException {
        long id=rs.getLong("id");
        LocalTime hi=rs.getTime("hora_inicio").toLocalTime(),hf=rs.getTime("hora_final").toLocalTime();
        long minutos=ChronoUnit.MINUTES.between(hi,hf);
        if(minutos<0)minutos+=1440; // Visitas que cruzan medianoche.
        var preguntas=jdbc.query("""
            select numero,pregunta,quien_respondio,respuesta from visita_invermet_preguntas
            where visita_id=? order by numero
            """,(q,i)->new VisitaInvermetController.Pregunta(
                q.getInt("numero"),q.getString("pregunta"),q.getString("quien_respondio"),q.getString("respuesta")),id);
        var fotos=jdbc.query("""
            select id,url_archivo from visita_invermet_fotos where visita_id=? order by id
            """,(f,i)->new VisitaInvermetController.Foto(f.getLong("id"),f.getString("url_archivo")),id);
        return new VisitaInvermetController.VisitaResponse(
            id,rs.getLong("plaza_id"),rs.getString("plaza_codigo"),rs.getString("controlador"),
            rs.getString("supervisor"),rs.getDate("fecha").toLocalDate(),hi,hf,minutos,
            rs.getInt("casetas_en_plaza"),rs.getInt("casetas_abiertas"),
            rs.getString("dato_visita_1"),rs.getString("dato_visita_2"),rs.getString("observaciones"),preguntas,fotos);
    }

    private long plazaRegistro(UsuarioActualUseCase.UsuarioActual u,Long requested) {
        if("OPERADOR".equalsIgnoreCase(u.rol())) {
            if(u.plazaId()==null)throw new ForbiddenException("El operador no tiene plaza asignada.");
            return u.plazaId();
        }
        if(requested==null)throw new BusinessException("Selecciona una plaza.");
        return requested;
    }
    private Long plazaConsulta(UsuarioActualUseCase.UsuarioActual u,Long requested) {
        if("OPERADOR".equalsIgnoreCase(u.rol())) {
            if(u.plazaId()==null)throw new ForbiddenException("El operador no tiene plaza asignada.");
            return u.plazaId();
        }
        return requested;
    }
    private void validarAcceso(UsuarioActualUseCase.UsuarioActual u,Long plazaId) {
        if("OPERADOR".equalsIgnoreCase(u.rol()) && (u.plazaId()==null || !u.plazaId().equals(plazaId)))
            throw new ForbiddenException("No tienes acceso a las visitas de otra plaza.");
    }
    private static String limpiar(String s) {return s==null?"":s.trim();}
}
