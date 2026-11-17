package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaAusencia;
import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaRegistro;
import com.sigo.asistencia.infrastructure.persistence.entity.MotivoAusencia;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaAusenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaEvidenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.MotivoAusenciaRepository;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaAusenciaExcepcionTest {
    @Mock AsistenciaRepository asistencias;
    @Mock AsistenciaAusenciaRepository ausencias;
    @Mock AsistenciaEvidenciaRepository evidencias;
    @Mock PlazaRepository plazas;
    @Mock TurnoRepository turnos;
    @Mock TrabajadorRepository trabajadores;
    @Mock MotivoAusenciaRepository motivos;

    @InjectMocks AsistenciaGestionJpaAdapter normal;
    @InjectMocks AsistenciaExcepcionJpaAdapter controladorExcepcional;

    private Plaza plazaRegistro;
    private Trabajador agenteOtraPlaza;

    @BeforeEach
    void prepararObjetos() {
        plazaRegistro = new Plaza();
        plazaRegistro.setId(4L);
        plazaRegistro.setCodigo("P4");
        Plaza otra = new Plaza();
        otra.setId(5L);
        otra.setCodigo("P5");
        agenteOtraPlaza = trabajador(202L, 2400, "Agente de Recaudación", otra, RolSistema.OPERADOR);
    }

    private Trabajador trabajador(Long id, int codigo, String puesto, Plaza plaza, RolSistema rol) {
        var t = new Trabajador();
        t.setId(id);
        t.setCodigo(codigo);
        t.setNombreCompleto("Persona " + codigo);
        t.setActivo(true);
        t.setRolSistema(rol);
        t.setPlaza(plaza);
        var p = new Puesto();
        p.setNombre(puesto);
        t.setPuesto(p);
        return t;
    }

    private GestionarAsistenciaUseCase.Command entrada(boolean excepcion) {
        return new GestionarAsistenciaUseCase.Command(
                4L, 1L, 101L, LocalDate.of(2026, 11, 16),
                1, 0, 0, null, null,
                List.of(new GestionarAsistenciaUseCase.AusenciaCommand(
                        202L, 301L, "Ausencia comunicada", excepcion)),
                List.of()
        );
    }

    private void prepararRegistro(boolean conMotivo) {
        var controlador = trabajador(101L, 2396, "Controlador", plazaRegistro,
                RolSistema.CONTROLADOR);
        var turno = new Turno();
        turno.setId(1L);
        turno.setCodigo("A");
        when(plazas.findById(4L)).thenReturn(Optional.of(plazaRegistro));
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        when(trabajadores.findById(101L)).thenReturn(Optional.of(controlador));
        when(trabajadores.findById(202L)).thenReturn(Optional.of(agenteOtraPlaza));
        if (conMotivo) {
            var motivo = new MotivoAusencia();
            motivo.setId(301L);
            motivo.setNombre("Falta");
            when(motivos.findById(301L)).thenReturn(Optional.of(motivo));
            when(asistencias.saveAndFlush(any())).thenAnswer(inv -> {
                AsistenciaRegistro entidad = inv.getArgument(0);
                entidad.setId(501L);
                return entidad;
            });
        }
    }

    @Test
    void sinExcepcionRechazaAgenteDeOtraPlaza() {
        prepararRegistro(false);
        assertThrows(BusinessException.class, () -> normal.registrar(entrada(false)));
        verify(asistencias, never()).saveAndFlush(any());
        verify(ausencias, never()).save(any());
    }

    @Test
    void conExcepcionPermiteAgenteDeOtraPlazaYGuardaMarca() {
        prepararRegistro(true);
        assertEquals(501L, normal.registrar(entrada(true)));
        verify(ausencias).save(argThat(
                a -> Boolean.TRUE.equals(a.getExcepcionPlaza())
                        && a.getTrabajador().getId().equals(202L)));
    }

    @Test
    void excepcionDelControladorSigueRespetandoBanderaDeAusencia() {
        prepararRegistro(false);
        assertThrows(BusinessException.class,
                () -> controladorExcepcional.registrar(entrada(false)));
        verify(ausencias, never()).save(any());
    }

    @Test
    void excepcionNoPermiteAusentarPuestoNoRecaudador() {
        agenteOtraPlaza.getPuesto().setNombre("Controlador");
        prepararRegistro(false);
        assertThrows(BusinessException.class, () -> normal.registrar(entrada(true)));
        verify(ausencias, never()).save(any());
    }
}
