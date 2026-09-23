package com.sigo.inventario.application.port.in;

import com.sigo.inventario.domain.InventarioEstado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface InventarioConsultaUseCase {

    List<Producto> productosPermitidos(
            Usuario usuario,
            Long inventarioId
    );

    Detalle detalle(
            Usuario usuario,
            Long inventarioId
    );

    Pagina<Resumen> historial(
            Usuario usuario,
            Long plazaId,
            Long responsableId,
            String rol,
            InventarioEstado estado,
            LocalDate desde,
            LocalDate hasta,
            int page,
            int size
    );

    record Usuario(
            Long trabajadorId,
            Long plazaId,
            String rolCodigo
    ) {
        public boolean esSupervisor() {
            return "SUPERVISOR".equalsIgnoreCase(rolCodigo);
        }
    }

    record Producto(
            Long id,
            String codigo,
            String nombre,
            String descripcion,
            String unidadMedida,
            Long categoriaId,
            String categoria,
            Long ambitoId,
            String ambito
    ) {
    }

    record DetalleItem(
            Long productoId,
            String nombre,
            String unidad,
            String categoria,
            String ambito,
            BigDecimal cantidad
    ) {
    }

    record Detalle(
            Long id,
            Long plazaId,
            String plaza,
            Long responsableId,
            Integer codigoResponsable,
            String responsable,
            String rol,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFinalizacion,
            InventarioEstado estado,
            String observacion,
            String motivoAnulacion,
            List<DetalleItem> productos
    ) {
    }

    record Resumen(
            Long id,
            Long plazaId,
            String plaza,
            Long responsableId,
            Integer codigoResponsable,
            String responsable,
            String rol,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFinalizacion,
            InventarioEstado estado,
            long productosRegistrados
    ) {
    }

    record Pagina<T>(
            List<T> contenido,
            int pagina,
            int tamanio,
            long totalElementos,
            int totalPaginas
    ) {
    }
}
