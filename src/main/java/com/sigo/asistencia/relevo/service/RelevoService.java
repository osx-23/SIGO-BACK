package com.sigo.asistencia.relevo.service;

import com.sigo.asistencia.asistencia.dto.*; import com.sigo.asistencia.relevo.dto.*;
import com.sigo.asistencia.personal.entity.*; import com.sigo.asistencia.asistencia.entity.*; import com.sigo.asistencia.relevo.entity.*;
import com.sigo.asistencia.shared.exception.*;
import com.sigo.asistencia.shared.storage.CloudinaryService;
import com.sigo.asistencia.personal.repository.*; import com.sigo.asistencia.asistencia.repository.*; import com.sigo.asistencia.relevo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class RelevoService {
 private final RelevoRepository relevoRepository;
 private final RelevoChecklistRepository checklistRepository;
 private final RelevoChecklistEvidenciaRepository checklistEvidenciaRepository;
 private final RelevoViaRepository relevoViaRepository;
 private final RelevoViaEvidenciaRepository viaEvidenciaRepository;
 private final ElementoRelevoRepository elementoRepository;
 private final ViaRepository viaRepository;
 private final PlazaRepository plazaRepository;
 private final TurnoRepository turnoRepository;
 private final TrabajadorRepository trabajadorRepository;
 private final CloudinaryService cloudinaryService;

 public List<ElementoRelevoResponse> listarElementos(){
  return elementoRepository.findByActivoTrueOrderByCategoriaAscOrdenAscNombreAsc().stream()
   .map(e->new ElementoRelevoResponse(e.getId(),e.getCodigo(),e.getNombre(),e.getCategoria(),e.getRequiereCantidad(),e.getOrden())).toList();
 }

 @Transactional
 public RelevoResponse registrar(RelevoRequest request){
  Plaza plaza=plazaRepository.findById(request.plazaId()).orElseThrow(()->new ResourceNotFoundException("Plaza no encontrada"));
  Turno turno=turnoRepository.findById(request.turnoId()).orElseThrow(()->new ResourceNotFoundException("Turno no encontrado"));
  Trabajador operador=trabajadorRepository.findById(request.operadorId()).orElseThrow(()->new ResourceNotFoundException("Operador no encontrado"));

  List<ElementoRelevo> activos=elementoRepository.findByActivoTrueOrderByCategoriaAscOrdenAscNombreAsc();
  Map<Long,ElementoRelevo> mapa=activos.stream().collect(Collectors.toMap(ElementoRelevo::getId,Function.identity()));
  validarChecklist(request.checklist(),mapa);
  validarVias(request.vias(),plaza.getId());

  Relevo r=new Relevo();
  r.setPlaza(plaza); r.setTurno(turno); r.setOperador(operador);
  r.setFecha(request.fecha()); r.setHora(request.hora());
  r.setObservaciones(limpiar(request.observaciones())); r.setResumen(limpiar(request.resumen()));
  r=relevoRepository.save(r);

  for(RelevoChecklistRequest x:request.checklist()){
   RelevoChecklist c=new RelevoChecklist();
   c.setRelevo(r); c.setElemento(mapa.get(x.elementoId())); c.setEstado(x.estado());
   c.setDetalle(limpiar(x.detalle())); c.setCantidad(x.cantidad());
   checklistRepository.save(c);
  }

  if(request.vias()!=null){
   for(RelevoViaRequest x:request.vias()){
    Via v=viaRepository.findById(x.viaId()).orElseThrow(()->new ResourceNotFoundException("Vía no encontrada"));
    RelevoVia rv=new RelevoVia();
    rv.setRelevo(r); rv.setVia(v); rv.setEstado(x.estado()); rv.setDetalle(limpiar(x.detalle()));
    relevoViaRepository.save(rv);
   }
  }
  return obtener(r.getId());
 }

 @Transactional
 public RelevoResponse actualizar(Long id,RelevoRequest request){
  Relevo r=relevoRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Relevo no encontrado"));
  Plaza plaza=plazaRepository.findById(request.plazaId()).orElseThrow(()->new ResourceNotFoundException("Plaza no encontrada"));
  Turno turno=turnoRepository.findById(request.turnoId()).orElseThrow(()->new ResourceNotFoundException("Turno no encontrado"));
  Trabajador operador=trabajadorRepository.findById(request.operadorId()).orElseThrow(()->new ResourceNotFoundException("Operador no encontrado"));

  List<ElementoRelevo> activos=elementoRepository.findByActivoTrueOrderByCategoriaAscOrdenAscNombreAsc();
  Map<Long,ElementoRelevo> elementos=activos.stream().collect(Collectors.toMap(ElementoRelevo::getId,Function.identity()));
  validarChecklist(request.checklist(),elementos);
  validarVias(request.vias(),plaza.getId());

  r.setPlaza(plaza); r.setTurno(turno); r.setOperador(operador);
  r.setFecha(request.fecha()); r.setHora(request.hora());
  r.setObservaciones(limpiar(request.observaciones())); r.setResumen(limpiar(request.resumen()));
  relevoRepository.save(r);

  Map<Long,RelevoChecklist> checklistActual=checklistRepository.findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(id).stream()
   .collect(Collectors.toMap(c->c.getElemento().getId(),Function.identity()));
  for(RelevoChecklistRequest x:request.checklist()){
   RelevoChecklist c=checklistActual.get(x.elementoId());
   if(c==null){ c=new RelevoChecklist(); c.setRelevo(r); c.setElemento(elementos.get(x.elementoId())); }
   c.setEstado(x.estado()); c.setDetalle(limpiar(x.detalle())); c.setCantidad(x.cantidad());
   checklistRepository.save(c);
  }

  Map<Long,RelevoVia> viasActual=relevoViaRepository.findByRelevoIdOrderByViaNumeroAsc(id).stream()
   .collect(Collectors.toMap(v->v.getVia().getId(),Function.identity()));
  if(request.vias()!=null){
   for(RelevoViaRequest x:request.vias()){
    RelevoVia rv=viasActual.get(x.viaId());
    if(rv==null){
     Via via=viaRepository.findById(x.viaId()).orElseThrow(()->new ResourceNotFoundException("Vía no encontrada"));
     rv=new RelevoVia(); rv.setRelevo(r); rv.setVia(via);
    }
    rv.setEstado(x.estado()); rv.setDetalle(limpiar(x.detalle()));
    relevoViaRepository.save(rv);
   }
  }
  return obtener(id);
 }

 @Transactional(readOnly=true)
 public RelevoResponse obtener(Long id){
  Relevo r=relevoRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Relevo no encontrado"));
  var checklist=checklistRepository.findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(id).stream().map(this::mapChecklist).toList();
  var vias=relevoViaRepository.findByRelevoIdOrderByViaNumeroAsc(id).stream().map(this::mapVia).toList();
  return mapRelevo(r,checklist,vias);
 }

 @Transactional(readOnly = true)
 public List<RelevoResponse> listar(LocalDate inicio,LocalDate fin) {
  LocalDate desde=inicio!=null?inicio:LocalDate.now().minusDays(1);
  LocalDate hasta=fin!=null?fin:LocalDate.now();
  if(hasta.isBefore(desde)) throw new BusinessException("La fecha fin no puede ser menor que la fecha inicio");
  return relevoRepository.findByFechaBetweenOrderByFechaDescHoraDesc(desde,hasta).stream().map(relevo->{
   Long relevoId=relevo.getId();
   var checklist=checklistRepository.findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(relevoId).stream().map(this::mapChecklist).toList();
   var vias=relevoViaRepository.findByRelevoIdOrderByViaNumeroAsc(relevoId).stream().map(this::mapVia).toList();
   return mapRelevo(relevo,checklist,vias);
  }).toList();
 }

 @Transactional
 public EvidenciaRelevoResponse subirEvidenciaChecklist(Long checklistId,MultipartFile file)throws IOException{
  RelevoChecklist c=checklistRepository.findById(checklistId).orElseThrow(()->new ResourceNotFoundException("Elemento de checklist no encontrado"));
  Map<String,Object> out=cloudinaryService.subirImagen(file,"sigo/relevos/checklist");
  RelevoChecklistEvidencia e=new RelevoChecklistEvidencia();
  e.setChecklist(c); e.setUrlArchivo(String.valueOf(out.get("secure_url"))); e.setPublicId(out.get("public_id")==null?null:String.valueOf(out.get("public_id"))); e.setTipo("foto");
  return mapEvidencia(checklistEvidenciaRepository.save(e));
 }

 @Transactional
 public void eliminarEvidenciaChecklist(Long checklistId,Long evidenciaId)throws IOException{
  RelevoChecklistEvidencia e=checklistEvidenciaRepository.findById(evidenciaId).orElseThrow(()->new ResourceNotFoundException("Evidencia no encontrada"));
  if(!Objects.equals(e.getChecklist().getId(),checklistId)) throw new BusinessException("La evidencia no pertenece al elemento indicado");
  if(e.getPublicId()!=null&&!e.getPublicId().isBlank()) cloudinaryService.eliminarImagen(e.getPublicId());
  checklistEvidenciaRepository.delete(e);
 }

 @Transactional
 public EvidenciaRelevoResponse subirEvidenciaVia(Long relevoViaId,MultipartFile file)throws IOException{
  RelevoVia rv=relevoViaRepository.findById(relevoViaId).orElseThrow(()->new ResourceNotFoundException("Reporte de vía no encontrado"));
  Map<String,Object> out=cloudinaryService.subirImagen(file,"sigo/relevos/vias");
  RelevoViaEvidencia e=new RelevoViaEvidencia();
  e.setRelevoVia(rv); e.setUrlArchivo(String.valueOf(out.get("secure_url"))); e.setPublicId(out.get("public_id")==null?null:String.valueOf(out.get("public_id"))); e.setTipo("foto");
  return mapEvidencia(viaEvidenciaRepository.save(e));
 }

 @Transactional
 public void eliminarEvidenciaVia(Long relevoViaId,Long evidenciaId)throws IOException{
  RelevoViaEvidencia e=viaEvidenciaRepository.findById(evidenciaId).orElseThrow(()->new ResourceNotFoundException("Evidencia no encontrada"));
  if(!Objects.equals(e.getRelevoVia().getId(),relevoViaId)) throw new BusinessException("La evidencia no pertenece a la vía indicada");
  if(e.getPublicId()!=null&&!e.getPublicId().isBlank()) cloudinaryService.eliminarImagen(e.getPublicId());
  viaEvidenciaRepository.delete(e);
 }

 private void validarChecklist(List<RelevoChecklistRequest> lista,Map<Long,ElementoRelevo> activos){
  Set<Long> ids=new HashSet<>();
  for(var x:lista){
   if(!ids.add(x.elementoId())) throw new BusinessException("No puedes registrar dos veces el mismo elemento");
   ElementoRelevo e=activos.get(x.elementoId());
   if(e==null) throw new BusinessException("El elemento "+x.elementoId()+" no existe o está inactivo");
   validarDetalle(x.estado(),x.detalle(),e.getNombre());
   if(Boolean.TRUE.equals(e.getRequiereCantidad()) && x.cantidad()==null) throw new BusinessException("Debes indicar la cantidad para "+e.getNombre());
  }
  if(ids.size()!=activos.size() || !ids.containsAll(activos.keySet())) throw new BusinessException("Debes registrar todos los elementos activos del checklist");
 }

 private void validarVias(List<RelevoViaRequest> lista,Long plazaId){
  if(lista==null||lista.isEmpty()) return;
  Set<Long> ids=new HashSet<>();
  for(var x:lista){
   if(!ids.add(x.viaId())) throw new BusinessException("No puedes registrar la misma vía dos veces");
   Via v=viaRepository.findById(x.viaId()).orElseThrow(()->new ResourceNotFoundException("Vía no encontrada: "+x.viaId()));
   if(!Boolean.TRUE.equals(v.getActiva())) throw new BusinessException("La vía "+v.getNumero()+" está inactiva");
   if(!Objects.equals(v.getPlaza().getId(),plazaId)) throw new BusinessException("La vía "+v.getNumero()+" no pertenece a la plaza seleccionada");
   validarDetalle(x.estado(),x.detalle(),"Vía "+v.getNumero());
  }
 }

 private void validarDetalle(EstadoOperativo estado,String detalle,String nombre){
  if((estado==EstadoOperativo.OBSERVADO||estado==EstadoOperativo.NO_OPERATIVO) && (detalle==null||detalle.isBlank()))
   throw new BusinessException("Debes indicar un detalle para "+nombre+" cuando el estado es "+estado);
 }
 private String limpiar(String s){ if(s==null)return null; s=s.trim(); return s.isEmpty()?null:s; }
 private RelevoChecklistResponse mapChecklist(RelevoChecklist c){
  var ev=checklistEvidenciaRepository.findByChecklistIdOrderByIdAsc(c.getId()).stream().map(this::mapEvidencia).toList();
  return new RelevoChecklistResponse(c.getId(),c.getElemento().getId(),c.getElemento().getCodigo(),c.getElemento().getNombre(),c.getElemento().getCategoria(),c.getEstado(),c.getDetalle(),c.getCantidad(),ev);
 }
 private RelevoViaResponse mapVia(RelevoVia v){
  var ev=viaEvidenciaRepository.findByRelevoViaIdOrderByIdAsc(v.getId()).stream().map(this::mapEvidencia).toList();
  return new RelevoViaResponse(v.getId(),v.getVia().getId(),v.getVia().getNumero(),v.getVia().getNombre(),v.getEstado(),v.getDetalle(),ev);
 }
 private EvidenciaRelevoResponse mapEvidencia(RelevoChecklistEvidencia e){ return new EvidenciaRelevoResponse(e.getId(),e.getUrlArchivo(),e.getPublicId(),e.getTipo(),e.getCreatedAt()); }
 private EvidenciaRelevoResponse mapEvidencia(RelevoViaEvidencia e){ return new EvidenciaRelevoResponse(e.getId(),e.getUrlArchivo(),e.getPublicId(),e.getTipo(),e.getCreatedAt()); }
 private RelevoResponse mapRelevo(Relevo r,List<RelevoChecklistResponse> checklist,List<RelevoViaResponse> vias){
  return new RelevoResponse(r.getId(),r.getPlaza().getId(),r.getPlaza().getCodigo(),r.getPlaza().getDescripcion(),
   r.getTurno().getId(),r.getTurno().getCodigo(),r.getTurno().getNombre(),
   r.getOperador().getId(),r.getOperador().getCodigo(),r.getOperador().getNombreCompleto(),
   r.getFecha(),r.getHora(),r.getObservaciones(),r.getResumen(),r.getCreatedAt(),r.getUpdatedAt(),checklist,vias);
 }
}
