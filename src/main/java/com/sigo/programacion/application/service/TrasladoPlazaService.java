package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.application.port.out.TrasladoPlazaPort;
import com.sigo.programacion.domain.ProgramacionValidationException;
import com.sigo.programacion.domain.TrasladoPlaza;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrasladoPlazaService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final TrasladoPlazaPort port;
    private final ProgramacionAccessPort access;

    @Transactional(readOnly = true)
    public List<TrasladoPlazaPort.Candidato> candidatos() {
        access.requireSupervisorId();
        return port.candidatos();
    }

    @Transactional(readOnly = true)
    public List<TrasladoPlaza> listar(Long plazaId, Integer anio, Integer mes) {
        access.requireSupervisorId();
        if (plazaId == null) return port.listar(null, null, null);
        if (anio == null || mes == null) return port.listar(plazaId, null, null);
        YearMonth periodo;
        try { periodo = YearMonth.of(anio, mes); }
        catch (RuntimeException e) { throw new ProgramacionValidationException("Periodo inválido"); }
        return port.listar(plazaId, periodo.atDay(1), periodo.atEndOfMonth());
    }

    @Transactional
    public TrasladoPlaza crear(Long trabajadorId, Long destinoId, LocalDate inicio, LocalDate fin) {
        Long supervisor = access.requireSupervisorId();
        if (trabajadorId == null || destinoId == null || inicio == null) {
            throw new ProgramacionValidationException("Trabajador, plaza destino y fecha de inicio son obligatorios");
        }
        if (inicio.isBefore(LocalDate.now(LIMA))) {
            throw new ProgramacionValidationException("No se permiten traslados con fecha de inicio anterior a hoy");
        }
        if (fin != null && fin.isBefore(inicio)) {
            throw new ProgramacionValidationException("La fecha de retorno debe ser posterior o igual al inicio");
        }
        return port.crear(trabajadorId, destinoId, inicio, fin, supervisor);
    }

    @Transactional
    public void cancelar(Long id) {
        access.requireSupervisorId();
        if (id == null) throw new ProgramacionValidationException("Traslado inválido");
        port.cancelar(id);
    }
}
