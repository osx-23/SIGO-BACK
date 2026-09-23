package com.sigo.inventario.security;
import com.sigo.personal.entity.Trabajador;
import com.sigo.inventario.entity.InventarioRol;
public record InventarioUsuarioActual(Trabajador trabajador,InventarioRol rol){
  public Long trabajadorId(){return trabajador.getId();}
  public Long plazaId(){return trabajador.getPlaza()==null?null:trabajador.getPlaza().getId();}
  public String rolCodigo(){return rol.getCodigo();}
}
