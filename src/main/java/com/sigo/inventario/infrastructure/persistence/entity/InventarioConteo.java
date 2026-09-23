package com.sigo.inventario.infrastructure.persistence.entity;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="inventario_conteo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioConteo {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="plaza_id",nullable=false) private Plaza plaza;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="responsable_id",nullable=false) private Trabajador responsable;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="rol_id",nullable=false) private InventarioRol rol;
  @Column(name="fecha_inicio",nullable=false) private OffsetDateTime fechaInicio;
  @Column(name="fecha_finalizacion") private OffsetDateTime fechaFinalizacion;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private EstadoInventario estado=EstadoInventario.EN_PROCESO;
  @Column(columnDefinition="text") private String observacion;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="anulado_por") private Trabajador anuladoPor;
  @Column(name="anulado_en") private OffsetDateTime anuladoEn;
  @Column(name="motivo_anulacion",length=500) private String motivoAnulacion;
  @Column(name="creado_en",nullable=false) private OffsetDateTime creadoEn;
  @Column(name="actualizado_en",nullable=false) private OffsetDateTime actualizadoEn;
  @PrePersist void prePersist(){ var n=OffsetDateTime.now(); if(fechaInicio==null)fechaInicio=n; if(creadoEn==null)creadoEn=n; if(actualizadoEn==null)actualizadoEn=n; if(estado==null)estado=EstadoInventario.EN_PROCESO; }
  @PreUpdate void preUpdate(){ actualizadoEn=OffsetDateTime.now(); }
}
