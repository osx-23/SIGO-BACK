package com.sigo.incidencia.api.controller;

import com.sigo.incidencia.application.port.out.IncidenciaStoragePort;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TRAZA integrado en SIGO: reportes operativos separados de incidencias.
 * Usa catálogos existentes, autenticación SIGO y almacenamiento de evidencias Cloudinary.
 */
@RestController
@RequestMapping("/api/incidencias/control-operativo")
@PreAuthorize("hasAnyRole('SUPERVISOR','CONTROLADOR')")
@RequiredArgsConstructor
public class ControlOperativoController {
    private final JdbcTemplate jdbc;
    private final UsuarioActualUseCase usuarioActual;
    private final IncidenciaStoragePort storage;

    public record ReporteInput(
        @NotNull Long plazaId, @NotNull Long turnoId,
        @NotBlank String tipo, Long viaId, Long responsableId,
        @NotNull LocalDate fecha, @NotNull LocalTime hora,
        @Size(max=60) String situacion, @Size(max=60) String ingreso,
        @Size(max=120) String incidente, @Size(max=10000) String descripcion,
        @Min(0) @Max(50000) Integer colaMetros
    ) {}

    private static final String BASE = """
        select r.id, r.plaza_id as "plazaId", p.codigo as "plazaCodigo",
        r.turno_id as "turnoId", trim(t.codigo) as "turnoCodigo",
        r.via_id as "viaId", v.numero as "viaNumero", r.registrado_por_id as "registradoPorId",
        tr.nombre_completo as "registradoPor",
        coalesce(r.responsable_id,r.registrado_por_id) as "responsableId",
        coalesce(resp.nombre_completo,tr.nombre_completo) as "responsableNombre", r.tipo,
        r.fecha::text as fecha, to_char(r.hora,'HH24:MI') as hora,
        r.situacion, r.ingreso, r.incidente, r.cola_metros as "colaMetros",
        r.descripcion, r.estado, r.creado_en as "creadoEn",
        r.actualizado_en as "actualizadoEn"
        from public.control_operativo_reportes r
        join public.plazas p on p.id=r.plaza_id
        join public.turnos t on t.id=r.turno_id
        join public.trabajadores tr on tr.id=r.registrado_por_id
        left join public.trabajadores resp on resp.id=r.responsable_id
        left join public.vias v on v.id=r.via_id
        """;

    private UsuarioActualUseCase.UsuarioActual actual() {
        return usuarioActual.requireActual();
    }

