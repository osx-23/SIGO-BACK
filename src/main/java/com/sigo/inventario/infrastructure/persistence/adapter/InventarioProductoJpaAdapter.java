package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.in.InventarioProductoUseCase;
import com.sigo.inventario.application.port.out.InventarioProductoGestionPort;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioAmbito;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioCategoria;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProducto;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProductoPlaza;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProductoRol;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioAmbitoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioCategoriaRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoPlazaRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoRolRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioRolRepository;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ConflictException;
import com.sigo.shared.exception.ForbiddenException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventarioProductoJpaAdapter
        implements InventarioProductoGestionPort {

    private final InventarioProductoRepository productos;
    private final InventarioCategoriaRepository categorias;
    private final InventarioAmbitoRepository ambitos;
    private final InventarioRolRepository roles;
    private final InventarioProductoRolRepository productoRoles;
    private final InventarioProductoPlazaRepository productoPlazas;
    private final PlazaRepository plazas;

    @Override
    public List<InventarioProductoUseCase.Producto> listar(
            Long plazaId
    ) {
        List<InventarioProducto> lista =
                plazaId == null
                        ? productos.listarAdministracion()
                        : productos.listarAdministracionPorPlaza(
                                plazaId
                        );

        if (lista.isEmpty()) {
            return List.of();
        }

        List<Long> ids =
                lista.stream()
                        .map(InventarioProducto::getId)
                        .toList();

        Map<Long, Set<String>> rolesPorProducto =
                productoRoles
                        .findAllByProductoIdsConRol(ids)
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        x -> x.getProducto().getId(),
                                        Collectors.mapping(
                                                x -> x.getRol().getCodigo(),
                                                Collectors.toCollection(
                                                        LinkedHashSet::new
                                                )
                                        )
                                )
                        );

        Map<Long, List<InventarioProductoUseCase.PlazaProducto>>
                plazasPorProducto =
                productoPlazas
                        .findAllByProductoIdsConPlaza(ids)
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        x -> x.getProducto().getId(),
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                items -> items
                                                        .stream()
                                                        .sorted(
                                                                Comparator.comparing(
                                                                        x -> x
                                                                                .getPlaza()
                                                                                .getCodigo()
                                                                )
                                                        )
                                                        .map(this::toPlaza)
                                                        .toList()
                                        )
                                )
                        );

        return lista
                .stream()
                .map(producto ->
                        toProducto(
                                producto,
                                rolesPorProducto.getOrDefault(
                                        producto.getId(),
                                        Set.of()
                                ),
                                plazasPorProducto.getOrDefault(
                                        producto.getId(),
                                        List.of()
                                )
                        )
                )
                .toList();
    }

    @Override
    public InventarioProductoUseCase.Producto crear(
            InventarioProductoUseCase.Usuario usuario,
            InventarioProductoUseCase.Command command
    ) {
        exigirCodigoDisponible(
                null,
                command.codigo()
        );

        InventarioProducto producto =
                new InventarioProducto();

        aplicarDatos(producto, command);

        producto = productos.saveAndFlush(producto);

        guardarConfiguracion(
                usuario,
                producto,
                command
        );

        return toProductoCompleto(producto);
    }

    @Override
    public InventarioProductoUseCase.Producto actualizar(
            InventarioProductoUseCase.Usuario usuario,
            Long productoId,
            InventarioProductoUseCase.Command command
    ) {
        InventarioProducto producto =
                requireProducto(productoId);

        exigirPuedeModificar(
                usuario,
                productoId
        );

        exigirCodigoDisponible(
                productoId,
                command.codigo()
        );

        aplicarDatos(producto, command);
        productos.saveAndFlush(producto);

        productoRoles.deleteByProductoId(productoId);
        productoPlazas.deleteByProductoId(productoId);
        productoRoles.flush();
        productoPlazas.flush();

        guardarConfiguracion(
                usuario,
                producto,
                command
        );

        return toProductoCompleto(producto);
    }

    @Override
    public InventarioProductoUseCase.Producto cambiarEstado(
            InventarioProductoUseCase.Usuario usuario,
            Long productoId,
            boolean activo
    ) {
        InventarioProducto producto =
                requireProducto(productoId);

        exigirPuedeModificar(
                usuario,
                productoId
        );

        producto.setActivo(activo);
        productos.saveAndFlush(producto);

        return toProductoCompleto(producto);
    }

    private void aplicarDatos(
            InventarioProducto producto,
            InventarioProductoUseCase.Command command
    ) {
        InventarioCategoria categoria =
                categorias.findById(command.categoriaId())
                        .filter(item ->
                                Boolean.TRUE.equals(
                                        item.getActivo()
                                )
                        )
                        .orElseThrow(() ->
                                bad("Categoría inválida")
                        );

        InventarioAmbito ambito =
                ambitos.findById(command.ambitoId())
                        .filter(item ->
                                Boolean.TRUE.equals(
                                        item.getActivo()
                                )
                        )
                        .orElseThrow(() ->
                                bad("Ámbito inválido")
                        );

        producto.setCodigo(command.codigo());
        producto.setNombre(command.nombre());
        producto.setDescripcion(command.descripcion());
        producto.setCategoria(categoria);
        producto.setAmbito(ambito);
        producto.setUnidadMedida(
                command.unidadMedida()
        );
        producto.setActivo(command.activo());
    }

    private void guardarConfiguracion(
            InventarioProductoUseCase.Usuario usuario,
            InventarioProducto producto,
            InventarioProductoUseCase.Command command
    ) {
        guardarRoles(
                usuario,
                producto,
                command.roles()
        );

        if (usuario.esControlador()) {
            Long plazaId = usuario.plazaId();

            Plaza plaza = plazas.findById(plazaId)
                    .filter(item ->
                            Boolean.TRUE.equals(
                                    item.getActivo()
                            )
                    )
                    .orElseThrow(() ->
                            bad(
                                    "Plaza del controlador inválida"
                            )
                    );

            int stockMinimo =
                    command.plazas()
                            .stream()
                            .filter(item ->
                                    plazaId.equals(
                                            item.plazaId()
                                    )
                            )
                            .findFirst()
                            .map(item ->
                                    item.stockMinimo() == null
                                            ? 0
                                            : item.stockMinimo()
                            )
                            .orElse(0);

            productoPlazas.save(
                    new InventarioProductoPlaza(
                            producto,
                            plaza,
                            stockMinimo
                    )
            );

            return;
        }

        for (InventarioProductoUseCase.PlazaConfig config
                : command.plazas()) {
            Plaza plaza = plazas
                    .findById(config.plazaId())
                    .filter(item ->
                            Boolean.TRUE.equals(
                                    item.getActivo()
                            )
                    )
                    .orElseThrow(() ->
                            bad(
                                    "Plaza inválida: "
                                            + config.plazaId()
                            )
                    );

            productoPlazas.save(
                    new InventarioProductoPlaza(
                            producto,
                            plaza,
                            config.stockMinimo() == null
                                    ? 0
                                    : config.stockMinimo()
                    )
            );
        }
    }

    private void guardarRoles(
            InventarioProductoUseCase.Usuario usuario,
            InventarioProducto producto,
            Set<String> rolesRequest
    ) {
        for (String codigo : rolesRequest) {
            InventarioRol rol = roles
                    .findByCodigoAndActivoTrue(codigo)
                    .orElseThrow(() ->
                            bad(
                                    "Rol inválido: "
                                            + codigo
                            )
                    );

            productoRoles.save(
                    new InventarioProductoRol(
                            producto,
                            rol
                    )
            );
        }
    }

    private void exigirCodigoDisponible(
            Long productoId,
            String codigo
    ) {
        productos
                .findByCodigoIgnoreCase(codigo)
                .filter(existente ->
                        productoId == null
                                || !existente
                                        .getId()
                                        .equals(productoId)
                )
                .ifPresent(existente -> {
                    throw new ConflictException(
                            "Código de producto duplicado"
                    );
                });
    }

    private void exigirPuedeModificar(
            InventarioProductoUseCase.Usuario usuario,
            Long productoId
    ) {
        if (usuario.esSupervisor()) {
            return;
        }

        boolean pertenece =
                productoPlazas
                        .findByProductoIdAndPlazaId(
                                productoId,
                                usuario.plazaId()
                        )
                        .isPresent();

        if (!pertenece) {
            throw new ForbiddenException(
                    "No puede modificar productos de otra plaza"
            );
        }
    }

    private InventarioProducto requireProducto(Long id) {
        return productos
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado"
                        )
                );
    }

    private InventarioProductoUseCase.Producto toProductoCompleto(
            InventarioProducto producto
    ) {
        Set<String> rolesResponse =
                productoRoles
                        .findByProductoId(producto.getId())
                        .stream()
                        .map(item ->
                                item.getRol().getCodigo()
                        )
                        .collect(
                                Collectors.toCollection(
                                        LinkedHashSet::new
                                )
                        );

        List<InventarioProductoUseCase.PlazaProducto>
                plazasResponse =
                productoPlazas
                        .findByProductoId(producto.getId())
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        item -> item
                                                .getPlaza()
                                                .getCodigo()
                                )
                        )
                        .map(this::toPlaza)
                        .toList();

        return toProducto(
                producto,
                rolesResponse,
                plazasResponse
        );
    }

    private InventarioProductoUseCase.PlazaProducto toPlaza(
            InventarioProductoPlaza item
    ) {
        return new InventarioProductoUseCase.PlazaProducto(
                item.getPlaza().getId(),
                item.getPlaza().getCodigo(),
                item.getStockMinimo()
        );
    }

    private InventarioProductoUseCase.Producto toProducto(
            InventarioProducto producto,
            Set<String> rolesResponse,
            List<InventarioProductoUseCase.PlazaProducto> plazasResponse
    ) {
        return new InventarioProductoUseCase.Producto(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
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
                        : producto.getAmbito().getNombre(),
                producto.getUnidadMedida(),
                producto.getActivo(),
                rolesResponse,
                plazasResponse
        );
    }

    private BusinessException bad(String mensaje) {
        return new BusinessException(mensaje);
    }
}
