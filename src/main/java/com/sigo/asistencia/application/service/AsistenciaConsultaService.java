package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.ConsultarAsistenciasUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaConsultaPort;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsistenciaConsultaService
        implements ConsultarAsistenciasUseCase {

    private final AsistenciaConsultaPort consultaPort;

    @Override
    @Transactional(readOnly = true)
    public Asistencia obtenerPorId(Long id) {
        return consultaPort.obtenerPorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asistencia> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId
    ) {
        LocalDate hoy = LocalDate.now();

        LocalDate fechaInicio =
                inicio == null ? hoy : inicio;

        LocalDate fechaFin =
                fin == null ? hoy : fin;

        if (fechaInicio.isAfter(fechaFin)) {
            throw new BusinessException(
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        if (plazaId != null
                && !consultaPort.existePlaza(plazaId)) {
            throw new ResourceNotFoundException(
                    "Plaza no encontrada"
            );
        }

        return consultaPort.listar(
                fechaInicio,
                fechaFin,
                plazaId
        );
    }
}
