package com.sigo.inventario.application.security;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
public record InventarioUsuarioActual(Trabajador trabajador,InventarioRol rol){
  public Long trabajadorId(){return trabajador.getId();}
  public Long plazaId(){return trabajador.getPlaza()==null?null:trabajador.getPlaza().getId();}
  public String rolCodigo(){return rol.getCodigo();}
}
