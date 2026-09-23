package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.out.InventarioConsultaPort;
import com.sigo.inventario.domain.InventarioEstado;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ConflictException;
import com.sigo.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InventarioConsultaService
        implements InventarioConsultaUseCase {

    private static final ZoneId ZONA_LIMA =
            ZoneId.of("America/Lima");

    private final InventarioConsultaPort consultaPort;

    @Override
    @Transactional(readOnly = true)
    public List<Producto> productosPermitidos(
            Usuario usuario,
            Long inventarioId
    ) {
        InventarioConsultaPort.Cabecera inventario =
                consultaPort.requireInventario(inventarioId);

        if (!inventario.responsableId().equals(
                usuario.trabajadorId()
        )) {
            throw new ForbiddenException("El inventario pertenece a otro trabajador"
            );
        }

        if (inventario.estado()
                != InventarioEstado.EN_PROCESO) {
            throw new ConflictException("Inventario cerrado"
            );
        }

        return consultaPort.productosPermitidos(
                inventario.rolId(),
                inventario.plazaId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Detalle detalle(
            Usuario usuario,
            Long inventarioId
    ) {
        InventarioConsultaPort.Cabecera inventario =
                consultaPort.requireInventario(inventarioId);

        exigirPuedeConsultarPlaza(
                usuario,
                inventario.plazaId()
        );

        return new Detalle(
                inventario.id(),
                inventario.plazaId(),
                inventario.plazaCodigo(),
                inventario.responsableId(),
                inventario.responsableCodigo(),
                inventario.responsableNombre(),
                inventario.rolCodigo(),
                inventario.fechaInicio(),
                inventario.fechaFinalizacion(),
                inventario.estado(),
                inventario.observacion(),
                inventario.motivoAnulacion(),
                consultaPort.detalleItems(
                        inventarioId
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Resumen> historial(
            Usuario usuario,
            Long plazaId,
            Long responsableId,
            String rol,
            InventarioEstado estado,
            LocalDate desde,
            LocalDate hasta,
            int page,
            int size
    ) {
        Long plazaFiltro = plazaId;

        if (!usuario.esSupervisor()) {
            exigirPlazaAsignada(usuario);

            if (plazaId != null
                    && !plazaId.equals(usuario.plazaId())) {
                throw new ForbiddenException("No puede consultar inventarios de otra plaza"
                );
            }

            plazaFiltro = usuario.plazaId();
        }

        Long rolId = null;

        if (rol != null && !rol.isBlank()) {
            String codigoRol =
                    rol.trim().toUpperCase(Locale.ROOT);

            rolId = consultaPort
                    .rolActivoId(codigoRol)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "Rol inválido"
                            )
                    );
        }

        if (desde != null
                && hasta != null
                && desde.isAfter(hasta)) {
            throw new BusinessException(
                    "La fecha desde no puede ser posterior a la fecha hasta"
            );
        }

        OffsetDateTime fechaDesde =
                desde == null
                        ? null
                        : desde
                                .atStartOfDay(ZONA_LIMA)
                                .toOffsetDateTime();

        OffsetDateTime fechaHasta =
                hasta == null
                        ? null
                        : hasta
                                .plusDays(1)
                                .atStartOfDay(ZONA_LIMA)
                                .minusNanos(1)
                                .toOffsetDateTime();

        int paginaSegura = Math.max(0, page);
        int tamanioSeguro =
                Math.min(
                        100,
                        Math.max(1, size)
                );

        return consultaPort.historial(
                new InventarioConsultaPort.FiltroHistorial(
                        plazaFiltro,
                        responsableId,
                        rolId,
                        estado,
                        fechaDesde,
                        fechaHasta,
                        paginaSegura,
                        tamanioSeguro
                )
        );
    }

    private void exigirPuedeConsultarPlaza(
            Usuario usuario,
            Long plazaId
    ) {
        if (usuario.esSupervisor()) {
            return;
        }

        exigirPlazaAsignada(usuario);

        if (!usuario.plazaId().equals(plazaId)) {
            throw new ForbiddenException("No puede consultar otra plaza"
            );
        }
    }

    private void exigirPlazaAsignada(Usuario usuario) {
        if (usuario.plazaId() == null) {
            throw new ForbiddenException("Trabajador sin plaza asignada"
            );
        }
    }
}
