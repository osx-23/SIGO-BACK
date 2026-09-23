package com.sigo.asistencia.inventario.controller;
import com.sigo.asistencia.inventario.dto.response.CatalogoItemResponse;
import com.sigo.asistencia.inventario.service.InventarioCatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/inventario/catalogos") @RequiredArgsConstructor
public class InventarioCatalogoController {
  private final InventarioCatalogoService service;
  @GetMapping("/categorias") public List<CatalogoItemResponse> categorias(){return service.categorias();}
  @GetMapping("/ambitos") public List<CatalogoItemResponse> ambitos(){return service.ambitos();}
  @GetMapping("/roles") public List<CatalogoItemResponse> roles(){return service.roles();}
  @GetMapping("/plazas") public List<CatalogoItemResponse> plazas(){return service.plazas();}
}
