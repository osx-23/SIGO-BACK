package com.sigo.inventario.api.controller;

import com.sigo.inventario.api.dto.request.ProductoGuardarRequest;
import com.sigo.inventario.api.dto.response.ProductoAdminResponse;
import com.sigo.inventario.application.security.InventarioUsuarioContextService;
import com.sigo.inventario.application.service.InventarioProductoService;
import com.sigo.inventario.application.service.InventarioProductoUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/productos")
@RequiredArgsConstructor
public class InventarioProductoController {
  private final InventarioProductoService service;
  private final InventarioProductoUpdateService updateService;
  private final InventarioUsuarioContextService usuarios;

  @GetMapping
  public List<ProductoAdminResponse> listar() {
    return service.listar(usuarios.obtenerActual());
  }

  @PostMapping
  public ProductoAdminResponse crear(@Valid @RequestBody ProductoGuardarRequest r) {
    return service.crear(usuarios.obtenerActual(), r);
  }

  @PutMapping("/{id}")
  public ProductoAdminResponse actualizar(
      @PathVariable Long id,
      @Valid @RequestBody ProductoGuardarRequest r
  ) {
    return updateService.actualizar(usuarios.obtenerActual(), id, r);
  }

  @PatchMapping("/{id}/estado")
  public ProductoAdminResponse estado(@PathVariable Long id, @RequestParam boolean activo) {
    return service.cambiarEstado(usuarios.obtenerActual(), id, activo);
  }
}
