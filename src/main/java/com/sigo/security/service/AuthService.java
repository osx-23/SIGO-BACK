package com.sigo.security.service;

import com.sigo.personal.entity.RolSistema;
import com.sigo.personal.entity.Trabajador;
import com.sigo.personal.repository.TrabajadorRepository;
import com.sigo.security.dto.AdminResetPasswordRequest;
import com.sigo.security.dto.ChangePasswordRequest;
import com.sigo.security.dto.LoginRequest;
import com.sigo.security.dto.LoginResponse;
import com.sigo.security.dto.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TrabajadorRepository trabajadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final ModuloAccesoService moduloAccesoService;

    public LoginResponse login(LoginRequest request) {
        Trabajador t = trabajadorRepository.findByCodigo(request.codigo())
                .filter(x -> Boolean.TRUE.equals(x.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (t.getPasswordHash() == null || !passwordEncoder.matches(request.password(), t.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        String token = jwtService.generar(t);
        return new LoginResponse(token, "Bearer", jwtService.expirationSeconds(), toSesion(t));
    }

    public MeResponse me() {
        Trabajador t = currentUserService.requireCurrent();
        return new MeResponse(
                t.getId(), t.getId(), t.getCodigo(), t.getNombreCompleto(), t.getRolSistema().name(),
                t.getPlaza() == null ? null : t.getPlaza().getId(),
                t.getPlaza() == null ? null : t.getPlaza().getCodigo(),
                moduloAccesoService.modulosPara(t.getRolSistema()),
                Boolean.TRUE.equals(t.getRequiereCambioPassword())
        );
    }

    @Transactional
    public void cambiarPassword(ChangePasswordRequest request) {
        Trabajador t = currentUserService.requireCurrent();

        if (t.getPasswordHash() == null || !passwordEncoder.matches(request.passwordActual(), t.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.passwordNueva(), t.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser diferente a la actual");
        }

        t.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        t.setRequiereCambioPassword(false);
        trabajadorRepository.save(t);
    }

    @Transactional
    public void resetPasswordSupervisor(Long trabajadorId, AdminResetPasswordRequest request) {
        Trabajador supervisor = currentUserService.requireCurrent();
        if (supervisor.getRolSistema() != RolSistema.SUPERVISOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un supervisor puede cambiar contraseñas de otros usuarios");
        }

        Trabajador objetivo = trabajadorRepository.findById(trabajadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trabajador no encontrado"));
        if (!Boolean.TRUE.equals(objetivo.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede cambiar la contraseña de un trabajador inactivo");
        }

        objetivo.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        objetivo.setRequiereCambioPassword(request.exigirCambioAlIngresar());
        trabajadorRepository.save(objetivo);
    }

    private LoginResponse.UsuarioSesion toSesion(Trabajador t) {
        return new LoginResponse.UsuarioSesion(
                t.getId(), t.getId(), t.getCodigo(), t.getNombreCompleto(), t.getRolSistema().name(),
                t.getPlaza() == null ? null : t.getPlaza().getId(),
                t.getPlaza() == null ? null : t.getPlaza().getCodigo(),
                moduloAccesoService.modulosPara(t.getRolSistema()),
                Boolean.TRUE.equals(t.getRequiereCambioPassword())
        );
    }
}
