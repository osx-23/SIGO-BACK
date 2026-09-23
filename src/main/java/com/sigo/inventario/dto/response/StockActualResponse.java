package com.sigo.inventario.dto.response;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record StockActualResponse(
  Long plazaId,String plaza,Long productoId,String producto,String unidadMedida,
  BigDecimal cantidadActual,Integer stockMinimo,boolean bajoMinimo,
  Long inventarioId,OffsetDateTime actualizadoEn){}
