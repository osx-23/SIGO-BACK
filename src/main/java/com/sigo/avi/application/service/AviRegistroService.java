package com.sigo.avi.application.service;

import com.sigo.avi.application.port.in.AviRegistroUseCase;
import com.sigo.avi.application.port.out.AviRegistroPersistencePort;
import com.sigo.avi.domain.AviAccion;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ConflictException;
import com.sigo.shared.exception.ForbiddenException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AviRegistroService
        implements AviRegistroUseCase {

    private final AviRegistroPersistencePort persistence;
    private final UsuarioActualUseCase usuarioActualUseCase;

    @Override
    @Transactional
    public Registro registrar(Command command) {
        if (command == null || command.id() == null) {
            throw new BusinessException(
                    "El identificador UUID es obligatorio"
            );
        }

        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        if (actual.plazaId() == null) {
            throw new ForbiddenException(
                    "El usuario no tiene una plaza asignada"
            );
        }

        String placa = normalizarPlaca(command.placa());

        if (command.via() == null || command.via() <= 0) {
            throw new BusinessException("La vía es obligatoria");
        }

        if (command.accion() == null) {
            throw new BusinessException("La acción es obligatoria");
        }

        if (command.fechaHoraEvento() == null) {
            throw new BusinessException(
                    "La hora del evento es obligatoria"
            );
        }

        String texto =
                normalizarTexto(command.textoReconocido());

        if (!persistence.viaActivaEnPlaza(
                actual.plazaId(),
                command.via()
        )) {
            throw new BusinessException(
                    "La vía no existe o no está activa en la plaza del usuario"
            );
        }

        Optional<AviRegistroPersistencePort.RegistroData> existente =
                persistence.buscarPorId(command.id());

        if (existente.isPresent()) {
            return resolverReenvio(
                    existente.get(),
                    actual,
                    placa,
                    command,
                    texto
            );
        }

        AviRegistroPersistencePort.RegistroData guardado =
                persistence.crearORecuperar(
                        command.id(),
                        actual.id(),
                        actual.plazaId(),
                        placa,
                        command.via(),
                        command.accion(),
                        command.fechaHoraEvento(),
                        texto
                );

        if (!equivalente(
                guardado,
                actual,
                placa,
                command,
                texto
        )) {
            throw conflictoUuid();
        }

        return map(guardado);
    }

    @Override
    @Transactional
    public Registro actualizar(
            UUID id,
            Command command
    ) {
        if (id == null || command == null) {
            throw new BusinessException(
                    "El registro y sus datos son obligatorios"
            );
        }

        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        AviRegistroPersistencePort.RegistroData existente =
                persistence.buscarPorId(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Registro AVI no encontrado"
                                )
                        );

        validarPermisoEdicion(actual, existente);

        String placa = normalizarPlaca(command.placa());

        if (command.via() == null || command.via() <= 0) {
            throw new BusinessException("La vía es obligatoria");
        }

        if (command.accion() == null) {
            throw new BusinessException("La acción es obligatoria");
        }

        if (command.fechaHoraEvento() == null) {
            throw new BusinessException(
                    "La hora del evento es obligatoria"
            );
        }

        if (!persistence.viaActivaEnPlaza(
                existente.plazaId(),
                command.via()
        )) {
            throw new BusinessException(
                    "La vía no existe o no está activa en la plaza del registro"
            );
        }

        String texto =
                normalizarTexto(command.textoReconocido());

        return map(
                persistence.actualizar(
                        id,
                        placa,
                        command.via(),
                        command.accion(),
                        command.fechaHoraEvento(),
                        texto
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Registro> listar(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            Long plazaId,
            Integer via,
            AviAccion accion
    ) {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        OffsetDateTime hastaEfectivo =
                hasta == null
                        ? OffsetDateTime.now(ZoneOffset.UTC)
                        : hasta;

        OffsetDateTime desdeEfectivo =
                desde == null
                        ? hastaEfectivo.minusDays(30)
                        : desde;

        if (desdeEfectivo.isAfter(hastaEfectivo)) {
            throw new BusinessException(
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        if (via != null && via <= 0) {
            throw new BusinessException(
                    "La vía debe ser mayor que cero"
            );
        }

        String rol = actual.rol() == null
                ? ""
                : actual.rol()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        Long plazaEfectiva = plazaId;
        Long usuarioId = null;

        switch (rol) {
            case "SUPERVISOR" -> {
            }
            case "CONTROLADOR" ->
                    plazaEfectiva =
                            exigirPlazaPropia(
                                    actual,
                                    plazaId
                            );
            case "OPERADOR" -> {
                plazaEfectiva =
                        exigirPlazaPropia(
                                actual,
                                plazaId
                        );
                usuarioId = actual.id();
            }
            default -> throw new ForbiddenException(
                    "El usuario no tiene acceso al historial AVI"
            );
        }

        return persistence.listar(
                        desdeEfectivo,
                        hastaEfectivo,
                        plazaEfectiva,
                        usuarioId,
                        via,
                        accion
                )
                .stream()
                .map(this::map)
                .toList();
    }

    private void validarPermisoEdicion(
            UsuarioActualUseCase.UsuarioActual actual,
            AviRegistroPersistencePort.RegistroData registro
    ) {
        String rol = actual.rol() == null
                ? ""
                : actual.rol()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        switch (rol) {
            case "SUPERVISOR" -> {
                return;
            }
            case "CONTROLADOR" -> {
                if (!Objects.equals(
                        actual.plazaId(),
                        registro.plazaId()
                )) {
                    throw new ForbiddenException(
                            "No puedes editar registros AVI de otra plaza"
                    );
                }
            }
            case "OPERADOR" -> {
                if (!Objects.equals(
                        actual.id(),
                        registro.usuarioId()
                )) {
                    throw new ForbiddenException(
                            "Solo puedes editar tus propios registros AVI"
                    );
                }
            }
            default -> throw new ForbiddenException(
                    "El usuario no tiene permiso para editar registros AVI"
            );
        }
    }

    private Long exigirPlazaPropia(
            UsuarioActualUseCase.UsuarioActual actual,
            Long plazaSolicitada
    ) {
        if (actual.plazaId() == null) {
            throw new ForbiddenException(
                    "El usuario no tiene una plaza asignada"
            );
        }

        if (plazaSolicitada != null
                && !actual.plazaId()
                        .equals(plazaSolicitada)) {
            throw new ForbiddenException(
                    "No tienes acceso a registros AVI de otra plaza"
            );
        }

        return actual.plazaId();
    }

    private Registro resolverReenvio(
            AviRegistroPersistencePort.RegistroData existente,
            UsuarioActualUseCase.UsuarioActual actual,
            String placa,
            Command command,
            String texto
    ) {
        if (!equivalente(
                existente,
                actual,
                placa,
                command,
                texto
        )) {
            throw conflictoUuid();
        }

        return map(existente);
    }

    private boolean equivalente(
            AviRegistroPersistencePort.RegistroData registro,
            UsuarioActualUseCase.UsuarioActual actual,
            String placa,
            Command command,
            String texto
    ) {
        return Objects.equals(registro.usuarioId(), actual.id())
                && Objects.equals(
                        registro.plazaId(),
                        actual.plazaId()
                )
                && Objects.equals(registro.placa(), placa)
                && Objects.equals(
                        registro.via(),
                        command.via()
                )
                && registro.accion() == command.accion()
                && mismoInstante(
                        registro.fechaHoraEvento(),
                        command.fechaHoraEvento()
                )
                && Objects.equals(
                        normalizarTexto(
                                registro.textoReconocido()
                        ),
                        texto
                );
    }

    private boolean mismoInstante(
            OffsetDateTime a,
            OffsetDateTime b
    ) {
        return a != null
                && b != null
                && a.toInstant().equals(b.toInstant());
    }

    private String normalizarPlaca(String valor) {
        if (valor == null) {
            throw new BusinessException(
                    "La placa es obligatoria"
            );
        }

        String placa = valor
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);

        if (placa.isBlank() || placa.length() > 15) {
            throw new BusinessException(
                    "La placa debe contener entre 1 y 15 caracteres alfanuméricos"
            );
        }

        return placa;
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }

    private ConflictException conflictoUuid() {
        return new ConflictException(
                "El UUID ya fue utilizado con datos diferentes"
        );
    }

    private Registro map(
            AviRegistroPersistencePort.RegistroData item
    ) {
        return new Registro(
                item.id(),
                item.usuarioId(),
                item.usuarioCodigo(),
                item.usuarioNombre(),
                item.plazaId(),
                item.plazaCodigo(),
                item.placa(),
                item.via(),
                item.accion(),
                item.fechaHoraEvento(),
                item.fechaHoraRecepcion(),
                item.textoReconocido()
        );
    }
}
