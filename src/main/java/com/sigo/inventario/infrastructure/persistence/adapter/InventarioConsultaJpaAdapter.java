package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.out.InventarioConsultaPort;
import com.sigo.inventario.domain.InventarioEstado;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteo;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteoDetalle;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProducto;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoDetalleRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioRolRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InventarioConsultaJpaAdapter
        implements InventarioConsultaPort {

    private final InventarioConteoRepository conteos;
    private final InventarioConteoDetalleRepository detalles;
    private final InventarioProductoRepository productos;
    private final InventarioRolRepository roles;

    @Override
    public Cabecera requireInventario(Long inventarioId) {
        InventarioConteo inventario = conteos
                .findConDetalleById(inventarioId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventario no encontrado"
                        )
                );

        return new Cabecera(
                inventario.getId(),
                inventario.getPlaza().getId(),
                inventario.getPlaza().getCodigo(),
                inventario.getResponsable().getId(),
                inventario.getResponsable().getCodigo(),
                inventario.getResponsable().getNombreCompleto(),
                inventario.getRol().getId(),
                inventario.getRol().getCodigo(),
                inventario.getFechaInicio(),
                inventario.getFechaFinalizacion(),
                InventarioEstado.valueOf(
                        inventario.getEstado().name()
                ),
                inventario.getObservacion(),
                inventario.getMotivoAnulacion()
        );
    }

    @Override
    public List<InventarioConsultaUseCase.Producto> productosPermitidos(
            Long rolId,
            Long plazaId
    ) {
        return productos
                .buscarPermitidos(rolId, plazaId)
                .stream()
                .map(this::toProducto)
                .toList();
    }

    @Override
    public List<InventarioConsultaUseCase.DetalleItem> detalleItems(
            Long inventarioId
    ) {
        return detalles
                .findByInventarioIdOrderByNombreProductoSnapshotAsc(
                        inventarioId
                )
                .stream()
                .map(this::toDetalle)
                .toList();
    }

    @Override
    public Optional<Long> rolActivoId(String codigo) {
        return roles
                .findByCodigoAndActivoTrue(codigo)
                .map(rol -> rol.getId());
    }

    @Override
    public InventarioConsultaUseCase.Pagina<InventarioConsultaUseCase.Resumen> historial(
            FiltroHistorial filtro
    ) {
        Specification<InventarioConteo> spec =
                Specification.where(null);

        if (filtro.plazaId() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("plaza").get("id"),
                                    filtro.plazaId()
                            )
            );
        }

        if (filtro.responsableId() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("responsable").get("id"),
                                    filtro.responsableId()
                            )
            );
        }

        if (filtro.rolId() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("rol").get("id"),
                                    filtro.rolId()
                            )
            );
        }

        if (filtro.estado() != null) {
            var estadoPersistencia =
                    com.sigo.inventario.infrastructure.persistence.entity.EstadoInventario
                            .valueOf(filtro.estado().name());

            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("estado"),
                                    estadoPersistencia
                            )
            );
        }

        if (filtro.fechaDesde() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.greaterThanOrEqualTo(
                                    root.get("fechaInicio"),
                                    filtro.fechaDesde()
                            )
            );
        }

        if (filtro.fechaHasta() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.lessThanOrEqualTo(
                                    root.get("fechaInicio"),
                                    filtro.fechaHasta()
                            )
            );
        }

        PageRequest pageable =
                PageRequest.of(
                        filtro.pagina(),
                        filtro.tamanio(),
                        Sort.by(
                                Sort.Direction.DESC,
                                "fechaInicio"
                        )
                );

        Page<InventarioConteo> page =
                conteos.findAll(spec, pageable);

        List<InventarioConsultaUseCase.Resumen> contenido =
                page.getContent()
                        .stream()
                        .map(this::toResumen)
                        .toList();

        return new InventarioConsultaUseCase.Pagina<>(
                contenido,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private InventarioConsultaUseCase.Producto toProducto(
            InventarioProducto producto
    ) {
        return new InventarioConsultaUseCase.Producto(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getUnidadMedida(),
                producto.getCategoria() == null
                        ? null
                        : producto.getCategoria().getId(),
                producto.getCategoria() == null
                        ? null
                        : producto.getCategoria().getNombre(),
                producto.getAmbito() == null
                        ? null
                        : producto.getAmbito().getId(),
                producto.getAmbito() == null
                        ? null
                        : producto.getAmbito().getNombre()
        );
    }

    private InventarioConsultaUseCase.DetalleItem toDetalle(
            InventarioConteoDetalle detalle
    ) {
        return new InventarioConsultaUseCase.DetalleItem(
                detalle.getProducto().getId(),
                detalle.getNombreProductoSnapshot(),
                detalle.getUnidadSnapshot(),
                detalle.getCategoriaSnapshot(),
                detalle.getAmbitoSnapshot(),
                detalle.getCantidadEncontrada()
        );
    }

    private InventarioConsultaUseCase.Resumen toResumen(
            InventarioConteo inventario
    ) {
        return new InventarioConsultaUseCase.Resumen(
                inventario.getId(),
                inventario.getPlaza().getId(),
                inventario.getPlaza().getCodigo(),
                inventario.getResponsable().getId(),
                inventario.getResponsable().getCodigo(),
                inventario.getResponsable().getNombreCompleto(),
                inventario.getRol().getCodigo(),
                inventario.getFechaInicio(),
                inventario.getFechaFinalizacion(),
                InventarioEstado.valueOf(
                        inventario.getEstado().name()
                ),
                detalles.countByInventarioId(
                        inventario.getId()
                )
        );
    }
}
