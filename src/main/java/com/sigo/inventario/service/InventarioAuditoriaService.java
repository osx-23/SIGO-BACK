package com.sigo.inventario.service;
import com.sigo.personal.entity.Trabajador;
import com.sigo.inventario.entity.InventarioAuditoria;
import com.sigo.inventario.repository.InventarioAuditoriaRepository;
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
