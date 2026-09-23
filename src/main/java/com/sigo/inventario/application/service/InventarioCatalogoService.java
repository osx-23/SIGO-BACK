package com.sigo.inventario.application.service;
import com.sigo.inventario.api.dto.response.CatalogoItemResponse;
import com.sigo.inventario.infrastructure.persistence.repository.*;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor
public class InventarioCatalogoService {
  private final InventarioCategoriaRepository categorias;
  private final InventarioAmbitoRepository ambitos;
  private final InventarioRolRepository roles;
  private final PlazaRepository plazas;
  @Transactional(readOnly=true) public List<CatalogoItemResponse> categorias(){
    return categorias.findByActivoTrueOrderByNombreAsc().stream().map(x->new CatalogoItemResponse(x.getId(),null,x.getNombre())).toList();
  }
  @Transactional(readOnly=true) public List<CatalogoItemResponse> ambitos(){
    return ambitos.findByActivoTrueOrderByNombreAsc().stream().map(x->new CatalogoItemResponse(x.getId(),x.getCodigo(),x.getNombre())).toList();
  }
  @Transactional(readOnly=true) public List<CatalogoItemResponse> roles(){
    return roles.findByActivoTrueOrderByNombreAsc().stream().map(x->new CatalogoItemResponse(x.getId(),x.getCodigo(),x.getNombre())).toList();
  }
  @Transactional(readOnly=true) public List<CatalogoItemResponse> plazas(){
    return plazas.findByActivoTrueOrderByCodigoAsc().stream().map(x->new CatalogoItemResponse(x.getId(),x.getCodigo(),x.getDescripcion())).toList();
  }
}
