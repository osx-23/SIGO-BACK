package com.sigo.programacion.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecuenciaOrdenPolicyTest {

    private final SecuenciaOrdenPolicy policy =
            new SecuenciaOrdenPolicy();

    @Test
    void aceptaOrdenConsecutivoConTodosLosAgentes() {
        assertDoesNotThrow(() ->
                policy.validar(
                        List.of(10L, 20L, 30L),
                        List.of(
                                new SecuenciaOrdenPolicy.OrdenSolicitado(
                                        20L,
                                        1
                                ),
                                new SecuenciaOrdenPolicy.OrdenSolicitado(
                                        10L,
                                        2
                                ),
                                new SecuenciaOrdenPolicy.OrdenSolicitado(
                                        30L,
                                        3
                                )
                        )
                )
        );
    }

    @Test
    void rechazaAgentesRepetidos() {
        assertThrows(
                SecuenciaOrdenInvalidoException.class,
                () -> policy.validar(
                        List.of(10L, 20L),
                        List.of(
                                new SecuenciaOrdenPolicy.OrdenSolicitado(
                                        10L,
                                        1
                                ),
                                new SecuenciaOrdenPolicy.OrdenSolicitado(
                                        10L,
                                        2
                                )
                        )
                )
        );
    }

    @Test
    void rechazaOrdenNoConsecutivo() {
        assertThrows(
                SecuenciaOrdenInvalidoException.class,
                () -> policy.validar(
                        List.of(10L, 20L),
                        List.of(
                                new SecuenciaOrdenPolicy.OrdenSolicitado(
                                        10L,
                                        1
                                ),
                                new SecuenciaOrdenPolicy.OrdenSolicitado(
                                        20L,
                                        3
                                )
                        )
                )
        );
    }
}
