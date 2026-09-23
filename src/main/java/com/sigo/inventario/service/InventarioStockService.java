package com.sigo.inventario.service;
import com.sigo.inventario.dto.response.StockActualResponse;
import com.sigo.inventario.repository.InventarioStockRepository;
import com.sigo.inventario.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
@Service @RequiredArgsConstructor
public class InventarioStockService {
  private final InventarioStockRepository repository;
  private final InventarioAuthorizationService auth;
  public List<StockActualResponse> consultar(InventarioUsuarioActual u,Long plazaId,String buscar){
    Long filtro=plazaId;
    if(!"SUPERVISOR".equalsIgnoreCase(u.rolCodigo())){
      auth.exigirPlazaAsignada(u); if(plazaId!=null) auth.exigirPuedeConsultarPlaza(u,plazaId); filtro=u.plazaId();
    }
    return repository.buscar(filtro,buscar);
  }
}
