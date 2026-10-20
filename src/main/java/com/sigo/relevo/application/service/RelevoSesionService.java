package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.in.RelevoAccesoUseCase;
import com.sigo.relevo.application.port.in.RelevoSesionUseCase;
import com.sigo.relevo.application.port.out.RelevoSesionPort;
import com.sigo.relevo.application.port.out.RelevoUsuarioActualPort;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RelevoSesionService implements RelevoSesionUseCase {
    private final RelevoSesionPort sesiones;
    private final RelevoUsuarioActualPort usuarios;
    private final GestionarRelevoUseCase gestionar;
    private final RelevoAccesoUseCase acceso;
    private final com.sigo.relevo.application.port.in.RelevoHistorialUseCase historialUseCase;

    @Override
    @Transactional
    public Sesion iniciar(Long plazaId, Long turnoId) {
        var actual = usuarios.requireActual();
        validarPlaza(actual, plazaId);
        if (plazaId == null || turnoId == null || plazaId <= 0 || turnoId <= 0) {
            throw new BusinessException("Selecciona una plaza y un turno para iniciar el relevo");
        }
        sesiones.anularVencidos();
        var enProceso = sesiones.enProceso(actual.id());
        if (enProceso.isPresent()) {
            return enProceso.get();
        }
        return sesiones.crear(actual.id(), plazaId, turnoId);
    }

    @Override
    @Transactional
    public Sesion actual() {
        var usuario = usuarios.requireActual();
        sesiones.anularVencidos();
        return sesiones.enProceso(usuario.id()).orElse(null);
    }

    @Override
    @Transactional
    public Sesion guardarBorrador(Long id, String borrador) {
        var actual = usuarios.requireActual();
        exigirEnProceso(id, actual.id());
        if (borrador == null || borrador.length() > 100_000 || !borrador.stripLeading().startsWith("{")) {
            throw new BusinessException("El avance del relevo no tiene un formato válido");
        }
        return sesiones.guardarBorrador(id, borrador);
    }

    @Override
    @Transactional
    public Sesion finalizar(Long id, GestionarRelevoUseCase.Command command) {
        var actual = usuarios.requireActual();
        var sesion = exigirEnProceso(id, actual.id());
        var preparado = acceso.prepararRegistro(command);
        if (!Objects.equals(preparado.operadorId(), actual.id())
                || !Objects.equals(preparado.plazaId(), sesion.plazaId())
                || !Objects.equals(preparado.turnoId(), sesion.turnoId())) {
            throw new ForbiddenException("Los datos del relevo no corresponden a la sesión iniciada");
        }
        var relevo = gestionar.registrar(preparado);
        return sesiones.finalizar(id, relevo.id());
    }

    @Override
    @Transactional
    public Sesion cancelar(Long id) {
        var actual = usuarios.requireActual();
        exigirEnProceso(id, actual.id());
        return sesiones.cancelar(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sesion> historial(LocalDate inicio, LocalDate fin) {
        var accesibles = historialUseCase.listarPara(acceso.usuarioActual(), inicio, fin);
        var ids = accesibles.stream().map(relevo -> relevo.id()).toList();
        return sesiones.listarPorRelevos(ids).stream()
                .filter(sesion -> "FINALIZADO".equals(sesion.estado()))
                .map(sesion -> new Sesion(sesion.id(), sesion.operadorId(), sesion.plazaId(),
                        sesion.turnoId(), sesion.estado(), sesion.iniciadoEn(),
                        sesion.finalizadoEn(), sesion.relevoId(), null))
                .toList();
    }

    private Sesion exigirEnProceso(Long id, Long operadorId) {
        var sesion = sesiones.obtener(id).orElseThrow(() ->
                new BusinessException("No se encontró la sesión de relevo"));
        if (!Objects.equals(sesion.operadorId(), operadorId)) {
            throw new ForbiddenException("Esta sesión de relevo pertenece a otro usuario");
        }
        if (!"EN_PROCESO".equals(sesion.estado())) {
            throw new BusinessException("El relevo ya fue finalizado o anulado");
        }
        if (sesion.iniciadoEn() == null ||
                !sesion.iniciadoEn().toInstant().isAfter(
                        java.time.Instant.now().minus(8, java.time.temporal.ChronoUnit.HOURS))) {
            throw new BusinessException("Relevo vencido: la sesion se anula a las 8 horas");
        }
        return sesion;
    }

    private void validarPlaza(RelevoUsuarioActualPort.UsuarioActual actual, Long plazaId) {
        if ("OPERADOR".equals(actual.rol()) && !Objects.equals(actual.plazaId(), plazaId)) {
            throw new ForbiddenException("Solo puedes iniciar relevos de tu plaza asignada");
        }
    }
}
