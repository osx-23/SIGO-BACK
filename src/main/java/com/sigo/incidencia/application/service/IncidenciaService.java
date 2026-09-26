package com.sigo.incidencia.application.service;

import com.sigo.incidencia.api.dto.IncidenciaRequest;
import com.sigo.incidencia.api.dto.IncidenciaResponse;
import com.sigo.incidencia.domain.EstadoIncidencia;
import com.sigo.incidencia.infrastructure.persistence.entity.Incidencia;
import com.sigo.incidencia.infrastructure.persistence.entity.IncidenciaEvidencia;
import com.sigo.incidencia.infrastructure.persistence.entity.TipoIncidencia;
import com.sigo.incidencia.infrastructure.persistence.repository.IncidenciaEvidenciaRepository;
import com.sigo.incidencia.infrastructure.persistence.repository.IncidenciaRepository;
import com.sigo.incidencia.infrastructure.persistence.repository.TipoIncidenciaRepository;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.relevo.infrastructure.persistence.entity.Via;
import com.sigo.relevo.infrastructure.persistence.repository.ViaRepository;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ForbiddenException;
import com.sigo.shared.exception.ResourceNotFoundException;
import com.sigo.shared.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final IncidenciaEvidenciaRepository evidenciaRepository;
    private final TipoIncidenciaRepository tipoRepository;
    private final PlazaRepository plazaRepository;
    private final TurnoRepository turnoRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final ViaRepository viaRepository;
    private final UsuarioActualUseCase usuarioActualUseCase;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public List<TipoResumen> listarTipos() {
        return tipoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(t -> new TipoResumen(t.getId(), t.getNombre()))
                .toList();
    }

    @Transactional
    public IncidenciaResponse registrar(IncidenciaRequest request) {
        UsuarioActualUseCase.UsuarioActual actual = usuarioActualUseCase.requireActual();

        Long plazaId = resolverPlazaRegistro(actual, request.plazaId());

        Plaza plaza = plazaRepository.findById(plazaId)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Plaza no encontrada"));

        Turno turno = turnoRepository.findById(request.turnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));

        Trabajador registradoPor = trabajadorRepository.findById(actual.id())
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado"));

        TipoIncidencia tipo = tipoRepository.findById(request.tipoId())
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de incidencia no encontrado"));

        Via via = null;
        if (request.viaId() != null) {
            via = viaRepository.findById(request.viaId())
                    .filter(v -> Boolean.TRUE.equals(v.getActiva()))
                    .orElseThrow(() -> new ResourceNotFoundException("Vía no encontrada"));

            if (!via.getPlaza().getId().equals(plazaId)) {
                throw new BusinessException("La vía seleccionada no pertenece a la plaza indicada");
            }
        }

        String descripcion = request.descripcion() == null ? "" : request.descripcion().trim();
        if (descripcion.length() < 3) {
            throw new BusinessException("Describe la incidencia con al menos 3 caracteres");
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setPlaza(plaza);
        incidencia.setTurno(turno);
        incidencia.setRegistradoPor(registradoPor);
        incidencia.setTipo(tipo);
        incidencia.setVia(via);
        incidencia.setFecha(request.fecha());
        incidencia.setHora(request.hora());
        incidencia.setDescripcion(descripcion);
        incidencia.setEstado(EstadoIncidencia.OBSERVACION);

        return map(incidenciaRepository.saveAndFlush(incidencia));
    }

    @Transactional(readOnly = true)
    public List<IncidenciaResponse> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            EstadoIncidencia estado
    ) {
        UsuarioActualUseCase.UsuarioActual actual = usuarioActualUseCase.requireActual();

        LocalDate hasta = fin == null ? LocalDate.now() : fin;
        LocalDate desde = inicio == null ? hasta.minusDays(30) : inicio;

        if (desde.isAfter(hasta)) {
            throw new BusinessException("La fecha inicial no puede ser posterior a la fecha final");
        }

        Long plazaEfectiva = resolverPlazaConsulta(actual, plazaId);

        List<Incidencia> items = plazaEfectiva == null
                ? incidenciaRepository.findByFechaBetweenOrderByFechaDescHoraDesc(desde, hasta)
                : incidenciaRepository.findByFechaBetweenAndPlazaIdOrderByFechaDescHoraDesc(
                        desde,
                        hasta,
                        plazaEfectiva
                );

        return items.stream()
                .filter(i -> estado == null || i.getEstado() == estado)
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidenciaResponse obtener(Long id) {
        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia no encontrada"));

        validarLectura(incidencia);
        return map(incidencia);
    }

    @Transactional
    public IncidenciaResponse atender(Long id) {
        UsuarioActualUseCase.UsuarioActual actual = usuarioActualUseCase.requireActual();
        validarControl(actual);

        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia no encontrada"));

        incidencia.setEstado(EstadoIncidencia.ATENDIDO);
        incidencia.setFechaAtendido(OffsetDateTime.now());

        return map(incidenciaRepository.saveAndFlush(incidencia));
    }

    @Transactional
    public IncidenciaResponse.Evidencia subirEvidencia(Long incidenciaId, MultipartFile file) {
        Incidencia incidencia = incidenciaRepository.findById(incidenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia no encontrada"));

        validarLectura(incidencia);

        long actuales = evidenciaRepository.findByIncidenciaIdOrderByIdAsc(incidenciaId).size();
        if (actuales >= 5) {
            throw new BusinessException("La incidencia ya tiene el máximo de 5 evidencias");
        }

        try {
            Map<String, Object> subida = cloudinaryService.subirImagen(file, "sigo/incidencias");

            IncidenciaEvidencia evidencia = new IncidenciaEvidencia();
            evidencia.setIncidencia(incidencia);
            evidencia.setUrlArchivo(String.valueOf(subida.get("secure_url")));
            evidencia.setPublicId(
                    subida.get("public_id") == null ? null : String.valueOf(subida.get("public_id"))
            );
            evidencia.setTipo("foto");

            return mapEvidencia(evidenciaRepository.saveAndFlush(evidencia));
        } catch (IOException e) {
            throw new BusinessException("No se pudo subir la evidencia de la incidencia");
        }
    }

    @Transactional(readOnly = true)
    public long contarPendientes() {
        UsuarioActualUseCase.UsuarioActual actual = usuarioActualUseCase.requireActual();
        validarControl(actual);
        return incidenciaRepository.countByEstado(EstadoIncidencia.OBSERVACION);
    }

    private Long resolverPlazaRegistro(
            UsuarioActualUseCase.UsuarioActual actual,
            Long plazaSolicitada
    ) {
        if ("OPERADOR".equals(normalizarRol(actual.rol()))) {
            if (actual.plazaId() == null) {
                throw new ForbiddenException("El operador no tiene una plaza asignada");
            }
            return actual.plazaId();
        }

        if (plazaSolicitada == null) {
            throw new BusinessException("Selecciona una plaza");
        }

        return plazaSolicitada;
    }

    private Long resolverPlazaConsulta(
            UsuarioActualUseCase.UsuarioActual actual,
            Long plazaSolicitada
    ) {
        if ("OPERADOR".equals(normalizarRol(actual.rol()))) {
            if (actual.plazaId() == null) {
                throw new ForbiddenException("El operador no tiene una plaza asignada");
            }
            return actual.plazaId();
        }
        return plazaSolicitada;
    }

    private void validarLectura(Incidencia incidencia) {
        UsuarioActualUseCase.UsuarioActual actual = usuarioActualUseCase.requireActual();
        if ("OPERADOR".equals(normalizarRol(actual.rol()))) {
            if (actual.plazaId() == null || !actual.plazaId().equals(incidencia.getPlaza().getId())) {
                throw new ForbiddenException("No tienes acceso a incidencias de otra plaza");
            }
        }
    }

    private void validarControl(UsuarioActualUseCase.UsuarioActual actual) {
        String rol = normalizarRol(actual.rol());
        if (!"SUPERVISOR".equals(rol) && !"CONTROLADOR".equals(rol)) {
            throw new ForbiddenException("Solo control y supervisión pueden atender incidencias");
        }
    }

    private String normalizarRol(String rol) {
        return rol == null ? "" : rol.trim().toUpperCase(Locale.ROOT);
    }

    private IncidenciaResponse map(Incidencia i) {
        List<IncidenciaResponse.Evidencia> evidencias =
                evidenciaRepository.findByIncidenciaIdOrderByIdAsc(i.getId())
                        .stream()
                        .map(this::mapEvidencia)
                        .toList();

        Via via = i.getVia();

        return new IncidenciaResponse(
                i.getId(),
                i.getPlaza().getId(),
                i.getPlaza().getCodigo(),
                i.getTurno().getId(),
                i.getTurno().getCodigo(),
                i.getRegistradoPor().getId(),
                i.getRegistradoPor().getCodigo(),
                i.getRegistradoPor().getNombreCompleto(),
                i.getTipo().getId(),
                i.getTipo().getNombre(),
                via == null ? null : via.getId(),
                via == null ? null : via.getNumero(),
                via == null ? null : via.getNombre(),
                i.getFecha(),
                i.getHora(),
                i.getDescripcion(),
                i.getEstado(),
                i.getFechaCreacion(),
                i.getFechaAtendido(),
                evidencias
        );
    }

    private IncidenciaResponse.Evidencia mapEvidencia(IncidenciaEvidencia e) {
        return new IncidenciaResponse.Evidencia(
                e.getId(),
                e.getUrlArchivo(),
                e.getPublicId(),
                e.getTipo(),
                e.getFechaCreacion()
        );
    }

    public record TipoResumen(Long id, String nombre) {
    }
}
