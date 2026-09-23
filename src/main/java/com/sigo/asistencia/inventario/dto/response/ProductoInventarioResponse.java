package com.sigo.asistencia.inventario.dto.response;
public record ProductoInventarioResponse(
  Long id,String codigo,String nombre,String descripcion,String unidadMedida,
  Long categoriaId,String categoria,Long ambitoId,String ambito){}
