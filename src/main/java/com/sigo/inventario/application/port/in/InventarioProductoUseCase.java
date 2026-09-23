package com.sigo.inventario.application.port.in;

import java.util.List;
import java.util.Set;

public interface InventarioProductoUseCase {

    List<Producto> listar(Usuario usuario);

    Producto crear(
            Usuario usuario,
            Command command
    );

    Producto actualizar(
            Usuario usuario,
            Long productoId,
            Command command
    );

    Producto cambiarEstado(
            Usuario usuario,
            Long productoId,
            boolean activo
    );

    record Usuario(
            Long trabajadorId,
            Long plazaId,
            String rolCodigo
    ) {
        public boolean esSupervisor() {
            return "SUPERVISOR".equalsIgnoreCase(rolCodigo);
        }

        public boolean esControlador() {
            return "CONTROLADOR".equalsIgnoreCase(rolCodigo);
        }
    }

    record PlazaConfig(
            Long plazaId,
            Integer stockMinimo
    ) {
    }

    record Command(
            String codigo,
            String nombre,
            String descripcion,
            Long categoriaId,
            Long ambitoId,
            String unidadMedida,
            Boolean activo,
            Set<String> roles,
            List<PlazaConfig> plazas
    ) {
    }

    record PlazaProducto(
            Long plazaId,
            String plaza,
            Integer stockMinimo
    ) {
    }

    record Producto(
            Long id,
            String codigo,
            String nombre,
            String descripcion,
            Long categoriaId,
            String categoria,
            Long ambitoId,
            String ambito,
            String unidadMedida,
            Boolean activo,
            Set<String> roles,
            List<PlazaProducto> plazas
    ) {
    }
}
