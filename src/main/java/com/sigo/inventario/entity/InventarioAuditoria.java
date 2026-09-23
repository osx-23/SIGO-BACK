package com.sigo.inventario.entity;
import com.sigo.personal.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
@Entity @Table(name="inventario_auditoria")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioAuditoria {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="usuario_id") private Trabajador usuario;
  @Column(nullable=false,length=80) private String accion;
  @Column(nullable=false,length=80) private String entidad;
  @Column(name="entidad_id") private Long entidadId;
  @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private Map<String,Object> detalle;
  @Column(nullable=false) private OffsetDateTime fecha;
  @PrePersist void prePersist(){ if(fecha==null) fecha=OffsetDateTime.now(); }
}
