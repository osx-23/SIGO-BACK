package com.sigo.inventario.dto.response;
import com.sigo.inventario.entity.EstadoInventario;
import java.time.OffsetDateTime;
import java.util.List;
public record InventarioDetalleResponse(
  Long id,Long plazaId,String plaza,Long responsableId,Integer codigoResponsable,String responsable,
  String rol,OffsetDateTime fechaInicio,OffsetDateTime fechaFinalizacion,EstadoInventario estado,
  String observacion,String motivoAnulacion,List<InventarioDetalleItemResponse> productos){}
