package com.sigo.inventario.api.dto.response;
import com.sigo.inventario.infrastructure.persistence.entity.EstadoInventario;
import java.time.OffsetDateTime;
public record InventarioResumenResponse(
  Long id,Long plazaId,String plaza,Long responsableId,Integer codigoResponsable,
  String responsable,String rol,OffsetDateTime fechaInicio,OffsetDateTime fechaFinalizacion,
  EstadoInventario estado,long productosRegistrados){}
