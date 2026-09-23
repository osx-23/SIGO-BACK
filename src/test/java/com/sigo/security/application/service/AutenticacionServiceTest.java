package com.sigo.security.application.service;

import com.sigo.security.application.port.in.AutenticacionUseCase;
import com.sigo.security.application.port.out.PasswordSeguridadPort;
import com.sigo.security.application.port.out.SujetoAutenticadoPort;
import com.sigo.security.application.port.out.TokenSeguridadPort;
import com.sigo.security.application.port.out.UsuarioSeguridadPort;
import com.sigo.security.domain.AutenticacionException;
import com.sigo.security.domain.RolSeguridad;
import com.sigo.security.domain.UsuarioSeguridad;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacionServiceTest {

    @Test
    void loginDevuelveTokenYSesion() {
        StubUsuarioPort usuarios = new StubUsuarioPort();
        usuarios.add(usuario(
                1L,
                287,
                RolSeguridad.SUPERVISOR,
                true,
                "hash"
        ));

        AutenticacionService service =
                new AutenticacionService(
                        usuarios,
                        new StubPasswordPort(),
                        new StubTokenPort(),
                        () -> 287
                );

        AutenticacionUseCase.LoginResult result =
                service.login(
                        new AutenticacionUseCase.LoginCommand(
                                287,
                                "correcta"
                        )
                );

        assertEquals("token-287", result.token());
        assertEquals("SUPERVISOR", result.usuario().rol());
        assertTrue(
                result.usuario()
                        .modulos()
                        .contains("PROGRAMACION")
        );
    }

    @Test
    void loginRechazaPasswordIncorrecta() {
        StubUsuarioPort usuarios = new StubUsuarioPort();
        usuarios.add(usuario(
                1L,
                287,
                RolSeguridad.CONTROLADOR,
                true,
                "hash"
        ));

        AutenticacionService service =
                new AutenticacionService(
                        usuarios,
                        new StubPasswordPort(),
                        new StubTokenPort(),
                        () -> 287
                );

        AutenticacionException exception =
                assertThrows(
                        AutenticacionException.class,
                        () -> service.login(
                                new AutenticacionUseCase.LoginCommand(
                                        287,
                                        "incorrecta"
                                )
                        )
                );

        assertEquals(
                AutenticacionException.Tipo.UNAUTHORIZED,
                exception.tipo()
        );
    }

    @Test
    void operadorNoPuedeResetearPasswordDeOtroUsuario() {
        StubUsuarioPort usuarios = new StubUsuarioPort();

        usuarios.add(usuario(
                1L,
                100,
                RolSeguridad.OPERADOR,
                true,
                "hash"
        ));

        usuarios.add(usuario(
                2L,
                200,
                RolSeguridad.OPERADOR,
                true,
                "hash"
        ));

        AutenticacionService service =
                new AutenticacionService(
                        usuarios,
                        new StubPasswordPort(),
                        new StubTokenPort(),
                        () -> 100
                );

        AutenticacionException exception =
                assertThrows(
                        AutenticacionException.class,
                        () -> service.resetPasswordSupervisor(
                                2L,
                                new AutenticacionUseCase.ResetPasswordCommand(
                                        "nueva123",
                                        true
                                )
                        )
                );

        assertEquals(
                AutenticacionException.Tipo.FORBIDDEN,
                exception.tipo()
        );
    }

    @Test
    void cambiarPasswordActualizaHashYQuitaCambioObligatorio() {
        StubUsuarioPort usuarios = new StubUsuarioPort();

        usuarios.add(
                new UsuarioSeguridad(
                        1L,
                        287,
                        "Usuario 287",
                        RolSeguridad.CONTROLADOR,
                        4L,
                        "P4",
                        "hash",
                        true,
                        true
                )
        );

        AutenticacionService service =
                new AutenticacionService(
                        usuarios,
                        new StubPasswordPort(),
                        new StubTokenPort(),
                        () -> 287
                );

        service.cambiarPassword(
                new AutenticacionUseCase.ChangePasswordCommand(
                        "correcta",
                        "nueva123"
                )
        );

        UsuarioSeguridad actualizado =
                usuarios.buscarPorCodigo(287).orElseThrow();

        assertEquals(
                "encoded-nueva123",
                actualizado.passwordHash()
        );
        assertEquals(
                false,
                actualizado.requiereCambioPassword()
        );
    }

    @Test
    void cambiarPasswordRechazaPasswordActualIncorrecta() {
        StubUsuarioPort usuarios = new StubUsuarioPort();

        usuarios.add(usuario(
                1L,
                287,
                RolSeguridad.CONTROLADOR,
                true,
                "hash"
        ));

        AutenticacionService service =
                new AutenticacionService(
                        usuarios,
                        new StubPasswordPort(),
                        new StubTokenPort(),
                        () -> 287
                );

        AutenticacionException exception =
                assertThrows(
                        AutenticacionException.class,
                        () -> service.cambiarPassword(
                                new AutenticacionUseCase.ChangePasswordCommand(
                                        "incorrecta",
                                        "nueva123"
                                )
                        )
                );

        assertEquals(
                AutenticacionException.Tipo.BAD_REQUEST,
                exception.tipo()
        );
    }

    @Test
    void supervisorReseteaPasswordYPuedeExigirCambioAlIngresar() {
        StubUsuarioPort usuarios = new StubUsuarioPort();

        usuarios.add(usuario(
                1L,
                100,
                RolSeguridad.SUPERVISOR,
                true,
                "hash"
        ));

        usuarios.add(usuario(
                2L,
                200,
                RolSeguridad.OPERADOR,
                true,
                "hash"
        ));

        AutenticacionService service =
                new AutenticacionService(
                        usuarios,
                        new StubPasswordPort(),
                        new StubTokenPort(),
                        () -> 100
                );

        service.resetPasswordSupervisor(
                2L,
                new AutenticacionUseCase.ResetPasswordCommand(
                        "temporal123",
                        true
                )
        );

        UsuarioSeguridad actualizado =
                usuarios.buscarPorId(2L).orElseThrow();

        assertEquals(
                "encoded-temporal123",
                actualizado.passwordHash()
        );
        assertTrue(actualizado.requiereCambioPassword());
    }

    @Test
    void supervisorNoPuedeResetearUsuarioInactivo() {
        StubUsuarioPort usuarios = new StubUsuarioPort();

        usuarios.add(usuario(
                1L,
                100,
                RolSeguridad.SUPERVISOR,
                true,
                "hash"
        ));

        usuarios.add(usuario(
                2L,
                200,
                RolSeguridad.OPERADOR,
                false,
                "hash"
        ));

        AutenticacionService service =
                new AutenticacionService(
                        usuarios,
                        new StubPasswordPort(),
                        new StubTokenPort(),
                        () -> 100
                );

        AutenticacionException exception =
                assertThrows(
                        AutenticacionException.class,
                        () -> service.resetPasswordSupervisor(
                                2L,
                                new AutenticacionUseCase.ResetPasswordCommand(
                                        "temporal123",
                                        true
                                )
                        )
                );

        assertEquals(
                AutenticacionException.Tipo.BAD_REQUEST,
                exception.tipo()
        );
    }

    private UsuarioSeguridad usuario(
            Long id,
            Integer codigo,
            RolSeguridad rol,
            boolean activo,
            String hash
    ) {
        return new UsuarioSeguridad(
                id,
                codigo,
                "Usuario " + codigo,
                rol,
                4L,
                "P4",
                hash,
                false,
                activo
        );
    }

    private static class StubUsuarioPort
            implements UsuarioSeguridadPort {

        private final Map<Integer, UsuarioSeguridad> porCodigo =
                new HashMap<>();

        private final Map<Long, UsuarioSeguridad> porId =
                new HashMap<>();

        void add(UsuarioSeguridad usuario) {
            porCodigo.put(
                    usuario.codigo(),
                    usuario
            );

            porId.put(
                    usuario.id(),
                    usuario
            );
        }

        @Override
        public Optional<UsuarioSeguridad> buscarPorCodigo(
                Integer codigo
        ) {
            return Optional.ofNullable(
                    porCodigo.get(codigo)
            );
        }

        @Override
        public Optional<UsuarioSeguridad> buscarPorId(Long id) {
            return Optional.ofNullable(
                    porId.get(id)
            );
        }

        @Override
        public void actualizarPassword(
                Long trabajadorId,
                String passwordHash,
                boolean requiereCambioPassword
        ) {
            UsuarioSeguridad actual =
                    porId.get(trabajadorId);

            UsuarioSeguridad actualizado =
                    new UsuarioSeguridad(
                            actual.id(),
                            actual.codigo(),
                            actual.nombre(),
                            actual.rol(),
                            actual.plazaId(),
                            actual.plazaCodigo(),
                            passwordHash,
                            requiereCambioPassword,
                            actual.activo()
                    );

            add(actualizado);
        }
    }

    private static class StubPasswordPort
            implements PasswordSeguridadPort {

        @Override
        public boolean matches(
                String raw,
                String encoded
        ) {
            return "correcta".equals(raw)
                    && "hash".equals(encoded);
        }

        @Override
        public String encode(String raw) {
            return "encoded-" + raw;
        }
    }

    private static class StubTokenPort
            implements TokenSeguridadPort {

        @Override
        public String generar(UsuarioSeguridad usuario) {
            return "token-" + usuario.codigo();
        }

        @Override
        public long expirationSeconds() {
            return 28800;
        }
    }
}
