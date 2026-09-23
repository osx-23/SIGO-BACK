package com.sigo.inventario.application.service;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioAuditoria;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service @RequiredArgsConstructor
public class InventarioAuditoriaService {
  private final InventarioAuditoriaRepository repository;
  public void registrar(Trabajador u,String accion,String entidad,Long entidadId,Map<String,Object> detalle){
    InventarioAuditoria a=new InventarioAuditoria();
    a.setUsuario(u); a.setAccion(accion); a.setEntidad(entidad); a.setEntidadId(entidadId); a.setDetalle(detalle);
    repository.save(a);
  }
}