    private void comprobarPlaza(UsuarioActualUseCase.UsuarioActual u, Long plazaId) {
        if (plazaId == null || (!"SUPERVISOR".equals(u.rol()) &&
            !plazaId.equals(u.plazaId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Sin permiso para acceder a esta plaza");
        }
    }

    private boolean existe(String sql, Object... params) {
        return jdbc.queryForObject(sql, Integer.class, params) > 0;
    }

    @GetMapping("/responsables")
    @Transactional(readOnly = true)
    public List<Map<String,Object>> responsables() {
        actual(); // Exige sesión SIGO; la anotación restringe a supervisor/controlador.
        return jdbc.queryForList("""
            select t.id, t.nombre_completo as "nombreCompleto"
            from public.trabajadores t
            where t.activo=true and t.rol_sistema in ('SUPERVISOR','CONTROLADOR')
            order by t.nombre_completo, t.id
            """);
    }

    private Long validarResponsable(Long id, UsuarioActualUseCase.UsuarioActual u) {
        Long elegido = id == null ? u.id() : id;
        if (!existe("""
            select count(*) from public.trabajadores
            where id=? and activo=true and rol_sistema in ('SUPERVISOR','CONTROLADOR')
            """, elegido)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Selecciona un responsable activo con rol controlador o supervisor");
        }
        return elegido;
    }

    private void validar(ReporteInput r, UsuarioActualUseCase.UsuarioActual u) {
        comprobarPlaza(u, r.plazaId());
        if (!List.of("REPORTE","MEDICION").contains(r.tipo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo inválido");
        if (r.colaMetros()==null && "MEDICION".equals(r.tipo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indica el tamaño de cola");
        if (!existe("select count(*) from public.plazas where id=? and activo=true",r.plazaId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Plaza inexistente o inactiva");
        // El turno se sugiere por hora en Angular, pero el controlador puede corregirlo.
        if (!existe("select count(*) from public.turnos where id=? and trim(codigo) in ('A','B','C')",
                r.turnoId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Selecciona un turno válido (A, B o C)");
        if (r.viaId()!=null && !existe(
                "select count(*) from public.vias where id=? and plaza_id=? and activa=true",
                r.viaId(),r.plazaId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La vía no pertenece a la plaza");
    }

    private Long plazaGuardada(Long id) {
        var ids=jdbc.queryForList(
            "select plaza_id from public.control_operativo_reportes where id=?",Long.class,id);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Reporte no encontrado");
        return ids.get(0);
    }

    /**
     * Estadísticas para el dashboard: se agregan en PostgreSQL sin el límite
     * de 500 registros del historial. Un día con mediciones pero sin reportes
     * sigue aportando a la serie de cola máxima.
     */
    public record DiaDashboard(
        String fecha, Integer colaMaxima, long incidencias, long mediciones
    ) {}

    public record DashboardResumen(
        long incidenciasRegistradas, int colaMaxima,
        double promedioMaximos, List<DiaDashboard> dias
    ) {}

    @GetMapping("/dashboard")
    @Transactional(readOnly=true)
    public DashboardResumen dashboard(
        @RequestParam(required=false) Long plazaId,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        var usuario = actual();
        LocalDate hasta = fin == null ? LocalDate.now(ZoneId.of("America/Lima")) : fin;
        LocalDate desde = inicio == null ? hasta.minusDays(29) : inicio;
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La fecha inicial no puede ser posterior a la final");
        }

        Long plaza = "SUPERVISOR".equals(usuario.rol()) ? plazaId : usuario.plazaId();
        if (!"SUPERVISOR".equals(usuario.rol())) comprobarPlaza(usuario,plaza);
        var sql = new StringBuilder("""
            select r.fecha, count(*) filter (where r.tipo='REPORTE') as incidencias,
                   count(r.cola_metros) as mediciones,
                   max(r.cola_metros) as cola_maxima
            from public.control_operativo_reportes r
            where r.fecha >= ? and r.fecha <= ?
            """);
        var args = new ArrayList<Object>();
        args.add(desde);
        args.add(hasta);
        if (plaza != null) {
            sql.append(" and r.plaza_id = ?");
            args.add(plaza);
        }
        sql.append(" group by r.fecha order by r.fecha");

        List<DiaDashboard> dias = jdbc.query(sql.toString(), (rs, row) ->
            new DiaDashboard(
                rs.getDate("fecha").toLocalDate().toString(),
                (Integer) rs.getObject("cola_maxima"),
                rs.getLong("incidencias"),
                rs.getLong("mediciones")
            ), args.toArray());

        long incidencias = dias.stream().mapToLong(DiaDashboard::incidencias).sum();
        int maxima = dias.stream().filter(d -> d.colaMaxima() != null)
            .mapToInt(DiaDashboard::colaMaxima).max().orElse(0);
        double promedio = dias.stream().filter(d -> d.colaMaxima() != null)
            .mapToInt(DiaDashboard::colaMaxima).average().orElse(0);
        return new DashboardResumen(incidencias, maxima, promedio, dias);
    }

    @GetMapping
    @Transactional(readOnly=true)
    public List<Map<String,Object>> listar(
        @RequestParam(required=false) Long plazaId,
        @RequestParam(required=false) String tipo,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fin) {
        var u=actual();
        Long plaza= "SUPERVISOR".equals(u.rol()) ? plazaId : u.plazaId();
        if (!"SUPERVISOR".equals(u.rol())) comprobarPlaza(u,plaza);
        var sql=new StringBuilder(BASE+" where r.fecha>=? and r.fecha<=?");
        var args=new ArrayList<Object>();
        var hasta=fin==null?LocalDate.now():fin;
        var desde=inicio==null?hasta.minusDays(30):inicio;
        if (desde.isAfter(hasta))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rango de fechas inválido");
        args.add(desde);args.add(hasta);
        if (plaza!=null) {sql.append(" and r.plaza_id=?");args.add(plaza);}
        if (tipo!=null && !tipo.isBlank()) {
            if (!List.of("REPORTE","MEDICION").contains(tipo))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo inválido");
            sql.append(" and r.tipo=?");args.add(tipo);
        }
        sql.append(" order by r.fecha desc,r.hora desc,r.id desc limit 500");
        return jdbc.queryForList(sql.toString(),args.toArray());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly=true)
    public Map<String,Object> obtener(@PathVariable Long id) {
        comprobarPlaza(actual(),plazaGuardada(id));
        var items=jdbc.queryForList(BASE+" where r.id=?",id);
        return items.get(0);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String,Object> crear(@Valid @RequestBody ReporteInput r) {
        var u=actual();
        validar(r,u);
        Long responsable = validarResponsable(r.responsableId(), u);
        Long id=jdbc.queryForObject("""
            insert into public.control_operativo_reportes
            (plaza_id,turno_id,via_id,registrado_por_id,responsable_id,tipo,fecha,hora,situacion,ingreso,
             incidente,cola_metros,descripcion)
            values (?,?,?,?,?,?,?,?,?,?,?,?,?) returning id
            """,Long.class,r.plazaId(),r.turnoId(),r.viaId(),u.id(),responsable,r.tipo(),r.fecha(),r.hora(),
            txt(r.situacion()),txt(r.ingreso()),txt(r.incidente()),r.colaMetros(),
            txt(r.descripcion()));
        return obtener(id);
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String,Object> actualizar(@PathVariable Long id,
                                            @Valid @RequestBody ReporteInput r) {
        var u=actual();
        comprobarPlaza(u,plazaGuardada(id));
        validar(r,u);
        Long responsable = r.responsableId() == null ? null
            : validarResponsable(r.responsableId(),u);
        jdbc.update("""
            update public.control_operativo_reportes
            set plaza_id=?,turno_id=?,via_id=?,tipo=?,fecha=?,hora=?,situacion=?,ingreso=?,
                incidente=?,cola_metros=?,descripcion=?,
                responsable_id=coalesce(?,responsable_id),actualizado_en=now()
            where id=?
            """,r.plazaId(),r.turnoId(),r.viaId(),r.tipo(),r.fecha(),r.hora(),
            txt(r.situacion()),txt(r.ingreso()),txt(r.incidente()),r.colaMetros(),
            txt(r.descripcion()),responsable,id);
        return obtener(id);
    }

    @GetMapping("/{id}/evidencias")
    @Transactional(readOnly=true)
    public List<Map<String,Object>> evidencias(@PathVariable Long id) {
        comprobarPlaza(actual(),plazaGuardada(id));
        return jdbc.queryForList("""
            select id,reporte_id as "reporteId",categoria,
                   url_archivo as "urlArchivo",creado_en as "creadoEn"
            from public.control_operativo_evidencias where reporte_id=?
            order by id
            """,id);
    }

    @PostMapping("/{id}/evidencias")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> subir(@PathVariable Long id,
        @RequestParam(defaultValue="PLAZA") String categoria,
        @RequestParam("file") MultipartFile file) throws IOException {
        comprobarPlaza(actual(),plazaGuardada(id));
        if (!List.of("PLAZA","INGRESO","INCIDENTE","COLA").contains(categoria))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Categoría inválida");
        if (file.isEmpty() || file.getSize()>10*1024*1024 ||
            file.getContentType()==null ||
            !List.of("image/jpeg","image/png","image/webp").contains(file.getContentType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Solo imágenes JPG, PNG o WEBP hasta 10 MB");
        Integer cantidad=jdbc.queryForObject("""
            select count(*) from public.control_operativo_evidencias where reporte_id=?
            """,Integer.class,id);
        if (cantidad>=5)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Máximo 5 evidencias por reporte");
        var subida=storage.subir(file.getBytes(),file.getContentType());
        try {
            Long evidenciaId=jdbc.queryForObject("""
                insert into public.control_operativo_evidencias
                (reporte_id,categoria,url_archivo,public_id) values (?,?,?,?) returning id
                """,Long.class,id,categoria,subida.urlArchivo(),subida.publicId());
            return Map.of("id",evidenciaId,"urlArchivo",subida.urlArchivo(),
                          "categoria",categoria);
        } catch(RuntimeException e) {
            storage.eliminar(subida.publicId());
            throw e;
        }
    }

    @DeleteMapping("/{id}/evidencias/{evidenciaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEvidencia(@PathVariable Long id,@PathVariable Long evidenciaId) {
        comprobarPlaza(actual(),plazaGuardada(id));
        var items=jdbc.queryForList("""
            select public_id from public.control_operativo_evidencias where id=? and reporte_id=?
            """,String.class,evidenciaId,id);
        if (items.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Evidencia no encontrada");
        jdbc.update("delete from public.control_operativo_evidencias where id=? and reporte_id=?",evidenciaId,id);
        storage.eliminar(items.get(0));
    }

    private static String txt(String value) { return value==null?"":value.trim(); }
}
