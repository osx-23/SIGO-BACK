package com.sigo.asistencia.inventario.dto.response;
import java.util.*;
public record ProductoAdminResponse(
  Long id,String codigo,String nombre,String descripcion,Long categoriaId,String categoria,
  Long ambitoId,String ambito,String unidadMedida,Boolean activo,Set<String> roles,
  List<ProductoPlazaConfigResponse> plazas){
  public record ProductoPlazaConfigResponse(Long plazaId,String plaza,Integer stockMinimo){}
}
