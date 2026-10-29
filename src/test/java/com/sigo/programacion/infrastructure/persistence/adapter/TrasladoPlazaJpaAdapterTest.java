package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.programacion.infrastructure.persistence.entity.TrasladoPlazaEntity;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TrasladoPlazaJpaAdapterTest {
    private Plaza plaza(long id, String codigo) {
        Plaza p = new Plaza();
        p.setId(id);
        p.setCodigo(codigo);
        p.setActivo(true);
        return p;
    }
    private Trabajador trabajador(Plaza origen, String puesto) {
        Trabajador t = new Trabajador();
        t.setId(9L);
        t.setPlaza(origen);
        t.setActivo(true);
        Puesto p = new Puesto();
        p.setNombre(puesto);
        t.setPuesto(p);
        return t;
    }

    @Test
    void trasladoTemporalSoloSeAplicaDentroDelRango() {
        Plaza origen = plaza(4L, "P4");
        Plaza destino = plaza(2L, "P2");
        Trabajador t = trabajador(origen, "Controlador");
        TrasladoPlazaEntity transferencia = new TrasladoPlazaEntity();
        transferencia.setTrabajador(t);
        transferencia.setOrigen(origen);
        transferencia.setDestino(destino);
        transferencia.setFechaInicio(LocalDate.of(2027,1,15));
        transferencia.setFechaFin(LocalDate.of(2027,1,22));

        assertEquals(4L, TrasladoPlazaJpaAdapter.plazaEfectiva(
                t, LocalDate.of(2027,1,14), List.of(transferencia)));
        assertEquals(2L, TrasladoPlazaJpaAdapter.plazaEfectiva(
                t, LocalDate.of(2027,1,15), List.of(transferencia)));
        assertEquals(2L, TrasladoPlazaJpaAdapter.plazaEfectiva(
                t, LocalDate.of(2027,1,22), List.of(transferencia)));
        assertEquals(4L, TrasladoPlazaJpaAdapter.plazaEfectiva(
                t, LocalDate.of(2027,1,23), List.of(transferencia)));
    }

    @Test
    void admiteControladoresYAgentesPeroNoSupervisores() {
        Plaza origen = plaza(4L, "P4");
        assertTrue(TrasladoPlazaJpaAdapter.programable(trabajador(origen, "Controlador ATF")));
        assertTrue(TrasladoPlazaJpaAdapter.programable(trabajador(origen, "Agente de Recaudación")));
        assertFalse(TrasladoPlazaJpaAdapter.programable(trabajador(origen, "Supervisor")));
    }
}
