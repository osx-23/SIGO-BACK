package com.sigo.relevo.controller;
import com.sigo.relevo.dto.ViaResponse;
import com.sigo.relevo.service.ViaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/vias") @RequiredArgsConstructor
public class ViaController {
 private final ViaService viaService;
 @GetMapping public List<ViaResponse> listarPorPlaza(@RequestParam Long plazaId){ return viaService.listarPorPlaza(plazaId); }
}
