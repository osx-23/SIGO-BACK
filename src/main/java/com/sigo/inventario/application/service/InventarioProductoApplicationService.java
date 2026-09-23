package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.InventarioProductoUseCase;
import com.sigo.inventario.application.port.out.InventarioProductoGestionPort;
import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventarioProductoApplicationService
        implements InventarioProductoUseCase {

    private final InventarioProductoGestionPort gestionPort;
    private final InventarioAuditoriaPort auditoria;

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listar(Usuario usuario) {
        exigirAdministrador(usuario);

        return gestionPort.listar(
                usuario.esSupervisor()
                        ? null
                        : usuario.plazaId()
        );
    }

    @Override
    @Transactional
    public Producto crear(
            Usuario usuario,
            Command command
    ) {
        exigirAdministrador(usuario);
        Command validado = validar(usuario, command);

        Producto producto =
                gestionPort.crear(
                        usuario,
                        validado
                );

        auditoria.registrar(
                usuario.trabajadorId(),
                "PRODUCTO_CREADO",
                "INVENTARIO_PRODUCTO",
                producto.id(),
                java.util.Map.of(
                        "codigo",
                        producto.codigo(),
                        "nombre",
                        producto.nombre()
                )
        );

        return producto;
    }

    @Override
    @Transactional
    public Producto actualizar(
            Usuario usuario,
            Long productoId,
            Command command
    ) {
        exigirAdministrador(usuario);
        Command validado = validar(usuario, command);

        Producto producto =
                gestionPort.actualizar(
                        usuario,
                        productoId,
                        validado
                );

        auditoria.registrar(
                usuario.trabajadorId(),
                "PRODUCTO_ACTUALIZADO",
                "INVENTARIO_PRODUCTO",
                producto.id(),
                java.util.Map.of(
                        "codigo",
                        producto.codigo()
                )
        );

        return producto;
    }

    @Override
    @Transactional
    public Producto cambiarEstado(
            Usuario usuario,
            Long productoId,
            boolean activo
    ) {
        exigirAdministrador(usuario);

        Producto producto =
                gestionPort.cambiarEstado(
                        usuario,
                        productoId,
                        activo
                );

        auditoria.registrar(
                usuario.trabajadorId(),
                activo
                        ? "PRODUCTO_ACTIVADO"
                        : "PRODUCTO_DESACTIVADO",
                "INVENTARIO_PRODUCTO",
                productoId,
                java.util.Map.of(
                        "activo",
                        activo
                )
        );

        return producto;
    }

    private Command validar(
            Usuario usuario,
            Command command
    ) {
        if (command == null) {
            throw bad("Los datos del producto son obligatorios");
        }

        String codigo = limpiar(command.codigo());
        String nombre = limpiar(command.nombre());
        String unidad = limpiar(command.unidadMedida());

        if (codigo == null) {
            throw bad("Código obligatorio");
        }

        if (nombre == null) {
            throw bad("Nombre obligatorio");
        }

        if (unidad == null) {
            throw bad("Unidad de medida obligatoria");
        }

        if (command.categoriaId() == null) {
            throw bad("Categoría obligatoria");
        }

        if (command.ambitoId() == null) {
            throw bad("Ámbito obligatorio");
        }

        if (command.roles() == null
                || command.roles().isEmpty()) {
            throw bad("Debe seleccionar al menos un rol");
        }

        if (command.plazas() == null
                || command.plazas().isEmpty()) {
            throw bad("Configuración de plazas obligatoria");
        }

        Set<String> roles = new LinkedHashSet<>();

        for (String rol : command.roles()) {
            String normalizado = limpiar(rol);

            if (normalizado == null) {
                throw bad("Rol inválido");
            }

            normalizado =
                    normalizado.toUpperCase(Locale.ROOT);

            if (usuario.esControlador()
                    && "SUPERVISOR".equals(normalizado)) {
                throw new ForbiddenException("El controlador no puede asignar el rol SUPERVISOR"
                );
            }

            roles.add(normalizado);
        }

        Set<Long> plazas = new LinkedHashSet<>();

        for (PlazaConfig plaza : command.plazas()) {
            if (plaza == null || plaza.plazaId() == null) {
                throw bad("Plaza inválida");
            }

            if (!plazas.add(plaza.plazaId())) {
                throw bad("Plaza repetida");
            }

            if (plaza.stockMinimo() != null
                    && plaza.stockMinimo() < 0) {
                throw bad("El stock mínimo no puede ser negativo");
            }
        }

        if (usuario.esControlador()
                && usuario.plazaId() == null) {
            throw new ForbiddenException("Trabajador sin plaza asignada"
            );
        }

        return new Command(
                codigo.toUpperCase(Locale.ROOT),
                nombre,
                limpiar(command.descripcion()),
                command.categoriaId(),
                command.ambitoId(),
                unidad,
                command.activo() == null
                        ? Boolean.TRUE
                        : command.activo(),
                roles,
                command.plazas()
        );
    }

    private void exigirAdministrador(Usuario usuario) {
        if (usuario == null
                || (!usuario.esSupervisor()
                && !usuario.esControlador())) {
            throw new ForbiddenException("No tiene permisos para administrar productos"
            );
        }

        if (usuario.esControlador()
                && usuario.plazaId() == null) {
            throw new ForbiddenException("Trabajador sin plaza asignada"
            );
        }
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private BusinessException bad(String mensaje) {
        return new BusinessException(mensaje);
    }
}
