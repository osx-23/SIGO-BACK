package com.sigo.security.controller;

import com.sigo.security.dto.AdminResetPasswordRequest;
import com.sigo.security.dto.ChangePasswordRequest;
import com.sigo.security.dto.LoginRequest;
import com.sigo.security.dto.LoginResponse;
import com.sigo.security.dto.MeResponse;
import com.sigo.security.service.AuthService;
import com.sigo.security.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        loginAttemptService.verificar(request.codigo(), ip);
        try {
            LoginResponse response = authService.login(request);
            loginAttemptService.limpiar(request.codigo(), ip);
            return response;
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                loginAttemptService.registrarFallo(request.codigo(), ip);
                loginAttemptService.verificar(request.codigo(), ip);
            }
            throw e;
        }
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me();
    }

    @PutMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.cambiarPassword(request);
    }

    @PutMapping("/admin/trabajadores/{trabajadorId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @PathVariable Long trabajadorId,
            @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        authService.resetPasswordSupervisor(trabajadorId, request);
    }
}
