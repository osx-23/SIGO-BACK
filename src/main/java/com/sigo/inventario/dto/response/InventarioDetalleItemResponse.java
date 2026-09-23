package com.sigo.inventario.dto.response;
import java.math.BigDecimal;
public record InventarioDetalleItemResponse(
  Long productoId,String nombre,String unidad,String categoria,String ambito,BigDecimal cantidad){}
