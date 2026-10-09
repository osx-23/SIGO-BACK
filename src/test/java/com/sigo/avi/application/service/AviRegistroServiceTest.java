package com.sigo.avi.application.service;

import com.sigo.avi.application.port.in.AviRegistroUseCase;
import com.sigo.avi.application.port.out.AviRegistroPersistencePort;
import com.sigo.avi.domain.AviAccion;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.ConflictException;
import com.sigo.shared.exception.ForbiddenException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AviRegistroServiceTest {

    @Test
    void registrarDerivaUsuarioYPlazaYNormalizaPlaca() {
        AviRegistroPersistencePort persistence =
                mock(AviRegistroPersistencePort.class);
        UsuarioActualUseCase actualUseCase =
                mock(UsuarioActualUseCase.class);

        AviRegistroService service =
                new AviRegistroService(
                        persistence,
                        actualUseCase
                );

        UUID id = UUID.randomUUID();
        OffsetDateTime evento =
                OffsetDateTime.parse(
                        "2026-10-06T19:00:00-05:00"
                );

        when(actualUseCase.requireActual())
                .thenReturn(usuario("OPERADOR"));
        when(persistence.viaVisibleAviEnPlaza(4L, 151))
                .thenReturn(true);
        when(persistence.buscarPorId(id))
                .thenReturn(Optional.empty());
        when(persistence.crearORecuperar(
                eq(id),
                eq(10L),
                eq(4L),
                eq("ABC123"),
                eq(151),
                eq(AviAccion.FUGA),
                eq(evento),
                eq("fuga vía 151")
        )).thenReturn(data(
                id,
                "ABC123",
                151,
                AviAccion.FUGA,
                evento,
                "fuga vía 151"
        ));

        AviRegistroUseCase.Registro result =
                service.registrar(
                        new AviRegistroUseCase.Command(
                                id,
                                "abc-123",
                                151,
                                AviAccion.FUGA,
                                evento,
                                " fuga vía 151 "
                        )
                );

        assertEquals("ABC123", result.placa());
        assertEquals(10L, result.usuarioId());
        assertEquals(4L, result.plazaId());
    }

    @Test
    void registrarRechazaViaOcultaPorSupervisor() {
        AviRegistroPersistencePort persistence =
                mock(AviRegistroPersistencePort.class);
        UsuarioActualUseCase actualUseCase =
                mock(UsuarioActualUseCase.class);

        AviRegistroService service =
                new AviRegistroService(
                        persistence,
                        actualUseCase
                );

        UUID id = UUID.randomUUID();
        OffsetDateTime evento =
                OffsetDateTime.parse(
                        "2026-10-09T16:00:00-05:00"
                );

        when(actualUseCase.requireActual())
                .thenReturn(usuario("OPERADOR"));
        when(persistence.viaVisibleAviEnPlaza(4L, 102))
                .thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> service.registrar(
                        new AviRegistroUseCase.Command(
                                id,
                                "ABC123",
                                102,
                                AviAccion.LIBERADO,
                                evento,
                                "liberado vía 102"
                        )
                )
        );

        verify(persistence, never())
                .crearORecuperar(
                        any(),
                        anyLong(),
                        anyLong(),
                        anyString(),
                        anyInt(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void reenvioMismoUuidConContenidoDistintoDaConflicto() {
        AviRegistroPersistencePort persistence =
                mock(AviRegistroPersistencePort.class);
        UsuarioActualUseCase actualUseCase =
                mock(UsuarioActualUseCase.class);

        AviRegistroService service =
                new AviRegistroService(
                        persistence,
                        actualUseCase
                );

        UUID id = UUID.randomUUID();
        OffsetDateTime evento =
                OffsetDateTime.parse(
                        "2026-10-06T19:00:00-05:00"
                );

        when(actualUseCase.requireActual())
                .thenReturn(usuario("OPERADOR"));
        when(persistence.viaVisibleAviEnPlaza(4L, 151))
                .thenReturn(true);
        when(persistence.buscarPorId(id))
                .thenReturn(Optional.of(
                        data(
                                id,
                                "ZZZ999",
                                151,
                                AviAccion.FUGA,
                                evento,
                                null
                        )
                ));

        assertThrows(
                ConflictException.class,
                () -> service.registrar(
                        new AviRegistroUseCase.Command(
                                id,
                                "ABC123",
                                151,
                                AviAccion.FUGA,
                                evento,
                                null
                        )
                )
        );

        verify(persistence, never())
                .crearORecuperar(
                        any(),
                        anyLong(),
                        anyLong(),
                        anyString(),
                        anyInt(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void operadorSoloListaSusRegistrosDeSuPlaza() {
        AviRegistroPersistencePort persistence =
                mock(AviRegistroPersistencePort.class);
        UsuarioActualUseCase actualUseCase =
                mock(UsuarioActualUseCase.class);

        AviRegistroService service =
                new AviRegistroService(
                        persistence,
                        actualUseCase
                );

        OffsetDateTime desde =
                OffsetDateTime.parse(
                        "2026-10-01T00:00:00-05:00"
                );
        OffsetDateTime hasta =
                OffsetDateTime.parse(
                        "2026-10-06T23:59:59-05:00"
                );

        when(actualUseCase.requireActual())
                .thenReturn(usuario("OPERADOR"));
        when(persistence.listar(
                desde,
                hasta,
                4L,
                10L,
                null,
                null
        )).thenReturn(List.of());

        service.listar(
                desde,
                hasta,
                null,
                null,
                null
        );

        verify(persistence).listar(
                desde,
                hasta,
                4L,
                10L,
                null,
                null
        );
    }

    @Test
    void controladorNoPuedeConsultarOtraPlaza() {
        AviRegistroPersistencePort persistence =
                mock(AviRegistroPersistencePort.class);
        UsuarioActualUseCase actualUseCase =
                mock(UsuarioActualUseCase.class);

        AviRegistroService service =
                new AviRegistroService(
                        persistence,
                        actualUseCase
                );

        when(actualUseCase.requireActual())
                .thenReturn(usuario("CONTROLADOR"));

        assertThrows(
                ForbiddenException.class,
                () -> service.listar(
                        OffsetDateTime.parse(
                                "2026-10-01T00:00:00-05:00"
                        ),
                        OffsetDateTime.parse(
                                "2026-10-06T23:59:59-05:00"
                        ),
                        5L,
                        null,
                        null
                )
        );
    }

    @Test
    void operadorPuedeEditarSuPropioRegistro() {
        AviRegistroPersistencePort persistence =
                mock(AviRegistroPersistencePort.class);
        UsuarioActualUseCase actualUseCase =
                mock(UsuarioActualUseCase.class);

        AviRegistroService service =
                new AviRegistroService(
                        persistence,
                        actualUseCase
                );

        UUID id = UUID.randomUUID();
        OffsetDateTime evento =
                OffsetDateTime.parse(
                        "2026-10-08T20:10:00-05:00"
                );

        when(actualUseCase.requireActual())
                .thenReturn(usuario("OPERADOR"));
        when(persistence.buscarPorId(id))
                .thenReturn(Optional.of(
                        data(
                                id,
                                "I1L111",
                                101,
                                AviAccion.FUGA,
                                evento,
                                "dictado original"
                        )
                ));
        when(persistence.viaVisibleAviEnPlaza(4L, 102))
                .thenReturn(true);
        when(persistence.actualizar(
                eq(id),
                eq("I1L110"),
                eq(102),
                eq(AviAccion.DERIVADO),
                eq(evento),
                eq("dictado original")
        )).thenReturn(
                data(
                        id,
                        "I1L110",
                        102,
                        AviAccion.DERIVADO,
                        evento,
                        "dictado original"
                )
        );

        AviRegistroUseCase.Registro result =
                service.actualizar(
                        id,
                        new AviRegistroUseCase.Command(
                                id,
                                "i1l-110",
                                102,
                                AviAccion.DERIVADO,
                                evento,
                                " dictado original "
                        )
                );

        assertEquals("I1L110", result.placa());
        assertEquals(102, result.via());
        assertEquals(
                AviAccion.DERIVADO,
                result.accion()
        );

        verify(persistence).actualizar(
                id,
                "I1L110",
                102,
                AviAccion.DERIVADO,
                evento,
                "dictado original"
        );
    }

    @Test
    void operadorNoPuedeEditarRegistroDeOtroUsuario() {
        AviRegistroPersistencePort persistence =
                mock(AviRegistroPersistencePort.class);
        UsuarioActualUseCase actualUseCase =
                mock(UsuarioActualUseCase.class);

        AviRegistroService service =
                new AviRegistroService(
                        persistence,
                        actualUseCase
                );

        UUID id = UUID.randomUUID();
        OffsetDateTime evento =
                OffsetDateTime.parse(
                        "2026-10-08T20:10:00-05:00"
                );

        when(actualUseCase.requireActual())
                .thenReturn(usuario("OPERADOR"));
        when(persistence.buscarPorId(id))
                .thenReturn(Optional.of(
                        dataConUsuario(
                                id,
                                99L,
                                "I1L111",
                                101,
                                AviAccion.FUGA,
                                evento
                        )
                ));

        assertThrows(
                ForbiddenException.class,
                () -> service.actualizar(
                        id,
                        new AviRegistroUseCase.Command(
                                id,
                                "I1L110",
                                101,
                                AviAccion.FUGA,
                                evento,
                                null
                        )
                )
        );

        verify(persistence, never()).actualizar(
                any(),
                anyString(),
                anyInt(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void editarRechazaViaQueNoPerteneceALaPlaza() {
        AviRegistroPersistencePort persistence =
                mock(AviRegistroPersistencePort.class);
        UsuarioActualUseCase actualUseCase =
                mock(UsuarioActualUseCase.class);

        AviRegistroService service =
                new AviRegistroService(
                        persistence,
                        actualUseCase
                );

        UUID id = UUID.randomUUID();
        OffsetDateTime evento =
                OffsetDateTime.parse(
                        "2026-10-08T20:10:00-05:00"
                );

        when(actualUseCase.requireActual())
                .thenReturn(usuario("OPERADOR"));
        when(persistence.buscarPorId(id))
                .thenReturn(Optional.of(
                        data(
                                id,
                                "I1L111",
                                101,
                                AviAccion.FUGA,
                                evento,
                                null
                        )
                ));
        when(persistence.viaVisibleAviEnPlaza(4L, 999))
                .thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> service.actualizar(
                        id,
                        new AviRegistroUseCase.Command(
                                id,
                                "I1L110",
                                999,
                                AviAccion.FUGA,
                                evento,
                                null
                        )
                )
        );

        verify(persistence, never()).actualizar(
                any(),
                anyString(),
                anyInt(),
                any(),
                any(),
                any()
        );
    }

    private AviRegistroPersistencePort.RegistroData dataConUsuario(
            UUID id,
            Long usuarioId,
            String placa,
            Integer via,
            AviAccion accion,
            OffsetDateTime evento
    ) {
        return new AviRegistroPersistencePort.RegistroData(
                id,
                usuarioId,
                9999,
                "Otro operador",
                4L,
                "P4",
                placa,
                via,
                accion,
                evento,
                OffsetDateTime.parse(
                        "2026-10-08T20:10:02-05:00"
                ),
                null
        );
    }

    private UsuarioActualUseCase.UsuarioActual usuario(
            String rol
    ) {
        return new UsuarioActualUseCase.UsuarioActual(
                10L,
                2396,
                "Usuario AVI",
                rol,
                4L,
                "P4",
                1L,
                "Agente"
        );
    }

    private AviRegistroPersistencePort.RegistroData data(
            UUID id,
            String placa,
            Integer via,
            AviAccion accion,
            OffsetDateTime evento,
            String texto
    ) {
        return new AviRegistroPersistencePort.RegistroData(
                id,
                10L,
                2396,
                "Usuario AVI",
                4L,
                "P4",
                placa,
                via,
                accion,
                evento,
                OffsetDateTime.parse(
                        "2026-10-06T19:00:02-05:00"
                ),
                texto
        );
    }
}
