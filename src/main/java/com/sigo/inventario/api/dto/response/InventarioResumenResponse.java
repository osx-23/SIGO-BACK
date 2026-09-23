package com.sigo.inventario.api.dto.response;
import com.sigo.inventario.domain.InventarioEstado;
import java.time.OffsetDateTime;
public record InventarioResumenResponse(
  Long id,Long plazaId,String plaza,Long responsableId,Integer codigoResponsable,
  String responsable,String rol,OffsetDateTime fechaInicio,OffsetDateTime fechaFinalizacion,
  InventarioEstado estado,long productosRegistrados){}
