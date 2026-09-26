package com.sigo.incidencia.infrastructure.persistence.adapter;

import com.sigo.incidencia.application.port.out.IncidenciaPersistencePort;
import com.sigo.incidencia.domain.EstadoIncidencia;
import com.sigo.incidencia.infrastructure.persistence.entity.Incidencia;
import com.sigo.incidencia.infrastructure.persistence.entity.IncidenciaEvidencia;
import com.sigo.incidencia.infrastructure.persistence.entity.TipoIncidencia;
import com.sigo.incidencia.infrastructure.persistence.repository.IncidenciaEvidenciaRepository;
import com.sigo.incidencia.infrastructure.persistence.repository.IncidenciaRepository;
import com.sigo.incidencia.infrastructure.persistence.repository.TipoIncidenciaRepository;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.relevo.infrastructure.persistence.entity.Via;
import com.sigo.relevo.infrastructure.persistence.repository.ViaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IncidenciaJpaAdapter implements IncidenciaPersistencePort {

    private final IncidenciaRepository incidenciaRepository;
    private final IncidenciaEvidenciaRepository evidenciaRepository;
    private final TipoIncidenciaRepository tipoRepository;
    private final PlazaRepository plazaRepository;
    private final TurnoRepository turnoRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final ViaRepository viaRepository;

    @Override
    public boolean plazaActiva(Long plazaId) {
        return plazaRepository.findById(plazaId)
                .map(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElse(false);
    }

    @Override
    public boolean turnoExiste(Long turnoId) {
        return turnoRepository.existsById(turnoId);
    }

    @Override
    public boolean trabajadorActivo(Long trabajadorId) {
        return trabajadorRepository.findById(trabajadorId)
                .map(t -> Boolean.TRUE.equals(t.getActivo()))
                .orElse(false);
    }

    @Override
    public boolean tipoActivo(Long tipoId) {
        return tipoRepository.findById(tipoId)
                .map(t -> Boolean.TRUE.equals(t.getActivo()))
                .orElse(false);
    }

    @Override
    public Optional<Long> plazaIdDeViaActiva(Long viaId) {
        return viaRepository.findById(viaId)
                .filter(v -> Boolean.TRUE.equals(v.getActiva()))
                .map(v -> v.getPlaza().getId());
    }

    @Override
    public List<TipoData> listarTiposActivos() {
        return tipoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(t -> new TipoData(t.getId(), t.getNombre()))
                .toList();
    }

    @Override
    @Transactional
    public IncidenciaData crear(
            Long plazaId,
            Long turnoId,
            Long registradoPorId,
            Long tipoId,
            Long viaId,
            LocalDate fecha,
            LocalTime hora,
            String descripcion
    ) {
        Incidencia incidencia = new Incidencia();
        incidencia.setPlaza(plazaRepository.getReferenceById(plazaId));
        incidencia.setTurno(turnoRepository.getReferenceById(turnoId));
        incidencia.setRegistradoPor(trabajadorRepository.getReferenceById(registradoPorId));
        incidencia.setTipo(tipoRepository.getReferenceById(tipoId));
        incidencia.setVia(viaId == null ? null : viaRepository.getReferenceById(viaId));
        incidencia.setFecha(fecha);
        incidencia.setHora(hora);
        incidencia.setDescripcion(descripcion);
        incidencia.setEstado(EstadoIncidencia.OBSERVACION);

        return map(incidenciaRepository.saveAndFlush(incidencia));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenciaData> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId
    ) {
        List<Incidencia> items = plazaId == null
                ? incidenciaRepository.findByFechaBetweenOrderByFechaDescHoraDesc(inicio, fin)
                : incidenciaRepository.findByFechaBetweenAndPlazaIdOrderByFechaDescHoraDesc(
                        inicio,
                        fin,
                        plazaId
                );

        return items.stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IncidenciaData> obtener(Long id) {
        return incidenciaRepository.findById(id).map(this::map);
    }

    @Override
    @Transactional
    public IncidenciaData marcarAtendido(
            Long id,
            OffsetDateTime fechaAtendido
    ) {
        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow();

        incidencia.setEstado(EstadoIncidencia.ATENDIDO);
        incidencia.setFechaAtendido(fechaAtendido);
        return map(incidenciaRepository.saveAndFlush(incidencia));
    }

    @Override
    public long contarPorEstado(EstadoIncidencia estado) {
        return incidenciaRepository.countByEstado(estado);
    }

    @Override
    public long contarEvidencias(Long incidenciaId) {
        return evidenciaRepository.countByIncidenciaId(incidenciaId);
    }

    @Override
    @Transactional
    public EvidenciaData guardarEvidencia(
            Long incidenciaId,
            String urlArchivo,
            String publicId
    ) {
        IncidenciaEvidencia evidencia = new IncidenciaEvidencia();
        evidencia.setIncidencia(incidenciaRepository.getReferenceById(incidenciaId));
        evidencia.setUrlArchivo(urlArchivo);
        evidencia.setPublicId(publicId);
        evidencia.setTipo("foto");

        return mapEvidencia(evidenciaRepository.saveAndFlush(evidencia));
    }

    private IncidenciaData map(Incidencia i) {
        Via via = i.getVia();

        List<EvidenciaData> evidencias =
                evidenciaRepository.findByIncidenciaIdOrderByIdAsc(i.getId())
                        .stream()
                        .map(this::mapEvidencia)
                        .toList();

        return new IncidenciaData(
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

    private EvidenciaData mapEvidencia(IncidenciaEvidencia e) {
        return new EvidenciaData(
                e.getId(),
                e.getUrlArchivo(),
                e.getPublicId(),
                e.getTipo(),
                e.getFechaCreacion()
        );
    }
}
