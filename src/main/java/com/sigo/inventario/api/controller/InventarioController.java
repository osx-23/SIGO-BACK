package com.sigo.inventario.api.controller;
import com.sigo.inventario.api.dto.request.*;
import com.sigo.inventario.api.dto.response.*;
import com.sigo.inventario.infrastructure.persistence.entity.EstadoInventario;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.inventario.application.security.InventarioUsuarioContextService;
import com.sigo.inventario.application.service.InventarioService;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/inventarios") @RequiredArgsConstructor
public class InventarioController {
  private final InventarioService service;
  private final InventarioUsuarioContextService usuarios;

  @PostMapping public InventarioResumenResponse iniciar(){
    return service.iniciar(usuarios.obtenerActual());
  }
  @GetMapping("/{id}/productos") public List<ProductoInventarioResponse> productos(@PathVariable Long id){
    return service.productosPermitidos(usuarios.obtenerActual(),id);
  }
  @PutMapping("/{id}/detalle") public InventarioDetalleResponse guardar(@PathVariable Long id,@Valid @RequestBody GuardarConteoRequest r){
    return service.guardarDetalle(usuarios.obtenerActual(),id,r);
  }
  @PostMapping("/{id}/finalizar") public InventarioDetalleResponse finalizar(@PathVariable Long id){
    return service.finalizar(usuarios.obtenerActual(),id);
  }
  @PostMapping("/{id}/anular") public InventarioDetalleResponse anular(@PathVariable Long id,@Valid @RequestBody AnularInventarioRequest r){
    return service.anular(usuarios.obtenerActual(),id,r);
  }
  @GetMapping("/{id}") public InventarioDetalleResponse detalle(@PathVariable Long id){
    return service.detalle(usuarios.obtenerActual(),id);
  }
  @GetMapping public Page<InventarioResumenResponse> historial(
      @RequestParam(required=false) Long plazaId,@RequestParam(required=false) Long responsableId,
      @RequestParam(required=false) String rol,@RequestParam(required=false) EstadoInventario estado,
      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
      @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size){
    InventarioUsuarioActual actual=usuarios.obtenerActual();
    validarConsultaHistorial(actual,plazaId,responsableId,rol,estado,desde,hasta,page,size);
    return service.historial(actual,plazaId,responsableId,rol,estado,desde,hasta,page,size);
  }

  private void validarConsultaHistorial(
      InventarioUsuarioActual actual, Long plazaId, Long responsableId, String rol,
      EstadoInventario estado, LocalDate desde, LocalDate hasta, int page, int size){
    RolSistema rolSistema=actual.trabajador().getRolSistema();
    if(rolSistema==RolSistema.CONTROLADOR || rolSistema==RolSistema.SUPERVISOR){
      return;
    }

    // El formulario de Nuevo inventario necesita recuperar el conteo EN_PROCESO
    // del propio operador. Esto no habilita el historial general para OPERADOR.
    boolean recuperacionPropia=rolSistema==RolSistema.OPERADOR
        && responsableId!=null
        && responsableId.equals(actual.trabajador().getId())
        && estado==EstadoInventario.EN_PROCESO
        && plazaId==null
        && rol==null
        && desde==null
        && hasta==null
        && page==0
        && size==1;

    if(!recuperacionPropia){
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Solo controladores y supervisores pueden consultar el historial de inventario");
    }
  }
}
