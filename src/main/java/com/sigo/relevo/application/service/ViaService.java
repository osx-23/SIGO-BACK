package com.sigo.relevo.application.service;
import com.sigo.relevo.api.dto.ViaResponse;
import com.sigo.shared.exception.ResourceNotFoundException;
import com.sigo.personal.infrastructure.persistence.repository.*; import com.sigo.asistencia.infrastructure.persistence.repository.*; import com.sigo.relevo.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
@Service @RequiredArgsConstructor
public class ViaService {
 private final ViaRepository viaRepository;
 private final PlazaRepository plazaRepository;
 public List<ViaResponse> listarPorPlaza(Long plazaId){
  if(!plazaRepository.existsById(plazaId)) throw new ResourceNotFoundException("Plaza no encontrada");
  return viaRepository.findByPlazaIdAndActivaTrueOrderByOrdenAscNumeroAsc(plazaId).stream()
   .map(v->new ViaResponse(v.getId(),v.getPlaza().getId(),v.getNumero(),v.getNombre(),v.getActiva(),v.getOrden())).toList();
 }
}
