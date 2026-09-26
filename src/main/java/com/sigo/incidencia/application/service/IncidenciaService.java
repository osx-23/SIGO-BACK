package com.sigo.incidencia.application.service;

import com.sigo.incidencia.application.port.in.IncidenciaUseCase;
import com.sigo.incidencia.application.port.out.IncidenciaPersistencePort;
import com.sigo.incidencia.application.port.out.IncidenciaStoragePort;
import com.sigo.incidencia.domain.EstadoIncidencia;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ForbiddenException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class IncidenciaService implements IncidenciaUseCase {

    private final IncidenciaPersistencePort persistence;
    private final IncidenciaStoragePort storage;
    private final UsuarioActualUseCase usuarioActualUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<Tipo> listarTipos() {
        return persistence.listarTiposActivos()
                .stream()
                .map(t -> new Tipo(t.id(), t.nombre()))
                .toList();
    }

    @Override
    @Transactional
    public Incidencia registrar(Command command) {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        Long plazaId =
                resolverPlazaRegistro(
                        actual,
                        command.plazaId()
                );

        validarCatalogos(
                plazaId,
                command.turnoId(),
                command.tipoId(),
                command.viaId(),
                actual.id()
        );

        String descripcion =
                command.descripcion() == null
                        ? ""
                        : command.descripcion().trim();

        if (descripcion.length() < 3) {
            throw new BusinessException(
                    "Describe la incidencia con al menos 3 caracteres"
            );
        }

        IncidenciaPersistencePort.IncidenciaData creada =
                persistence.crear(
                        plazaId,
                        command.turnoId(),
                        actual.id(),
                        command.tipoId(),
                        command.viaId(),
                        command.fecha(),
                        command.hora(),
                        descripcion
                );

        return map(creada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incidencia> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            EstadoIncidencia estado
    ) {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        LocalDate hasta =
                fin == null
                        ? LocalDate.now()
                        : fin;

        LocalDate desde =
                inicio == null
                        ? hasta.minusDays(30)
                        : inicio;

        if (desde.isAfter(hasta)) {
            throw new BusinessException(
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        Long plazaEfectiva =
                resolverPlazaConsulta(
                        actual,
                        plazaId
                );

        return persistence
                .listar(
                        desde,
                        hasta,
                        plazaEfectiva
                )
                .stream()
                .filter(i ->
                        estado == null ||
                        i.estado() == estado
                )
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Incidencia obtener(Long id) {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        IncidenciaPersistencePort.IncidenciaData item =
                persistence.obtener(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Incidencia no encontrada"
                                )
                        );

        validarLectura(actual, item.plazaId());

        return map(item);
    }

    @Override
    @Transactional
    public Incidencia atender(Long id) {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        validarControl(actual);

        persistence.obtener(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Incidencia no encontrada"
                        )
                );

        return map(
                persistence.marcarAtendido(
                        id,
                        OffsetDateTime.now()
                )
        );
    }

    @Override
    @Transactional
    public Evidencia subirEvidencia(
            Long incidenciaId,
            byte[] contenido,
            String contentType
    ) {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        IncidenciaPersistencePort.IncidenciaData item =
                persistence.obtener(incidenciaId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Incidencia no encontrada"
                                )
                        );

        validarLectura(
                actual,
                item.plazaId()
        );

        if (persistence.contarEvidencias(incidenciaId) >= 5) {
            throw new BusinessException(
                    "La incidencia ya tiene el máximo de 5 evidencias"
            );
        }

        IncidenciaStoragePort.UploadResult subida =
                storage.subir(
                        contenido,
                        contentType
                );

        return map(
                persistence.guardarEvidencia(
                        incidenciaId,
                        subida.urlArchivo(),
                        subida.publicId()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPendientes() {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        validarControl(actual);

        return persistence.contarPorEstado(
                EstadoIncidencia.OBSERVACION
        );
    }

    private void validarCatalogos(
            Long plazaId,
            Long turnoId,
            Long tipoId,
            Long viaId,
            Long trabajadorId
    ) {
        if (!persistence.plazaActiva(plazaId)) {
            throw new ResourceNotFoundException(
                    "Plaza no encontrada"
            );
        }

        if (turnoId == null ||
                !persistence.turnoExiste(turnoId)) {

            throw new ResourceNotFoundException(
                    "Turno no encontrado"
            );
        }

        if (!persistence.trabajadorActivo(trabajadorId)) {
            throw new ResourceNotFoundException(
                    "Trabajador no encontrado"
            );
        }

        if (tipoId == null ||
                !persistence.tipoActivo(tipoId)) {

            throw new ResourceNotFoundException(
                    "Tipo de incidencia no encontrado"
            );
        }

        if (viaId != null) {
            Long plazaVia =
                    persistence.plazaIdDeViaActiva(viaId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Vía no encontrada"
                                    )
                            );

            if (!plazaId.equals(plazaVia)) {
                throw new BusinessException(
                        "La vía seleccionada no pertenece a la plaza indicada"
                );
            }
        }
    }

    private Long resolverPlazaRegistro(
            UsuarioActualUseCase.UsuarioActual actual,
            Long plazaSolicitada
    ) {
        if ("OPERADOR".equals(
                normalizarRol(actual.rol())
        )) {
            if (actual.plazaId() == null) {
                throw new ForbiddenException(
                        "El operador no tiene una plaza asignada"
                );
            }

            return actual.plazaId();
        }

        if (plazaSolicitada == null) {
            throw new BusinessException(
                    "Selecciona una plaza"
            );
        }

        return plazaSolicitada;
    }

    private Long resolverPlazaConsulta(
            UsuarioActualUseCase.UsuarioActual actual,
            Long plazaSolicitada
    ) {
        if ("OPERADOR".equals(
                normalizarRol(actual.rol())
        )) {
            if (actual.plazaId() == null) {
                throw new ForbiddenException(
                        "El operador no tiene una plaza asignada"
                );
            }

            return actual.plazaId();
        }

        return plazaSolicitada;
    }

    private void validarLectura(
            UsuarioActualUseCase.UsuarioActual actual,
            Long plazaIdIncidencia
    ) {
        if (!"OPERADOR".equals(
                normalizarRol(actual.rol())
        )) {
            return;
        }

        if (actual.plazaId() == null ||
                !actual.plazaId()
                        .equals(plazaIdIncidencia)) {

            throw new ForbiddenException(
                    "No tienes acceso a incidencias de otra plaza"
            );
        }
    }

    private void validarControl(
            UsuarioActualUseCase.UsuarioActual actual
    ) {
        String rol =
                normalizarRol(actual.rol());

        if (!"SUPERVISOR".equals(rol) &&
                !"CONTROLADOR".equals(rol)) {

            throw new ForbiddenException(
                    "Solo control y supervisión pueden atender incidencias"
            );
        }
    }

    private String normalizarRol(String rol) {
        return rol == null
                ? ""
                : rol.trim()
                        .toUpperCase(Locale.ROOT);
    }

    private Incidencia map(
            IncidenciaPersistencePort.IncidenciaData i
    ) {
        return new Incidencia(
                i.id(),
                i.plazaId(),
                i.plazaCodigo(),
                i.turnoId(),
                i.turnoCodigo(),
                i.registradoPorId(),
                i.registradoPorCodigo(),
                i.registradoPorNombre(),
                i.tipoId(),
                i.tipoNombre(),
                i.viaId(),
                i.viaNumero(),
                i.viaNombre(),
                i.fecha(),
                i.hora(),
                i.descripcion(),
                i.estado(),
                i.fechaCreacion(),
                i.fechaAtendido(),
                i.evidencias()
                        .stream()
                        .map(this::map)
                        .toList()
        );
    }

    private Evidencia map(
            IncidenciaPersistencePort.EvidenciaData e
    ) {
        return new Evidencia(
                e.id(),
                e.urlArchivo(),
                e.publicId(),
                e.tipo(),
                e.fechaCreacion()
        );
    }
}
