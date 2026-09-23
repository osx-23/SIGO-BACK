package com.sigo.asistencia.inventario.dto.response;
import com.sigo.asistencia.inventario.entity.EstadoInventario;
import java.time.OffsetDateTime;
public record InventarioResumenResponse(
  Long id,Long plazaId,String plaza,Long responsableId,Integer codigoResponsable,
  String responsable,String rol,OffsetDateTime fechaInicio,OffsetDateTime fechaFinalizacion,
  EstadoInventario estado,long productosRegistrados){}
