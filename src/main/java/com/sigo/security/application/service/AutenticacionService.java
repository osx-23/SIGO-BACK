package com.sigo.security.application.service;

import com.sigo.security.application.port.in.AutenticacionUseCase;
import com.sigo.security.application.port.out.PasswordSeguridadPort;
import com.sigo.security.application.port.out.SujetoAutenticadoPort;
import com.sigo.security.application.port.out.TokenSeguridadPort;
import com.sigo.security.application.port.out.UsuarioSeguridadPort;
import com.sigo.security.domain.AutenticacionException;
import com.sigo.security.domain.ModuloAccesoPolicy;
import com.sigo.security.domain.RolSeguridad;
import com.sigo.security.domain.UsuarioSeguridad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutenticacionService
        implements AutenticacionUseCase {

    private final UsuarioSeguridadPort usuarioPort;
    private final PasswordSeguridadPort passwordPort;
    private final TokenSeguridadPort tokenPort;
    private final SujetoAutenticadoPort sujetoPort;

    private final ModuloAccesoPolicy moduloPolicy =
            new ModuloAccesoPolicy();

    @Override
    public LoginResult login(LoginCommand command) {
        UsuarioSeguridad usuario = usuarioPort
                .buscarPorCodigo(command.codigo())
                .filter(UsuarioSeguridad::estaActivo)
                .orElseThrow(this::credencialesInvalidas);

        if (usuario.passwordHash() == null
                || !passwordPort.matches(
                        command.password(),
                        usuario.passwordHash()
                )) {
            throw credencialesInvalidas();
        }

        return new LoginResult(
                tokenPort.generar(usuario),
                "Bearer",
                tokenPort.expirationSeconds(),
                toSesion(usuario)
        );
    }

    @Override
    public Sesion me() {
        return toSesion(requireCurrent());
    }

    @Override
    @Transactional
    public void cambiarPassword(
            ChangePasswordCommand command
    ) {
        UsuarioSeguridad usuario = requireCurrent();

        if (usuario.passwordHash() == null
                || !passwordPort.matches(
                        command.passwordActual(),
                        usuario.passwordHash()
                )) {
            throw new AutenticacionException(
                    AutenticacionException.Tipo.BAD_REQUEST,
                    "La contraseña actual no es correcta"
            );
        }

        if (passwordPort.matches(
                command.passwordNueva(),
                usuario.passwordHash()
        )) {
            throw new AutenticacionException(
                    AutenticacionException.Tipo.BAD_REQUEST,
                    "La nueva contraseña debe ser diferente a la actual"
            );
        }

        usuarioPort.actualizarPassword(
                usuario.id(),
                passwordPort.encode(command.passwordNueva()),
                false
        );
    }

    @Override
    @Transactional
    public void resetPasswordSupervisor(
            Long trabajadorId,
            ResetPasswordCommand command
    ) {
        UsuarioSeguridad supervisor = requireCurrent();

        if (supervisor.rol() != RolSeguridad.SUPERVISOR) {
            throw new AutenticacionException(
                    AutenticacionException.Tipo.FORBIDDEN,
                    "Solo un supervisor puede cambiar contraseñas de otros usuarios"
            );
        }

        UsuarioSeguridad objetivo = usuarioPort
                .buscarPorId(trabajadorId)
                .orElseThrow(() ->
                        new AutenticacionException(
                                AutenticacionException.Tipo.NOT_FOUND,
                                "Trabajador no encontrado"
                        )
                );

        if (!objetivo.estaActivo()) {
            throw new AutenticacionException(
                    AutenticacionException.Tipo.BAD_REQUEST,
                    "No se puede cambiar la contraseña de un trabajador inactivo"
            );
        }

        usuarioPort.actualizarPassword(
                objetivo.id(),
                passwordPort.encode(command.passwordNueva()),
                command.exigirCambioAlIngresar()
        );
    }

    private UsuarioSeguridad requireCurrent() {
        Integer codigo = sujetoPort.requireCodigo();

        return usuarioPort
                .buscarPorCodigo(codigo)
                .filter(UsuarioSeguridad::estaActivo)
                .orElseThrow(() ->
                        new AutenticacionException(
                                AutenticacionException.Tipo.UNAUTHORIZED,
                                "Usuario no encontrado o inactivo"
                        )
                );
    }

    private Sesion toSesion(UsuarioSeguridad usuario) {
        return new Sesion(
                usuario.id(),
                usuario.id(),
                usuario.codigo(),
                usuario.nombre(),
                usuario.rol().name(),
                usuario.plazaId(),
                usuario.plazaCodigo(),
                moduloPolicy.modulosPara(usuario.rol()),
                Boolean.TRUE.equals(
                        usuario.requiereCambioPassword()
                )
        );
    }

    private AutenticacionException credencialesInvalidas() {
        return new AutenticacionException(
                AutenticacionException.Tipo.UNAUTHORIZED,
                "Credenciales inválidas"
        );
    }
}
