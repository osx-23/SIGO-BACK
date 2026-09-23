package com.sigo.inventario.api.controller;
import com.sigo.inventario.api.dto.response.StockActualResponse;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.inventario.application.security.InventarioUsuarioContextService;
import com.sigo.inventario.application.service.InventarioStockService;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController @RequestMapping("/api/inventario/stock") @RequiredArgsConstructor
public class InventarioStockController {
  private final InventarioStockService service; private final InventarioUsuarioContextService usuarios;
  @GetMapping public List<StockActualResponse> consultar(@RequestParam(required=false) Long plazaId,@RequestParam(required=false) String buscar){
    InventarioUsuarioActual actual=usuarios.obtenerActual();
    exigirGestion(actual);
    return service.consultar(actual,plazaId,buscar);
  }
  private void exigirGestion(InventarioUsuarioActual actual){
    RolSistema rol=actual.trabajador().getRolSistema();
    if(rol!=RolSistema.CONTROLADOR && rol!=RolSistema.SUPERVISOR){
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo controladores y supervisores pueden consultar el stock de inventario");
    }
  }
}
