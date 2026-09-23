package com.sigo.security.api.controller;

import com.sigo.security.api.dto.AdminResetPasswordRequest;
import com.sigo.security.api.dto.ChangePasswordRequest;
import com.sigo.security.api.dto.LoginRequest;
import com.sigo.security.api.dto.LoginResponse;
import com.sigo.security.api.dto.MeResponse;
import com.sigo.security.application.port.in.AutenticacionUseCase;
import com.sigo.security.application.service.LoginAttemptService;
import com.sigo.security.domain.AutenticacionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AutenticacionUseCase autenticacionUseCase;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();

        loginAttemptService.verificar(
                request.codigo(),
                ip
        );

        try {
            AutenticacionUseCase.LoginResult result =
                    autenticacionUseCase.login(
                            new AutenticacionUseCase.LoginCommand(
                                    request.codigo(),
                                    request.password()
                            )
                    );

            loginAttemptService.limpiar(
                    request.codigo(),
                    ip
            );

            return toLoginResponse(result);

        } catch (AutenticacionException exception) {
            if (exception.tipo()
                    == AutenticacionException.Tipo.UNAUTHORIZED) {
                loginAttemptService.registrarFallo(
                        request.codigo(),
                        ip
                );

                loginAttemptService.verificar(
                        request.codigo(),
                        ip
                );
            }

            throw exception;
        }
    }

    @GetMapping("/me")
    public MeResponse me() {
        return toMeResponse(
                autenticacionUseCase.me()
        );
    }

    @PutMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(
            @Valid
            @RequestBody ChangePasswordRequest request
    ) {
        autenticacionUseCase.cambiarPassword(
                new AutenticacionUseCase.ChangePasswordCommand(
                        request.passwordActual(),
                        request.passwordNueva()
                )
        );
    }

    @PutMapping("/admin/trabajadores/{trabajadorId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @PathVariable Long trabajadorId,
            @Valid
            @RequestBody AdminResetPasswordRequest request
    ) {
        autenticacionUseCase.resetPasswordSupervisor(
                trabajadorId,
                new AutenticacionUseCase.ResetPasswordCommand(
                        request.passwordNueva(),
                        request.exigirCambioAlIngresar()
                )
        );
    }

    private LoginResponse toLoginResponse(
            AutenticacionUseCase.LoginResult result
    ) {
        return new LoginResponse(
                result.token(),
                result.tipo(),
                result.expiresIn(),
                new LoginResponse.UsuarioSesion(
                        result.usuario().id(),
                        result.usuario().trabajadorId(),
                        result.usuario().codigo(),
                        result.usuario().nombre(),
                        result.usuario().rol(),
                        result.usuario().plazaId(),
                        result.usuario().plaza(),
                        result.usuario().modulos(),
                        result.usuario().requiereCambioPassword()
                )
        );
    }

    private MeResponse toMeResponse(
            AutenticacionUseCase.Sesion sesion
    ) {
        return new MeResponse(
                sesion.id(),
                sesion.trabajadorId(),
                sesion.codigo(),
                sesion.nombre(),
                sesion.rol(),
                sesion.plazaId(),
                sesion.plaza(),
                sesion.modulos(),
                sesion.requiereCambioPassword()
        );
    }
}
