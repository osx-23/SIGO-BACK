package com.sigo.inventario.api.controller;

import com.sigo.inventario.api.dto.response.CatalogoItemResponse;
import com.sigo.inventario.application.port.in.InventarioCatalogoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/catalogos")
@RequiredArgsConstructor
public class InventarioCatalogoController {

    private final InventarioCatalogoUseCase useCase;

    @GetMapping("/categorias")
    public List<CatalogoItemResponse> categorias() {
        return map(useCase.categorias());
    }

    @GetMapping("/ambitos")
    public List<CatalogoItemResponse> ambitos() {
        return map(useCase.ambitos());
    }

    @GetMapping("/roles")
    public List<CatalogoItemResponse> roles() {
        return map(useCase.roles());
    }

    @GetMapping("/plazas")
    public List<CatalogoItemResponse> plazas() {
        return map(useCase.plazas());
    }

    private List<CatalogoItemResponse> map(
            List<InventarioCatalogoUseCase.Item> items
    ) {
        return items
                .stream()
                .map(item ->
                        new CatalogoItemResponse(
                                item.id(),
                                item.codigo(),
                                item.nombre()
                        )
                )
                .toList();
    }
}
