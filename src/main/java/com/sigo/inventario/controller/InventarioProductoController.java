package com.sigo.inventario.controller;

import com.sigo.inventario.dto.request.ProductoGuardarRequest;
import com.sigo.inventario.dto.response.ProductoAdminResponse;
import com.sigo.inventario.security.InventarioUsuarioContextService;
import com.sigo.inventario.service.InventarioProductoService;
import com.sigo.inventario.service.InventarioProductoUpdateService;
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
