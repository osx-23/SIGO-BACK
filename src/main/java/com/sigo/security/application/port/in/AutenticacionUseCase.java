package com.sigo.security.application.port.in;

import java.util.List;

public interface AutenticacionUseCase {

    LoginResult login(LoginCommand command);

    Sesion me();

    void cambiarPassword(ChangePasswordCommand command);

    void resetPasswordSupervisor(
            Long trabajadorId,
            ResetPasswordCommand command
    );

    record LoginCommand(
            Integer codigo,
            String password
    ) {
    }

    record ChangePasswordCommand(
            String passwordActual,
            String passwordNueva
    ) {
    }

    record ResetPasswordCommand(
            String passwordNueva,
            boolean exigirCambioAlIngresar
    ) {
    }

    record Sesion(
            Long id,
            Long trabajadorId,
            Integer codigo,
            String nombre,
            String rol,
            Long plazaId,
            String plaza,
            List<String> modulos,
            Boolean requiereCambioPassword
    ) {
    }

    record LoginResult(
            String token,
            String tipo,
            long expiresIn,
            Sesion usuario
    ) {
    }
}
