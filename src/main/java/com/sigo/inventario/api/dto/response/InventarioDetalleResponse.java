package com.sigo.inventario.api.dto.response;
import com.sigo.inventario.domain.InventarioEstado;
import java.time.OffsetDateTime;
import java.util.List;
public record InventarioDetalleResponse(
  Long id,Long plazaId,String plaza,Long responsableId,Integer codigoResponsable,String responsable,
  String rol,OffsetDateTime fechaInicio,OffsetDateTime fechaFinalizacion,InventarioEstado estado,
  String observacion,String motivoAnulacion,List<InventarioDetalleItemResponse> productos){}
