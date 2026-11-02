package com.sigo.security.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuloAccesoPolicyTest {

    private final ModuloAccesoPolicy policy = new ModuloAccesoPolicy();

    @Test
    void controladorTieneAccesoAlModuloIncidencias() {
        assertTrue(policy.modulosPara(RolSeguridad.CONTROLADOR).contains("INCIDENCIAS"));
    }

    @Test
    void supervisorMantieneAccesoAlModuloIncidencias() {
        assertTrue(policy.modulosPara(RolSeguridad.SUPERVISOR).contains("INCIDENCIAS"));
    }
}
